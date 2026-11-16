package com.toolrental.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toolrental.dto.BookingDto.BookingResponse;
import com.toolrental.dto.BookingDto.CalculateRequest;
import com.toolrental.dto.BookingDto.CreateBookingRequest;
import com.toolrental.dto.BookingDto.ExtendRequest;
import com.toolrental.dto.BookingDto.PriceResponse;
import com.toolrental.dto.BookingDto.UpdateBookingRequest;
import com.toolrental.entity.Booking;
import com.toolrental.entity.BookingStatus;
import com.toolrental.entity.DeliveryType;
import com.toolrental.entity.PaymentMethod;
import com.toolrental.entity.Tool;
import com.toolrental.entity.User;
import com.toolrental.exception.ApiException;
import com.toolrental.repository.BookingRepository;
import com.toolrental.repository.ToolRepository;
import com.toolrental.repository.UserRepository;

@Service
@Transactional
public class BookingService {

    // Брони в этих статусах занимают инструмент
    private static final List<BookingStatus> BUSY =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.ACTIVE);

    private final BookingRepository bookingRepository;
    private final ToolRepository toolRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, ToolRepository toolRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.toolRepository = toolRepository;
        this.userRepository = userRepository;
    }

    // ---------- Создание и просмотр ----------

    public BookingResponse create(Long userId, CreateBookingRequest request) {
        User renter = userRepository.getOrThrow(userId);
        Tool tool = toolRepository.getOrThrow(request.toolId());

        if (tool.getOwner().getId().equals(userId)) {
            throw ApiException.badRequest("Нельзя арендовать собственный инструмент");
        }
        if (!tool.isAvailable()) {
            throw ApiException.badRequest("Инструмент сейчас не сдаётся");
        }
        checkDates(request.startDate(), request.endDate());
        checkFree(tool.getId(), request.startDate(), request.endDate(), 0L);

        Booking booking = new Booking();
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setStartDate(request.startDate());
        booking.setEndDate(request.endDate());
        booking.setPaymentMethod(request.paymentMethod());
        booking.setPricePerDay(tool.getPricePerDay());
        booking.setDeposit(tool.getDeposit());
        setDelivery(booking, request.deliveryType(), request.deliveryAddress());
        recalculate(booking);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    // Список броней. role = renter (по умолчанию) — мои аренды, landlord — брони моих инструментов
    public List<BookingResponse> list(Long userId, String role, BookingStatus status) {
        List<Booking> bookings;
        if ("landlord".equalsIgnoreCase(role)) {
            bookings = bookingRepository.findByToolOwnerIdOrderByCreatedAtDesc(userId);
        } else {
            bookings = bookingRepository.findByRenterIdOrderByCreatedAtDesc(userId);
        }
        return bookings.stream()
                .filter(b -> status == null || b.getStatus() == status)
                .map(BookingResponse::from)
                .toList();
    }

    public BookingResponse getById(Long userId, Long id) {
        return BookingResponse.from(getForParticipant(userId, id));
    }

    public PriceResponse getPrice(Long userId, Long id) {
        return PriceResponse.from(getForParticipant(userId, id));
    }

    // Предварительный расчёт стоимости без создания брони
    public PriceResponse calculate(CalculateRequest request) {
        Tool tool = toolRepository.getOrThrow(request.toolId());
        if (request.endDate().isBefore(request.startDate())) {
            throw ApiException.badRequest("Дата окончания раньше даты начала");
        }
        // Черновик брони нужен только для расчёта, в базу он не сохраняется
        Booking draft = new Booking();
        draft.setTool(tool);
        draft.setStartDate(request.startDate());
        draft.setEndDate(request.endDate());
        draft.setDeliveryType(request.deliveryType());
        draft.setPricePerDay(tool.getPricePerDay());
        draft.setDeposit(tool.getDeposit());
        recalculate(draft);
        return PriceResponse.from(draft);
    }

    // ---------- Действия арендатора ----------

    // Изменить бронь можно, пока арендодатель её не подтвердил
    public BookingResponse update(Long userId, Long id, UpdateBookingRequest request) {
        Booking booking = getForRenter(userId, id);
        requireStatus(booking, BookingStatus.PENDING);

        if (request.startDate() != null) {
            booking.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            booking.setEndDate(request.endDate());
        }
        if (request.paymentMethod() != null) {
            booking.setPaymentMethod(request.paymentMethod());
        }
        if (request.deliveryType() != null) {
            setDelivery(booking, request.deliveryType(), request.deliveryAddress());
        } else if (request.deliveryAddress() != null && booking.getDeliveryType() == DeliveryType.DELIVERY) {
            booking.setDeliveryAddress(request.deliveryAddress());
        }

        checkDates(booking.getStartDate(), booking.getEndDate());
        checkFree(booking.getTool().getId(), booking.getStartDate(), booking.getEndDate(), booking.getId());
        recalculate(booking);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    public BookingResponse extend(Long userId, Long id, ExtendRequest request) {
        Booking booking = getForRenter(userId, id);
        requireStatus(booking, BookingStatus.CONFIRMED, BookingStatus.ACTIVE);

        if (!request.newEndDate().isAfter(booking.getEndDate())) {
            throw ApiException.badRequest("Новая дата окончания должна быть позже текущей");
        }
        // Проверяем только дни, на которые продлеваем
        checkFree(booking.getTool().getId(), booking.getEndDate().plusDays(1), request.newEndDate(), booking.getId());

        booking.setEndDate(request.newEndDate());
        recalculate(booking);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    // ---------- Действия арендодателя ----------

    public BookingResponse confirm(Long userId, Long id) {
        Booking booking = getForLandlord(userId, id);
        requireStatus(booking, BookingStatus.PENDING);
        booking.setStatus(BookingStatus.CONFIRMED);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    // Арендодатель выдал инструмент
    public BookingResponse start(Long userId, Long id) {
        Booking booking = getForLandlord(userId, id);
        requireStatus(booking, BookingStatus.CONFIRMED);
        booking.setStatus(BookingStatus.ACTIVE);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    // Арендодатель принял инструмент обратно. После этого можно оставлять отзывы.
    public BookingResponse returnTool(Long userId, Long id) {
        Booking booking = getForLandlord(userId, id);
        requireStatus(booking, BookingStatus.ACTIVE);
        booking.setStatus(BookingStatus.COMPLETED);

        Tool tool = booking.getTool();
        tool.setBookingsCount(tool.getBookingsCount() + 1);
        toolRepository.save(tool);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    // ---------- Отмена ----------

    /**
     * Правила возврата:
     * - оплата при получении: возвращать нечего (0);
     * - отменил арендодатель или арендатор до даты начала: возврат полной суммы;
     * - арендатор отменил в день начала или позже: возврат суммы за вычетом половины стоимости аренды.
     */
    public BookingResponse cancel(Long userId, Long id) {
        Booking booking = getForParticipant(userId, id);
        requireStatus(booking, BookingStatus.PENDING, BookingStatus.CONFIRMED);

        boolean byLandlord = booking.getTool().getOwner().getId().equals(userId);
        boolean beforeStart = LocalDate.now().isBefore(booking.getStartDate());

        BigDecimal refund;
        if (booking.getPaymentMethod() == PaymentMethod.ON_RECEIPT) {
            refund = BigDecimal.ZERO;
        } else if (byLandlord || beforeStart) {
            refund = booking.getTotalPrice();
        } else {
            BigDecimal penalty = booking.getRentPrice().multiply(new BigDecimal("0.5"));
            refund = booking.getTotalPrice().subtract(penalty).setScale(2, RoundingMode.HALF_UP);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setRefundAmount(refund);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    // ---------- Вспомогательные методы ----------

    // Считает стоимость по датам и способу получения
    private void recalculate(Booking booking) {
        BigDecimal rent = booking.getPricePerDay().multiply(BigDecimal.valueOf(booking.getDays()));
        BigDecimal delivery = BigDecimal.ZERO;
        if (booking.getDeliveryType() == DeliveryType.DELIVERY) {
            delivery = booking.getTool().getDeliveryPrice();
        }
        booking.setRentPrice(rent);
        booking.setDeliveryPrice(delivery);
        booking.setTotalPrice(rent.add(delivery).add(booking.getDeposit()));
    }

    // Для доставки нужен адрес: берём из запроса, а если его нет, то из профиля
    private void setDelivery(Booking booking, DeliveryType type, String address) {
        booking.setDeliveryType(type);
        if (type == DeliveryType.PICKUP) {
            booking.setDeliveryAddress(null);
            return;
        }
        if (address == null || address.isBlank()) {
            address = booking.getRenter().getAddress();
        }
        if (address == null || address.isBlank()) {
            throw ApiException.badRequest("Для доставки укажите адрес");
        }
        booking.setDeliveryAddress(address);
    }

    private void checkDates(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw ApiException.badRequest("Дата окончания раньше даты начала");
        }
        if (start.isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Дата начала не может быть в прошлом");
        }
    }

    // excludeId — бронь, которую не учитываем (при изменении и продлении); для новой брони передаём 0
    private void checkFree(Long toolId, LocalDate start, LocalDate end, Long excludeId) {
        if (bookingRepository.countOverlapping(toolId, start, end, BUSY, excludeId) > 0) {
            throw ApiException.conflict("Инструмент занят на выбранные даты");
        }
    }

    private void requireStatus(Booking booking, BookingStatus... allowed) {
        if (!Arrays.asList(allowed).contains(booking.getStatus())) {
            throw ApiException.conflict("Действие недоступно: статус брони " + booking.getStatus());
        }
    }

    // Бронь видит арендатор и арендодатель инструмента
    private Booking getForParticipant(Long userId, Long id) {
        Booking booking = bookingRepository.getOrThrow(id);
        boolean isRenter = booking.getRenter().getId().equals(userId);
        boolean isLandlord = booking.getTool().getOwner().getId().equals(userId);
        if (!isRenter && !isLandlord) {
            throw ApiException.forbidden("Нет доступа к этой брони");
        }
        return booking;
    }

    private Booking getForRenter(Long userId, Long id) {
        Booking booking = bookingRepository.getOrThrow(id);
        if (!booking.getRenter().getId().equals(userId)) {
            throw ApiException.forbidden("Действие доступно только арендатору");
        }
        return booking;
    }

    private Booking getForLandlord(Long userId, Long id) {
        Booking booking = bookingRepository.getOrThrow(id);
        if (!booking.getTool().getOwner().getId().equals(userId)) {
            throw ApiException.forbidden("Действие доступно только арендодателю");
        }
        return booking;
    }
}
