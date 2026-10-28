package com.wordbook.infrastructure.web.json;

import java.lang.reflect.RecordComponent;
import java.util.*;

/**
 * Lightweight, zero-dependency JSON serializer and parser tailored for WordBook DTOs,
 * maps, collections, and primitives.
 */
public final class SimpleJson {

    private SimpleJson() {}

    // ================= SERIALIZER =================

    public static String serialize(Object obj) {
        StringBuilder sb = new StringBuilder();
        serializeValue(obj, sb);
        return sb.toString();
    }

    private static void serializeValue(Object obj, StringBuilder sb) {
        if (obj == null) {
            sb.append("null");
        } else if (obj instanceof String s) {
            escapeString(s, sb);
        } else if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(obj);
        } else if (obj instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                escapeString(String.valueOf(entry.getKey()), sb);
                sb.append(':');
                serializeValue(entry.getValue(), sb);
            }
            sb.append('}');
        } else if (obj instanceof Iterable<?> iter) {
            sb.append('[');
            boolean first = true;
            for (Object item : iter) {
                if (!first) sb.append(',');
                first = false;
                serializeValue(item, sb);
            }
            sb.append(']');
        } else if (obj.getClass().isArray()) {
            sb.append('[');
            int len = java.lang.reflect.Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(',');
                serializeValue(java.lang.reflect.Array.get(obj, i), sb);
            }
            sb.append(']');
        } else if (obj.getClass().isRecord()) {
            sb.append('{');
            RecordComponent[] components = obj.getClass().getRecordComponents();
            for (int i = 0; i < components.length; i++) {
                if (i > 0) sb.append(',');
                RecordComponent rc = components[i];
                escapeString(rc.getName(), sb);
                sb.append(':');
                try {
                    java.lang.reflect.Method m = rc.getAccessor();
                    m.setAccessible(true);
                    Object val = m.invoke(obj);
                    serializeValue(val, sb);
                } catch (Exception e) {
                    sb.append("null");
                }
            }
            sb.append('}');
        } else {
            escapeString(obj.toString(), sb);
        }
    }

    private static void escapeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ================= PARSER =================

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object parsed = parse(json);
        if (parsed instanceof Map) {
            return (Map<String, Object>) parsed;
        }
        throw new IllegalArgumentException("Expected JSON object, got: " + (parsed != null ? parsed.getClass().getSimpleName() : "null"));
    }

    @SuppressWarnings("unchecked")
    public static List<Object> parseList(String json) {
        Object parsed = parse(json);
        if (parsed instanceof List) {
            return (List<Object>) parsed;
        }
        throw new IllegalArgumentException("Expected JSON array, got: " + (parsed != null ? parsed.getClass().getSimpleName() : "null"));
    }

    public static Object parse(String json) {
        if (json == null) return null;
        Parser parser = new Parser(json.trim());
        return parser.parseValue();
    }

    private static final class Parser {
        private final String src;
        private int pos = 0;

        Parser(String src) {
            this.src = src;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);

            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> {
                    if (c == '-' || Character.isDigit(c)) {
                        yield parseNumber();
                    }
                    throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + pos);
                }
            };
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            consume('{');
            skipWhitespace();
            if (peek() == '}') {
                consume('}');
                return map;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                consume(':');
                Object val = parseValue();
                map.put(key, val);

                skipWhitespace();
                char next = peek();
                if (next == '}') {
                    consume('}');
                    break;
                } else if (next == ',') {
                    consume(',');
                } else {
                    throw new IllegalArgumentException("Expected ',' or '}' at position " + pos);
                }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            consume('[');
            skipWhitespace();
            if (peek() == ']') {
                consume(']');
                return list;
            }

            while (true) {
                Object val = parseValue();
                list.add(val);
                skipWhitespace();
                char next = peek();
                if (next == ']') {
                    consume(']');
                    break;
                } else if (next == ',') {
                    consume(',');
                } else {
                    throw new IllegalArgumentException("Expected ',' or ']' at position " + pos);
                }
            }
            return list;
        }

        private String parseString() {
            consume('"');
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                } else if (c == '\\') {
                    if (pos >= src.length()) throw new IllegalArgumentException("Unterminated escape sequence");
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 > src.length()) throw new IllegalArgumentException("Malformed unicode escape");
                            String hex = src.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                        }
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private Number parseNumber() {
            int start = pos;
            if (src.charAt(pos) == '-') pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            boolean isFloating = false;
            if (pos < src.length() && src.charAt(pos) == '.') {
                isFloating = true;
                pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
                isFloating = true;
                pos++;
                if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }

            String numStr = src.substring(start, pos);
            if (isFloating) {
                return Double.parseDouble(numStr);
            }
            long val = Long.parseLong(numStr);
            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                return (int) val;
            }
            return val;
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            } else if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid boolean literal at position " + pos);
        }

        private Object parseNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid null literal at position " + pos);
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            skipWhitespace();
            if (pos >= src.length()) return '\0';
            return src.charAt(pos);
        }

        private void consume(char expected) {
            skipWhitespace();
            if (pos >= src.length() || src.charAt(pos) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + pos);
            }
            pos++;
        }
    }
}
