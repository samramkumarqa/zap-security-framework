package io.github.samramkumarqa.zap.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Settings in priority order:
 * 1. JVM system property   (mvn verify -Dzap.port=8080)
 * 2. Environment variable  (ZAP_PORT=8080; dots become underscores, upper case)
 * 3. config.properties on the test classpath
 *
 * The ZAP API key is read from this chain too, but belongs in an environment variable or CI secret, never in the file.
 */
public final class Config {

    private static final Properties FILE = load();

    private Config() {
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read config.properties", e);
        }
        return props;
    }

    public static String getOrNull(String key) {
        String value = System.getProperty(key);
        if (isBlank(value)) {
            value = System.getenv(key.toUpperCase().replace('.', '_'));
        }
        if (isBlank(value)) {
            value = FILE.getProperty(key);
        }
        return isBlank(value) ? null : value.trim();
    }

    public static String get(String key) {
        String value = getOrNull(key);
        if (value == null) {
            throw new IllegalStateException("Missing required setting: " + key);
        }
        return value;
    }

    public static String get(String key, String fallback) {
        String value = getOrNull(key);
        return value == null ? fallback : value;
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static String targetUrl() {
        return get("target.url").replaceAll("/+$", "");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
