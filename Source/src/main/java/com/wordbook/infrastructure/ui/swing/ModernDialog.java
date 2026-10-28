package com.wordbook.infrastructure.ui.swing;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Modern Custom Modal Dialog & Alert System.
 * Appears centered in the parent window with rounded corners, vector icons,
 * theme-adaptive styling, and modern action buttons.
 */
public class ModernDialog extends JDialog {

    public enum MessageType {
        INFO, SUCCESS, WARNING, ERROR, CONFIRM
    }

    public enum Type {
        INFO, SUCCESS, WARNING, ERROR, CONFIRM
    }

    private boolean confirmed = false;
    private String inputValue = null;
    private int selectedOption = -1;

    public static void showMessage(Component parent, String title, String message, MessageType type) {
        Window owner = getWindowOwner(parent);
        ModernDialog dlg = new ModernDialog(owner, title, message, type, false, null, null);
        dlg.setVisible(true);
    }

    public static void showMessage(Component parent, String title, String message, Type type) {
        showMessage(parent, title, message, MessageType.valueOf(type.name()));
    }

    public static boolean showConfirm(Component parent, String title, String message, MessageType type) {
        Window owner = getWindowOwner(parent);
        ModernDialog dlg = new ModernDialog(owner, title, message, type, true, null, null);
        dlg.setVisible(true);
        return dlg.confirmed;
    }

    public static boolean showConfirm(Component parent, String title, String message, Type type) {
        return showConfirm(parent, title, message, MessageType.valueOf(type.name()));
    }

    public static String showInput(Component parent, String title, String message, String defaultValue) {
        Window owner = getWindowOwner(parent);
        ModernDialog dlg = new ModernDialog(owner, title, message, MessageType.INFO, true, defaultValue, null);
        dlg.setVisible(true);
        return dlg.confirmed ? dlg.inputValue : null;
    }

    public static int showOption(Component parent, String title, String message, String[] options, int defaultOption) {
        Window owner = getWindowOwner(parent);
        ModernDialog dlg = new ModernDialog(owner, title, message, MessageType.CONFIRM, false, null, options);
        dlg.selectedOption = defaultOption;
        dlg.setVisible(true);
        return dlg.selectedOption;
    }

    private static Window getWindowOwner(Component parent) {
        if (parent == null) return null;
        if (parent instanceof Window w) return w;
        return SwingUtilities.getWindowAncestor(parent);
    }

    private ModernDialog(Window owner, String title, String message, MessageType type,
                         boolean isConfirmOrInput, String defaultInput, String[] customOptions) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setResizable(false);

        boolean isDark = ThemeManager.isDark();
        Color bg = isDark ? new Color(24, 33, 47) : Color.WHITE;
        Color textPrimary = isDark ? new Color(248, 250, 252) : new Color(15, 23, 42);
        Color textSecondary = isDark ? new Color(148, 163, 184) : new Color(71, 85, 105);
        Color borderColor = isDark ? new Color(51, 65, 85) : new Color(226, 232, 240);

        JPanel root = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        setContentPane(root);

        // Header with Vector Icon and Title
        JPanel headerPanel = new JPanel(new BorderLayout(12, 0));
        headerPanel.setOpaque(false);

        Color iconColor = switch (type) {
            case SUCCESS -> new Color(22, 163, 74);
            case WARNING -> new Color(217, 119, 6);
            case ERROR -> new Color(220, 38, 38);
            case CONFIRM -> new Color(124, 58, 237);
            default -> new Color(37, 99, 235);
        };

        VectorIcons.Type iconType = switch (type) {
            case SUCCESS -> VectorIcons.Type.CHECKMARK;
            case WARNING -> VectorIcons.Type.ALERT_WARN;
            case ERROR -> VectorIcons.Type.CLOSE;
            case CONFIRM -> VectorIcons.Type.ALERT_INFO;
            default -> VectorIcons.Type.ALERT_INFO;
        };

        JLabel iconLabel = new JLabel(VectorIcons.create(iconType, 24, iconColor));
        headerPanel.add(iconLabel, BorderLayout.WEST);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(textPrimary);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        root.add(headerPanel, BorderLayout.NORTH);

