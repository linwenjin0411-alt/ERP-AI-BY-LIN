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
        enabled = Boolean.parseBoolean(properties.getProperty("db.enabled", "false"));
        fallbackToDemo = Boolean.parseBoolean(properties.getProperty("db.fallbackToDemo", "true"));
        host = properties.getProperty("db.host", "localhost").trim();
        port = parseInt(properties.getProperty("db.port"), 3306);
        database = properties.getProperty("db.database", "linova_erp").trim();
        username = properties.getProperty("db.username", "").trim();
        password = properties.getProperty("db.password", "");
        useSsl = Boolean.parseBoolean(properties.getProperty("db.useSsl", "true"));
        allowPublicKeyRetrieval = Boolean.parseBoolean(properties.getProperty("db.allowPublicKeyRetrieval", "false"));
        serverTimezone = properties.getProperty("db.serverTimezone", "UTC").trim();
        connectTimeoutMs = parseInt(properties.getProperty("db.connectTimeoutMs"), 3000);
        socketTimeoutMs = parseInt(properties.getProperty("db.socketTimeoutMs"), 5000);
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

    private int parseInt(String value, int fallback) {
        if (value == null || value.trim().length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
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
