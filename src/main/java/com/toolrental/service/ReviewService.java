package com.toolrental.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.toolrental.dto.CommonDto.MessageResponse;
import com.toolrental.dto.ReviewDto.CreateReviewRequest;
import com.toolrental.dto.ReviewDto.PhotoResponse;
import com.toolrental.dto.ReviewDto.RatingResponse;
import com.toolrental.dto.ReviewDto.ReportRequest;
import com.toolrental.dto.ReviewDto.ReportResponse;
import com.toolrental.dto.ReviewDto.ReviewResponse;
import com.toolrental.dto.ReviewDto.UpdateReviewRequest;
import com.toolrental.entity.Booking;
import com.toolrental.entity.BookingStatus;
import com.toolrental.entity.Review;
import com.toolrental.entity.ReviewPhoto;
import com.toolrental.entity.ReviewReport;
import com.toolrental.entity.Role;
import com.toolrental.entity.Tool;
import com.toolrental.entity.User;
import com.toolrental.exception.ApiException;
import com.toolrental.repository.BookingRepository;
import com.toolrental.repository.ReviewReportRepository;
import com.toolrental.repository.ReviewRepository;
import com.toolrental.repository.ToolRepository;
import com.toolrental.repository.UserRepository;

@Service
@Transactional
public class ReviewService {

