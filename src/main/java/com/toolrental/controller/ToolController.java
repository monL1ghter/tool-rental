package com.toolrental.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.toolrental.dto.CommonDto.PageResponse;
import com.toolrental.dto.ReviewDto.ReviewResponse;
import com.toolrental.dto.ToolDto.AvailabilityResponse;
import com.toolrental.dto.ToolDto.ToolRequest;
import com.toolrental.dto.ToolDto.ToolResponse;
import com.toolrental.entity.ToolCondition;
import com.toolrental.service.ReviewService;
import com.toolrental.service.ToolService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/tools")
public class ToolController {

    private final ToolService toolService;
    private final ReviewService reviewService;

    public ToolController(ToolService toolService, ReviewService reviewService) {
        this.toolService = toolService;
        this.reviewService = reviewService;
    }

    /**
     * Каталог с поиском, фильтрами, сортировкой и пагинацией.
     * sort: popular (по умолчанию), price_asc, price_desc, new, rating
     */
    @GetMapping
    public PageResponse<ToolResponse> list(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) String brand,
                                           @RequestParam(required = false) BigDecimal minPrice,
                                           @RequestParam(required = false) BigDecimal maxPrice,
                                           @RequestParam(required = false) ToolCondition condition,
                                           @RequestParam(required = false) Double minRating,
                                           @RequestParam(required = false) Boolean available,
                                           @RequestParam(required = false) String sort,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return toolService.search(q, category, brand, minPrice, maxPrice, condition, minRating, available,
                sort, page, size);
    }

    @GetMapping("/popular")
    public List<ToolResponse> popular() {
        return toolService.popular();
    }

    @GetMapping("/new")
    public List<ToolResponse> newest() {
        return toolService.newest();
    }

    @GetMapping("/{id}")
    public ToolResponse getById(@PathVariable Long id) {
        return toolService.getById(id);
    }

    @GetMapping("/{id}/availability")
    public AvailabilityResponse availability(@PathVariable Long id,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return toolService.availability(id, from, to);
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewResponse> reviews(@PathVariable Long id) {
        return reviewService.getToolReviews(id);
    }

    @GetMapping("/{id}/similar")
    public List<ToolResponse> similar(@PathVariable Long id) {
        return toolService.similar(id);
    }

    // ---------- Для арендодателя ----------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ToolResponse create(@AuthenticationPrincipal Long userId, @Valid @RequestBody ToolRequest request) {
        return toolService.create(userId, request);
    }

    @PutMapping("/{id}")
    public ToolResponse update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                               @Valid @RequestBody ToolRequest request) {
        return toolService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        toolService.delete(userId, id);
    }
}
