package com.wordbook.core.domain.repository;

import com.wordbook.core.domain.model.Book;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for Book persistence operations.
 */
public interface BookRepository {
    void save(Book book);
    Optional<Book> findById(String id);
    List<Book> findAll();
    void update(Book book);
    boolean deleteById(String id);
    boolean existsById(String id);
    int count();
}
