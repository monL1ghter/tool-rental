package com.toolrental.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
import com.toolrental.service.AuthService;

import jakarta.validation.Valid;

// Контроллер принимает HTTP-запросы и передаёт их в сервис, сам логики не содержит.
// Здесь регистрация, вход, обновление токена и работа с паролем. Адрес /auth/** открыт без токена,
// кроме /auth/logout/all и /auth/password/change (см. SecurityConfig).
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Регистрация по email и/или телефону. @Valid проверяет поля запроса по правилам из DTO.
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // Вход и регистрация через соцсеть (google, vk, apple). Это заглушка: токен соцсети не проверяется.
    @PostMapping("/register/social/{provider}")
    public AuthResponse registerSocial(@PathVariable String provider,
                                       @Valid @RequestBody SocialRegisterRequest request) {
        return authService.registerSocial(provider, request);
    }

    // Вход по email и паролю, в ответе access- и refresh-токены.
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // Вход по телефону и паролю.
    @PostMapping("/login/phone")
    public AuthResponse loginByPhone(@Valid @RequestBody PhoneLoginRequest request) {
        return authService.loginByPhone(request);
    }

    // Отправка одноразового кода на телефон (в учебной версии код пишется в лог).
    @PostMapping("/otp/send")
    public CodeResponse sendOtp(@Valid @RequestBody OtpSendRequest request) {
        return authService.sendOtp(request);
    }

    // Проверка кода из SMS, при успехе выдаются токены.
    @PostMapping("/otp/verify")
    public AuthResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return authService.verifyOtp(request);
    }

    // Обмен refresh-токена на новую пару токенов.
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    // Выход: refresh-токен удаляется из базы.
    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody RefreshRequest request) {
        return authService.logout(request);
    }

    // Выход со всех устройств: удаляются все refresh-токены пользователя. @AuthenticationPrincipal даёт id из JWT.
    @PostMapping("/logout/all")
    public MessageResponse logoutAll(@AuthenticationPrincipal Long userId) {
        return authService.logoutAll(userId);
    }

    // Запрос на восстановление пароля: создаётся токен сброса.
    @PostMapping("/password/forgot")
    public CodeResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    // Установка нового пароля по токену из предыдущего шага.
    @PostMapping("/password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    // Смена пароля в личном кабинете, нужен старый пароль.
    @PostMapping("/password/change")
    public MessageResponse changePassword(@AuthenticationPrincipal Long userId,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(userId, request);
    }
}
