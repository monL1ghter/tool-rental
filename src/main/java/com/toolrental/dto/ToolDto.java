package com.toolrental.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.toolrental.entity.Tool;
import com.toolrental.entity.ToolCondition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class ToolDto {

    public record ToolRequest(
            @NotBlank String name,
            @Size(max = 2000) String description,
            @NotBlank String category,
            String brand,
            @NotNull @Positive BigDecimal pricePerDay,
            @NotNull @PositiveOrZero BigDecimal deposit,
            @NotNull @PositiveOrZero BigDecimal deliveryPrice,
            @NotNull ToolCondition condition,
            String imageUrl,
            Boolean available) {
    }

    public record ToolResponse(Long id, Long ownerId, String ownerName, String name, String description,
                               String category, String brand, BigDecimal pricePerDay, BigDecimal deposit,
                               BigDecimal deliveryPrice, ToolCondition condition, String imageUrl,
                               boolean available, double rating, int reviewsCount, LocalDateTime createdAt) {

        public static ToolResponse from(Tool t) {
            return new ToolResponse(t.getId(), t.getOwner().getId(), t.getOwner().getFullName(), t.getName(),
                    t.getDescription(), t.getCategory(), t.getBrand(), t.getPricePerDay(), t.getDeposit(),
                    t.getDeliveryPrice(), t.getCondition(), t.getImageUrl(), t.isAvailable(), t.getRating(),
                    t.getReviewsCount(), t.getCreatedAt());
        }
    }

    public record AvailabilityResponse(Long toolId, LocalDate from, LocalDate to, boolean available) {
    }
}
