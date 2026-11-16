package com.toolrental.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toolrental.dto.AuthDto.AuthResponse;
import com.toolrental.dto.UserDto.UpdateProfileRequest;
import com.toolrental.dto.UserDto.UserResponse;
import com.toolrental.entity.Role;
import com.toolrental.entity.User;
import com.toolrental.exception.ApiException;
import com.toolrental.repository.UserRepository;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    public UserResponse getProfile(Long userId) {
        return UserResponse.from(userRepository.getOrThrow(userId));
    }

    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.getOrThrow(userId);
        user.setFullName(request.fullName());

        // Телефон и адрес меняем, только если они переданы
        if (request.phone() != null && !request.phone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.phone())) {
                throw ApiException.conflict("Этот телефон уже занят");
            }
            user.setPhone(request.phone());
        }
        if (request.address() != null) {
            user.setAddress(request.address());
        }
        return UserResponse.from(userRepository.save(user));
    }

    // Арендатор становится арендодателем. Роль лежит в токене, поэтому сразу выдаём новые токены.
    public AuthResponse becomeLandlord(Long userId) {
        User user = userRepository.getOrThrow(userId);
        if (user.getRole() == Role.ADMIN) {
            throw ApiException.badRequest("Администратору менять роль не нужно");
        }
        user.setRole(Role.LANDLORD);
        userRepository.save(user);
        return authService.issueTokens(user);
    }
}
