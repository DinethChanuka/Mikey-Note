# UI Responsiveness & System Enhancement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate typing latency, optimize scrolling, enable smooth mouse-wheel zooming with subpixel text antialiasing, fix bookshelf card grid wrapping responsiveness, smooth split pane resizing, and enhance overall UI feedback.

**Architecture:** Swing / AWT Event Dispatch Thread (EDT) optimization, hardware-accelerated viewport blitting, $O(N)$ zero-allocation word/char scanning, dynamic responsive container wrapping for shelf cards, and subpixel font rendering hints.

**Tech Stack:** Java 24, Java Swing/AWT, SQLite (WAL mode).

**Spec:** System-wide UI responsiveness & enhancement specification.

## Global Constraints

- Preserve all existing 29 test suite validations in `WordBookTestSuite.java`.
- Maintain strict modularity and zero-bloat architecture.
- Follow Clean Architecture and Domain-Driven Design principles.
- Use Swing EDT best practices: never block EDT, avoid redundant repaints, respect double-buffering.

## Review Focus

- Typing latency under rapid keystrokes: verify no redundant `pageSheet.repaint()` calls on every character change.
- Scroll performance: verify hardware `BLIT_SCROLL_MODE` without redundant canvas viewport listeners.
- Responsive wrapping: verify `ResponsiveShelfPanel` recomputes preferred height when cards wrap into multiple rows.
- High-DPI text crispness: verify `KEY_TEXT_ANTIALIASING` and `KEY_FRACTIONALMETRICS` are active on editor and controls.
- Zooming: verify Ctrl/Cmd + MouseWheel adjusts zoom fluidly between 50% and 200%.

---

### Task 1: Zero-Allocation Text Scanner & Typography Anti-Aliasing in `DocumentCanvasPanel`

**Files:**
- Modify: `Source/src/main/java/com/wordbook/infrastructure/ui/swing/DocumentCanvasPanel.java`
- Test: `Source/src/test/java/com/wordbook/WordBookTestSuite.java`

**Interfaces:**
- Consumes: `DocumentCanvasPanel.countWords(String)`, `DocumentCanvasPanel.onDocChange()`
- Produces: Fast $O(N)$ zero-allocation word count; subpixel text antialiasing hints; decoupled key typing from full page sheet drop-shadow repainting.

- [x] **Step 1: Write test for word count performance and accuracy in `WordBookTestSuite.java`**
  Add unit tests validating `countWords` handles whitespace, empty strings, multiple spaces, tabs, and newlines accurately and without memory bloat.
- [x] **Step 2: Run test suite to verify test setup**
  Run: `./test.sh`
- [x] **Step 3: Implement $O(N)$ zero-allocation word counter and debounced change handler in `DocumentCanvasPanel.java`**
  - Replace `s.trim().split("\\s+").length` with loop-based counter.
  - In `onDocChange()`, remove immediate `pageSheet.repaint()` calls on every keystroke, letting `pageLayoutTimer` (150ms) handle page height adjustments.
  - In `textPane`, set `KEY_TEXT_ANTIALIASING`, `KEY_ANTIALIASING`, and `KEY_FRACTIONALMETRICS` before rendering.
- [x] **Step 4: Run test suite to verify passes**
  Run: `./test.sh`
- [x] **Step 5: Commit**

---

### Task 2: Smooth Hardware Blit Scrolling & Ctrl/Cmd + Wheel Zoom in `WordBookMainFrame`

**Files:**
- Modify: `Source/src/main/java/com/wordbook/infrastructure/ui/swing/WordBookMainFrame.java`
- Modify: `Source/src/main/java/com/wordbook/infrastructure/ui/swing/DocumentCanvasPanel.java`
- Test: `Source/src/test/java/com/wordbook/WordBookTestSuite.java`

**Interfaces:**
- Consumes: `canvasScroll.getViewport()`, `canvasPanel.setZoom(double)`
- Produces: Hardware blitting for canvas scroll; mouse wheel zooming handler; continuous layout on split panes.

