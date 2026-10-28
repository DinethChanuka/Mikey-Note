package com.wordbook.core.application.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-Level Intelligent Grammar, Proofreading & Spell Correction Engine.
 * Detects homophones, article agreement flaws, repeated words, capitalization flaws,
 * punctuation spacing, readability metrics, and 130+ common spelling/grammar errors.
 */
public class GrammarEngineService {

    public record GrammarIssue(
            String original,
            String suggestion,
            String ruleExplanation,
            int startOffset,
            int endOffset,
            String sentenceContext
    ) {
        public String category() {
            String rule = ruleExplanation != null ? ruleExplanation.toLowerCase() : "";
            if (rule.contains("spelling") || rule.contains("typo")) return "Spelling";
            if (rule.contains("comma") || rule.contains("punctuation") || rule.contains("space")) return "Punctuation";
            if (rule.contains("homophone") || rule.contains("article") || rule.contains("repeated") || rule.contains("grammar")) return "Grammar";
            if (rule.contains("capitalize")) return "Capitalization";
            return "Style";
        }
    }

    public record ReadabilityStats(
            int wordCount,
            int sentenceCount,
            int syllableCount,
            double fleschReadingEase,
            String readingLevel,
            double gradeLevel
    ) {}

    private static final Map<String, String> COMMON_TYPOS = new HashMap<>();

