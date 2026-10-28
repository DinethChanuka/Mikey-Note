package com.wordbook.core.application.service;

import com.wordbook.core.application.dto.*;
import com.wordbook.core.domain.exception.EntityNotFoundException;
import com.wordbook.core.domain.model.Book;
import com.wordbook.core.domain.model.Lesson;
import com.wordbook.core.domain.repository.BookRepository;
import com.wordbook.core.domain.repository.LessonRepository;
import com.wordbook.core.domain.repository.UnitOfWork;

import java.util.List;

/**
 * Application service orchestrating Book use cases.
 */
public class BookService {
    private final BookRepository bookRepository;
    private final LessonRepository lessonRepository;
    private final UnitOfWork unitOfWork;

    public BookService(BookRepository bookRepository, LessonRepository lessonRepository, UnitOfWork unitOfWork) {
        this.bookRepository = bookRepository;
        this.lessonRepository = lessonRepository;
        this.unitOfWork = unitOfWork;
    }

    public List<BookDto> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(b -> {
                    List<Lesson> lessons = lessonRepository.findByBookId(b.getId());
                    return BookDto.fromDomain(b, lessons.size());
                })
                .toList();
    }

    public BookWithLessonsDto getBookWithLessons(String id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book", id));
        List<Lesson> lessons = lessonRepository.findByBookId(id);
        return BookWithLessonsDto.fromDomain(book, lessons);
    }

    public BookDto createBook(BookCreateRequest request) {
        Book book;
        if (request.id() != null && !request.id().trim().isEmpty()) {
            book = Book.createWithId(request.id().trim(), request.title(), request.color());
        } else {
            book = Book.create(request.title(), request.color());
        }
        bookRepository.save(book);
        return BookDto.fromDomain(book, 0);
    }

    public BookDto updateBook(String id, BookUpdateRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book", id));

        if (request.title() != null && !request.title().trim().isEmpty()) {
            book.rename(request.title());
        }
        if (request.color() != null && !request.color().trim().isEmpty()) {
            book.changeColor(request.color());
        }
        bookRepository.update(book);

        int count = lessonRepository.findByBookId(id).size();
        return BookDto.fromDomain(book, count);
    }

    public boolean deleteBook(String id) {
        if (!bookRepository.existsById(id)) {
            throw new EntityNotFoundException("Book", id);
        }
        return unitOfWork.executeInTransaction(() -> {
            lessonRepository.deleteByBookId(id);
            return bookRepository.deleteById(id);
        });
    }

    public int getTotalBooksCount() {
        return bookRepository.count();
    }
}
