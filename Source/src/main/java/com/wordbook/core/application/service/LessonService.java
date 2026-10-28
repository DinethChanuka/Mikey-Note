package com.wordbook.core.application.service;

import com.wordbook.core.application.dto.LessonCreateRequest;
import com.wordbook.core.application.dto.LessonDto;
import com.wordbook.core.application.dto.LessonUpdateRequest;
import com.wordbook.core.domain.exception.EntityNotFoundException;
import com.wordbook.core.domain.model.Book;
import com.wordbook.core.domain.model.Lesson;
import com.wordbook.core.domain.repository.BookRepository;
import com.wordbook.core.domain.repository.LessonRepository;

import java.util.List;

/**
 * Application service orchestrating Lesson use cases.
 */
public class LessonService {
    private final BookRepository bookRepository;
    private final LessonRepository lessonRepository;

    public LessonService(BookRepository bookRepository, LessonRepository lessonRepository) {
        this.bookRepository = bookRepository;
        this.lessonRepository = lessonRepository;
    }

    public List<LessonDto> getLessonsForBook(String bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new EntityNotFoundException("Book", bookId);
        }
        return lessonRepository.findByBookId(bookId).stream()
                .map(LessonDto::fromDomain)
                .toList();
    }

    public LessonDto getLessonById(String id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lesson", id));
        return LessonDto.fromDomain(lesson);
    }

    public LessonDto createLesson(String bookId, LessonCreateRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book", bookId));

        int nextOrder = request.sortOrder() != null
                ? request.sortOrder()
                : lessonRepository.findByBookId(bookId).size();

        Lesson lesson;
        if (request.id() != null && !request.id().trim().isEmpty()) {
            lesson = Lesson.createWithId(request.id().trim(), bookId, request.title(), request.html(), nextOrder);
        } else {
            lesson = Lesson.create(bookId, request.title(), request.html(), nextOrder);
        }

        lessonRepository.save(lesson);
        book.touch();
        bookRepository.update(book);

        return LessonDto.fromDomain(lesson);
    }

    public LessonDto updateLesson(String id, LessonUpdateRequest request) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lesson", id));

        if (request.title() != null && !request.title().trim().isEmpty()) {
            lesson.updateTitle(request.title());
        }

        if (request.html() != null) {
            if (request.wordCount() != null && request.charCount() != null) {
                lesson.updateContentWithMetrics(request.html(), request.wordCount(), request.charCount());
            } else {
                lesson.updateContent(request.html());
            }
        }

        if (request.sortOrder() != null) {
            lesson.updateSortOrder(request.sortOrder());
        }

        lessonRepository.update(lesson);

        // Touch the parent book so its updated_at reflects the latest note revision
        bookRepository.findById(lesson.getBookId()).ifPresent(b -> {
            b.touch();
            bookRepository.update(b);
        });

        return LessonDto.fromDomain(lesson);
    }

    public boolean deleteLesson(String id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lesson", id));
        boolean deleted = lessonRepository.deleteById(id);
        if (deleted) {
            bookRepository.findById(lesson.getBookId()).ifPresent(b -> {
                b.touch();
                bookRepository.update(b);
            });
        }
        return deleted;
    }

    public int getTotalLessonsCount() {
        return lessonRepository.count();
    }

    public long getTotalWords() {
        return lessonRepository.sumTotalWords();
    }

    public long getTotalCharacters() {
        return lessonRepository.sumTotalCharacters();
    }
}
