package com.wordbook.core.domain.repository;

import com.wordbook.core.domain.model.Lesson;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for Lesson persistence operations.
 */
public interface LessonRepository {
    void save(Lesson lesson);
    Optional<Lesson> findById(String id);
    List<Lesson> findByBookId(String bookId);
    void update(Lesson lesson);
    boolean deleteById(String id);
    int deleteByBookId(String bookId);
    boolean existsById(String id);
    int count();
    long sumTotalWords();
    long sumTotalCharacters();
}
