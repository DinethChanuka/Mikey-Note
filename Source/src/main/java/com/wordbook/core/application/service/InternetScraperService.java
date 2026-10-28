package com.wordbook.core.application.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent High-Level Internet Scraping & Web Research Engine.
 * Features:
 * - Smart Reader-Mode extraction (locates main article content, bypasses ads/nav/footer)
 * - Deep HTML cleansing (removes scripts, styles, tracking, modals, SVG, forms)
 * - Table extraction (extracts and formats web tables into clean document tables)
 * - Executive Insights & Key Takeaways auto-generator
 * - Multi-format rendering (Research Card, Executive Summary, Data Tables, Markdown, AI Prompt)
 */
public class InternetScraperService {

    private final HttpClient httpClient;

    public record ScrapedArticle(
            String url,
            String title,
            String description,
            List<String> headings,
            List<String> paragraphs,
            List<String> keyPoints,
            String fullText,
            int estimatedReadingTimeMinutes,
            String siteName,
            String author,
            List<String> tables
    ) {
        // Backwards compatibility constructor
        public ScrapedArticle(
                String url,
                String title,
                String description,
                List<String> headings,
                List<String> paragraphs,
                List<String> keyPoints,
                String fullText,
                int estimatedReadingTimeMinutes
        ) {
            this(url, title, description, headings, paragraphs, keyPoints, fullText, estimatedReadingTimeMinutes, "", "", List.of());
        }

        /**
         * Comprehensive Modern Research Card (visual document card).
         */
        public String toDocumentHtml() {
            StringBuilder sb = new StringBuilder();
            sb.append("<div class='wb-research-card' style='border:1px solid #cbd5e1;background:#f8fafc;padding:20px 24px;margin:18px 0;border-radius:12px;font-family:Segoe UI, sans-serif;'>");

            // Header Pill & Source Badge
            sb.append("<div style='display:flex;align-items:center;justify-content:space-between;margin-bottom:10px;'>");
            sb.append("<div style='display:flex;align-items:center;gap:8px;'>");
            sb.append("<span style='font-size:20px;'>🌐</span>");
            String displayTitle = title.isEmpty() ? "Web Research Clip" : title;
            sb.append("<span style='font-size:16px;font-weight:bold;color:#0f172a;'>")
              .append(escapeHtml(displayTitle))
              .append("</span>");
            sb.append("</div>");
            sb.append("</div>");

            // Metadata row
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
            String host = extractHost(url);
            sb.append("<div style='font-size:11px;color:#64748b;margin-bottom:14px;border-bottom:1px solid #e2e8f0;padding-bottom:8px;'>");
            sb.append("<b>Source:</b> <a href='").append(escapeHtml(url)).append("' style='color:#2563eb;text-decoration:none;'>")
              .append(escapeHtml(host.isEmpty() ? url : host)).append("</a> &bull; ");
            if (author != null && !author.isBlank()) {
                sb.append("<b>Author:</b> ").append(escapeHtml(author)).append(" &bull; ");
            }
            sb.append("<b>Read time:</b> ~").append(estimatedReadingTimeMinutes).append(" min &bull; Scraped on ").append(dateStr);
            sb.append("</div>");

            // Description / Executive Lead
            if (description != null && !description.isBlank()) {
                sb.append("<div style='border-left:4px solid #3b82f6;background:#eff6ff;padding:10px 14px;border-radius:6px;margin-bottom:14px;font-size:13px;color:#1e3a8a;line-height:1.5;'>");
                sb.append("<b>Overview:</b> ").append(escapeHtml(description));
                sb.append("</div>");
            }

            // Key Insights
            if (!keyPoints.isEmpty()) {
                sb.append("<div style='background:#ffffff;border:1px solid #e2e8f0;padding:12px 16px;border-radius:8px;margin-bottom:14px;'>");
                sb.append("<div style='font-size:12px;font-weight:bold;color:#0f172a;text-transform:uppercase;letter-spacing:0.5px;margin-bottom:6px;'>💡 Key Insights:</div>");
                sb.append("<ul style='margin:0 0 0 16px;padding:0;color:#334155;font-size:12px;line-height:1.6;'>");
                for (String kp : keyPoints) {
                    sb.append("<li style='margin-bottom:4px;'>").append(escapeHtml(kp)).append("</li>");
                }
                sb.append("</ul></div>");
            }

            // Headings / Content Highlights
            int pCount = Math.min(4, paragraphs.size());
            for (int i = 0; i < pCount; i++) {
                if (i < headings.size() && headings.get(i).length() > 3) {
                    sb.append("<div style='font-size:13px;font-weight:bold;color:#1e293b;margin:10px 0 4px 0;'>")
                      .append(escapeHtml(headings.get(i)))
                      .append("</div>");
                }
                sb.append("<p style='font-size:12px;line-height:1.6;color:#334155;margin:4px 0 8px 0;'>")
                  .append(escapeHtml(paragraphs.get(i)))
                  .append("</p>");
            }

            // Discovered Tables (if any)
            if (tables != null && !tables.isEmpty()) {
                sb.append("<div style='margin-top:12px;'>");
                sb.append("<div style='font-size:12px;font-weight:bold;color:#0f172a;margin-bottom:6px;'>📊 Extracted Data Table:</div>");
                sb.append(tables.get(0));
                sb.append("</div>");
            }

            sb.append("</div><p><br></p>");
            return sb.toString();
        }

        /**
         * Executive Brief Card (concise bullet-only format).
         */
        public String toExecutiveSummaryHtml() {
            StringBuilder sb = new StringBuilder();
            sb.append("<div class='wb-research-card' style='border-left:4px solid #10b981;background:#ecfdf5;padding:14px 18px;margin:14px 0;border-radius:6px;font-family:Segoe UI, sans-serif;'>");
            sb.append("<div style='font-weight:bold;font-size:14px;color:#065f46;margin-bottom:4px;'>📌 Executive Brief: ").append(escapeHtml(title)).append("</div>");
            sb.append("<div style='font-size:11px;color:#047857;margin-bottom:10px;'>Source: <a href='").append(escapeHtml(url)).append("' style='color:#059669;'>").append(escapeHtml(url)).append("</a></div>");
            if (description != null && !description.isBlank()) {
                sb.append("<div style='font-size:12px;color:#064e3b;margin-bottom:8px;'>").append(escapeHtml(description)).append("</div>");
            }
            if (!keyPoints.isEmpty()) {
                sb.append("<ul style='margin:0 0 0 16px;padding:0;color:#064e3b;font-size:12px;'>");
                for (String kp : keyPoints) {
                    sb.append("<li>").append(escapeHtml(kp)).append("</li>");
                }
                sb.append("</ul>");
            }
            sb.append("</div><p><br></p>");
            return sb.toString();
        }

        /**
         * Full Markdown representation.
         */
        public String toMarkdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("# ").append(title).append("\n\n");
            sb.append("> **Source:** ").append(url).append("\n\n");
            if (description != null && !description.isBlank()) {
                sb.append(description).append("\n\n");
            }
            if (!keyPoints.isEmpty()) {
                sb.append("### Key Takeaways\n");
                for (String kp : keyPoints) {
                    sb.append("- ").append(kp).append("\n");
                }
                sb.append("\n");
            }
            sb.append("### Content Summary\n");
            for (String p : paragraphs) {
                sb.append(p).append("\n\n");
            }
            return sb.toString();
        }

