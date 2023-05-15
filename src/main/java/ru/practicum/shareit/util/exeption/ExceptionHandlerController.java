package ru.practicum.shareit.util.exeption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionHandlerController {
    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.CONFLICT)
    private ErrorResponse exceptionHandler(ValidationException e) {
        log.warn(e.getMessage());
        return new ErrorResponse(e.getMessage(), System.currentTimeMillis());
    }

    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    private ErrorResponse exceptionHandler(NotFoundExceptionEntity e) {
        log.warn(e.getMessage());
        return new ErrorResponse(e.getMessage(), System.currentTimeMillis());
    }

    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    private ErrorResponse exceptionHandler(NotFoundException e) {
        log.warn(e.getMessage());
        return new ErrorResponse(e.getMessage(), System.currentTimeMillis());
    }
}