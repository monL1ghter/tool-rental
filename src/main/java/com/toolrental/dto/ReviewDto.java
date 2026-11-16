package com.toolrental.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.toolrental.entity.ReportReason;
import com.toolrental.entity.Review;
import com.toolrental.entity.ReviewPhoto;
import com.toolrental.entity.ReviewReport;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewDto {

    public record CreateReviewRequest(
            @NotNull Long bookingId,
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 2000) String comment) {
    }

    // Все поля необязательные: меняются только те, что переданы
    public record UpdateReviewRequest(
            @Min(1) @Max(5) Integer rating,
            @Size(max = 2000) String comment) {
    }

    public record ReportRequest(
            @NotNull ReportReason reason,
            @Size(max = 1000) String details) {
    }

    public record PhotoResponse(Long id, String url) {

        public static PhotoResponse from(ReviewPhoto p) {
            return new PhotoResponse(p.getId(), "/uploads/" + p.getFileName());
        }
    }

    public record ReviewResponse(Long id, Long bookingId, Long toolId, Long authorId, String authorName,
                                 Long targetUserId, int rating, String comment, List<PhotoResponse> photos,
                                 LocalDateTime createdAt) {

        public static ReviewResponse from(Review r) {
            Long toolId = r.getTool() == null ? null : r.getTool().getId();
            List<PhotoResponse> photos = r.getPhotos().stream().map(PhotoResponse::from).toList();
            return new ReviewResponse(r.getId(), r.getBooking().getId(), toolId, r.getAuthor().getId(),
                    r.getAuthor().getFullName(), r.getTarget().getId(), r.getRating(), r.getComment(),
                    photos, r.getCreatedAt());
        }
    }

    public record RatingResponse(Long userId, double averageRating, long reviewsCount) {
    }

    public record ReportResponse(Long id, Long reviewId, Long reporterId, ReportReason reason,
                                 String details, LocalDateTime createdAt) {

        public static ReportResponse from(ReviewReport r) {
            return new ReportResponse(r.getId(), r.getReview().getId(), r.getReporter().getId(),
                    r.getReason(), r.getDetails(), r.getCreatedAt());
        }
    }
}
