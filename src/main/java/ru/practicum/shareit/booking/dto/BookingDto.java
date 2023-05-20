package ru.practicum.shareit.booking.dto;

import lombok.*;
import ru.practicum.shareit.util.enums.BookingStatus;

import java.time.LocalDateTime;

@Value
public class BookingDto {
    Long id;
    LocalDateTime start;
    LocalDateTime end;
    Long item;
    Long booker;
    BookingStatus status;
}