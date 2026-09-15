package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DbLicenseRepository {
    private static final int DEFAULT_API_TIMEOUT_MS = 5000;
    private static final int DEFAULT_API_CACHE_DAYS = 30;
    private static final int MAX_API_RESPONSE_BYTES = 65536;
    private static final String PRODUCT_CODE = "LinovaOneERP";

    private final DbConfig config;

    public DbLicenseRepository(DbConfig config) {
        this.config = config;
    }

    public LicenseStatus currentStatus() throws SQLException {
        return currentStatus("");
    }

    public LicenseStatus currentStatus(String userCode) throws SQLException {
        if (!config.isEnabled()) {
            return currentLocalStatus();
        }
        LicenseStatus databaseStatus = currentDatabaseStatus();
        String currentKey = databaseStatus.getLicenseKey();
        if (currentKey != null && currentKey.trim().length() > 0) {
            LicenseStatus verified = verifyConfiguredApi(databaseStatus.getLicenseKey(), userCode);
            if (verified.isValid()) {
                if (!sameDate(databaseStatus.getValidUntil(), verified.getValidUntil())) {
                    registerDatabaseLicense(databaseStatus.getLicenseKey(), verified.getValidUntil());
                }
                return new LicenseStatus(true, databaseStatus.getLicenseKey(), verified.getValidUntil());
            }
            return verified;
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
            Date date = resultSet.getDate("valid_until");
            if (date == null) {
                return new LicenseStatus(false, resultSet.getString("license_key"), null);
            }
            LocalDate validUntil = date.toLocalDate();
            return new LicenseStatus(!validUntil.isBefore(LocalDate.now()), resultSet.getString("license_key"), validUntil);
        } finally {
            close(resultSet, statement, connection);
        }
    }

    public LicenseStatus registerLicense(String licenseKey) throws SQLException {
        return registerLicense(licenseKey, "");
    }

    public LicenseStatus registerLicense(String licenseKey, String userCode) throws SQLException {
        if (!config.isEnabled()) {
            String normalized = licenseKey == null ? "" : licenseKey.trim();
            if (normalized.length() == 0) {
                return new LicenseStatus(false, licenseKey, null);
            }
            LocalDate validUntil = LocalDate.now().plusDays(loadLicenseApiConfig().getCacheDays());
            return registerLocalLicense(normalized, validUntil);
        }

        String normalized = licenseKey == null ? "" : licenseKey.trim();
        if (normalized.length() == 0) {
            return new LicenseStatus(false, licenseKey, null);
        }

        LicenseStatus apiStatus = verifyConfiguredApi(normalized, userCode);
        if (apiStatus.isValid()) {
            LocalDate validUntil = apiStatus.getValidUntil();
            return registerDatabaseLicense(normalized, validUntil);
        }
        return apiStatus;
    }

    private LicenseStatus registerDatabaseLicense(String licenseKey, LocalDate validUntil) throws SQLException {
        Connection connection = null;
        PreparedStatement deactivate = null;
        PreparedStatement insert = null;
        boolean originalAutoCommit = true;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            deactivate = connection.prepareStatement("update erp_licenses set active = 0, updated_by = ? where active = 1");
            deactivate.setString(1, auditUser());
            deactivate.executeUpdate();
            insert = connection.prepareStatement(
                    "insert into erp_licenses (license_key, valid_from, valid_until, active, created_by, updated_by, source_machine) "
                            + "values (?, ?, ?, 1, ?, ?, ?)"
            );
            insert.setString(1, licenseKey.trim());
            insert.setDate(2, Date.valueOf(LocalDate.now()));
            insert.setDate(3, Date.valueOf(validUntil));
            insert.setString(4, auditUser());
            insert.setString(5, auditUser());
            insert.setString(6, localMachineName());
            insert.executeUpdate();
            connection.commit();
            return new LicenseStatus(true, licenseKey, validUntil);
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    // Keep the original license registration error.
                }
            }
            throw e;
        } finally {
            if (deactivate != null) {
                deactivate.close();
            }
            if (insert != null) {
                insert.close();
            }
            if (connection != null) {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {
                    // The connection is closing; keep any previous failure.
                }
                connection.close();
            }
        }
    }

    private LicenseStatus verifyConfiguredApi(String licenseKey, String userCode) throws SQLException {
        LicenseApiConfig apiConfig = loadLicenseApiConfig();
        if (!apiConfig.isConfigured()) {
            return new LicenseStatus(false, licenseKey, null);
        }
        if (licenseKey == null || licenseKey.trim().length() == 0) {
            return new LicenseStatus(false, licenseKey, null);
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiConfig.getVerifyApiUrl(licenseKey, userCode));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(apiConfig.getTimeoutMs());
            connection.setReadTimeout(apiConfig.getTimeoutMs());
            connection.setUseCaches(false);
            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                ApiLicenseResponse response = responseLicense(connection, licenseKey);
                if (response.valid) {
                    return new LicenseStatus(true, licenseKey, response.validUntil);
                }
                return new LicenseStatus(false, licenseKey, response.validUntil);
            }
            return new LicenseStatus(false, licenseKey, null);
        } catch (IOException e) {
            return new LicenseStatus(false, licenseKey, null);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private ApiLicenseResponse responseLicense(HttpURLConnection connection, String licenseKey) {
        try {
            byte[] bytes = readAll(connection.getInputStream());
            String body = new String(bytes, StandardCharsets.UTF_8);
            if (!jsonBoolean(body, "ok")) {
                return ApiLicenseResponse.invalid(null);
            }
            String product = jsonString(body, "product_code");
            if (product != null && !PRODUCT_CODE.equals(product.trim())) {
                return ApiLicenseResponse.invalid(null);
            }
            String responseKey = jsonString(body, "license_key");
            if (responseKey != null && responseKey.trim().length() > 0
                    && !responseKey.trim().equals(licenseKey.trim())) {
                return ApiLicenseResponse.invalid(null);
            }
            String expiresAt = jsonString(body, "expires_at");
            if (expiresAt == null) {
                return ApiLicenseResponse.invalid(null);
            }
            LocalDate validUntil = LocalDate.parse(expiresAt.trim());
            if (validUntil.isBefore(LocalDate.now())) {
                return ApiLicenseResponse.invalid(validUntil);
            }
            return ApiLicenseResponse.valid(validUntil);
        } catch (Exception ignored) {
            return ApiLicenseResponse.invalid(null);
        }
    }

    private byte[] readAll(java.io.InputStream input) throws IOException {
        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (output.size() + read > MAX_API_RESPONSE_BYTES) {
                    throw new IOException("License API response is too large.");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
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
            if (!config.isEnabled()) {
                LocalDate validUntil = parseLocalDate(properties.getProperty("license.validUntil"), null);
                if (validUntil == null) {
                    validUntil = LocalDate.now().plusDays(parseInt(properties.getProperty("license.cacheDays"), DEFAULT_API_CACHE_DAYS));
                }
                boolean valid = key != null && key.trim().length() > 0 && !validUntil.isBefore(LocalDate.now());
                return new LicenseStatus(valid, key, validUntil);
            }
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
        if (file.isFile()) {
            FileInputStream input = null;
            try {
                input = new FileInputStream(file);
                properties.load(input);
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
        properties.setProperty("license.key", licenseKey.trim());
        properties.setProperty("license.validUntil", validUntil.toString());
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
                            + "created_by varchar(80),"
                            + "updated_by varchar(80),"
                            + "source_machine varchar(160),"
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
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "created_by", "created_by varchar(80)");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "updated_by", "updated_by varchar(80)");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "source_machine", "source_machine varchar(160)");
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

    private LocalDate parseLocalDate(String value, LocalDate fallback) {
        if (value == null || value.trim().length() == 0) {
            return fallback;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
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

        private String getVerifyApiUrl(String licenseKey, String userCode) throws IOException {
            String url = verifyApiUrl.replace("{license_key}", urlEncode(licenseKey))
                    .replace("{license}", urlEncode(licenseKey));
            try {
                URI uri = new URI(url);
                Map<String, String> params = parseQuery(uri.getRawQuery());
                params.put("license_key", licenseKey);
                params.put("product_code", PRODUCT_CODE);
                String user = userCode == null ? "" : userCode.trim();
                if (user.length() > 0) {
                    params.put("user_code", user);
                }
                URI base = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, uri.getFragment());
                return appendEncodedQuery(base.toASCIIString(), buildQuery(params));
            } catch (URISyntaxException e) {
                throw new IOException("Invalid license API URL.", e);
            }
        }

        private int getCacheDays() {
            return cacheDays;
        }

        private int getTimeoutMs() {
            return timeoutMs;
        }

        private static Map<String, String> parseQuery(String rawQuery) throws IOException {
            Map<String, String> params = new LinkedHashMap<String, String>();
            if (rawQuery == null || rawQuery.length() == 0) {
                return params;
            }
            String[] pairs = rawQuery.split("&");
            for (String pair : pairs) {
                if (pair.length() == 0) {
                    continue;
                }
                int equals = pair.indexOf('=');
                String rawName = equals >= 0 ? pair.substring(0, equals) : pair;
                String rawValue = equals >= 0 ? pair.substring(equals + 1) : "";
                String name = urlDecode(rawName).toLowerCase(Locale.ROOT);
                if (params.containsKey(name)) {
                    throw new IOException("Duplicate license API query parameter: " + name);
                }
                params.put(name, urlDecode(rawValue));
            }
            return params;
        }

        private static String buildQuery(Map<String, String> params) {
            StringBuilder query = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
            }
            return query.toString();
        }

        private static String appendEncodedQuery(String baseUrl, String query) {
            int fragmentIndex = baseUrl.indexOf('#');
            if (fragmentIndex >= 0) {
                return baseUrl.substring(0, fragmentIndex) + "?" + query + baseUrl.substring(fragmentIndex);
            }
            return baseUrl + "?" + query;
        }
    }

    private static boolean sameDate(LocalDate left, LocalDate right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private static boolean jsonBoolean(String body, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*(true|false)").matcher(body);
        return matcher.find() && "true".equals(matcher.group(1));
    }

    private static String jsonString(String body, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
        return matcher.find() ? unescapeJson(matcher.group(1)) : null;
    }

    private static String unescapeJson(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String auditUser() {
        String value = System.getProperty("user.name", "system");
        return value == null || value.trim().length() == 0 ? "system" : value.trim();
    }

    private static String localMachineName() {
        String computer = System.getenv("COMPUTERNAME");
        if (computer == null || computer.trim().length() == 0) {
            computer = System.getenv("HOSTNAME");
        }
        return computer == null ? "" : computer.trim();
    }

    private static String urlDecode(String value) throws IOException {
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid URL encoding.", e);
        }
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (IOException e) {
            return value;
        }
    }

    private static final class ApiLicenseResponse {
        private final boolean valid;
        private final LocalDate validUntil;

        private ApiLicenseResponse(boolean valid, LocalDate validUntil) {
            this.valid = valid;
            this.validUntil = validUntil;
        }

        private static ApiLicenseResponse valid(LocalDate validUntil) {
            return new ApiLicenseResponse(true, validUntil);
        }

        private static ApiLicenseResponse invalid(LocalDate validUntil) {
            return new ApiLicenseResponse(false, validUntil);
        }
    }
}
