package com.toolrental.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.toolrental.entity.Review;
import com.toolrental.exception.ApiException;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookingIdAndAuthorId(Long bookingId, Long authorId);

    List<Review> findByToolIdOrderByCreatedAtDesc(Long toolId);

    List<Review> findByTargetIdOrderByCreatedAtDesc(Long targetId);

    List<Review> findByToolIdAndTargetIdOrderByCreatedAtDesc(Long toolId, Long targetId);

    long countByToolId(Long toolId);

    long countByTargetId(Long targetId);

    // Средний рейтинг инструмента (null, если отзывов нет)
    @Query("select avg(r.rating) from Review r where r.tool.id = :toolId")
    Double averageByTool(@Param("toolId") Long toolId);

    // Средний рейтинг пользователя (null, если отзывов нет)
    @Query("select avg(r.rating) from Review r where r.target.id = :userId")
    Double averageByTarget(@Param("userId") Long userId);

    default Review getOrThrow(Long id) {
        return findById(id).orElseThrow(() -> ApiException.notFound("Отзыв не найден"));
    }
}
