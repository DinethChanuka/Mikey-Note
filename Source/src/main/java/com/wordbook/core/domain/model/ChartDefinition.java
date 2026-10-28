package com.wordbook.core.domain.model;

/**
 * Model representing a chart specification and its raw textual data.
 */
public record ChartDefinition(
        ChartType type,
        String rawData,
        String renderedSvg
) {
    public enum ChartType {
        AUTO("auto"),
        BAR("bar"),
        COLUMN("column"),
        PIE("pie"),
        DONUT("donut"),
        LINE("line"),
        AREA("area"),
        RADAR("radar"),
        SCATTER("scatter"),
        TREE("tree"),
        DIAGRAM("diagram"),
        LOLLIPOP("lollipop"),
        WATERFALL("waterfall"),
        FUNNEL("funnel"),
        GAUGE("gauge"),
        POLAR("polar");

        private final String code;

        ChartType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static ChartType fromString(String val) {
            if (val == null) return AUTO;
            String normalized = val.trim().toLowerCase();
            for (ChartType type : values()) {
                if (type.code.equalsIgnoreCase(normalized) || type.name().equalsIgnoreCase(normalized)) {
                    return type;
                }
            }
            return AUTO;
        }
    }
}
