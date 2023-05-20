package ru.practicum.shareit.request.model;

import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ItemRequest {
    private Long id;
    private String description;
    private Long requestor;
    private LocalDateTime created;
}