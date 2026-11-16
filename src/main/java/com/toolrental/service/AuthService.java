package com.toolrental.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toolrental.dto.AuthDto.AuthResponse;
import com.toolrental.dto.AuthDto.ChangePasswordRequest;
import com.toolrental.dto.AuthDto.CodeResponse;
import com.toolrental.dto.AuthDto.ForgotPasswordRequest;
import com.toolrental.dto.AuthDto.LoginRequest;
import com.toolrental.dto.AuthDto.OtpSendRequest;
import com.toolrental.dto.AuthDto.OtpVerifyRequest;
import com.toolrental.dto.AuthDto.PhoneLoginRequest;
import com.toolrental.dto.AuthDto.RefreshRequest;
import com.toolrental.dto.AuthDto.RegisterRequest;
import com.toolrental.dto.AuthDto.ResetPasswordRequest;
import com.toolrental.dto.AuthDto.SocialRegisterRequest;
import com.toolrental.dto.CommonDto.MessageResponse;
import com.toolrental.entity.OtpCode;
import com.toolrental.entity.PasswordResetToken;
import com.toolrental.entity.RefreshToken;
import com.toolrental.entity.Role;
import com.toolrental.entity.User;
import com.toolrental.exception.ApiException;
import com.toolrental.repository.OtpCodeRepository;
import com.toolrental.repository.PasswordResetTokenRepository;
import com.toolrental.repository.RefreshTokenRepository;
import com.toolrental.repository.UserRepository;
import com.toolrental.security.JwtService;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Set<String> SOCIAL_PROVIDERS = Set.of("google", "vk", "apple");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshDays;
    private final boolean devMode;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       OtpCodeRepository otpCodeRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.jwt.refresh-days}") long refreshDays,
                       @Value("${app.dev-mode}") boolean devMode) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshDays = refreshDays;
        this.devMode = devMode;
    }

    // ---------- Регистрация ----------

    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        String phone = normalize(request.phone());
        if (email == null && phone == null) {
            throw ApiException.badRequest("Укажите email или телефон");
        }
        if (request.role() == Role.ADMIN) {
            throw ApiException.badRequest("Нельзя зарегистрироваться как администратор");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Пользователь с таким email уже существует");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw ApiException.conflict("Пользователь с таким телефоном уже существует");
        }

        User user = new User();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(request.role() == null ? Role.RENTER : request.role());
        userRepository.save(user);
        return issueTokens(user);
    }

    /**
     * Вход через соцсеть. Настоящую проверку токена у Google / VK / Apple здесь не делаем:
     * считаем, что socialId уже подтверждён провайдером. Если пользователя нет, он создаётся.
     */
    public AuthResponse registerSocial(String provider, SocialRegisterRequest request) {
        provider = provider.toLowerCase();
        if (!SOCIAL_PROVIDERS.contains(provider)) {
            throw ApiException.badRequest("Неизвестный провайдер. Доступны: google, vk, apple");
        }

        User user = userRepository.findBySocialProviderAndSocialId(provider, request.socialId()).orElse(null);
        if (user == null) {
            String email = normalize(request.email());
            if (email != null && userRepository.existsByEmail(email)) {
                throw ApiException.conflict("Пользователь с таким email уже существует");
            }
            user = new User();
            user.setEmail(email);
            user.setFullName(request.fullName());
            user.setRole(Role.RENTER);
            user.setSocialProvider(provider);
            user.setSocialId(request.socialId());
            // Пароль не нужен, но поле обязательное, поэтому кладём случайный
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            userRepository.save(user);
        }
        return issueTokens(user);
    }

    // ---------- Вход ----------

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> ApiException.unauthorized("Неверный email или пароль"));
        checkPassword(user, request.password(), "Неверный email или пароль");
        return issueTokens(user);
    }

    public AuthResponse loginByPhone(PhoneLoginRequest request) {
        User user = userRepository.findByPhone(normalize(request.phone()))
                .orElseThrow(() -> ApiException.unauthorized("Неверный телефон или пароль"));
        checkPassword(user, request.password(), "Неверный телефон или пароль");
        return issueTokens(user);
    }

    // ---------- OTP-код (вход по коду) ----------

    public CodeResponse sendOtp(OtpSendRequest request) {
        String phone = normalize(request.phone());
        String code = String.format("%06d", random.nextInt(1_000_000));

        otpCodeRepository.deleteByPhone(phone);
        otpCodeRepository.save(new OtpCode(phone, code, LocalDateTime.now().plusMinutes(5)));

        // Вместо отправки SMS пишем код в лог
        log.info("SMS-заглушка: код {} для номера {}", code, phone);
        return new CodeResponse("Код отправлен", devMode ? code : null);
    }

    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        String phone = normalize(request.phone());
        OtpCode otp = otpCodeRepository.findFirstByPhoneOrderByIdDesc(phone)
                .orElseThrow(() -> ApiException.badRequest("Код для этого номера не запрашивался"));
        if (otp.getExpiresAt().isBefore(LocalDateTime.now()) || !otp.getCode().equals(request.code())) {
            throw ApiException.badRequest("Неверный или просроченный код");
        }
        otpCodeRepository.deleteByPhone(phone);

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> ApiException.notFound("Пользователь с таким телефоном не зарегистрирован"));
        return issueTokens(user);
    }

    // ---------- Токены ----------

    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> ApiException.unauthorized("Неверный refresh-токен"));
        // Старый токен удаляем и выдаём новую пару
        refreshTokenRepository.deleteByToken(token.getToken());
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.unauthorized("Refresh-токен просрочен, войдите заново");
        }
        return issueTokens(token.getUser());
    }

    public MessageResponse logout(RefreshRequest request) {
        refreshTokenRepository.deleteByToken(request.refreshToken());
        return new MessageResponse("Вы вышли из аккаунта");
    }

    public MessageResponse logoutAll(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        return new MessageResponse("Вы вышли со всех устройств");
    }

    // Создаёт пару токенов: access (JWT, живёт недолго) и refresh (случайная строка, хранится в БД)
    public AuthResponse issueTokens(User user) {
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();
        refreshTokenRepository.save(new RefreshToken(refreshToken, user, LocalDateTime.now().plusDays(refreshDays)));
        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getRole());
    }

    // ---------- Пароль ----------

    public CodeResponse forgotPassword(ForgotPasswordRequest request) {
        String message = "Если такой email зарегистрирован, мы отправили на него инструкцию по сбросу пароля";
        User user = userRepository.findByEmail(normalize(request.email())).orElse(null);
        if (user == null) {
            // Не сообщаем, что такого email нет в системе
            return new CodeResponse(message, null);
        }

        String token = UUID.randomUUID().toString();
        resetTokenRepository.save(new PasswordResetToken(token, user, LocalDateTime.now().plusMinutes(30)));

        // Вместо отправки письма пишем токен в лог
        log.info("Email-заглушка: токен сброса пароля для {}: {}", user.getEmail(), token);
        return new CodeResponse(message, devMode ? token : null);
    }

    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> ApiException.badRequest("Неверный или использованный токен"));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("Токен просрочен, запросите восстановление заново");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        resetTokenRepository.delete(token);
        refreshTokenRepository.deleteByUserId(user.getId());
        return new MessageResponse("Пароль изменён, войдите с новым паролем");
    }

    public MessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.getOrThrow(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Старый пароль указан неверно");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return new MessageResponse("Пароль изменён");
    }

    // ---------- Вспомогательные методы ----------

    private void checkPassword(User user, String rawPassword, String errorMessage) {
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw ApiException.unauthorized(errorMessage);
        }
    }

    // Убирает пробелы и приводит к нижнему регистру. Пустая строка превращается в null.
    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