    static {
        // User requested & project typos
        COMMON_TYPOS.put("grammer", "grammar");
        COMMON_TYPOS.put("diffrent", "different");
        COMMON_TYPOS.put("acessible", "accessible");
        COMMON_TYPOS.put("cahrt", "chart");
        COMMON_TYPOS.put("cardt", "chart");
        COMMON_TYPOS.put("hndling", "handling");
        COMMON_TYPOS.put("reffrnced", "referenced");
        COMMON_TYPOS.put("customerize", "customize");
        COMMON_TYPOS.put("recieve", "receive");
        COMMON_TYPOS.put("seperate", "separate");
        COMMON_TYPOS.put("definately", "definitely");
        COMMON_TYPOS.put("occured", "occurred");
        COMMON_TYPOS.put("untill", "until");
        COMMON_TYPOS.put("truely", "truly");
        COMMON_TYPOS.put("accomodate", "accommodate");
        COMMON_TYPOS.put("wierd", "weird");
        COMMON_TYPOS.put("teh", "the");
        COMMON_TYPOS.put("adn", "and");
        COMMON_TYPOS.put("thier", "their");
        COMMON_TYPOS.put("calender", "calendar");
        COMMON_TYPOS.put("succesful", "successful");
        COMMON_TYPOS.put("goverment", "government");
        COMMON_TYPOS.put("enviroment", "environment");
        COMMON_TYPOS.put("beleive", "believe");
        COMMON_TYPOS.put("neccessary", "necessary");
        COMMON_TYPOS.put("tommorow", "tomorrow");
        COMMON_TYPOS.put("alot", "a lot");
        COMMON_TYPOS.put("noone", "no one");
        COMMON_TYPOS.put("dont", "don't");
        COMMON_TYPOS.put("cant", "can't");
        COMMON_TYPOS.put("wont", "won't");
        COMMON_TYPOS.put("inteligent", "intelligent");

        // Specific typo variations & common phonetic errors
        COMMON_TYPOS.put("redesoign", "redesign");
        COMMON_TYPOS.put("redesignn", "redesign");
        COMMON_TYPOS.put("tabvels", "tables");
        COMMON_TYPOS.put("adanced", "advanced");
        COMMON_TYPOS.put("optioon", "option");
        COMMON_TYPOS.put("neeed", "need");
        COMMON_TYPOS.put("impriove", "improve");
        COMMON_TYPOS.put("lect", "left");
        COMMON_TYPOS.put("rool", "rule");
        COMMON_TYPOS.put("slecting", "selecting");
        COMMON_TYPOS.put("ribon", "ribbon");
        COMMON_TYPOS.put("completly", "completely");
        COMMON_TYPOS.put("beeter", "better");
        COMMON_TYPOS.put("bteer", "better");
        COMMON_TYPOS.put("preum", "premium");
        COMMON_TYPOS.put("preukm", "premium");
        COMMON_TYPOS.put("deisingg", "design");
        COMMON_TYPOS.put("pnael", "panel");
        COMMON_TYPOS.put("pnaeel", "panel");
        COMMON_TYPOS.put("iwndow", "window");
        COMMON_TYPOS.put("soizes", "sizes");
        COMMON_TYPOS.put("scrab", "scrape");
        COMMON_TYPOS.put("lunching", "launching");
        COMMON_TYPOS.put("visulaize", "visualize");
        COMMON_TYPOS.put("repressent", "represent");
        COMMON_TYPOS.put("desfult", "default");
        COMMON_TYPOS.put("alwys", "always");
        COMMON_TYPOS.put("keyboarf", "keyboard");
        COMMON_TYPOS.put("manger", "manager");

        // High frequency spelling mistakes in documentation & essays
        COMMON_TYPOS.put("embarass", "embarrass");
        COMMON_TYPOS.put("privelege", "privilege");
        COMMON_TYPOS.put("priviledge", "privilege");
        COMMON_TYPOS.put("pronounciation", "pronunciation");
        COMMON_TYPOS.put("rythm", "rhythm");
        COMMON_TYPOS.put("resturant", "restaurant");
        COMMON_TYPOS.put("maintainance", "maintenance");
        COMMON_TYPOS.put("persue", "pursue");
        COMMON_TYPOS.put("concensus", "consensus");
        COMMON_TYPOS.put("guarentee", "guarantee");
        COMMON_TYPOS.put("judgement", "judgment");
        COMMON_TYPOS.put("liason", "liaison");
        COMMON_TYPOS.put("millenium", "millennium");
        COMMON_TYPOS.put("noticable", "noticeable");
        COMMON_TYPOS.put("occurence", "occurrence");
        COMMON_TYPOS.put("pastime", "pastime");
        COMMON_TYPOS.put("publically", "publicly");
        COMMON_TYPOS.put("recommand", "recommend");
        COMMON_TYPOS.put("refered", "referred");
        COMMON_TYPOS.put("relavent", "relevant");
        COMMON_TYPOS.put("resistence", "resistance");
        COMMON_TYPOS.put("seige", "siege");
        COMMON_TYPOS.put("supercede", "supersede");
        COMMON_TYPOS.put("tendancy", "tendency");
        COMMON_TYPOS.put("threshhold", "threshold");
        COMMON_TYPOS.put("transfered", "transferred");
        COMMON_TYPOS.put("unforseen", "unforeseen");
        COMMON_TYPOS.put("vaccum", "vacuum");
        COMMON_TYPOS.put("writting", "writing");
        COMMON_TYPOS.put("begining", "beginning");
        COMMON_TYPOS.put("acheive", "achieve");
        COMMON_TYPOS.put("acrosss", "across");
        COMMON_TYPOS.put("aggresive", "aggressive");
        COMMON_TYPOS.put("allready", "already");
        COMMON_TYPOS.put("amature", "amateur");
        COMMON_TYPOS.put("apparantly", "apparently");
        COMMON_TYPOS.put("appearence", "appearance");
        COMMON_TYPOS.put("arguement", "argument");
        COMMON_TYPOS.put("basicly", "basically");
        COMMON_TYPOS.put("bussiness", "business");
        COMMON_TYPOS.put("collegue", "colleague");
        COMMON_TYPOS.put("commited", "committed");
        COMMON_TYPOS.put("concious", "conscious");
        COMMON_TYPOS.put("dissappoint", "disappoint");
        COMMON_TYPOS.put("existance", "existence");
        COMMON_TYPOS.put("familar", "familiar");
        COMMON_TYPOS.put("finaly", "finally");
        COMMON_TYPOS.put("foriegn", "foreign");
        COMMON_TYPOS.put("fourty", "forty");
        COMMON_TYPOS.put("foward", "forward");
        COMMON_TYPOS.put("freind", "friend");
        COMMON_TYPOS.put("greatful", "grateful");
        COMMON_TYPOS.put("happenning", "happening");
        COMMON_TYPOS.put("heigth", "height");
        COMMON_TYPOS.put("humerous", "humorous");
        COMMON_TYPOS.put("ignorence", "ignorance");
        COMMON_TYPOS.put("immediatly", "immediately");
        COMMON_TYPOS.put("incidently", "incidentally");
        COMMON_TYPOS.put("independant", "independent");
        COMMON_TYPOS.put("interupt", "interrupt");
        COMMON_TYPOS.put("irresistable", "irresistible");
        COMMON_TYPOS.put("knowlege", "knowledge");
        COMMON_TYPOS.put("lightening", "lightning");
        COMMON_TYPOS.put("lonelyness", "loneliness");
        COMMON_TYPOS.put("mischievious", "mischievous");
        COMMON_TYPOS.put("misspell", "misspell");
        COMMON_TYPOS.put("mispell", "misspell");
        COMMON_TYPOS.put("neccessity", "necessity");
        COMMON_TYPOS.put("oppurtunity", "opportunity");
        COMMON_TYPOS.put("paralel", "parallel");
        COMMON_TYPOS.put("parrallel", "parallel");
        COMMON_TYPOS.put("peice", "piece");
        COMMON_TYPOS.put("possession", "possession");
        COMMON_TYPOS.put("possesion", "possession");
        COMMON_TYPOS.put("prefered", "preferred");
        COMMON_TYPOS.put("presense", "presence");
        COMMON_TYPOS.put("procede", "proceed");
        COMMON_TYPOS.put("realy", "really");
        COMMON_TYPOS.put("religous", "religious");
        COMMON_TYPOS.put("remeber", "remember");
        COMMON_TYPOS.put("sence", "sense");
        COMMON_TYPOS.put("succes", "success");
        COMMON_TYPOS.put("suprise", "surprise");
        COMMON_TYPOS.put("temperture", "temperature");
        COMMON_TYPOS.put("tommorrow", "tomorrow");
        COMMON_TYPOS.put("twelth", "twelfth");
        COMMON_TYPOS.put("usefull", "useful");
    }

