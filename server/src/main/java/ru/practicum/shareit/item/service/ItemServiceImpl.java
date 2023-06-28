package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentResponseDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exeption.NotFoundException;
import ru.practicum.shareit.exeption.NotFoundExceptionEntity;
import ru.practicum.shareit.item.CommentMapper;
import ru.practicum.shareit.item.ItemMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.shareit.booking.BookingMapper.toBookingItemDto;
import static ru.practicum.shareit.item.CommentMapper.toComment;
import static ru.practicum.shareit.item.CommentMapper.toCommentResponseDto;
import static ru.practicum.shareit.item.ItemMapper.*;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRequestRepository itemRequestRepository;

    @Transactional
    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        final Item item = toItem(itemDto);
        final User user = chekUser(userId);
        item.setOwner(user);
        final Long requestId = itemDto.getRequestId();
        if (requestId != null) {
            final ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                    .orElseThrow(() -> new NotFoundExceptionEntity("Запрос на бронирование вещи не найден."));
            item.setRequest(itemRequest);
        }
        return toItemDto(itemRepository.save(item));
    }

    @Transactional
    @Override
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        chekUser(userId);
        final Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundExceptionEntity("Item с идентификатором : " + itemId + " не найден."));
        if (!userId.equals(item.getOwner().getId())) {
            throw new NotFoundExceptionEntity("Владелец item с идентификатором : " + userId + " указан не верно.");
        }
        if (itemDto.getName() != null)
            item.setName(itemDto.getName());
        if (itemDto.getDescription() != null)
            item.setDescription(itemDto.getDescription());
        if (itemDto.getAvailable() != null)
            item.setIsAvailable(itemDto.getAvailable());
        return toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemResponseDto findById(Long itemId, Long userId) {
        chekUser(userId);
        final Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundExceptionEntity("Item с идентификатором : " + itemId + " не найден."));
        final List<CommentResponseDto> comments = commentRepository.findAllByItemId(itemId)
                .stream()
                .map(CommentMapper::toCommentResponseDto)
                .collect(Collectors.toList());
        final ItemResponseDto itemResponseDto = toItemResponseDto(item);
        itemResponseDto.setComments(comments);
        final Long id = item.getOwner().getId();
        if (Objects.equals(userId, id)) {
            final List<Booking> bookingList = bookingRepository.findByItem_IdAndStatusIs(itemId, BookingStatus.APPROVED);
            return setDateBookings(itemResponseDto, bookingList);
        }
        return itemResponseDto;
    }

    @Override
    public List<ItemResponseDto> findAll(Long userId, PageRequest page) {
        chekUser(userId);
        final List<ItemResponseDto> itemsList = itemRepository.findAllByOwnerId(userId, page)
                .map(ItemMapper::toItemResponseDto)
                .getContent();
        final List<Long> itemsId = itemsList
                .stream()
                .map(ItemResponseDto::getId)
                .collect(Collectors.toList());
        final List<Comment> comments = commentRepository.findAll();
        final List<Booking> bookingList = bookingRepository.findAllByItem_IdInAndStatusIs(itemsId, BookingStatus.APPROVED);
        return itemsList
                .stream()
                .map(itemDto -> addCommentsToItem(itemDto, comments))
                .map(itemDto -> setDateBookings(itemDto, bookingList))
                .collect(Collectors.toList());
    }

    private ItemResponseDto setDateBookings(ItemResponseDto itemsDto, List<Booking> bookingList) {
        final LocalDateTime time = LocalDateTime.now();
        Optional<Booking> bookingLast = bookingList
                .stream()
                .filter(booking -> Objects.equals(booking.getItem().getId(), itemsDto.getId()))
                .sorted(Comparator.comparing(Booking::getEnd).reversed())
                .filter(booking -> booking.getStart().isBefore(time))
                .limit(1)
                .findAny();
        bookingLast.ifPresent(booking -> itemsDto.setLastBooking(toBookingItemDto(booking)));

        Optional<Booking> bookingNext = bookingList
                .stream()
                .filter(booking -> Objects.equals(booking.getItem().getId(), itemsDto.getId()))
                .sorted(Comparator.comparing(Booking::getStart))
                .filter(booking -> booking.getStart().isAfter(time))
                .limit(1)
                .findAny();
        bookingNext.ifPresent(booking -> itemsDto.setNextBooking(toBookingItemDto(booking)));
        return itemsDto;
    }

    @Override
    public List<ItemDto> searchItems(Long userId, String text, PageRequest page) {
        if (text == null || text.isBlank())
            return Collections.emptyList();
        chekUser(userId);
        return itemRepository.findAllByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndIsAvailableIsTrue(text, text, page)
                .map(ItemMapper::toItemDto)
                .getContent();
    }

    @Transactional
    @Override
    public CommentResponseDto createComment(Long userId, CommentDto commentDto, Long itemId) {
        final Comment comment = toComment(commentDto);
        final User author = chekUser(userId);
        final Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundExceptionEntity("Item с идентификатором : " + itemId + " не найден."));
        List<Booking> bookings = bookingRepository.findByItem_IdAndEndIsBefore(itemId, comment.getCreated())
                .stream()
                .filter(booking -> Objects.equals(booking.getBooker().getId(), userId))
                .collect(Collectors.toList());
        if (bookings.isEmpty()) {
            throw new NotFoundException("Пользователь не может оставить отзыв этой вещи.");
        }
        comment.setAuthor(author);
        comment.setItem(item);
        commentRepository.save(comment);
        return toCommentResponseDto(comment);
    }

    private User chekUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundExceptionEntity("Пользователь с идентификатором : " + userId + " не найден."));
    }

    private ItemResponseDto addCommentsToItem(ItemResponseDto item, List<Comment> comments) {
        List<CommentResponseDto> commentResponseDtoList = comments
                .stream()
                .filter(c -> c.getItem().getId().equals(item.getId()))
                .map(CommentMapper::toCommentResponseDto)
                .collect(Collectors.toList());
        item.setComments(commentResponseDtoList);
        return item;
    }
}