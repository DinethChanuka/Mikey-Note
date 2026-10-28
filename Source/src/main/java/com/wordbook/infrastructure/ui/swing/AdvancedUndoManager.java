package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import javax.swing.undo.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Advanced High-Level Undo/Redo Manager with named operations,
 * 200-step memory, coalesced typing groups, history dropdown UI.
 */
public class AdvancedUndoManager extends UndoManager {

    private static final int MAX_STEPS = 200;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static class UndoEntry {
        public final String name;
        public final String timestamp;
        public final boolean isTyping;
        public UndoEntry(String name, String timestamp, boolean isTyping) {
            this.name = name; this.timestamp = timestamp; this.isTyping = isTyping;
        }
    }

    private final List<UndoEntry> history = new ArrayList<>();
    private int currentIndex = -1;
    private final List<Runnable> stateListeners = new ArrayList<>();
    private int coalesceCount = 0;
    private static final int COALESCE_THRESHOLD = 15;

    @Override
    public synchronized boolean addEdit(UndoableEdit anEdit) {
        boolean added = super.addEdit(anEdit);
        if (added) {
            // Trim beyond max
            if (history.size() >= MAX_STEPS) history.remove(0);
            // Trim redo tail
            if (currentIndex < history.size() - 1)
                history.subList(currentIndex + 1, history.size()).clear();

            String name = anEdit.getPresentationName();
            boolean isTyping = "typing".equalsIgnoreCase(name) || name.isEmpty();

            if (isTyping && !history.isEmpty()
                    && history.get(history.size() - 1).isTyping
                    && coalesceCount < COALESCE_THRESHOLD) {
                coalesceCount++;
            } else {
                if (isTyping) coalesceCount = 0;
                history.add(new UndoEntry(humanize(name), LocalTime.now().format(TIME_FMT), isTyping));
                currentIndex = history.size() - 1;
            }
            notifyListeners();
        }
        return added;
    }

    @Override
    public synchronized void undo() throws CannotUndoException {
        if (canUndo()) {
            try {
                super.undo();
            } catch (Exception ignored) {}
        }
        if (currentIndex >= 0) currentIndex--;
        notifyListeners();
    }

    @Override
    public synchronized void redo() throws CannotRedoException {
        if (canRedo()) {
            try {
                super.redo();
            } catch (Exception ignored) {}
        }
        if (currentIndex < history.size() - 1) currentIndex++;
        notifyListeners();
    }

    @Override
    public synchronized void discardAllEdits() {
        super.discardAllEdits();
        history.clear();
        currentIndex = -1;
        notifyListeners();
    }

    public synchronized void registerNamedAction(String name) {
        if (currentIndex < history.size() - 1)
            history.subList(currentIndex + 1, history.size()).clear();
        if (history.size() >= MAX_STEPS) history.remove(0);
        history.add(new UndoEntry(name, LocalTime.now().format(TIME_FMT), false));
        currentIndex = history.size() - 1;
        super.addEdit(new AbstractUndoableEdit() {
            @Override
            public String getPresentationName() {
                return name;
            }
        });
        notifyListeners();
    }

    public boolean canUndoAction() { return canUndo() || currentIndex >= 0; }
    public boolean canRedoAction() { return canRedo() || (currentIndex < history.size() - 1); }

    public String getUndoName() {
        if (!canUndoAction() || currentIndex < 0 || currentIndex >= history.size()) return "Nothing to undo";
        return history.get(currentIndex).name;
    }

    public String getRedoName() {
        if (!canRedoAction()) return "Nothing to redo";
        int next = currentIndex + 1;
        return next < history.size() ? history.get(next).name : "Redo";
    }

    public List<UndoEntry> getUndoHistory(int maxItems) {
        int end = currentIndex + 1;
        int start = Math.max(0, end - maxItems);
        List<UndoEntry> slice = new ArrayList<>(history.subList(start, end));
        Collections.reverse(slice);
        return slice;
    }

    public List<UndoEntry> getRedoHistory(int maxItems) {
        int start = currentIndex + 1;
        int end = Math.min(history.size(), start + maxItems);
        return new ArrayList<>(history.subList(start, end));
    }

    public void addStateListener(Runnable r) { stateListeners.add(r); }

    private void notifyListeners() {
        SwingUtilities.invokeLater(() -> stateListeners.forEach(Runnable::run));
    }

    public void showHistoryPopup(Component anchor, boolean isUndo, Runnable action) {
        List<UndoEntry> entries = isUndo ? getUndoHistory(15) : getRedoHistory(15);
        if (entries.isEmpty()) return;
        ModernPopupMenu popup = new ModernPopupMenu();
        popup.addHeader(isUndo ? "Undo History" : "Redo History");
        popup.addModernSeparator();
        for (int i = 0; i < entries.size(); i++) {
            UndoEntry e = entries.get(i);
            final int stepCount = i + 1;
            popup.addModernItem(e.name + "   " + e.timestamp, VectorIcons.createIcon(isUndo ? VectorIcons.Type.UNDO : VectorIcons.Type.REDO, 11, new Color(100, 116, 139)), ev -> {
                for (int s = 0; s < stepCount; s++) action.run();
            });
        }
        popup.show(anchor, 0, anchor.getHeight());
    }

    private static String humanize(String name) {
        if (name == null || name.isBlank()) return "Text Edit";
        return switch (name.toLowerCase()) {
            case "typing"           -> "Typed Text";
            case "addition"         -> "Text Added";
            case "deletion"         -> "Text Deleted";
            default                 -> capitalize(name);
        };
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
