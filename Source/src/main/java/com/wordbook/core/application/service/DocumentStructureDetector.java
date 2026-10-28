package com.wordbook.core.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Document Structure Engine:
 * Auto-detects topics (H1), subtopics (H2/H3), list models (numbered, bullets, task checklists),
 * key-value property models, code snippets, callouts, and tabular data from raw text.
 */
public class DocumentStructureDetector {

    private static final Pattern NUMBERED_LIST_PATTERN = Pattern.compile("^\\s*(\\d+[.)]|\\([a-zA-Z0-9]+\\)|[a-zA-Z][.)])\\s+(.+)$");
    private static final Pattern BULLET_LIST_PATTERN = Pattern.compile("^\\s*([-*+•])\\s+(.+)$");
    private static final Pattern CHECKLIST_PATTERN = Pattern.compile("^\\s*\\[([ xX])\\]\\s+(.+)$");
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^\\s*([A-Za-z0-9_\\s]{2,28}):\\s*(.+)$");

    public String autoStructureToHtml(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "<p><br></p>";
        }

        String[] lines = rawText.split("\n");
        StringBuilder sb = new StringBuilder();

        boolean inBulletList = false;
        boolean inNumberedList = false;
        List<String[]> currentKeyValueBatch = new ArrayList<>();
        boolean isFirstHeading = true;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                if (inBulletList) { sb.append("</ul>"); inBulletList = false; }
                if (inNumberedList) { sb.append("</ol>"); inNumberedList = false; }
                if (!currentKeyValueBatch.isEmpty()) {
                    sb.append(renderKeyValueCard(currentKeyValueBatch));
                    currentKeyValueBatch.clear();
                }
                continue;
            }

            // 1. Check for Markdown Table (| a | b |)
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.contains("|")) {
                if (inBulletList) { sb.append("</ul>"); inBulletList = false; }
                if (inNumberedList) { sb.append("</ol>"); inNumberedList = false; }
                if (!currentKeyValueBatch.isEmpty()) {
                    sb.append(renderKeyValueCard(currentKeyValueBatch));
                    currentKeyValueBatch.clear();
                }

                List<String> tableLines = new ArrayList<>();
                while (i < lines.length && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    tableLines.add(lines[i].trim());
                    i++;
                }
                i--; // Step back one for loop increment
                sb.append(renderMarkdownTable(tableLines));
                continue;
            }

            // 2. Check for Key-Value Model (e.g. "Status: Active", "Author: Alice")
            Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(trimmed);
            if (kvMatcher.matches() && !trimmed.toLowerCase().startsWith("http") && !trimmed.startsWith("#")) {
                if (inBulletList) { sb.append("</ul>"); inBulletList = false; }
                if (inNumberedList) { sb.append("</ol>"); inNumberedList = false; }
                currentKeyValueBatch.add(new String[]{kvMatcher.group(1).trim(), kvMatcher.group(2).trim()});
                continue;
            } else if (!currentKeyValueBatch.isEmpty()) {
                sb.append(renderKeyValueCard(currentKeyValueBatch));
                currentKeyValueBatch.clear();
            }

            // 3. Check for Checklists ([ ] or [x])
            Matcher checkMatcher = CHECKLIST_PATTERN.matcher(trimmed);
            if (checkMatcher.matches()) {
                if (inBulletList) { sb.append("</ul>"); inBulletList = false; }
                if (inNumberedList) { sb.append("</ol>"); inNumberedList = false; }
                boolean checked = !" ".equals(checkMatcher.group(1));
                String content = checkMatcher.group(2).trim();
                sb.append(String.format("<p style='margin:4px 0;'><span style='font-family:monospace;font-weight:bold;color:%s;'>[%s]</span> %s</p>",
                        checked ? "#16a34a" : "#64748b", checked ? "✓" : " ", escapeHtml(content)));
                continue;
            }

            // 4. Check for Bullet Lists
            Matcher bulletMatcher = BULLET_LIST_PATTERN.matcher(trimmed);
            if (bulletMatcher.matches()) {
                if (inNumberedList) { sb.append("</ol>"); inNumberedList = false; }
                if (!inBulletList) {
                    sb.append("<ul style='margin:8px 0;padding-left:22px;'>");
                    inBulletList = true;
                }
                sb.append("<li>").append(escapeHtml(bulletMatcher.group(2).trim())).append("</li>");
                continue;
            } else if (inBulletList) {
                sb.append("</ul>");
                inBulletList = false;
            }

            // 5. Check for Numbered Lists
            Matcher numMatcher = NUMBERED_LIST_PATTERN.matcher(trimmed);
            if (numMatcher.matches()) {
                if (inBulletList) { sb.append("</ul>"); inBulletList = false; }
                if (!inNumberedList) {
                    sb.append("<ol style='margin:8px 0;padding-left:22px;'>");
                    inNumberedList = true;
                }
                sb.append("<li>").append(escapeHtml(numMatcher.group(2).trim())).append("</li>");
                continue;
            } else if (inNumberedList) {
                sb.append("</ol>");
                inNumberedList = false;
            }

            // 6. Check for Callouts (> quote or Note: ...)
            if (trimmed.startsWith(">") || trimmed.toLowerCase().startsWith("note:") || trimmed.toLowerCase().startsWith("warning:") || trimmed.toLowerCase().startsWith("tip:")) {
                String calloutText = trimmed.replaceFirst("^[>:]+\\s*", "");
                if (trimmed.toLowerCase().startsWith("warning:")) {
                    sb.append("<div style='background-color:#fffbeb;border-left:4px solid #f59e0b;padding:10px 14px;margin:12px 0;border-radius:4px;'>")
                            .append("<font color='#92400e'><b>Caution:</b> ").append(escapeHtml(calloutText)).append("</font></div>");
                } else if (trimmed.toLowerCase().startsWith("tip:")) {
                    sb.append("<div style='background-color:#ecfdf5;border-left:4px solid #10b981;padding:10px 14px;margin:12px 0;border-radius:4px;'>")
                            .append("<font color='#065f46'><b>Pro-Tip:</b> ").append(escapeHtml(calloutText)).append("</font></div>");
                } else {
                    sb.append("<blockquote style='border-left:4px solid #2563eb;padding:8px 14px;margin:12px 0;background:#eff6ff;color:#1e40af;'>")
                            .append(escapeHtml(calloutText)).append("</blockquote>");
                }
                continue;
            }

            // 7. Check for Code Block (``` ... ``` or 4-space indent)
            if (trimmed.startsWith("```")) {
                StringBuilder code = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().startsWith("```")) {
                    code.append(escapeHtml(lines[i])).append("\n");
                    i++;
                }
                sb.append("<pre style='background:#0f172a;color:#f8fafc;padding:12px;border-radius:6px;font-family:monospace;'><code>")
                        .append(code.toString()).append("</code></pre>");
                continue;
            }

            // 8. Topic & Subtopic Detection
            if (trimmed.startsWith("# ")) {
                sb.append("<h1 style='color:#0f172a;margin-top:20px;margin-bottom:8px;'>").append(escapeHtml(trimmed.substring(2).trim())).append("</h1>");
                isFirstHeading = false;
            } else if (trimmed.startsWith("## ")) {
                sb.append("<h2 style='color:#1e293b;margin-top:16px;margin-bottom:6px;'>").append(escapeHtml(trimmed.substring(3).trim())).append("</h2>");
            } else if (trimmed.startsWith("### ")) {
                sb.append("<h3 style='color:#334155;margin-top:14px;margin-bottom:4px;'>").append(escapeHtml(trimmed.substring(4).trim())).append("</h3>");
            } else if (isFirstHeading && i == 0 && trimmed.length() < 70 && !trimmed.endsWith(".")) {
                // First line without ending dot -> Title H1
                sb.append("<h1 style='color:#0f172a;margin-top:16px;margin-bottom:8px;'>").append(escapeHtml(trimmed)).append("</h1>");
                isFirstHeading = false;
            } else if (trimmed.length() < 50 && (trimmed.startsWith("Section ") || trimmed.startsWith("Chapter ") || (trimmed.toUpperCase().equals(trimmed) && trimmed.matches(".*[A-Z].*")))) {
                // Subtopic Header H2
                sb.append("<h2 style='color:#1e293b;margin-top:16px;margin-bottom:6px;'>").append(escapeHtml(trimmed)).append("</h2>");
            } else {
                // Regular Paragraph
                sb.append("<p style='margin:8px 0;line-height:1.6;'>").append(escapeHtml(trimmed)).append("</p>");
            }
        }

        if (inBulletList) sb.append("</ul>");
        if (inNumberedList) sb.append("</ol>");
        if (!currentKeyValueBatch.isEmpty()) sb.append(renderKeyValueCard(currentKeyValueBatch));

        sb.append("<p><br></p>");
        return sb.toString();
    }

    private String renderKeyValueCard(List<String[]> pairs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table cellpadding='6' cellspacing='0' style='background-color:#f8fafc;border:1px solid #cbd5e1;border-radius:8px;margin:12px 0;width:100%;border-collapse:collapse;'>");
        for (int i = 0; i < pairs.size(); i++) {
            String[] kv = pairs.get(i);
            String bg = (i % 2 == 0) ? "#f8fafc" : "#ffffff";
            sb.append("<tr style='background-color:").append(bg).append(";'>");
            sb.append("<td style='width:30%;padding:8px 12px;font-weight:bold;color:#475569;border-bottom:1px solid #e2e8f0;'>")
                    .append(escapeHtml(kv[0])).append("</td>");
            sb.append("<td style='padding:8px 12px;color:#0f172a;border-bottom:1px solid #e2e8f0;'>")
                    .append(escapeHtml(kv[1])).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String renderMarkdownTable(List<String> lines) {
        if (lines.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;margin:14px 0;border:1px solid #cbd5e1;'>");

        boolean isHeader = true;
        for (String line : lines) {
            String trimmed = line.trim();
            // Skip separator line | --- | --- |
            if (trimmed.matches("^\\|[\\s\\-:]+\\|.*")) continue;

            String[] cells = trimmed.split("\\|");
            sb.append("<tr");
            if (isHeader) sb.append(" style='background-color:#2563eb;color:#ffffff;'");
            sb.append(">");

            for (String cell : cells) {
                if (cell.trim().isEmpty() && cell == cells[0]) continue;
                String content = escapeHtml(cell.trim());
                if (isHeader) {
                    sb.append("<th style='padding:8px;'><font color='#ffffff'>").append(content).append("</font></th>");
                } else {
                    sb.append("<td style='padding:8px;'>").append(content).append("</td>");
                }
            }
            sb.append("</tr>");
            isHeader = false;
        }

        sb.append("</table>");
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