    private static final int MAX_PHOTOS = 5;

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reportRepository;
    private final BookingRepository bookingRepository;
    private final ToolRepository toolRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public ReviewService(ReviewRepository reviewRepository, ReviewReportRepository reportRepository,
                         BookingRepository bookingRepository, ToolRepository toolRepository,
                         UserRepository userRepository, FileStorageService fileStorageService) {
        this.reviewRepository = reviewRepository;
        this.reportRepository = reportRepository;
        this.bookingRepository = bookingRepository;
        this.toolRepository = toolRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    // ---------- Создание, просмотр, изменение ----------

    public ReviewResponse create(Long userId, CreateReviewRequest request) {
        Booking booking = bookingRepository.getOrThrow(request.bookingId());
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw ApiException.badRequest("Отзыв можно оставить только после возврата инструмента");
        }

        boolean isRenter = booking.getRenter().getId().equals(userId);
        boolean isLandlord = booking.getTool().getOwner().getId().equals(userId);
        if (!isRenter && !isLandlord) {
            throw ApiException.forbidden("Вы не участник этой аренды");
        }
        if (reviewRepository.existsByBookingIdAndAuthorId(booking.getId(), userId)) {
            throw ApiException.conflict("Вы уже оставили отзыв по этой аренде");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setAuthor(userRepository.getOrThrow(userId));
        if (isRenter) {
            // Арендатор оценивает инструмент и арендодателя
            review.setTool(booking.getTool());
            review.setTarget(booking.getTool().getOwner());
        } else {
            // Арендодатель оценивает арендатора
            review.setTarget(booking.getRenter());
        }
        review.setRating(request.rating());
        review.setComment(request.comment());
        reviewRepository.save(review);

        updateToolRating(review.getTool());
        return ReviewResponse.from(review);
    }

    // Отзывы по инструменту и / или по пользователю (userId — тот, о ком написан отзыв)
    public List<ReviewResponse> list(Long toolId, Long userId) {
        List<Review> reviews;
        if (toolId != null && userId != null) {
            reviews = reviewRepository.findByToolIdAndTargetIdOrderByCreatedAtDesc(toolId, userId);
        } else if (toolId != null) {
            reviews = reviewRepository.findByToolIdOrderByCreatedAtDesc(toolId);
        } else if (userId != null) {
            reviews = reviewRepository.findByTargetIdOrderByCreatedAtDesc(userId);
        } else {
            throw ApiException.badRequest("Укажите toolId или userId");
        }
        return reviews.stream().map(ReviewResponse::from).toList();
    }

    public List<ReviewResponse> getToolReviews(Long toolId) {
        toolRepository.getOrThrow(toolId);
        return list(toolId, null);
    }

    // GET /users/{id}/reviews: все отзывы о пользователе
    public List<ReviewResponse> getUserReviews(Long userId) {
        userRepository.getOrThrow(userId);
        return list(null, userId);
    }

    // GET /users/{id}/rating: средняя оценка и количество отзывов
    public RatingResponse getUserRating(Long userId) {
        userRepository.getOrThrow(userId);
        Double average = reviewRepository.averageByTarget(userId);
        long count = reviewRepository.countByTargetId(userId);
        return new RatingResponse(userId, average == null ? 0.0 : round(average), count);
    }

    public ReviewResponse getById(Long id) {
        return ReviewResponse.from(reviewRepository.getOrThrow(id));
    }

    public ReviewResponse update(Long userId, Long id, UpdateReviewRequest request) {
        Review review = getOwnReview(userId, id);
        if (request.rating() != null) {
            review.setRating(request.rating());
        }
        if (request.comment() != null) {
            review.setComment(request.comment());
        }
        reviewRepository.save(review);

        updateToolRating(review.getTool());
        return ReviewResponse.from(review);
    }

    // Удалить отзыв может автор или администратор
    public void delete(Long userId, Long id) {
        Review review = reviewRepository.getOrThrow(id);
        User user = userRepository.getOrThrow(userId);
        if (!review.getAuthor().getId().equals(userId) && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Нельзя удалить чужой отзыв");
        }

        for (ReviewPhoto photo : review.getPhotos()) {
            fileStorageService.delete(photo.getFileName());
        }
        Tool tool = review.getTool();
        reviewRepository.delete(review);
        updateToolRating(tool);
    }

    // ---------- Фото ----------

    public PhotoResponse addPhoto(Long userId, Long reviewId, MultipartFile file) {
        Review review = getOwnReview(userId, reviewId);
        if (file.isEmpty()) {
            throw ApiException.badRequest("Файл пустой");
        }
        if (review.getPhotos().size() >= MAX_PHOTOS) {
            throw ApiException.badRequest("К отзыву можно прикрепить не больше " + MAX_PHOTOS + " фото");
        }

        ReviewPhoto photo = new ReviewPhoto();
        photo.setReview(review);
        photo.setFileName(fileStorageService.save(file));
        review.getPhotos().add(photo);
        reviewRepository.flush(); // сохраняет фото в БД, чтобы у него появился id
        return PhotoResponse.from(photo);
    }

    public void deletePhoto(Long userId, Long reviewId, Long photoId) {
        Review review = getOwnReview(userId, reviewId);
        ReviewPhoto photo = review.getPhotos().stream()
                .filter(p -> p.getId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Фото не найдено"));
        review.getPhotos().remove(photo); // orphanRemoval удалит запись из БД
        fileStorageService.delete(photo.getFileName());
    }

    // ---------- Жалобы ----------

    // POST /reviews/{id}/report: пожаловаться на отзыв
    public MessageResponse report(Long userId, Long reviewId, ReportRequest request) {
        Review review = reviewRepository.getOrThrow(reviewId);
        if (review.getAuthor().getId().equals(userId)) {
            throw ApiException.badRequest("Нельзя пожаловаться на собственный отзыв");
        }
        if (reportRepository.existsByReviewIdAndReporterId(reviewId, userId)) {
            throw ApiException.conflict("Вы уже отправляли жалобу на этот отзыв");
        }

        ReviewReport report = new ReviewReport();
        report.setReview(review);
        report.setReporter(userRepository.getOrThrow(userId));
        report.setReason(request.reason());
        report.setDetails(request.details());
        reportRepository.save(report);
        return new MessageResponse("Жалоба отправлена и будет рассмотрена администратором");
    }

    // Список жалоб для администратора
    public List<ReportResponse> getReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc().stream().map(ReportResponse::from).toList();
    }

    // ---------- Вспомогательные методы ----------

    // Отзыв, который можно менять только его автору
    private Review getOwnReview(Long userId, Long reviewId) {
        Review review = reviewRepository.getOrThrow(reviewId);
        if (!review.getAuthor().getId().equals(userId)) {
            throw ApiException.forbidden("Это не ваш отзыв");
        }
        return review;
    }

    // Пересчитывает средний рейтинг инструмента (для отзывов арендодателя об арендаторе tool == null)
    private void updateToolRating(Tool tool) {
        if (tool == null) {
            return;
        }
        Double average = reviewRepository.averageByTool(tool.getId());
        tool.setRating(average == null ? 0.0 : round(average));
        tool.setReviewsCount((int) reviewRepository.countByToolId(tool.getId()));
        toolRepository.save(tool);
    }

    // Округляет до одного знака: 4.3333 -> 4.3
    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
