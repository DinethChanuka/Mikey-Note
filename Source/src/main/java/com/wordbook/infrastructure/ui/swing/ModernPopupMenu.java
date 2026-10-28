package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.plaf.basic.BasicMenuUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

/**
 * ModernPopupMenu provides a compact, premium, macOS/Modern Studio styled context menu.
 * Features:
 * - Anti-aliased 10px rounded borders with subtle depth shadow and solid elevated surface.
 * - Uniform background colors across root context menus AND sub-menus.
 * - Reduced width with sleek compact padding, 27-28px item height, and smooth pill hover.
 * - Modern typography, vector chevrons for submenus, styled category headers, and elegant dividers.
 * - Full theme awareness (Light & Dark mode support).
 */
public class ModernPopupMenu extends JPopupMenu {

    public ModernPopupMenu() {
        initPopupMenu(this);
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
        if (comp instanceof JComponent jc) {
            jc.setAlignmentX(0.0f);
        }
    }

    public static void initPopupMenu(JPopupMenu popup) {
        popup.setOpaque(false);
        popup.setUI(new ModernPopupMenuUI());
        popup.setBorder(new ModernMenuBorder());
        Color bg = ThemeManager.isDark() ? new Color(15, 23, 42) : ThemeManager.getSurface();
        popup.setBackground(bg);

        // Ensure transparent window and container hierarchy so the solid rounded popup displays cleanly
        popup.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && popup.isShowing()) {
                applyWindowTransparency(popup);
            }
        });

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                applyWindowTransparency(popup);
                SwingUtilities.invokeLater(() -> applyWindowTransparency(popup));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });
    }

    public static void applyWindowTransparency(Component comp) {
        if (comp == null) return;
        Color bg = ThemeManager.isDark() ? new Color(15, 23, 42) : ThemeManager.getSurface();
        Window w = SwingUtilities.getWindowAncestor(comp);
        if (w != null) {
            try {
                w.setBackground(new Color(0, 0, 0, 0));
            } catch (Exception ignored) {}
            if (w instanceof RootPaneContainer rpc) {
                try {
                    rpc.getRootPane().setOpaque(false);
                    if (rpc.getContentPane() instanceof JComponent jc) {
                        jc.setOpaque(false);
                        jc.setBackground(bg);
                    }
                } catch (Exception ignored) {}
            }
        }
        Container p = comp.getParent();
        while (p != null && !(p instanceof Window)) {
            if (p instanceof JComponent jc) {
                jc.setOpaque(false);
                jc.setBackground(bg);
            }
            p = p.getParent();
        }
    }

    public static void paintModernPopupBackground(Graphics g, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = 10;
        boolean dark = ThemeManager.isDark();
        Color bg = dark ? new Color(15, 23, 42) : ThemeManager.getSurface();
        Color border = dark ? new Color(51, 65, 85) : ThemeManager.getBorder();
        Color shadow = dark ? new Color(0, 0, 0, 60) : new Color(15, 23, 42, 20);

        // Soft outer shadow simulation
        g2.setColor(shadow);
        g2.drawRoundRect(0, 1, w - 1, h - 1, arc + 2, arc + 2);

        // Solid rounded elevated background
        g2.setColor(bg);
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        // Crisp border outline
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        paintModernPopupBackground(g, getWidth(), getHeight());
    }

    /**
     * Creates and adds a modern uppercase category header with an accent dot.
     */
    public JLabel addHeader(String text) {
        String cleanText = text.trim();
        if (cleanText.startsWith("•") || cleanText.startsWith("●")) {
            cleanText = cleanText.substring(1).trim();
        }
        JLabel hdr = new JLabel(cleanText.toUpperCase());
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 10));
        hdr.setForeground(new Color(37, 99, 235));
        hdr.setAlignmentX(0.0f);
        hdr.setBorder(new EmptyBorder(4, 6, 2, 6));
        hdr.setIcon(new HeaderDotIcon(new Color(37, 99, 235)));
        hdr.setIconTextGap(5);
        add(hdr);
        return hdr;
    }

    /**
     * Adds a thin elegant 1px separator with balanced vertical margin.
     */
    public void addModernSeparator() {
        JSeparator sep = new JSeparator() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Color c = ThemeManager.isDark() ? new Color(51, 65, 85) : new Color(226, 232, 240);
                g2.setColor(c);
                int y = getHeight() / 2;
                g2.drawLine(5, y, getWidth() - 5, y);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(0, 5);
            }
        };
        sep.setAlignmentX(0.0f);
        add(sep);
    }

    /**
     * Adds a standard interactive menu item with snug width, smooth hover pill, and modern typography.
     */
    public JMenuItem addModernItem(String text, Icon icon, ActionListener action) {
        JMenuItem item = new JMenuItem(text, icon);
        styleMenuItem(item, false);
        if (action != null) item.addActionListener(action);
        add(item);
        return item;
    }

    /**
     * Adds a destructive/danger menu item (e.g. Delete) with soft red styling.
     */
    public JMenuItem addDangerItem(String text, Icon icon, ActionListener action) {
        JMenuItem item = new JMenuItem(text, icon);
        styleMenuItem(item, true);
        if (action != null) item.addActionListener(action);
        add(item);
        return item;
    }

    /**
     * Creates a submenu with ModernMenuUI, solid background popup, and vector chevron arrow.
     */
    public ModernMenu createSubMenu(String text, Icon icon) {
        ModernMenu menu = new ModernMenu(text, icon);
        add(menu);
        return menu;
    }

    /**
     * Styles an existing JMenuItem with compact modern UI, snug dimensions, and typography.
     */
    public static void styleMenuItem(JMenuItem item, boolean isDanger) {
        item.setUI(new ModernMenuItemUI(isDanger));
        item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        item.setOpaque(false);
        item.setAlignmentX(0.0f);
        item.setBorder(new EmptyBorder(0, 5, 0, 5));
        item.setIconTextGap(5);
        if (isDanger) {
            item.setForeground(new Color(220, 38, 38));
        } else {
            item.setForeground(ThemeManager.getTextPrimary());
        }
    }

    /**
     * Recursively styles any existing JPopupMenu, JMenuItem, or JMenu tree to compact modern styling.
     */
    public static void applyModernStyle(JPopupMenu popup) {
        initPopupMenu(popup);
        for (Component c : popup.getComponents()) {
            if (c instanceof JComponent jc) {
                jc.setAlignmentX(0.0f);
            }
            if (c instanceof ModernMenu mm) {
                applyModernStyle(mm.getPopupMenu());
            } else if (c instanceof JMenu jm) {
                jm.setUI(new ModernMenuUI());
                jm.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                jm.setOpaque(false);
                jm.setAlignmentX(0.0f);
                jm.setBorder(new EmptyBorder(0, 5, 0, 5));
                jm.setIconTextGap(5);
                applyModernStyle(jm.getPopupMenu());
            } else if (c instanceof JMenuItem mi) {
                boolean danger = mi.getText() != null && mi.getText().toLowerCase().contains("delete");
                styleMenuItem(mi, danger);
            }
        }
    }

    // =========================================================================
    // SUB-COMPONENTS & UIS
    // =========================================================================

    public static class ModernPopupMenuUI extends BasicPopupMenuUI {
        @Override
        public void update(Graphics g, JComponent c) {
            paintModernPopupBackground(g, c.getWidth(), c.getHeight());
            paint(g, c);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            paintModernPopupBackground(g, c.getWidth(), c.getHeight());
        }
    }

    public static class ModernMenu extends JMenu {
        public ModernMenu(String text, Icon icon) {
            super(text);
            if (icon != null) setIcon(icon);
            setUI(new ModernMenuUI());
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
            setOpaque(false);
            setAlignmentX(0.0f);
            setBorder(new EmptyBorder(0, 5, 0, 5));
            setIconTextGap(5);
            initPopupMenu(getPopupMenu());
        }

        @Override
        public JPopupMenu getPopupMenu() {
            JPopupMenu popup = super.getPopupMenu();
            initPopupMenu(popup);
            return popup;
        }

        @Override
        protected void addImpl(Component comp, Object constraints, int index) {
            super.addImpl(comp, constraints, index);
            if (comp instanceof JComponent jc) {
                jc.setAlignmentX(0.0f);
            }
        }

        public JMenuItem addModernItem(String text, Icon icon, ActionListener action) {
            JMenuItem item = new JMenuItem(text, icon);
            styleMenuItem(item, false);
            if (action != null) item.addActionListener(action);
            add(item);
            return item;
        }

        public JMenuItem addDangerItem(String text, Icon icon, ActionListener action) {
            JMenuItem item = new JMenuItem(text, icon);
            styleMenuItem(item, true);
            if (action != null) item.addActionListener(action);
            add(item);
            return item;
        }

        public ModernMenu createSubMenu(String text, Icon icon) {
            ModernMenu sub = new ModernMenu(text, icon);
            add(sub);
            return sub;
        }
    }

    public static class ModernMenuItemUI extends BasicMenuItemUI {
        private final boolean isDanger;

        public ModernMenuItemUI(boolean isDanger) {
            this.isDanger = isDanger;
            this.defaultTextIconGap = 5;
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            Dimension d = super.getPreferredSize(c);
            if (d != null) {
                d.height = Math.max(d.height, 27);
            }
            return d;
        }

        @Override
        protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
            ButtonModel model = menuItem.getModel();
            if (model.isArmed() || (menuItem instanceof JMenu && model.isSelected())) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean dark = ThemeManager.isDark();
                Color hoverBg = isDanger
                        ? (dark ? new Color(69, 26, 26) : new Color(254, 242, 242))
                        : (dark ? new Color(30, 41, 59) : new Color(241, 245, 249));

                Color hoverBorder = isDanger
                        ? (dark ? new Color(127, 29, 29) : new Color(254, 202, 202))
                        : (dark ? new Color(51, 65, 85) : new Color(226, 232, 240));

                g2.setColor(hoverBg);
                g2.fillRoundRect(2, 1, menuItem.getWidth() - 4, menuItem.getHeight() - 2, 6, 6);

                g2.setColor(hoverBorder);
                g2.drawRoundRect(2, 1, menuItem.getWidth() - 5, menuItem.getHeight() - 3, 6, 6);
                g2.dispose();
            }
        }

        @Override
        protected void paintText(Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
            ButtonModel model = menuItem.getModel();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(menuItem.getFont());

            FontMetrics fm = g2.getFontMetrics();
            int textY = textRect.y + fm.getAscent();

            boolean armed = model.isArmed() || (menuItem instanceof JMenu && model.isSelected());
            boolean dark = ThemeManager.isDark();

            Color textColor;
            if (!menuItem.isEnabled()) {
                textColor = dark ? new Color(100, 116, 139) : new Color(148, 163, 184);
            } else if (isDanger) {
                textColor = armed ? new Color(220, 38, 38) : new Color(239, 68, 68);
            } else if (armed) {
                textColor = new Color(37, 99, 235);
            } else {
                textColor = ThemeManager.getTextPrimary();
            }

            g2.setColor(textColor);
            g2.drawString(text, textRect.x, textY);
            g2.dispose();
        }
    }

    public static class ModernMenuUI extends BasicMenuUI {
        public ModernMenuUI() {
            this.arrowIcon = new ChevronIcon(10);
            this.defaultTextIconGap = 5;
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            Dimension d = super.getPreferredSize(c);
            if (d != null) {
                d.height = Math.max(d.height, 27);
            }
            return d;
        }

        @Override
        protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
            ButtonModel model = menuItem.getModel();
            if (model.isArmed() || model.isSelected()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean dark = ThemeManager.isDark();
                Color hoverBg = dark ? new Color(30, 41, 59) : new Color(241, 245, 249);
                Color hoverBorder = dark ? new Color(51, 65, 85) : new Color(226, 232, 240);

                g2.setColor(hoverBg);
                g2.fillRoundRect(2, 1, menuItem.getWidth() - 4, menuItem.getHeight() - 2, 6, 6);

                g2.setColor(hoverBorder);
                g2.drawRoundRect(2, 1, menuItem.getWidth() - 5, menuItem.getHeight() - 3, 6, 6);
                g2.dispose();
            }
        }

        @Override
        protected void paintText(Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
            ButtonModel model = menuItem.getModel();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(menuItem.getFont());

            FontMetrics fm = g2.getFontMetrics();
            int textY = textRect.y + fm.getAscent();

            boolean armed = model.isArmed() || model.isSelected();
            boolean dark = ThemeManager.isDark();

            Color textColor;
            if (!menuItem.isEnabled()) {
                textColor = dark ? new Color(100, 116, 139) : new Color(148, 163, 184);
            } else if (armed) {
                textColor = new Color(37, 99, 235);
            } else {
                textColor = ThemeManager.getTextPrimary();
            }

            g2.setColor(textColor);
            g2.drawString(text, textRect.x, textY);
            g2.dispose();
        }
    }

    public static class ChevronIcon implements Icon {
        private final int size;

        public ChevronIcon(int size) {
            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            boolean isArmed = false;
            if (c instanceof AbstractButton b) {
                isArmed = b.getModel().isArmed() || b.getModel().isSelected();
            }

            g2.setColor(isArmed ? new Color(37, 99, 235) : new Color(148, 163, 184));
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int midY = y + size / 2;
            int startX = x + 1;
            g2.drawLine(startX, midY - 3, startX + 3, midY);
            g2.drawLine(startX + 3, midY, startX, midY + 3);

            g2.dispose();
        }
    }

    private static class HeaderDotIcon implements Icon {
        private final Color color;

        public HeaderDotIcon(Color color) {
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return 5;
        }

        @Override
        public int getIconHeight() {
            return 5;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y + 1, 4, 4);
            g2.dispose();
        }
    }

    public static class ModernMenuBorder implements Border {
        private final Insets insets = new Insets(3, 2, 3, 2);

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            // Painted by ModernPopupMenu.paintComponent and ModernPopupMenuUI
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return insets;
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
