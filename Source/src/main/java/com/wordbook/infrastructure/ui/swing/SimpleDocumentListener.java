package com.wordbook.infrastructure.ui.swing;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Functional adapter for Swing DocumentListener allowing lambda callbacks.
 */
public record SimpleDocumentListener(Runnable callback) implements DocumentListener {
    @Override
    public void insertUpdate(DocumentEvent e) { callback.run(); }

    @Override
    public void removeUpdate(DocumentEvent e) { callback.run(); }

    @Override
    public void changedUpdate(DocumentEvent e) { callback.run(); }
}
