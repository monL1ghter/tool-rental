package com.toolrental.config;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.toolrental.dto.ReviewDto.CreateReviewRequest;
import com.toolrental.entity.Booking;
import com.toolrental.entity.BookingStatus;
import com.toolrental.entity.DeliveryType;
import com.toolrental.entity.PaymentMethod;
import com.toolrental.entity.Role;
import com.toolrental.entity.Tool;
import com.toolrental.entity.ToolCondition;
import com.toolrental.entity.User;
import com.toolrental.repository.BookingRepository;
import com.toolrental.repository.ToolRepository;
import com.toolrental.repository.UserRepository;
import com.toolrental.service.ReviewService;

/**
 * Заполняет пустую базу тестовыми данными, чтобы сразу можно было пробовать API.
 *
 * Логины (пароль password123, у администратора admin123):
 *   admin@toolrental.com, landlord@example.com, renter@example.com, renter2@example.com
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ToolRepository toolRepository;
    private final BookingRepository bookingRepository;
    private final ReviewService reviewService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, ToolRepository toolRepository,
                           BookingRepository bookingRepository, ReviewService reviewService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.toolRepository = toolRepository;
        this.bookingRepository = bookingRepository;
        this.reviewService = reviewService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // данные уже есть
        }

        createUser("admin@toolrental.com", "+79000000001", "Администратор", Role.ADMIN, "admin123");
        User landlord = createUser("landlord@example.com", "+79000000002", "Иван Строителев", Role.LANDLORD, "password123");
        User renter = createUser("renter@example.com", "+79000000003", "Пётр Арендаторов", Role.RENTER, "password123");
        User renter2 = createUser("renter2@example.com", "+79000000004", "Анна Мастерова", Role.RENTER, "password123");

        Tool drill = createTool(landlord, "Перфоратор Bosch GBH 2-26", "Мощный перфоратор для бетона и кирпича",
                "Электроинструмент", "Bosch", "800", "5000", "500", ToolCondition.GOOD);
        createTool(landlord, "Шуруповёрт Makita DDF485", "Аккумуляторный шуруповёрт, два аккумулятора",
                "Электроинструмент", "Makita", "400", "3000", "300", ToolCondition.NEW);
        createTool(landlord, "Болгарка DeWalt DWE4157", "УШМ 125 мм, 900 Вт",
                "Электроинструмент", "DeWalt", "500", "3500", "300", ToolCondition.USED);
        createTool(landlord, "Бетономешалка Союз 160 л", "Строительная бетономешалка",
                "Строительное оборудование", "Союз", "1200", "8000", "1500", ToolCondition.GOOD);
        createTool(landlord, "Триммер бензиновый Stihl FS 55", "Для покоса травы на участке",
                "Садовая техника", "Stihl", "700", "4000", "500", ToolCondition.GOOD);
        createTool(landlord, "Лазерный уровень Bosch GLL 3-80", "Построитель плоскостей, штатив в комплекте",
                "Измерительный инструмент", "Bosch", "600", "6000", "300", ToolCondition.NEW);

        // Две завершённые аренды перфоратора и отзывы по ним, чтобы был рейтинг
        Booking first = createCompletedBooking(drill, renter, 10, 8);
        Booking second = createCompletedBooking(drill, renter2, 6, 5);
        reviewService.create(renter.getId(), new CreateReviewRequest(first.getId(), 5, "Отличный перфоратор, владелец вежливый"));
        reviewService.create(landlord.getId(), new CreateReviewRequest(first.getId(), 5, "Аккуратный арендатор, вернул вовремя"));
        reviewService.create(renter2.getId(), new CreateReviewRequest(second.getId(), 4, "Всё хорошо, но сверла были тупые"));
    }

    private User createUser(String email, String phone, String fullName, Role role, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPhone(phone);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    private Tool createTool(User owner, String name, String description, String category, String brand,
                            String pricePerDay, String deposit, String deliveryPrice, ToolCondition condition) {
        Tool tool = new Tool();
        tool.setOwner(owner);
        tool.setName(name);
        tool.setDescription(description);
        tool.setCategory(category);
        tool.setBrand(brand);
        tool.setPricePerDay(new BigDecimal(pricePerDay));
        tool.setDeposit(new BigDecimal(deposit));
        tool.setDeliveryPrice(new BigDecimal(deliveryPrice));
        tool.setCondition(condition);
        return toolRepository.save(tool);
    }

    // Аренда в прошлом: началась daysAgoStart дней назад, закончилась daysAgoEnd дней назад
    private Booking createCompletedBooking(Tool tool, User renter, int daysAgoStart, int daysAgoEnd) {
        Booking booking = new Booking();
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setStartDate(LocalDate.now().minusDays(daysAgoStart));
        booking.setEndDate(LocalDate.now().minusDays(daysAgoEnd));
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setDeliveryType(DeliveryType.PICKUP);
        booking.setPaymentMethod(PaymentMethod.ONLINE);
        booking.setPricePerDay(tool.getPricePerDay());
        booking.setDeposit(tool.getDeposit());
        booking.setDeliveryPrice(BigDecimal.ZERO);
        BigDecimal rent = tool.getPricePerDay().multiply(BigDecimal.valueOf(booking.getDays()));
        booking.setRentPrice(rent);
        booking.setTotalPrice(rent.add(tool.getDeposit()));
        bookingRepository.save(booking);

        tool.setBookingsCount(tool.getBookingsCount() + 1);
        toolRepository.save(tool);
        return booking;
    }
}
