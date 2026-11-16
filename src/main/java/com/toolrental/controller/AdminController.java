package com.toolrental.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toolrental.dto.ReviewDto.ReportResponse;
import com.toolrental.service.ReviewService;

/**
 * Эндпоинты администратора. Доступ закрыт в SecurityConfig (роль ADMIN).
 * Удалить спорный отзыв администратор может обычным DELETE /reviews/{id}.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ReviewService reviewService;

    public AdminController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // Все жалобы на отзывы
    @GetMapping("/reports")
    public List<ReportResponse> reports() {
        return reviewService.getReports();
    }
}
