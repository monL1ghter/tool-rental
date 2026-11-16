package com.toolrental.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toolrental.dto.AuthDto.AuthResponse;
import com.toolrental.dto.ReviewDto.RatingResponse;
import com.toolrental.dto.ReviewDto.ReviewResponse;
import com.toolrental.dto.UserDto.UpdateProfileRequest;
import com.toolrental.dto.UserDto.UserResponse;
import com.toolrental.service.ReviewService;
import com.toolrental.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final ReviewService reviewService;

    public UserController(UserService userService, ReviewService reviewService) {
        this.userService = userService;
        this.reviewService = reviewService;
    }

    @GetMapping("/me")
    public UserResponse getProfile(@AuthenticationPrincipal Long userId) {
        return userService.getProfile(userId);
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal Long userId,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId, request);
    }

    @PostMapping("/me/become-landlord")
    public AuthResponse becomeLandlord(@AuthenticationPrincipal Long userId) {
        return userService.becomeLandlord(userId);
    }

    // Отзывы о пользователе
    @GetMapping("/{id}/reviews")
    public List<ReviewResponse> getUserReviews(@PathVariable Long id) {
        return reviewService.getUserReviews(id);
    }

    // Рейтинг пользователя
    @GetMapping("/{id}/rating")
    public RatingResponse getUserRating(@PathVariable Long id) {
        return reviewService.getUserRating(id);
    }
}