        /**
         * Optimized text prompt formatted for AI assistants (Claude, ChatGPT, Gemini, DeepSeek).
         */
        public String toPromptText() {
            StringBuilder sb = new StringBuilder();
            sb.append("Analyze and summarize this web research material from ").append(url).append(":\n\n");
            sb.append("TITLE: ").append(title).append("\n");
            if (!description.isBlank()) sb.append("ABSTRACT: ").append(description).append("\n");
            sb.append("\nKEY TAKEAWAYS:\n");
            for (String kp : keyPoints) sb.append("- ").append(kp).append("\n");
            sb.append("\nARTICLE CONTENT:\n");
            for (int i = 0; i < Math.min(6, paragraphs.size()); i++) {
                sb.append(paragraphs.get(i)).append("\n\n");
            }
            sb.append("\nTASK: Provide a comprehensive technical critique, list 3 actionable applications, and highlight potential edge cases.");
            return sb.toString();
        }

        private static String extractHost(String urlStr) {
            try {
                URI uri = URI.create(urlStr);
                String host = uri.getHost();
                return host != null ? host : urlStr;
            } catch (Exception e) {
                return urlStr;
            }
        }
    }

    public InternetScraperService() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(12))
                .build();
    }

    /**
     * Scrapes, cleans, and parses article content from the given URL.
     */
    public ScrapedArticle scrapeUrl(String rawUrl) throws IOException, InterruptedException {
        String url = normalizeUrl(rawUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(18))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + " received when fetching: " + url);
        }

        String rawHtml = response.body();
        return parseHtml(url, rawHtml);
    }

    public ScrapedArticle parseHtml(String url, String rawHtml) {
        String title = extractTitle(rawHtml);
        String description = extractMetaDescription(rawHtml);
        String siteName = extractSiteName(rawHtml);
        String author = extractAuthor(rawHtml);

        // Reader Mode: Try to isolate the main article container
        String mainContent = isolateMainContent(rawHtml);

        // Extract HTML tables before stripping tags
        List<String> tables = extractTables(mainContent);

        // Deep Clean: Strip non-article elements
        String cleanHtml = deepCleanHtml(mainContent);

        List<String> headings = extractHeadings(cleanHtml);
        List<String> paragraphs = extractParagraphs(cleanHtml);
        List<String> keyPoints = extractKeyPoints(paragraphs);

        StringBuilder fullTextBuilder = new StringBuilder();
        for (String p : paragraphs) {
            fullTextBuilder.append(p).append("\n\n");
        }
        String fullText = fullTextBuilder.toString().trim();

        int wordCount = fullText.isBlank() ? 0 : fullText.split("\\s+").length;
        int readingTime = Math.max(1, (int) Math.ceil(wordCount / 220.0));

        return new ScrapedArticle(
                url,
                title,
                description,
                headings,
                paragraphs,
                keyPoints,
                fullText,
                readingTime,
                siteName,
                author,
                tables
        );
    }

    /**
     * Smart Reader-Mode Heuristic:
     * Identifies <article>, <main>, role="main", or content-specific divs.
     */
    private String isolateMainContent(String html) {
        // 1. Check for <article>
        Matcher articleMatcher = Pattern.compile("(?is)<article[^>]*>(.*?)</article>").matcher(html);
        if (articleMatcher.find()) {
            return articleMatcher.group(1);
        }

        // 2. Check for <main>
        Matcher mainMatcher = Pattern.compile("(?is)<main[^>]*>(.*?)</main>").matcher(html);
        if (mainMatcher.find()) {
            return mainMatcher.group(1);
        }

        // 3. Check for role="main"
        Matcher roleMatcher = Pattern.compile("(?is)<div[^>]+role=[\"']main[\"'][^>]*>(.*?)</div>").matcher(html);
        if (roleMatcher.find()) {
            return roleMatcher.group(1);
        }

        // 4. Check for common article class identifiers
        Pattern contentDiv = Pattern.compile("(?is)<div[^>]+class=[\"'][^\"']*(?:post-content|article-body|entry-content|mw-parser-output|story-content)[^\"']*[\"'][^>]*>(.*?)</div>");
        Matcher divMatcher = contentDiv.matcher(html);
        if (divMatcher.find()) {
            return divMatcher.group(1);
        }

        // 5. Fallback to whole HTML
        return html;
    }

    /**
     * Strips scripts, styles, forms, navigations, footers, cookies, and ads.
     */
    private String deepCleanHtml(String html) {
        return html
                .replaceAll("(?is)<script.*?</script>", "")
                .replaceAll("(?is)<style.*?</style>", "")
                .replaceAll("(?is)<svg.*?</svg>", "")
                .replaceAll("(?is)<nav.*?</nav>", "")
                .replaceAll("(?is)<header.*?</header>", "")
                .replaceAll("(?is)<footer.*?</footer>", "")
                .replaceAll("(?is)<aside.*?</aside>", "")
                .replaceAll("(?is)<form.*?</form>", "")
                .replaceAll("(?is)<iframe.*?</iframe>", "")
                .replaceAll("(?is)<noscript.*?</noscript>", "")
                .replaceAll("(?is)<!--.*?-->", "")
                // Filter ad/cookie banners
                .replaceAll("(?is)<div[^>]+(?:class|id)=[\"'][^\"']*(?:cookie|consent|gdpr|advertisement|ad-banner|popup)[^\"']*[\"'][^>]*>.*?</div>", "");
    }

    private String extractTitle(String html) {
        Matcher ogTitle = Pattern.compile("(?is)<meta[^>]+property=[\"']og:title[\"'][^>]+content=[\"'](.*?)[\"']").matcher(html);
        if (ogTitle.find()) {
            return cleanText(ogTitle.group(1));
        }
        Matcher m = Pattern.compile("(?is)<title>(.*?)</title>").matcher(html);
        if (m.find()) {
            return cleanText(m.group(1));
        }
        Matcher h1 = Pattern.compile("(?is)<h1[^>]*>(.*?)</h1>").matcher(html);
        if (h1.find()) {
            return cleanText(h1.group(1));
        }
        return "Web Document";
    }

    private String extractMetaDescription(String html) {
        Matcher ogDesc = Pattern.compile("(?is)<meta[^>]+property=[\"']og:description[\"'][^>]+content=[\"'](.*?)[\"']").matcher(html);
        if (ogDesc.find()) {
            return cleanText(ogDesc.group(1));
        }
        Matcher m = Pattern.compile("(?is)<meta[^>]+name=[\"']description[\"'][^>]+content=[\"'](.*?)[\"']").matcher(html);
        if (m.find()) {
            return cleanText(m.group(1));
        }
        return "";
    }

    private String extractSiteName(String html) {
        Matcher m = Pattern.compile("(?is)<meta[^>]+property=[\"']og:site_name[\"'][^>]+content=[\"'](.*?)[\"']").matcher(html);
        if (m.find()) return cleanText(m.group(1));
        return "";
    }

    private String extractAuthor(String html) {
        Matcher m = Pattern.compile("(?is)<meta[^>]+name=[\"']author[\"'][^>]+content=[\"'](.*?)[\"']").matcher(html);
        if (m.find()) return cleanText(m.group(1));
        return "";
    }

    private List<String> extractHeadings(String html) {
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("(?is)<h[1-4][^>]*>(.*?)</h[1-4]>").matcher(html);
        while (m.find() && list.size() < 16) {
            String text = cleanText(m.group(1));
            if (text.length() > 3 && text.length() < 120 && !list.contains(text)) {
                list.add(text);
            }
        }
        return list;
    }

    private List<String> extractParagraphs(String html) {
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("(?is)<p[^>]*>(.*?)</p>").matcher(html);
        while (m.find() && list.size() < 30) {
            String text = cleanText(m.group(1));
            // Filter out cookie notices, boilerplate, copyright lines
            if (text.length() > 40
                    && !text.toLowerCase().contains("cookie")
                    && !text.toLowerCase().contains("all rights reserved")
                    && !text.toLowerCase().contains("terms of service")
                    && !text.toLowerCase().contains("privacy policy")) {
                list.add(text);
            }
        }
        return list;
    }

    private List<String> extractKeyPoints(List<String> paragraphs) {
        List<String> keyPoints = new ArrayList<>();
        for (String p : paragraphs) {
            if (keyPoints.size() >= 5) break;
            String[] sentences = p.split("(?<=[.!?])\\s+");
            if (sentences.length > 0) {
                String first = sentences[0].trim();
                if (first.length() >= 35 && first.length() <= 160) {
                    keyPoints.add(first);
                }
            }
        }
        return keyPoints;
    }

    /**
     * Extracts and reformats HTML tables found on the page into clean, stylized WordBook tables.
     */
    private List<String> extractTables(String html) {
        List<String> tables = new ArrayList<>();
        Matcher m = Pattern.compile("(?is)<table[^>]*>(.*?)</table>").matcher(html);
        while (m.find() && tables.size() < 3) {
            String tableBody = m.group(1);
            // Verify table has rows and meaningful text
            if (tableBody.contains("<tr") && cleanText(tableBody).length() > 30) {
                String cleanTable = "<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;width:100%;margin:12px 0;border:1px solid #cbd5e1;background:#ffffff;font-size:12px;'>"
                        + tableBody
                        .replaceAll("(?is)<th[^>]*>", "<th style='background:#f1f5f9;font-weight:bold;padding:8px 12px;border:1px solid #cbd5e1;text-align:left;color:#0f172a;'>")
                        .replaceAll("(?is)<td[^>]*>", "<td style='padding:8px 12px;border:1px solid #cbd5e1;color:#334155;'>")
                        + "</table>";
                tables.add(cleanTable);
            }
        }
        return tables;
    }

    private static String normalizeUrl(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    private static String cleanText(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("&mdash;", "—")
                .replaceAll("&ndash;", "–")
                .replaceAll("&hellip;", "…")
                .replaceAll("&ldquo;", "“")
                .replaceAll("&rdquo;", "”")
                .replaceAll("&rsquo;", "’")
                .replaceAll("&lsquo;", "‘")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
