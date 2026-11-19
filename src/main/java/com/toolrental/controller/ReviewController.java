package com.toolrental.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.toolrental.dto.CommonDto.MessageResponse;
import com.toolrental.dto.ReviewDto.CreateReviewRequest;
import com.toolrental.dto.ReviewDto.PhotoResponse;
import com.toolrental.dto.ReviewDto.ReportRequest;
import com.toolrental.dto.ReviewDto.ReviewResponse;
import com.toolrental.dto.ReviewDto.UpdateReviewRequest;
import com.toolrental.service.ReviewService;

import jakarta.validation.Valid;

// Отзывы, фото к отзывам и жалобы. Чтение (GET) открыто всем, остальное требует токен.
@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // Оставить отзыв. Можно только по завершённой аренде и один раз от каждого участника.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal Long userId,
                                 @Valid @RequestBody CreateReviewRequest request) {
        return reviewService.create(userId, request);
    }

    // ?toolId= — отзывы об инструменте, ?userId= — отзывы о пользователе
    @GetMapping
    public List<ReviewResponse> list(@RequestParam(required = false) Long toolId,
                                     @RequestParam(required = false) Long userId) {
        return reviewService.list(toolId, userId);
    }

    // Один отзыв по id.
    @GetMapping("/{id}")
    public ReviewResponse getById(@PathVariable Long id) {
        return reviewService.getById(id);
    }

    // Изменить свой отзыв (оценку и/или текст).
    @PatchMapping("/{id}")
    public ReviewResponse update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                 @Valid @RequestBody UpdateReviewRequest request) {
        return reviewService.update(userId, id, request);
    }

    // Удалить отзыв: его автор или администратор.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        reviewService.delete(userId, id);
    }

    // Загрузить фото к своему отзыву (JPEG или PNG, до 5 штук). Запрос в формате multipart/form-data, поле file.
    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoResponse addPhoto(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                  @RequestParam("file") MultipartFile file) {
        return reviewService.addPhoto(userId, id, file);
    }

    // Удалить фото из своего отзыва.
    @DeleteMapping("/{id}/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                            @PathVariable Long photoId) {
        reviewService.deletePhoto(userId, id, photoId);
    }

    // Жалоба на отзыв
    @PostMapping("/{id}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse report(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                  @Valid @RequestBody ReportRequest request) {
        return reviewService.report(userId, id, request);
    }
}
