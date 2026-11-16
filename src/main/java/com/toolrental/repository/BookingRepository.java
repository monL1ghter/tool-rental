package com.toolrental.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.toolrental.entity.Booking;
import com.toolrental.entity.BookingStatus;
import com.toolrental.exception.ApiException;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByRenterIdOrderByCreatedAtDesc(Long renterId);

    List<Booking> findByToolOwnerIdOrderByCreatedAtDesc(Long ownerId);

    boolean existsByToolId(Long toolId);

    // Сколько броней пересекается с периодом [start, end]. excludeId — бронь, которую не учитываем (0, если такой нет).
    @Query("select count(b) from Booking b where b.tool.id = :toolId and b.status in :statuses "
            + "and b.startDate <= :end and b.endDate >= :start and b.id <> :excludeId")
    long countOverlapping(@Param("toolId") Long toolId,
                          @Param("start") LocalDate start,
                          @Param("end") LocalDate end,
                          @Param("statuses") List<BookingStatus> statuses,
                          @Param("excludeId") Long excludeId);

    default Booking getOrThrow(Long id) {
        return findById(id).orElseThrow(() -> ApiException.notFound("Бронь не найдена"));
    }
}
