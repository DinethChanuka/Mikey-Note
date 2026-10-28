package com.wordbook.core.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * High-performance Academic & Professional Citation & Reference Engine.
 * Supports APA (7th), MLA (9th), IEEE, Harvard, and Chicago citation styles,
 * in-text citation insertion, and automatic bibliography compilation.
 */
public class CitationEngineService {

    public enum CitationStyle {
        APA("APA (7th edition)"),
        MLA("MLA (9th edition)"),
        IEEE("IEEE (Numbered)"),
        HARVARD("Harvard"),
        CHICAGO("Chicago (Author-Date)");

        private final String displayName;

        CitationStyle(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }

        @Override
        public String toString() { return displayName; }
    }

    public record Citation(
            String id,
            String author,
            String title,
            String year,
            String source,
            String urlOrDoi,
            String pages
    ) {
        public String getFirstAuthorLastName() {
            if (author == null || author.isBlank()) return "Unknown";
            String[] parts = author.split("[,&]");
            return parts[0].trim();
        }
    }

    private final List<Citation> registry = new ArrayList<>();

    public CitationEngineService() {
        // Pre-populate standard references for instant convenience
        registry.add(new Citation("cit-1", "Knuth, Donald E.", "The Art of Computer Programming", "1997", "Addison-Wesley", "https://cs.stanford.edu/~knuth", "120-145"));
        registry.add(new Citation("cit-2", "Martin, Robert C.", "Clean Architecture: A Craftsman's Guide", "2017", "Prentice Hall", "", "42-58"));
        registry.add(new Citation("cit-3", "Gamma, E., Helm, R., Johnson, R., & Vlissides, J.", "Design Patterns: Elements of Reusable Object-Oriented Software", "1994", "Addison-Wesley", "", "1-30"));
    }

    public List<Citation> getRegisteredCitations() {
        return Collections.unmodifiableList(registry);
    }

    public void addCitation(Citation citation) {
        if (citation != null) {
            registry.removeIf(c -> c.id().equals(citation.id()));
            registry.add(citation);
        }
    }

    public String formatInTextCitation(Citation c, CitationStyle style, int index) {
        if (c == null) return "[Citation]";
        String author = c.getFirstAuthorLastName();
        String year = c.year() != null && !c.year().isBlank() ? c.year() : "n.d.";

        return switch (style) {
            case IEEE -> "<sup>[" + index + "]</sup>";
            case APA -> "(" + author + ", " + year + ")";
            case MLA -> "(" + author + (c.pages() != null && !c.pages().isBlank() ? " " + c.pages() : "") + ")";
            case HARVARD -> "(" + author + " " + year + ")";
            case CHICAGO -> "(" + author + " " + year + (c.pages() != null && !c.pages().isBlank() ? ", " + c.pages() : "") + ")";
        };
    }

    public String formatBibliographyEntry(Citation c, CitationStyle style, int index) {
        if (c == null) return "";
        String author = c.author() != null && !c.author().isBlank() ? c.author() : "Unknown Author";
        String title = c.title() != null && !c.title().isBlank() ? c.title() : "Untitled";
        String year = c.year() != null && !c.year().isBlank() ? c.year() : "n.d.";
        String source = c.source() != null && !c.source().isBlank() ? c.source() : "";
        String pages = c.pages() != null && !c.pages().isBlank() ? c.pages() : "";
        String link = c.urlOrDoi() != null && !c.urlOrDoi().isBlank() ? c.urlOrDoi() : "";

        return switch (style) {
            case IEEE -> String.format("[%d] %s, \"%s,\" <i>%s</i>%s, %s.%s",
                    index, author, title, source,
                    pages.isEmpty() ? "" : ", pp. " + pages, year,
                    link.isEmpty() ? "" : " Available: " + link);

            case APA -> String.format("%s (%s). <i>%s</i>. %s%s.%s",
                    author, year, title, source,
                    pages.isEmpty() ? "" : ", " + pages,
                    link.isEmpty() ? "" : " " + link);

            case MLA -> String.format("%s. \"%s.\" <i>%s</i>, %s%s.%s",
                    author, title, source, year,
                    pages.isEmpty() ? "" : ", pp. " + pages,
                    link.isEmpty() ? "" : " " + link);

            case HARVARD -> String.format("%s (%s) '%s', <i>%s</i>%s.%s",
                    author, year, title, source,
                    pages.isEmpty() ? "" : ", pp. " + pages,
                    link.isEmpty() ? "" : " Available at: " + link);

            case CHICAGO -> String.format("%s. %s. \"%s.\" <i>%s</i>%s.%s",
                    author, year, title, source,
                    pages.isEmpty() ? "" : ": " + pages,
                    link.isEmpty() ? "" : " " + link);
        };
    }

    public String generateBibliographyHtml(CitationStyle style) {
        return generateBibliographyHtml(registry, style);
    }

    public String generateBibliographyHtml(List<Citation> citations, CitationStyle style) {
        if (citations == null || citations.isEmpty()) {
            citations = registry;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='border-top:2px solid #2563eb;margin:32px 0 16px 0;padding-top:16px;'>");
        sb.append("<h2 style='color:#1e293b;font-size:18px;margin-bottom:12px;'>References & Bibliography</h2>");
        sb.append("<ol style='padding-left:22px;color:#334155;font-size:12px;line-height:1.7;'>");

        int idx = 1;
        for (Citation c : citations) {
            sb.append("<li style='margin-bottom:8px;'>");
            sb.append(formatBibliographyEntry(c, style, idx));
            sb.append("</li>");
            idx++;
        }

        sb.append("</ol></div><p><br></p>");
        return sb.toString();
    }
}