- [x] **Step 1: Write test for mouse wheel zoom clamping and continuous split layout**
- [x] **Step 2: Run test to verify fail/pass**
- [x] **Step 3: Update `WordBookMainFrame.java` and `DocumentCanvasPanel.java`**
  - Switch `canvasScroll.getViewport().setScrollMode` to `JViewport.BLIT_SCROLL_MODE`.
  - Remove redundant `canvasScroll.getViewport().addChangeListener(e -> canvasPanel.repaint())`.
  - Enable `setContinuousLayout(true)` on `splitPane` and `rightSplit`.
  - Add `MouseWheelListener` on `canvasScroll` to zoom in/out with Ctrl/Cmd + wheel.
  - Optimize `setZoom` to replace or update body font size cleanly without runaway stylesheet accumulation.
- [x] **Step 4: Run test suite to verify passes**
  Run: `./test.sh`
- [x] **Step 5: Commit**

---

### Task 3: Dynamic Responsive Wrapping in `BookshelfPanel`

**Files:**
- Modify: `Source/src/main/java/com/wordbook/infrastructure/ui/swing/BookshelfPanel.java`
- Test: `Source/src/test/java/com/wordbook/WordBookTestSuite.java`

**Interfaces:**
- Consumes: `ResponsiveShelfPanel.getPreferredSize()`
- Produces: Dynamically calculated preferred height based on actual container width and component wrapping.

- [x] **Step 1: Write test for `ResponsiveShelfPanel` height calculation when wrapping across multiple rows**
- [x] **Step 2: Run test to verify failure before fix**
- [x] **Step 3: Implement dynamic row-wrapping height in `ResponsiveShelfPanel`**
  Calculate rows based on `parent.getWidth()` or component width, card dimensions, and insets/gaps.
- [x] **Step 4: Run test suite to verify passes**
  Run: `./test.sh`
- [x] **Step 5: Commit**

---

### Task 4: UI Polish & Enhancements for Sidebar, Dialogs, and Text Controls

- [x] **Step 1: In-memory lesson caching in `LessonSidebarPanel.java`**
- [x] **Step 2: Antialiased text rendering on dialogs and custom combo boxes**
- [x] **Step 3: Full test suite verification**

---

### Task 5: Memory Leak, Color Contrast & Adaptive Width Bug Fixes

- [x] **Step 1: Prevent stylesheet rule accumulation in `DocumentCanvasPanel.setZoom()` by swapping child StyleSheet**
- [x] **Step 2: Synchronize HTML text colors in `toggleHighContrastMode()`**
- [x] **Step 3: Preserve dyslexic font settings across document reloads in `loadHtml()`**
- [x] **Step 4: Make `RibbonPanel` cards container width responsive (no hardcoded 1200px limit)**
- [x] **Step 5: Add regression tests for all 3 edge cases in `WordBookTestSuite.java` (35/35 passing)**
- [x] **Step 6: Commit and push branch to `origin`**

**Files:**
- Modify: `Source/src/main/java/com/wordbook/infrastructure/ui/swing/LessonSidebarPanel.java`
- Modify: `Source/src/main/java/com/wordbook/infrastructure/ui/swing/ModernComboBox.java`
- Modify: `Source/src/main/java/com/wordbook/WordBookApplication.java`
- Test: `Source/src/test/java/com/wordbook/WordBookTestSuite.java`

**Interfaces:**
- Consumes: `ThemeManager`, `VectorIcons`
- Produces: Crisp subpixel dropdown lists, responsive sidebar titles with tooltip support, and enhanced text anti-aliasing system properties.

- [ ] **Step 1: Add tests for sidebar lesson title responsiveness and combo box rendering**
- [ ] **Step 2: Run test suite**
- [ ] **Step 3: Implement enhancements in `LessonSidebarPanel.java`, `ModernComboBox.java`, and `WordBookApplication.java`**
- [ ] **Step 4: Run full test suite and verify all 29+ test suites pass**
  Run: `./test.sh`
- [ ] **Step 5: Commit**