    private static final Pattern REPEATED_WORDS_PATTERN =
            Pattern.compile("\\b(the|is|in|and|to|of|that|we|it|a|for|very)\\s+\\1\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern COMMA_SPACE_PATTERN =
            Pattern.compile("\\b([A-Za-z0-9]+)\\s+([,.:;?!])");

    private static final Pattern MISSING_COMMA_SPACE =
            Pattern.compile("\\b([A-Za-z]+),([A-Za-z]+)\\b");

    private static final Pattern CAPITALIZATION_PATTERN =
            Pattern.compile("([.!?]\\s+)([a-z])");

    public List<GrammarIssue> analyzeText(String text) {
        List<GrammarIssue> issues = new ArrayList<>();
        if (text == null || text.isBlank()) return issues;

        // 1. Repeated words ("the the", "very very")
        Matcher repMatcher = REPEATED_WORDS_PATTERN.matcher(text);
        while (repMatcher.find()) {
            String word = repMatcher.group(1);
            issues.add(new GrammarIssue(
                    repMatcher.group(),
                    word,
                    "Repeated word detected: '" + word + "'",
                    repMatcher.start(),
                    repMatcher.end(),
                    extractContext(text, repMatcher.start(), repMatcher.end())
            ));
        }

        // 2. Unwanted space before punctuation ("word ,")
        Matcher commaSpace = COMMA_SPACE_PATTERN.matcher(text);
        while (commaSpace.find()) {
            String word = commaSpace.group(1);
            String punct = commaSpace.group(2);
            issues.add(new GrammarIssue(
                    commaSpace.group(),
                    word + punct,
                    "Remove unnecessary space before punctuation",
                    commaSpace.start(),
                    commaSpace.end(),
                    extractContext(text, commaSpace.start(), commaSpace.end())
            ));
        }

        // 3. Missing space after comma ("word,word")
        Matcher missSpace = MISSING_COMMA_SPACE.matcher(text);
        while (missSpace.find()) {
            String w1 = missSpace.group(1);
            String w2 = missSpace.group(2);
            issues.add(new GrammarIssue(
                    missSpace.group(),
                    w1 + ", " + w2,
                    "Add space after comma",
                    missSpace.start(),
                    missSpace.end(),
                    extractContext(text, missSpace.start(), missSpace.end())
            ));
        }

        // 4. Missing sentence capitalization
        Matcher capMatcher = CAPITALIZATION_PATTERN.matcher(text);
        while (capMatcher.find()) {
            String lead = capMatcher.group(1);
            String letter = capMatcher.group(2);
            issues.add(new GrammarIssue(
                    letter,
                    letter.toUpperCase(),
                    "Capitalize first letter of sentence",
                    capMatcher.start(2),
                    capMatcher.end(2),
                    extractContext(text, capMatcher.start(2), capMatcher.end(2))
            ));
        }

        // 5. Homophones & Confused Word Pairs
        checkHomophoneRule(text, issues, "\\b(their)\\s+(going|coming|here|ready|making|doing)\\b", "they're", "Homophone: Use 'they're' (they are)");
        checkHomophoneRule(text, issues, "\\b(there)\\s+(car|house|dog|cat|project|book|code|computer|team|work|lessons?)\\b", "their", "Homophone: Use possessive 'their'");
        checkHomophoneRule(text, issues, "\\b(they're)\\s+(is|are|was|were)\\b", "there", "Homophone: Use 'there' for existential 'there is/are'");
        checkHomophoneRule(text, issues, "\\b(your)\\s+(welcome|right|wrong|correct|great)\\b", "you're", "Homophone: Use 'you're' (you are)");
        checkHomophoneRule(text, issues, "\\b(you're)\\s+(car|house|dog|cat|project|book|code|computer|team|work|lessons?)\\b", "your", "Homophone: Use possessive 'your'");
        checkHomophoneRule(text, issues, "\\b(its)\\s+(a|an|the|not|very|too|so)\\b", "it's", "Homophone: Use 'it's' (it is)");
        checkHomophoneRule(text, issues, "\\b(to)\\s+(much|many|late|early|fast|slow|bad|good|cold|hot|hard)\\b", "too", "Homophone: Use 'too' (excessive/also)");
        checkHomophoneRule(text, issues, "\\b(loose)\\s+(weight|the\\s+game|my\\s+mind|your\\s+way|hope)\\b", "lose", "Confused word: Use 'lose' (verb) instead of 'loose'");
        checkHomophoneRule(text, issues, "\\b(more|less|better|worse|greater|faster|slower|higher|lower)\\s+(then)\\b", "than", "Comparative: Use 'than' for comparisons");

        // 6. Article Agreement ("a apple" -> "an apple", "an book" -> "a book")
        Pattern aVowel = Pattern.compile("\\b(a)\\s+([aeio][a-z]+)\\b", Pattern.CASE_INSENSITIVE);
        Matcher aMatcher = aVowel.matcher(text);
        while (aMatcher.find()) {
            String word = aMatcher.group(2).toLowerCase();
            if (!word.startsWith("one") && !word.startsWith("uni") && !word.startsWith("use") && !word.startsWith("euro")) {
                issues.add(new GrammarIssue(
                        aMatcher.group(1),
                        Character.isUpperCase(aMatcher.group(1).charAt(0)) ? "An" : "an",
                        "Article agreement: Use 'an' before vowel sounds (" + aMatcher.group(2) + ")",
                        aMatcher.start(1),
                        aMatcher.end(1),
                        extractContext(text, aMatcher.start(), aMatcher.end())
                ));
            }
        }

        Pattern anConsonant = Pattern.compile("\\b(an)\\s+([bcdfghjklmnpqrstvwxyz][a-z]+)\\b", Pattern.CASE_INSENSITIVE);
        Matcher anMatcher = anConsonant.matcher(text);
        while (anMatcher.find()) {
            String word = anMatcher.group(2).toLowerCase();
            if (!word.startsWith("hour") && !word.startsWith("honest") && !word.startsWith("honor") && !word.startsWith("heir")) {
                issues.add(new GrammarIssue(
                        anMatcher.group(1),
                        Character.isUpperCase(anMatcher.group(1).charAt(0)) ? "A" : "a",
                        "Article agreement: Use 'a' before consonant sounds (" + anMatcher.group(2) + ")",
                        anMatcher.start(1),
                        anMatcher.end(1),
                        extractContext(text, anMatcher.start(), anMatcher.end())
                ));
            }
        }

        // 7. Common dictionary typos
        Pattern wordPattern = Pattern.compile("\\b([A-Za-z]+)\\b");
        Matcher wMatcher = wordPattern.matcher(text);
        while (wMatcher.find()) {
            String word = wMatcher.group(1);
            String lower = word.toLowerCase();
            if (COMMON_TYPOS.containsKey(lower)) {
                String repl = COMMON_TYPOS.get(lower);
                if (Character.isUpperCase(word.charAt(0))) {
                    repl = Character.toUpperCase(repl.charAt(0)) + repl.substring(1);
                }
                issues.add(new GrammarIssue(
                        word,
                        repl,
                        "Spelling correction for '" + word + "'",
                        wMatcher.start(),
                        wMatcher.end(),
                        extractContext(text, wMatcher.start(), wMatcher.end())
                ));
            }
        }

        // Sort by start offset
        issues.sort(Comparator.comparingInt(GrammarIssue::startOffset));
        return issues;
    }

    private void checkHomophoneRule(String text, List<GrammarIssue> issues, String regex, String replacement, String explanation) {
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        while (m.find()) {
            String wrongWord = m.group(1);
            String repl = Character.isUpperCase(wrongWord.charAt(0))
                    ? Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1)
                    : replacement;
            issues.add(new GrammarIssue(
                    wrongWord,
                    repl,
                    explanation,
                    m.start(1),
                    m.end(1),
                    extractContext(text, m.start(), m.end())
            ));
        }
    }

