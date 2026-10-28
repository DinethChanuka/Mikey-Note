package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;

/**
 * Customized modern Swing dropdown component with rounded corners,
 * anti-aliased vector chevron arrow, theme adaptability, and clean padding.
 */
public class ModernComboBox<T> extends JComboBox<T> implements ThemeManager.ThemeChangeListener {

    public ModernComboBox() {
        super();
        initModernStyle();
    }

    public ModernComboBox(T[] items) {
        super(items);
        initModernStyle();
    }

    public ModernComboBox(ComboBoxModel<T> model) {
        super(model);
        initModernStyle();
    }

    private void initModernStyle() {
        setUI(new ModernComboBoxUI());
        setFont(new Font("SansSerif", Font.PLAIN, 12));
        setBackground(ThemeManager.getSurface());
        setForeground(ThemeManager.getTextPrimary());
        setFocusable(true);
        ThemeManager.addListener(this);

        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(new EmptyBorder(5, 8, 5, 8));
                lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                lbl.putClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                if (isSelected) {
                    lbl.setBackground(ThemeManager.getAccent());
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(index % 2 == 0 ? ThemeManager.getSurface() : ThemeManager.getSurfaceElevated());
                    lbl.setForeground(ThemeManager.getTextPrimary());
                }

                if (value instanceof CoverDesign cd) {
                    lbl.setIcon(cd.getThumbnail(ThemeManager.getAccent()));
                    lbl.setText(cd.getDisplayName() + " (" + cd.getCategory() + ")");
                }

                return lbl;
            }
        });
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme newTheme) {
        setBackground(ThemeManager.getSurface());
        setForeground(ThemeManager.getTextPrimary());
        repaint();
    }

    private static class ModernComboBoxUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton btn = new JButton();
            btn.setBorder(BorderFactory.createEmptyBorder());
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setIcon(VectorIcons.create(VectorIcons.Type.CHEVRON_DOWN, 12, ThemeManager.getTextSecondary()));
            return btn;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(ThemeManager.getSurface());
            g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            g2.dispose();
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.hasFocus() ? ThemeManager.getAccent() : ThemeManager.getBorder());
            g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 6, 6);
            g2.dispose();
        }
    }
}
