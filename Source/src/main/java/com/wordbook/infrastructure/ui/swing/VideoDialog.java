package com.wordbook.infrastructure.ui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

/**
 * Modern Swing dialog for embedding Web and Local Video Media Cards.
 */
public class VideoDialog extends JDialog {
    private JTextField titleField;
    private JTextField urlField;
    private JComboBox<String> aspectCombo;
    private boolean confirmed = false;

    public VideoDialog(Frame owner) {
        super(owner, "Insert Video Media", true);
        setSize(460, 310);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(18, 22, 16, 22));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Row 0: Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        form.add(new JLabel("Video Title:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.8;
        titleField = new JTextField("Document Video Overview");
        titleField.setPreferredSize(new Dimension(240, 28));
        form.add(titleField, gbc);

        // Row 1: URL / Path
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        form.add(new JLabel("Video URL / File:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.8;
        JPanel urlPanel = new JPanel(new BorderLayout(6, 0));
        urlField = new JTextField("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        urlField.setPreferredSize(new Dimension(200, 28));
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Video File");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Videos (MP4, MKV, MOV, WEBM, AVI)", "mp4", "mkv", "mov", "webm", "avi"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                urlField.setText(f.getAbsolutePath());
                if (titleField.getText().isBlank() || titleField.getText().equals("Document Video Overview")) {
                    titleField.setText(f.getName());
                }
            }
        });
        urlPanel.add(urlField, BorderLayout.CENTER);
        urlPanel.add(browseBtn, BorderLayout.EAST);
        form.add(urlPanel, gbc);

        // Row 2: Aspect Ratio
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.2;
        form.add(new JLabel("Aspect Ratio:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.8;
        aspectCombo = new JComboBox<>(new String[]{"16:9 Widescreen (Modern)", "4:3 Standard (Classic)"});
        aspectCombo.setPreferredSize(new Dimension(240, 28));
        form.add(aspectCombo, gbc);

        // Row 3: Helper Hint
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JLabel hintLabel = new JLabel("Supports YouTube, Vimeo, direct MP4 links, or local video files.");
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintLabel.setForeground(new Color(100, 116, 139));
        form.add(hintLabel, gbc);

        add(form, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton insertBtn = new JButton("Insert Video", VectorIcons.createIcon(VectorIcons.VIDEO, 14, Color.WHITE));
        insertBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        insertBtn.setBackground(new Color(225, 29, 72));
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

    public String generateVideoHtml() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) title = "Embedded Video";
        String url = urlField.getText().trim();
        if (url.isEmpty()) url = "https://www.youtube.com";

        boolean isLocal = !url.startsWith("http://") && !url.startsWith("https://");
        String platform = isLocal ? "Local Video Player" : (url.contains("youtu") ? "YouTube" : (url.contains("vimeo") ? "Vimeo" : "Web Video"));

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='wb-video-card' data-video-url='").append(url.replace("'", "&apos;")).append("' style='border:1px solid #cbd5e1;background:#0f172a;border-radius:10px;padding:16px 20px;margin:14px auto;color:#f8fafc;max-width:100%;box-sizing:border-box;'>");
        sb.append("<div style='font-size:12px;color:#94a3b8;margin-bottom:6px;'>🎬 <b>VIDEO MEDIA</b> &bull; ").append(platform).append("</div>");
        sb.append("<div style='font-size:16px;font-weight:bold;color:#ffffff;margin-bottom:8px;'>").append(escapeHtml(title)).append("</div>");
        sb.append("<div style='background:#1e293b;padding:10px 14px;border-radius:6px;font-size:13px;color:#38bdf8;'>▶ Click or Launch to Play: ").append(escapeHtml(url)).append("</div>");
        sb.append("</div><p><br></p>");
        return sb.toString();
    }

    private String escapeHtml(String str) {
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
