package io.github.samramkumarqa.zap.support;

/** One ZAP alert, flattened to what a report and a gate need. */
public record Alert(String name, Risk risk, String confidence, String url, String param) {

    @Override
    public String toString() {
        return "[" + risk + "] " + name + (param == null || param.isBlank() ? "" : " (param: " + param + ")") + " -> " + url;
    }
}
