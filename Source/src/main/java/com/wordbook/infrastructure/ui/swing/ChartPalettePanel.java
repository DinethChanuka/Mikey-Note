package com.wordbook.infrastructure.ui.swing;

import com.wordbook.core.application.service.ChartEngineService;
import com.wordbook.core.application.service.ChartEngineService.ChartTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Colour palette chooser: a gallery of built-in themes (each drawn as the real card colours + series
 * stripes) and an editable row of custom series colours that override the theme's palette.
 */
public class ChartPalettePanel extends JPanel {
    private ChartTheme theme = ChartTheme.CORPORATE_BLUE;
    private Color[] custom = null; // null = use theme palette as-is
    private final List<ThumbCard> themeCards = new ArrayList<>();
    private final JPanel customRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JLabel customHint = new JLabel();
    private Runnable onChange = () -> {};

    public ChartPalettePanel() {
        setLayout(new BorderLayout(0, 8));
        setOpaque(false);

        JPanel grid = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        grid.setOpaque(false);
        for (ChartTheme t : ChartTheme.values()) {
            ThumbCard card = new ThumbCard(t.getDisplayName(), t.getDisplayName() + " palette", (g2, r) -> paintTheme(g2, r, t, t.getPalette()), 96, 40);
            card.setOnClick(() -> {
                theme = t;
                custom = null;
                refreshSelection();
                rebuildCustomRow();
                onChange.run();
            });
            themeCards.add(card);
            grid.add(card);
        }
        add(grid, BorderLayout.CENTER);

        JPanel customBox = new JPanel(new BorderLayout(0, 4));
        customBox.setOpaque(false);
        customBox.setBorder(new EmptyBorder(2, 6, 0, 6));
        JLabel title = new JLabel("Custom series colours — click a swatch to change it");
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(ThemeManager.getTextSecondary());
        customBox.add(title, BorderLayout.NORTH);
        customRow.setOpaque(false);
        customBox.add(customRow, BorderLayout.CENTER);
        customHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        customHint.setForeground(ThemeManager.getTextSecondary());
        customBox.add(customHint, BorderLayout.SOUTH);
        add(customBox, BorderLayout.SOUTH);

        refreshSelection();
        rebuildCustomRow();
    }

    public void setOnChange(Runnable r) { this.onChange = r == null ? () -> {} : r; }

    public ChartTheme getTheme() { return theme; }

    /** "#rrggbb,..." when the user customised the colours, otherwise null. */
    public String getPaletteHex() { return custom == null ? null : ChartEngineService.paletteToHex(custom); }

    public void setSelection(ChartTheme t, String paletteHex) {
        this.theme = t == null ? ChartTheme.CORPORATE_BLUE : t;
        this.custom = ChartEngineService.parsePalette(paletteHex);
        refreshSelection();
        rebuildCustomRow();
    }

    private void refreshSelection() {
        ChartTheme[] all = ChartTheme.values();
        for (int i = 0; i < themeCards.size(); i++) themeCards.get(i).setSelected(custom == null && all[i] == theme);
    }

    private Color[] activeColors() {
        return custom != null ? custom : theme.getPalette();
    }

    private void rebuildCustomRow() {
        customRow.removeAll();
        Color[] cols = activeColors();
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            JComponent sw = new JComponent() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(activeColors()[idx]);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.setColor(new Color(0, 0, 0, 60));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.dispose();
                }
            };
            sw.setPreferredSize(new Dimension(38, 28));
            sw.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            sw.setToolTipText("Series " + (i + 1) + " colour — click to change");
            sw.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    Color picked = ModernColorPickerDialog.showDialog(ChartPalettePanel.this, "Series " + (idx + 1) + " colour", activeColors()[idx]);
                    if (picked != null) {
                        Color[] base = activeColors().clone();
                        base[idx] = picked;
                        custom = base;
                        refreshSelection();
                        rebuildCustomRow();
                        onChange.run();
                    }
                }
            });
            customRow.add(sw);
        }
        JButton add = smallButton("+", "Add a colour");
        add.addActionListener(e -> {
            Color[] base = activeColors();
            if (base.length >= 8) return;
            Color[] next = java.util.Arrays.copyOf(base, base.length + 1);
            next[base.length] = base[base.length % base.length].darker();
            custom = next;
            refreshSelection();
            rebuildCustomRow();
            onChange.run();
        });
        JButton del = smallButton("−", "Remove the last colour");
        del.addActionListener(e -> {
            Color[] base = activeColors();
            if (base.length <= 2) return;
            custom = java.util.Arrays.copyOf(base, base.length - 1);
            refreshSelection();
            rebuildCustomRow();
            onChange.run();
        });
        JButton reset = smallButton("Reset", "Back to the theme's own colours");
        reset.addActionListener(e -> {
            custom = null;
            refreshSelection();
            rebuildCustomRow();
            onChange.run();
        });
        customRow.add(add);
        customRow.add(del);
        customRow.add(reset);
        customHint.setText(custom == null ? "Using the “" + theme.getDisplayName() + "” theme colours."
                : "Custom colours active — cards keep the “" + theme.getDisplayName() + "” surface.");
        customRow.revalidate();
        customRow.repaint();
    }

    private static JButton smallButton(String text, String tip) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setFocusPainted(false);
        b.setToolTipText(tip);
        b.setMargin(new Insets(2, 8, 2, 8));
        return b;
    }

    /** Draws a theme as it appears on a chart card: surface colour + a mini bar chart in the series colours. */
    public static void paintTheme(Graphics2D g2, Rectangle r, ChartTheme t, Color[] colors) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(t.getBackground());
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(t.getBorder());
        g2.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        int n = Math.min(colors.length, 5);
        int pad = 8;
        int slot = (r.width - pad * 2) / n;
        int bw = Math.max(6, slot - 5);
        int[] hs = {14, 24, 18, 28, 20};
        for (int i = 0; i < n; i++) {
            int bh = Math.min(r.height - 10, hs[i % hs.length] * r.height / 40);
            g2.setColor(colors[i]);
            g2.fillRoundRect(r.x + pad + i * slot, r.y + r.height - 5 - bh, bw, bh, 4, 4);
        }
    }

    /** Opens the palette picker in a small modal dialog. Returns true when the user pressed Apply. */
    public static boolean showDialog(Component parent, ChartPaletteChoice choice) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dlg = new JDialog(owner, "Chart Colour Palette", Dialog.ModalityType.APPLICATION_MODAL);
        ChartPalettePanel panel = new ChartPalettePanel();
        panel.setSelection(choice.theme, choice.paletteHex);
        panel.setBorder(new EmptyBorder(12, 12, 6, 12));
        boolean[] ok = {false};
        JButton apply = new JButton("Apply palette");
        apply.setFont(new Font("Segoe UI", Font.BOLD, 12));
        apply.addActionListener(e -> { ok[0] = true; dlg.dispose(); });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dlg.dispose());
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bar.add(cancel);
        bar.add(apply);
        dlg.setLayout(new BorderLayout());
        dlg.add(panel, BorderLayout.CENTER);
        dlg.add(bar, BorderLayout.SOUTH);
        dlg.setSize(560, 400);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
        if (ok[0]) {
            choice.theme = panel.getTheme();
            choice.paletteHex = panel.getPaletteHex();
        }
        return ok[0];
    }

    /** Mutable holder used by {@link #showDialog}. */
    public static final class ChartPaletteChoice {
        public ChartTheme theme;
        public String paletteHex;
        public ChartPaletteChoice(ChartTheme theme, String paletteHex) {
            this.theme = theme;
            this.paletteHex = paletteHex;
        }
    }
}
