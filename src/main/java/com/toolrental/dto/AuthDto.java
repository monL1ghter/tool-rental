package com.toolrental.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.toolrental.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public static final String PHONE_REGEX = "^\\+?[0-9]{10,15}$";
    public static final String PHONE_MESSAGE = "телефон в формате +79991234567";

    public record RegisterRequest(
            @Email(message = "некорректный email") String email,
            @Pattern(regexp = PHONE_REGEX, message = PHONE_MESSAGE) String phone,
            @NotBlank @Size(min = 6, message = "минимум 6 символов") String password,
            @NotBlank String fullName,
            Role role) {
    }

    public record SocialRegisterRequest(
            @NotBlank String socialId,
            @Email(message = "некорректный email") String email,
            @NotBlank String fullName) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record PhoneLoginRequest(@NotBlank String phone, @NotBlank String password) {
    }

    public record OtpSendRequest(@NotBlank @Pattern(regexp = PHONE_REGEX, message = PHONE_MESSAGE) String phone) {
    }

    public record OtpVerifyRequest(@NotBlank String phone, @NotBlank String code) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email(message = "некорректный email") String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 6, message = "минимум 6 символов") String newPassword) {
    }

    public record ChangePasswordRequest(
            @NotBlank String oldPassword,
            @NotBlank @Size(min = 6, message = "минимум 6 символов") String newPassword) {
    }

    public record AuthResponse(String accessToken, String refreshToken, Long userId, Role role) {
    }

    // code заполняется только в тестовом режиме (app.dev-mode=true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CodeResponse(String message, String code) {
    }
}
