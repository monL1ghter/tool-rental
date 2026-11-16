package com.toolrental.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.toolrental.entity.ReviewReport;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    boolean existsByReviewIdAndReporterId(Long reviewId, Long reporterId);

    List<ReviewReport> findAllByOrderByCreatedAtDesc();
}