        // Body Content (Message + Optional Input)
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(false);

        // HTML-wrapped message label for clean auto-wrap
        String htmlMsg = "<html><body style='width: 320px; font-family: Segoe UI, sans-serif; font-size: 12px; color: "
                + (isDark ? "#94a3b8" : "#475569") + "; line-height: 1.45;'>"
                + message.replace("\n", "<br>") + "</body></html>";
        JLabel msgLabel = new JLabel(htmlMsg);
        bodyPanel.add(msgLabel);

        JTextField inputField = null;
        if (defaultInput != null) {
            bodyPanel.add(Box.createVerticalStrut(12));
            inputField = new JTextField(defaultInput);
            inputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            inputField.setBackground(isDark ? new Color(15, 23, 42) : Color.WHITE);
            inputField.setForeground(textPrimary);
            inputField.setCaretColor(textPrimary);
            inputField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(147, 197, 253), 1, true),
                    new EmptyBorder(6, 10, 6, 10)
            ));
            inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            bodyPanel.add(inputField);
        }

        root.add(bodyPanel, BorderLayout.CENTER);

        // Bottom Action Button Row
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.setOpaque(false);

        final JTextField finalInput = inputField;

        if (customOptions != null && customOptions.length > 0) {
            for (int i = 0; i < customOptions.length; i++) {
                final int optIdx = i;
                JButton optBtn = createButton(customOptions[i], i == 0, isDark);
                optBtn.addActionListener(e -> {
                    selectedOption = optIdx;
                    dispose();
                });
                buttonRow.add(optBtn);
            }
        } else if (isConfirmOrInput) {
            JButton cancelBtn = createButton("Cancel", false, isDark);
            cancelBtn.addActionListener(e -> {
                confirmed = false;
                dispose();
            });
            buttonRow.add(cancelBtn);

            String confirmLabel = type == MessageType.ERROR || type == MessageType.WARNING ? "Delete" : "Confirm";
            if (defaultInput != null) confirmLabel = "OK";
            JButton confirmBtn = createButton(confirmLabel, true, isDark);
            if (type == MessageType.ERROR || (type == MessageType.WARNING && "Delete".equals(confirmLabel))) {
                confirmBtn.setBackground(new Color(220, 38, 38));
            }
            confirmBtn.addActionListener(e -> {
                confirmed = true;
                if (finalInput != null) {
                    inputValue = finalInput.getText().trim();
                }
                dispose();
            });
            buttonRow.add(confirmBtn);
        } else {
            JButton okBtn = createButton("OK", true, isDark);
            okBtn.addActionListener(e -> dispose());
            buttonRow.add(okBtn);
        }

        root.add(buttonRow, BorderLayout.SOUTH);

        // Keyboard navigation: Enter confirms, Escape cancels
        KeyAdapter keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmed = true;
                    if (finalInput != null) inputValue = finalInput.getText().trim();
                    dispose();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    confirmed = false;
                    dispose();
                }
            }
        };

        if (inputField != null) {
            inputField.addKeyListener(keyListener);
        }
        addKeyListener(keyListener);

        pack();
        int width = Math.max(380, getWidth());
        int height = Math.max(160, getHeight());
        setSize(width, height);

        // Center on parent window or screen
        if (owner != null && owner.isShowing()) {
            setLocationRelativeTo(owner);
        } else {
            Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((scr.width - width) / 2, (scr.height - height) / 2);
        }
    }

    private JButton createButton(String text, boolean primary, boolean isDark) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 16, 6, 16));

        if (primary) {
            btn.setBackground(new Color(37, 99, 235));
            btn.setForeground(Color.WHITE);
            btn.setOpaque(true);
            btn.setBorderPainted(false);
        } else {
            btn.setBackground(isDark ? new Color(30, 41, 59) : new Color(241, 245, 249));
            btn.setForeground(isDark ? new Color(226, 232, 240) : new Color(51, 65, 85));
            btn.setOpaque(true);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isDark ? new Color(51, 65, 85) : new Color(203, 213, 225), 1, true),
                    new EmptyBorder(5, 15, 5, 15)
            ));
        }

        return btn;
    }
}
