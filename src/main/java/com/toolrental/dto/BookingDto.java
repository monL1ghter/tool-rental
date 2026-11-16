package com.toolrental.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.toolrental.entity.Booking;
import com.toolrental.entity.BookingStatus;
import com.toolrental.entity.DeliveryType;
import com.toolrental.entity.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public class BookingDto {

    public record CreateBookingRequest(
            @NotNull Long toolId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull DeliveryType deliveryType,
            String deliveryAddress,
            @NotNull PaymentMethod paymentMethod) {
    }

    // Все поля необязательные: меняются только те, что переданы
    public record UpdateBookingRequest(
            LocalDate startDate,
            LocalDate endDate,
            DeliveryType deliveryType,
            String deliveryAddress,
            PaymentMethod paymentMethod) {
    }

    public record ExtendRequest(@NotNull LocalDate newEndDate) {
    }

    public record CalculateRequest(
            @NotNull Long toolId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull DeliveryType deliveryType) {
    }

    public record PriceResponse(long days, BigDecimal pricePerDay, BigDecimal rentPrice,
                                BigDecimal deliveryPrice, BigDecimal deposit, BigDecimal totalPrice) {

        public static PriceResponse from(Booking b) {
            return new PriceResponse(b.getDays(), b.getPricePerDay(), b.getRentPrice(),
                    b.getDeliveryPrice(), b.getDeposit(), b.getTotalPrice());
        }
    }

    public record BookingResponse(Long id, Long toolId, String toolName, Long renterId, String renterName,
                                  Long landlordId, LocalDate startDate, LocalDate endDate, long days,
                                  BookingStatus status, DeliveryType deliveryType, String deliveryAddress,
                                  PaymentMethod paymentMethod, BigDecimal rentPrice, BigDecimal deliveryPrice,
                                  BigDecimal deposit, BigDecimal totalPrice, BigDecimal refundAmount,
                                  LocalDateTime createdAt) {

        public static BookingResponse from(Booking b) {
            return new BookingResponse(b.getId(), b.getTool().getId(), b.getTool().getName(),
                    b.getRenter().getId(), b.getRenter().getFullName(), b.getTool().getOwner().getId(),
                    b.getStartDate(), b.getEndDate(), b.getDays(), b.getStatus(), b.getDeliveryType(),
                    b.getDeliveryAddress(), b.getPaymentMethod(), b.getRentPrice(), b.getDeliveryPrice(),
                    b.getDeposit(), b.getTotalPrice(), b.getRefundAmount(), b.getCreatedAt());
        }
    }
}
