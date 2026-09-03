package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Properties;

public class DbLicenseRepository {
    private static final int DEFAULT_API_TIMEOUT_MS = 5000;
    private static final int DEFAULT_API_CACHE_DAYS = 30;

    private final DbConfig config;

    public DbLicenseRepository(DbConfig config) {
        this.config = config;
    }

    public LicenseStatus currentStatus() throws SQLException {
        if (!config.isEnabled()) {
            return currentLocalStatus();
        }
        LicenseStatus databaseStatus = currentDatabaseStatus();
        if (databaseStatus.isValid()) {
            return databaseStatus;
        }

        LicenseStatus apiStatus = verifyConfiguredApi(null);
        if (apiStatus.isValid()) {
            return registerDatabaseLicense(apiStatus.getLicenseKey(), apiStatus.getValidUntil());
        }
        return databaseStatus;
    }

    private LicenseStatus currentDatabaseStatus() throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(
                    "select license_key, valid_until from erp_licenses "
                            + "where active = 1 order by valid_until desc, id desc limit 1"
            );
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return new LicenseStatus(false, null, null);
            }
            LocalDate validUntil = resultSet.getDate("valid_until").toLocalDate();
            return new LicenseStatus(!validUntil.isBefore(LocalDate.now()), resultSet.getString("license_key"), validUntil);
        } finally {
            close(resultSet, statement, connection);
        }
    }

    public LicenseStatus registerLicense(String licenseKey) throws SQLException {
        LicenseStatus apiStatus = verifyConfiguredApi(licenseKey);
        if (apiStatus.isValid()) {
            LocalDate validUntil = apiStatus.getValidUntil();
            if (!config.isEnabled()) {
                return registerLocalLicense(apiStatus.getLicenseKey(), validUntil);
            }
            return registerDatabaseLicense(apiStatus.getLicenseKey(), validUntil);
        }

        LicenseKeyVerifier.Result verification = LicenseKeyVerifier.verify(licenseKey, false);
        if (!verification.isValid()) {
            return new LicenseStatus(false, licenseKey, verification.getValidUntil());
        }
        LocalDate validUntil = verification.getValidUntil();
        if (!config.isEnabled()) {
            return registerLocalLicense(licenseKey, validUntil);
        }
        return registerDatabaseLicense(licenseKey, validUntil);
    }

    private LicenseStatus registerDatabaseLicense(String licenseKey, LocalDate validUntil) throws SQLException {
        Connection connection = null;
        PreparedStatement deactivate = null;
        PreparedStatement insert = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            deactivate = connection.prepareStatement("update erp_licenses set active = 0 where active = 1");
            deactivate.executeUpdate();
            insert = connection.prepareStatement(
                    "insert into erp_licenses (license_key, valid_from, valid_until, active) values (?, ?, ?, 1)"
            );
            insert.setString(1, licenseKey.trim());
            insert.setDate(2, Date.valueOf(LocalDate.now()));
            insert.setDate(3, Date.valueOf(validUntil));
            insert.executeUpdate();
            return new LicenseStatus(true, licenseKey, validUntil);
        } finally {
            if (deactivate != null) {
                deactivate.close();
            }
            if (insert != null) {
                insert.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    private LicenseStatus verifyConfiguredApi(String licenseKey) throws SQLException {
        LicenseApiConfig apiConfig = loadLicenseApiConfig();
        if (!apiConfig.isConfigured()) {
            return new LicenseStatus(false, null, null);
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiConfig.getVerifyApiUrl(licenseKey));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(apiConfig.getTimeoutMs());
            connection.setReadTimeout(apiConfig.getTimeoutMs());
            connection.setUseCaches(false);
            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                LocalDate validUntil = LocalDate.now().plusDays(apiConfig.getCacheDays());
                return new LicenseStatus(true, apiConfig.getDatabaseLicenseKey(licenseKey), validUntil);
            }
            return new LicenseStatus(false, apiConfig.getDatabaseLicenseKey(licenseKey), null);
        } catch (IOException e) {
            return new LicenseStatus(false, licenseKey, null);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private LicenseStatus currentLocalStatus() throws SQLException {
        File file = localLicenseFile();
        if (!file.isFile()) {
            return new LicenseStatus(false, null, null);
        }
        Properties properties = new Properties();
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            properties.load(input);
            String key = properties.getProperty("license.key");
            LicenseKeyVerifier.Result verification = LicenseKeyVerifier.verify(key, false);
            return new LicenseStatus(verification.isValid(), key, verification.getValidUntil());
        } catch (IOException e) {
            throw new SQLException("Failed to read config/license.properties: " + e.getMessage(), e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                    // Nothing useful to do after reading the local license.
                }
            }
        }
    }

    private LicenseStatus registerLocalLicense(String licenseKey, LocalDate validUntil) throws SQLException {
        File file = localLicenseFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new SQLException("Failed to create config directory for local license.");
        }
        Properties properties = new Properties();
        properties.setProperty("license.key", licenseKey.trim());
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            properties.store(output, "Linova One ERP local license");
            return new LicenseStatus(true, licenseKey, validUntil);
        } catch (IOException e) {
            throw new SQLException("Failed to write config/license.properties: " + e.getMessage(), e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                    // The store call already completed or failed.
                }
            }
        }
    }

    private File localLicenseFile() {
        return new File("config", "license.properties");
    }

    private void ensureSchema(Connection connection) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "create table if not exists erp_licenses ("
                            + "id bigint primary key auto_increment,"
                            + "license_key varchar(160) not null,"
                            + "valid_from date not null,"
                            + "valid_until date not null,"
                            + "active tinyint(1) not null default 1,"
                            + "created_at timestamp not null default current_timestamp,"
                            + "updated_at timestamp not null default current_timestamp on update current_timestamp"
                            + ") engine=InnoDB default charset=utf8mb4"
            );
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        widenLicenseKey(connection);
    }

    private void widenLicenseKey(Connection connection) throws SQLException {
        ResultSet columns = null;
        PreparedStatement statement = null;
        try {
            columns = connection.getMetaData().getColumns(connection.getCatalog(), null, "erp_licenses", "license_key");
            if (columns.next() && columns.getInt("COLUMN_SIZE") >= 500) {
                return;
            }
            statement = connection.prepareStatement("alter table erp_licenses modify license_key varchar(500) not null");
            statement.executeUpdate();
        } finally {
            if (columns != null) {
                columns.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    private LicenseApiConfig loadLicenseApiConfig() throws SQLException {
        File file = localLicenseFile();
        if (!file.isFile()) {
            return LicenseApiConfig.empty();
        }
        Properties properties = new Properties();
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            properties.load(input);
            return new LicenseApiConfig(
                    properties.getProperty("license.verifyApiUrl", "").trim(),
                    parseInt(properties.getProperty("license.cacheDays"), DEFAULT_API_CACHE_DAYS),
                    parseInt(properties.getProperty("license.timeoutMs"), DEFAULT_API_TIMEOUT_MS)
            );
        } catch (IOException e) {
            throw new SQLException("Failed to read config/license.properties: " + e.getMessage(), e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                    // Nothing useful to do after reading the local license.
                }
            }
        }
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

    private void close(ResultSet resultSet, PreparedStatement statement, Connection connection) throws SQLException {
        SQLException error = null;
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            error = e;
        }
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            error = e;
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            error = e;
        }
        if (error != null) {
            throw error;
        }
    }

    private static final class LicenseApiConfig {
        private final String verifyApiUrl;
        private final int cacheDays;
        private final int timeoutMs;

        private LicenseApiConfig(String verifyApiUrl, int cacheDays, int timeoutMs) {
            this.verifyApiUrl = verifyApiUrl;
            this.cacheDays = cacheDays <= 0 ? DEFAULT_API_CACHE_DAYS : cacheDays;
            this.timeoutMs = timeoutMs <= 0 ? DEFAULT_API_TIMEOUT_MS : timeoutMs;
        }

        private static LicenseApiConfig empty() {
            return new LicenseApiConfig("", DEFAULT_API_CACHE_DAYS, DEFAULT_API_TIMEOUT_MS);
        }

        private boolean isConfigured() {
            return verifyApiUrl.length() > 0;
        }

        private String getVerifyApiUrl(String licenseKey) {
            String url = verifyApiUrl;
            String key = licenseKey == null ? "" : licenseKey.trim();
            if (key.length() > 0) {
                String encodedKey = urlEncode(key);
                url = url.replace("{license_key}", encodedKey).replace("{license}", encodedKey);
                if (!hasQueryParam(url, "license_key")) {
                    url = appendQueryParam(url, "license_key", encodedKey);
                }
            }
            if (!hasQueryParam(url, "product_code")) {
                url = appendQueryParam(url, "product_code", "LinovaOneERP");
            }
            return url;
        }

        private int getCacheDays() {
            return cacheDays;
        }

        private int getTimeoutMs() {
            return timeoutMs;
        }

        private String getDatabaseLicenseKey(String licenseKey) {
            String key = licenseKey == null ? "" : licenseKey.trim();
            return key.length() == 0 ? "API:" + verifyApiUrl : key;
        }

        private static boolean hasQueryParam(String url, String name) {
            return url.toLowerCase().contains(name.toLowerCase() + "=");
        }

        private static String appendQueryParam(String url, String name, String encodedValue) {
            String separator = url.contains("?") ? "&" : "?";
            if (url.endsWith("?") || url.endsWith("&")) {
                separator = "";
            }
            return url + separator + name + "=" + encodedValue;
        }

        private static String urlEncode(String value) {
            try {
                return URLEncoder.encode(value, "UTF-8");
            } catch (IOException e) {
                return value;
            }
        }
    }
}
