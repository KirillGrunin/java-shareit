package ru.practicum.shareit.util.exeption;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class ErrorResponse extends RuntimeException {
    private String message;
    private long timestamp;
}