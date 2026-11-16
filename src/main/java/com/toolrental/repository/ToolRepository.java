package com.toolrental.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.toolrental.entity.Tool;
import com.toolrental.exception.ApiException;

public interface ToolRepository extends JpaRepository<Tool, Long>, JpaSpecificationExecutor<Tool> {

    List<Tool> findTop10ByAvailableTrueOrderByBookingsCountDesc();

    List<Tool> findTop10ByAvailableTrueOrderByCreatedAtDesc();

    List<Tool> findTop5ByCategoryAndIdNotAndAvailableTrueOrderByRatingDesc(String category, Long id);

    default Tool getOrThrow(Long id) {
        return findById(id).orElseThrow(() -> ApiException.notFound("Инструмент не найден"));
    }
}