    public String autoFixAll(String text) {
        if (text == null || text.isBlank()) return text;

        String result = text;

        // Apply typos replacements
        for (Map.Entry<String, String> entry : COMMON_TYPOS.entrySet()) {
            String typo = entry.getKey();
            String fix = entry.getValue();
            result = result.replaceAll("(?i)\\b" + Pattern.quote(typo) + "\\b", fix);
        }

        // Fix repeated words
        result = REPEATED_WORDS_PATTERN.matcher(result).replaceAll("$1");

        // Fix space before punctuation
        result = COMMA_SPACE_PATTERN.matcher(result).replaceAll("$1$2");

        // Fix missing comma space
        result = MISSING_COMMA_SPACE.matcher(result).replaceAll("$1, $2");

        // Fix sentence capitalization
        Matcher capMatcher = CAPITALIZATION_PATTERN.matcher(result);
        StringBuilder sb = new StringBuilder();
        while (capMatcher.find()) {
            capMatcher.appendReplacement(sb, capMatcher.group(1) + capMatcher.group(2).toUpperCase());
        }
        capMatcher.appendTail(sb);
        result = sb.toString();

        return result;
    }

    /**
     * Compute comprehensive Readability statistics based on Flesch-Kincaid methodology.
     */
    public ReadabilityStats calculateReadability(String text) {
        if (text == null || text.isBlank()) {
            return new ReadabilityStats(0, 0, 0, 100.0, "Very Easy", 0.0);
        }

        String cleaned = text.trim();
        String[] words = cleaned.split("\\s+");
        int wordCount = 0;
        int syllableCount = 0;

        for (String w : words) {
            String cleanWord = w.replaceAll("[^a-zA-Z]", "").toLowerCase();
            if (!cleanWord.isEmpty()) {
                wordCount++;
                syllableCount += countSyllables(cleanWord);
            }
        }
        if (wordCount == 0) wordCount = 1;

        String[] sentences = cleaned.split("[.!?]+");
        int sentenceCount = 0;
        for (String s : sentences) {
            if (!s.trim().isEmpty()) sentenceCount++;
        }
        if (sentenceCount == 0) sentenceCount = 1;

        // Flesch Reading Ease Formula
        double flesch = 206.835 - (1.015 * ((double) wordCount / sentenceCount)) - (84.6 * ((double) syllableCount / wordCount));
        flesch = Math.max(0.0, Math.min(100.0, Math.round(flesch * 10.0) / 10.0));

        // Flesch-Kincaid Grade Level
        double grade = (0.39 * ((double) wordCount / sentenceCount)) + (11.8 * ((double) syllableCount / wordCount)) - 15.59;
        grade = Math.max(1.0, Math.round(grade * 10.0) / 10.0);

        String level;
        if (flesch >= 90.0) level = "Very Easy (5th grade)";
        else if (flesch >= 80.0) level = "Easy (6th grade)";
        else if (flesch >= 70.0) level = "Fairly Easy (7th grade)";
        else if (flesch >= 60.0) level = "Standard (8th-9th grade)";
        else if (flesch >= 50.0) level = "Fairly Difficult (High School)";
        else if (flesch >= 30.0) level = "Difficult (College)";
        else level = "Very Difficult (Graduate)";

        return new ReadabilityStats(wordCount, sentenceCount, syllableCount, flesch, level, grade);
    }

    private int countSyllables(String word) {
        if (word.length() <= 3) return 1;
        String w = word.toLowerCase();
        if (w.endsWith("e") && !w.endsWith("le") && !w.endsWith("ee")) {
            w = w.substring(0, w.length() - 1);
        }
        Matcher m = Pattern.compile("[aeiouy]+").matcher(w);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return Math.max(1, count);
    }

    private String extractContext(String text, int start, int end) {
        int ctxStart = Math.max(0, start - 25);
        int ctxEnd = Math.min(text.length(), end + 25);
        String prefix = ctxStart > 0 ? "…" : "";
        String suffix = ctxEnd < text.length() ? "…" : "";
        return prefix + text.substring(ctxStart, ctxEnd).replaceAll("\\s+", " ") + suffix;
    }
}
