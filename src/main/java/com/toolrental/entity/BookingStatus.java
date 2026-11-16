package com.toolrental.entity;

import java.util.List;

public enum BookingStatus {
    PENDING,    // ожидает подтверждения арендодателя
    CONFIRMED,  // подтверждена
    ACTIVE,     // инструмент выдан, аренда идёт
    COMPLETED,  // инструмент возвращён, аренда завершена
    CANCELLED;  // отменена

    // Статусы, при которых инструмент занят на даты брони
    public static final List<BookingStatus> BUSY = List.of(PENDING, CONFIRMED, ACTIVE);
}
