package com.wordbook.core.application.service;

import com.wordbook.core.domain.exception.EntityNotFoundException;
import com.wordbook.core.domain.model.Book;
import com.wordbook.core.domain.model.Lesson;
import com.wordbook.core.domain.repository.BookRepository;
import com.wordbook.core.domain.repository.LessonRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Application service for exporting notes, lessons, and entire books
 * to standalone standalone HTML, Markdown, and formatted documents.
 */
public class ExportService {
    private final BookRepository bookRepository;
    private final LessonRepository lessonRepository;

    public ExportService(BookRepository bookRepository, LessonRepository lessonRepository) {
        this.bookRepository = bookRepository;
        this.lessonRepository = lessonRepository;
    }

    public record ExportResult(String filename, String contentType, byte[] content) {}

    public ExportResult exportLessonAsHtml(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson", lessonId));
        Book book = bookRepository.findById(lesson.getBookId()).orElse(null);
        String bookTitle = book != null ? book.getTitle() : "Mikey Note";

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s - %s</title>
                  <style>
                    :root { --font-main: Calibri, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
                    body {
                      font-family: var(--font-main);
                      font-size: 14px;
                      line-height: 1.5;
                      color: #222;
                      background: #f7f9fa;
                      margin: 0;
                      padding: 24px;
                    }
                    .document-container {
                      max-width: 794px;
                      min-height: 1050px;
                      margin: 0 auto;
                      background: #ffffff;
                      padding: 72px 76px;
                      box-shadow: 0 4px 16px rgba(0,0,0,0.1);
                      border-radius: 4px;
                      box-sizing: border-box;
                    }
                    table { border-collapse: collapse; width: 100%%; margin: 14px 0; }
                    td, th { border: 1px solid #888; padding: 7px 10px; vertical-align: top; }
                    th { background-color: #dbeafe; font-weight: bold; }
                    blockquote { border-left: 4px solid #2b579a; margin: 12px 0; padding: 4px 14px; color: #555; background: #f8fafc; }
                    pre { background: #f1f5f9; padding: 12px; border-radius: 6px; overflow-x: auto; font-family: monospace; }
                    img { max-width: 100%%; height: auto; border-radius: 4px; }
                    .pbk { page-break-after: always; border-top: 1px dashed #cbd5e1; margin: 32px 0; }
                    .chart { margin: 16px auto; text-align: center; }
                    @media print {
                      body { background: #fff; padding: 0; }
                      .document-container { box-shadow: none; padding: 0; margin: 0; max-width: 100%%; }
                      .pbk { border: none; }
                    }
                  </style>
                </head>
                <body>
                  <div class="document-container">
                    %s
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(bookTitle),
                escapeHtml(lesson.getTitle()),
                lesson.getContentHtml()
        );

        String safeTitle = lesson.getTitle().replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replace(' ', '_');
        if (safeTitle.isEmpty()) safeTitle = "lesson_" + lesson.getId();
        return new ExportResult(safeTitle + ".html", "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    public ExportResult exportLessonAsMarkdown(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson", lessonId));

        String md = convertHtmlToMarkdown(lesson.getTitle(), lesson.getContentHtml());
        String safeTitle = lesson.getTitle().replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replace(' ', '_');
        if (safeTitle.isEmpty()) safeTitle = "lesson_" + lesson.getId();
        return new ExportResult(safeTitle + ".md", "text/markdown; charset=UTF-8", md.getBytes(StandardCharsets.UTF_8));
    }

    public ExportResult exportBookAsHtml(String bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book", bookId));
        List<Lesson> lessons = lessonRepository.findByBookId(bookId);

        StringBuilder body = new StringBuilder();
        body.append("<header style='text-align:center; padding: 40px 0 60px;'>");
        body.append("<h1 style='font-size:2.6em; color:").append(book.getHexColor()).append(";'>").append(escapeHtml(book.getTitle())).append("</h1>");
        body.append("<p style='color:#666;'>Mikey Note Export &bull; ").append(lessons.size()).append(" Lessons</p>");
        body.append("</header>");

        for (int i = 0; i < lessons.size(); i++) {
            Lesson l = lessons.get(i);
            body.append("<section class='lesson-section'>");
            body.append(l.getContentHtml());
            body.append("</section>");
            if (i < lessons.size() - 1) {
                body.append("<div class='pbk'></div>");
            }
        }

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <title>%s</title>
                  <style>
                    body { font-family: Calibri, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 14px; line-height: 1.5; color: #222; background: #fff; max-width: 820px; margin: 30px auto; padding: 20px; }
                    table { border-collapse: collapse; width: 100%%; margin: 12px 0; }
                    td, th { border: 1px solid #888; padding: 6px 10px; }
                    th { background: #dbeafe; }
                    .pbk { page-break-after: always; margin: 40px 0; }
                    @media print { .pbk { border: none; } }
                  </style>
                </head>
                <body>
                  %s
                </body>
                </html>
                """.formatted(escapeHtml(book.getTitle()), body.toString());

        String safeTitle = book.getTitle().replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replace(' ', '_');
        if (safeTitle.isEmpty()) safeTitle = "book_" + book.getId();
        return new ExportResult(safeTitle + ".html", "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
    }

    private String convertHtmlToMarkdown(String title, String html) {
        if (html == null) return "# " + title + "\n";
        String s = html;
        s = s.replaceAll("<h1>(.*?)</h1>", "\n# $1\n\n");
        s = s.replaceAll("<h2>(.*?)</h2>", "\n## $1\n\n");
        s = s.replaceAll("<h3>(.*?)</h3>", "\n### $1\n\n");
        s = s.replaceAll("<b>(.*?)</b>", "**$1**");
        s = s.replaceAll("<strong>(.*?)</strong>", "**$1**");
        s = s.replaceAll("<i>(.*?)</i>", "*$1*");
        s = s.replaceAll("<em>(.*?)</em>", "*$1*");
        s = s.replaceAll("<u>(.*?)</u>", "_$1_");
        s = s.replaceAll("<s>(.*?)</s>", "~~$1~~");
        s = s.replaceAll("<blockquote>(.*?)</blockquote>", "\n> $1\n");
        s = s.replaceAll("<pre>(.*?)</pre>", "\n```\n$1\n```\n");
        s = s.replaceAll("<p>(.*?)</p>", "$1\n\n");
        s = s.replaceAll("<br\\s*/?>", "\n");
        s = s.replaceAll("<[^>]*>", "");
        s = s.replaceAll("&amp;", "&")
             .replaceAll("&lt;", "<")
             .replaceAll("&gt;", ">")
             .replaceAll("&quot;", "\"");
        return s.trim() + "\n";
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
