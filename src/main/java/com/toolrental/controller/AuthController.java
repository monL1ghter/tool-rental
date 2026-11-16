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

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/register/social/{provider}")
    public AuthResponse registerSocial(@PathVariable String provider,
                                       @Valid @RequestBody SocialRegisterRequest request) {
        return authService.registerSocial(provider, request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/login/phone")
    public AuthResponse loginByPhone(@Valid @RequestBody PhoneLoginRequest request) {
        return authService.loginByPhone(request);
    }

    @PostMapping("/otp/send")
    public CodeResponse sendOtp(@Valid @RequestBody OtpSendRequest request) {
        return authService.sendOtp(request);
    }

    @PostMapping("/otp/verify")
    public AuthResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return authService.verifyOtp(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody RefreshRequest request) {
        return authService.logout(request);
    }

    @PostMapping("/logout/all")
    public MessageResponse logoutAll(@AuthenticationPrincipal Long userId) {
        return authService.logoutAll(userId);
    }

    @PostMapping("/password/forgot")
    public CodeResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/password/change")
    public MessageResponse changePassword(@AuthenticationPrincipal Long userId,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(userId, request);
    }
}
