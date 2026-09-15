package com.lin.erp.config;

import com.lin.erp.logging.AppLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.net.URLEncoder;
import java.util.Properties;

public class DbConfig {
    private final boolean enabled;
    private final boolean fallbackToDemo;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSsl;
    private final boolean allowPublicKeyRetrieval;
    private final String serverTimezone;
    private final int connectTimeoutMs;
    private final int socketTimeoutMs;

    private DbConfig(Properties properties) {
        enabled = parseBoolean(properties, "db.enabled", false);
        fallbackToDemo = parseBoolean(properties, "db.fallbackToDemo", true);
        host = properties.getProperty("db.host", "localhost").trim();
        port = parseInt(properties, "db.port", 3306, 1, 65535);
        database = properties.getProperty("db.database", "linova_erp").trim();
        username = properties.getProperty("db.username", "").trim();
        password = properties.getProperty("db.password", "");
        useSsl = parseBoolean(properties, "db.useSsl", true);
        allowPublicKeyRetrieval = parseBoolean(properties, "db.allowPublicKeyRetrieval", false);
        serverTimezone = properties.getProperty("db.serverTimezone", "UTC").trim();
        connectTimeoutMs = parseInt(properties, "db.connectTimeoutMs", 3000, 500, 60000);
        socketTimeoutMs = parseInt(properties, "db.socketTimeoutMs", 5000, 500, 120000);
        validate();
    }

    public static DbConfig loadDefault() {
        Properties properties = new Properties();
        File file = new File("config", "db.properties");
        if (file.isFile()) {
            FileInputStream input = null;
            try {
                input = new FileInputStream(file);
                properties.load(input);
            } catch (IOException e) {
                AppLogger.error("Failed to read config/db.properties.", e);
                throw new IllegalStateException("Cannot read config/db.properties. Check file permissions and encoding.", e);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return new DbConfig(properties);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFallbackToDemo() {
        return fallbackToDemo;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String jdbcUrl() {
        return "jdbc:mysql://" + jdbcHost() + ":" + port + "/" + urlEncode(database)
                + "?useUnicode=true"
                + "&characterEncoding=utf8"
                + "&useSSL=" + useSsl
                + "&allowPublicKeyRetrieval=" + allowPublicKeyRetrieval
                + "&serverTimezone=" + urlEncode(serverTimezone)
                + "&connectTimeout=" + connectTimeoutMs
                + "&socketTimeout=" + socketTimeoutMs;
    }

    public String summary() {
        return host + ":" + port + "/" + database;
    }

    private void validate() {
        if (!enabled) {
            return;
        }
        if (host.length() == 0) {
            throw new IllegalArgumentException("db.host is required when db.enabled=true.");
        }
        if (host.indexOf('/') >= 0 || host.indexOf('\\') >= 0 || host.indexOf('?') >= 0 || host.indexOf('#') >= 0) {
            throw new IllegalArgumentException("db.host contains invalid URL characters.");
        }
        if (database.length() == 0) {
            throw new IllegalArgumentException("db.database is required when db.enabled=true.");
        }
        if (database.indexOf('/') >= 0 || database.indexOf('\\') >= 0 || database.indexOf('?') >= 0 || database.indexOf('#') >= 0) {
            throw new IllegalArgumentException("db.database contains invalid URL characters.");
        }
        if (serverTimezone.length() == 0) {
            throw new IllegalArgumentException("db.serverTimezone is required.");
        }
    }

    private static boolean parseBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().length() == 0) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase();
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        throw new IllegalArgumentException(key + " must be true or false.");
    }

    private static int parseInt(Properties properties, String key, int fallback, int min, int max) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().length() == 0) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException(key + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number.", e);
        }
    }

    private String jdbcHost() {
        if (host.startsWith("[") && host.endsWith("]")) {
            return host;
        }
        if (host.indexOf(':') >= 0) {
            return "[" + host + "]";
        }
        return IDN.toASCII(host);
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not available.", e);
        }
    }
}
