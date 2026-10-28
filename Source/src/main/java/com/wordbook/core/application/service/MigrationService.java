package com.wordbook.core.application.service;

import com.wordbook.core.application.dto.BookWithLessonsDto;
import com.wordbook.core.application.dto.LessonDto;
import com.wordbook.core.application.dto.SystemStatsDto;
import com.wordbook.core.domain.model.Book;
import com.wordbook.core.domain.model.Lesson;
import com.wordbook.core.domain.repository.BookRepository;
import com.wordbook.core.domain.repository.LessonRepository;
import com.wordbook.core.domain.repository.UnitOfWork;

import java.io.File;
import java.util.*;

/**
 * Service managing database import/export, legacy browser localStorage migration,
 * and system storage diagnostics.
 */
public class MigrationService {
    private final BookRepository bookRepository;
    private final LessonRepository lessonRepository;
    private final UnitOfWork unitOfWork;
    private final String databaseFilePath;
    private final long startTimeMillis;

    public MigrationService(BookRepository bookRepository, LessonRepository lessonRepository,
                            UnitOfWork unitOfWork, String databaseFilePath) {
        this.bookRepository = bookRepository;
        this.lessonRepository = lessonRepository;
        this.unitOfWork = unitOfWork;
        this.databaseFilePath = databaseFilePath;
        this.startTimeMillis = System.currentTimeMillis();
    }

    public SystemStatsDto getSystemStats() {
        int totalBooks = bookRepository.count();
        int totalLessons = lessonRepository.count();
        long totalWords = lessonRepository.sumTotalWords();
        long totalChars = lessonRepository.sumTotalCharacters();

        long dbSize = 0;
        if (databaseFilePath != null) {
            File dbFile = new File(databaseFilePath);
            if (dbFile.exists()) {
                dbSize = dbFile.length();
            }
        }

        return new SystemStatsDto(
                totalBooks,
                totalLessons,
                totalWords,
                totalChars,
                dbSize,
                databaseFilePath,
                System.getProperty("java.version"),
                System.currentTimeMillis() - startTimeMillis
        );
    }

    public List<BookWithLessonsDto> exportAllData() {
        List<Book> books = bookRepository.findAll();
        List<BookWithLessonsDto> result = new ArrayList<>();
        for (Book b : books) {
            List<Lesson> lessons = lessonRepository.findByBookId(b.getId());
            result.add(BookWithLessonsDto.fromDomain(b, lessons));
        }
        return result;
    }

    public int importBooks(List<Map<String, Object>> booksData) {
        if (booksData == null || booksData.isEmpty()) {
            return 0;
        }

        return unitOfWork.executeInTransaction(() -> {
            int importedCount = 0;
            for (Map<String, Object> bMap : booksData) {
                String title = (String) bMap.get("title");
                if (title == null || title.trim().isEmpty()) continue;

                String color = (String) bMap.getOrDefault("color", Book.DEFAULT_COLOR);
                String id = (String) bMap.get("id");

                Book book;
                if (id != null && !id.trim().isEmpty()) {
                    if (bookRepository.existsById(id.trim())) {
                        book = bookRepository.findById(id.trim()).orElse(null);
                        if (book != null) {
                            book.rename(title);
                            book.changeColor(color);
                            bookRepository.update(book);
                        }
                    } else {
                        book = Book.createWithId(id.trim(), title, color);
                        bookRepository.save(book);
                    }
                } else {
                    book = Book.create(title, color);
                    bookRepository.save(book);
                }

                if (book == null) continue;

                Object lessonsObj = bMap.get("lessons");
                if (lessonsObj instanceof List<?> lessonsList) {
                    int order = 0;
                    for (Object lItem : lessonsList) {
                        if (lItem instanceof Map<?, ?> lMap) {
                            String lTitle = (String) lMap.get("title");
                            if (lTitle == null || lTitle.trim().isEmpty()) continue;
                            String lHtml = (String) lMap.get("html");
                            String lId = (String) lMap.get("id");

                            if (lId != null && lessonRepository.existsById(lId.trim())) {
                                Lesson existing = lessonRepository.findById(lId.trim()).orElse(null);
                                if (existing != null) {
                                    existing.updateTitle(lTitle);
                                    existing.updateContent(lHtml != null ? lHtml : "");
                                    existing.updateSortOrder(order++);
                                    lessonRepository.update(existing);
                                }
                            } else {
                                Lesson newL = (lId != null && !lId.trim().isEmpty())
                                        ? Lesson.createWithId(lId.trim(), book.getId(), lTitle, lHtml, order++)
                                        : Lesson.create(book.getId(), lTitle, lHtml, order++);
                                lessonRepository.save(newL);
                            }
                        }
                    }
                }
                importedCount++;
            }
            return importedCount;
        });
    }
}
