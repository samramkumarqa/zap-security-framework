package io.github.samramkumarqa.zap.support;

/** ZAP risk levels and their API ids. */
public enum Risk {
    INFORMATIONAL(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int id;

    Risk(int id) {
        this.id = id;
    }

    public String apiId() {
        return String.valueOf(id);
    }

    /** Parse ZAP's "High", "Medium (Low)", "Low" labels. The confidence in parentheses is ignored. */
    public static Risk fromLabel(String label) {
        String head = label.split("\\(")[0].trim().toUpperCase();
        return switch (head) {
            case "HIGH" -> HIGH;
            case "MEDIUM" -> MEDIUM;
            case "LOW" -> LOW;
            default -> INFORMATIONAL;
        };
    }
}
