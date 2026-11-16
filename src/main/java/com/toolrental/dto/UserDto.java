package com.toolrental.dto;

import java.time.LocalDateTime;

import com.toolrental.entity.Role;
import com.toolrental.entity.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserDto {

    public record UpdateProfileRequest(
            @NotBlank String fullName,
            @Pattern(regexp = AuthDto.PHONE_REGEX, message = AuthDto.PHONE_MESSAGE) String phone,
            String address) {
    }

    public record UserResponse(Long id, String email, String phone, String fullName, String address,
                               Role role, LocalDateTime createdAt) {

        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getPhone(), u.getFullName(),
                    u.getAddress(), u.getRole(), u.getCreatedAt());
        }
    }
}
