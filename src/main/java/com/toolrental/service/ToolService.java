package com.toolrental.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toolrental.dto.CommonDto.PageResponse;
import com.toolrental.dto.ToolDto.AvailabilityResponse;
import com.toolrental.dto.ToolDto.ToolRequest;
import com.toolrental.dto.ToolDto.ToolResponse;
import com.toolrental.entity.BookingStatus;
import com.toolrental.entity.Role;
import com.toolrental.entity.Tool;
import com.toolrental.entity.ToolCondition;
import com.toolrental.entity.User;
import com.toolrental.exception.ApiException;
import com.toolrental.repository.BookingRepository;
import com.toolrental.repository.ToolRepository;
import com.toolrental.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class ToolService {

    private final ToolRepository toolRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public ToolService(ToolRepository toolRepository, UserRepository userRepository,
                       BookingRepository bookingRepository) {
        this.toolRepository = toolRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    // ---------- Каталог ----------

    public PageResponse<ToolResponse> search(String q, String category, String brand, BigDecimal minPrice,
                                             BigDecimal maxPrice, ToolCondition condition, Double minRating,
                                             Boolean available, String sort, int page, int size) {
        // По умолчанию в каталоге только инструменты, которые сдаются (скрытые не показываем)
        boolean onlyAvailable = available == null || available;

        // Условия добавляются в запрос только для тех параметров, которые переданы
        Specification<Tool> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                conditions.add(cb.or(
                        cb.like(cb.lower(root.<String>get("name")), like),
                        cb.like(cb.lower(root.<String>get("category")), like),
                        cb.like(cb.lower(root.<String>get("brand")), like)));
            }
            if (category != null && !category.isBlank()) {
                conditions.add(cb.equal(cb.lower(root.<String>get("category")), category.trim().toLowerCase()));
            }
            if (brand != null && !brand.isBlank()) {
                conditions.add(cb.equal(cb.lower(root.<String>get("brand")), brand.trim().toLowerCase()));
            }
            if (minPrice != null) {
                conditions.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("pricePerDay"), minPrice));
            }
            if (maxPrice != null) {
                conditions.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("pricePerDay"), maxPrice));
            }
            if (condition != null) {
                conditions.add(cb.equal(root.get("condition"), condition));
            }
            if (minRating != null) {
                conditions.add(cb.greaterThanOrEqualTo(root.<Double>get("rating"), minRating));
            }
            conditions.add(cb.equal(root.<Boolean>get("available"), onlyAvailable));
            return cb.and(conditions.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), toSort(sort));
        return PageResponse.of(toolRepository.findAll(spec, pageable).map(ToolResponse::from));
    }

    private Sort toSort(String sort) {
        if (sort == null) {
            sort = "";
        }
        return switch (sort) {
            case "price_asc" -> Sort.by("pricePerDay").ascending();
            case "price_desc" -> Sort.by("pricePerDay").descending();
            case "new" -> Sort.by("createdAt").descending();
            case "rating" -> Sort.by("rating").descending();
            default -> Sort.by("bookingsCount").descending(); // popular
        };
    }

    public ToolResponse getById(Long id) {
        return ToolResponse.from(toolRepository.getOrThrow(id));
    }

    public List<ToolResponse> popular() {
        return toolRepository.findTop10ByAvailableTrueOrderByBookingsCountDesc().stream()
                .map(ToolResponse::from).toList();
    }

    public List<ToolResponse> newest() {
        return toolRepository.findTop10ByAvailableTrueOrderByCreatedAtDesc().stream()
                .map(ToolResponse::from).toList();
    }

    // Похожие инструменты: та же категория
    public List<ToolResponse> similar(Long id) {
        Tool tool = toolRepository.getOrThrow(id);
        return toolRepository.findTop5ByCategoryAndIdNotAndAvailableTrueOrderByRatingDesc(tool.getCategory(), id)
                .stream().map(ToolResponse::from).toList();
    }

    // Свободен ли инструмент на выбранные даты
    public AvailabilityResponse availability(Long id, LocalDate from, LocalDate to) {
        Tool tool = toolRepository.getOrThrow(id);
        if (to.isBefore(from)) {
            throw ApiException.badRequest("Дата окончания раньше даты начала");
        }
        List<BookingStatus> busy = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.ACTIVE);
        boolean free = bookingRepository.countOverlapping(id, from, to, busy, 0L) == 0;
        return new AvailabilityResponse(id, from, to, tool.isAvailable() && free);
    }

    // ---------- Управление инструментами (арендодатель) ----------

    public ToolResponse create(Long userId, ToolRequest request) {
        Tool tool = new Tool();
        tool.setOwner(userRepository.getOrThrow(userId));
        fill(tool, request);
        return ToolResponse.from(toolRepository.save(tool));
    }

    public ToolResponse update(Long userId, Long id, ToolRequest request) {
        Tool tool = getOwnTool(userId, id);
        fill(tool, request);
        return ToolResponse.from(toolRepository.save(tool));
    }

    public void delete(Long userId, Long id) {
        Tool tool = getOwnTool(userId, id);
        if (bookingRepository.existsByToolId(id)) {
            throw ApiException.conflict("У инструмента есть бронирования. Скройте его: available = false");
        }
        toolRepository.delete(tool);
    }

    // Инструмент можно менять владельцу или администратору
    private Tool getOwnTool(Long userId, Long toolId) {
        Tool tool = toolRepository.getOrThrow(toolId);
        User user = userRepository.getOrThrow(userId);
        if (!tool.getOwner().getId().equals(userId) && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Это не ваш инструмент");
        }
        return tool;
    }

    private void fill(Tool tool, ToolRequest request) {
        tool.setName(request.name());
        tool.setDescription(request.description());
        tool.setCategory(request.category());
        tool.setBrand(request.brand());
        tool.setPricePerDay(request.pricePerDay());
        tool.setDeposit(request.deposit());
        tool.setDeliveryPrice(request.deliveryPrice());
        tool.setCondition(request.condition());
        tool.setImageUrl(request.imageUrl());
        if (request.available() != null) {
            tool.setAvailable(request.available());
        }
    }
}
