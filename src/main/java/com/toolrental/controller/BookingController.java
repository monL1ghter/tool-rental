package com.toolrental.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.toolrental.dto.BookingDto.BookingResponse;
import com.toolrental.dto.BookingDto.CalculateRequest;
import com.toolrental.dto.BookingDto.CreateBookingRequest;
import com.toolrental.dto.BookingDto.ExtendRequest;
import com.toolrental.dto.BookingDto.PriceResponse;
import com.toolrental.dto.BookingDto.UpdateBookingRequest;
import com.toolrental.entity.BookingStatus;
import com.toolrental.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@AuthenticationPrincipal Long userId,
                                  @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(userId, request);
    }

    // ?role=renter (по умолчанию) или landlord, ?status=PENDING|CONFIRMED|ACTIVE|COMPLETED|CANCELLED
    @GetMapping
    public List<BookingResponse> list(@AuthenticationPrincipal Long userId,
                                      @RequestParam(defaultValue = "renter") String role,
                                      @RequestParam(required = false) BookingStatus status) {
        return bookingService.list(userId, role, status);
    }

    @PostMapping("/calculate")
    public PriceResponse calculate(@Valid @RequestBody CalculateRequest request) {
        return bookingService.calculate(request);
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return bookingService.getById(userId, id);
    }

    @GetMapping("/{id}/price")
    public PriceResponse getPrice(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return bookingService.getPrice(userId, id);
    }

    @PatchMapping("/{id}")
    public BookingResponse update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                  @RequestBody UpdateBookingRequest request) {
        return bookingService.update(userId, id, request);
    }

    @PostMapping("/{id}/confirm")
    public BookingResponse confirm(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return bookingService.confirm(userId, id);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return bookingService.cancel(userId, id);
    }

    @PostMapping("/{id}/extend")
    public BookingResponse extend(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                  @Valid @RequestBody ExtendRequest request) {
        return bookingService.extend(userId, id, request);
    }

    // Арендодатель выдаёт инструмент
    @PostMapping("/{id}/start")
    public BookingResponse start(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return bookingService.start(userId, id);
    }

    // Арендодатель принимает инструмент обратно
    @PostMapping("/{id}/return")
    public BookingResponse returnTool(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return bookingService.returnTool(userId, id);
    }
}
