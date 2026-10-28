package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Swing dialog for configuring and inserting tables into notes.
 */
public class TableDialog extends JDialog {
    private JSpinner rowsSpinner;
    private JSpinner colsSpinner;
    private JCheckBox headerCheck;
    private boolean confirmed = false;

    public TableDialog(Frame owner) {
        super(owner, "Insert Table", true);
        setSize(320, 240);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 12));
        form.setBorder(new EmptyBorder(16, 20, 16, 20));

        form.add(new JLabel("Rows (1 - 30):"));
        rowsSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 30, 1));
        form.add(rowsSpinner);

        form.add(new JLabel("Columns (1 - 12):"));
        colsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 12, 1));
        form.add(colsSpinner);

        form.add(new JLabel("Header Row:"));
        headerCheck = new JCheckBox("Highlight First Row", true);
        form.add(headerCheck);

        add(form, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton insertBtn = new JButton("Insert Table", VectorIcons.createIcon(VectorIcons.TABLE, 14, Color.WHITE));
        insertBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        insertBtn.setBackground(new Color(37, 99, 235));
        insertBtn.setForeground(Color.WHITE);
        insertBtn.setOpaque(true);
        insertBtn.setBorderPainted(false);
        insertBtn.setFocusPainted(false);
        insertBtn.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(insertBtn);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String generateTableHtml() {
        int r = (Integer) rowsSpinner.getValue();
        int c = (Integer) colsSpinner.getValue();
        boolean h = headerCheck.isSelected();

        StringBuilder sb = new StringBuilder("<table align='center' border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;margin:14px auto;border:1px solid #cbd5e1;'><tbody>");
        for (int i = 0; i < r; i++) {
            sb.append("<tr>");
            for (int j = 0; j < c; j++) {
                if (i == 0 && h) {
                    sb.append("<th style='background-color:#f1f5f9;padding:8px 12px;font-weight:bold;color:#0f172a;border:1px solid #cbd5e1;'>Header ").append(j + 1).append("</th>");
                } else {
                    sb.append("<td style='padding:8px 12px;color:#334155;border:1px solid #cbd5e1;'>&nbsp;</td>");
                }
            }
            sb.append("</tr>");
        }
        sb.append("</tbody></table><p><br></p>");
        return sb.toString();
    }
}
