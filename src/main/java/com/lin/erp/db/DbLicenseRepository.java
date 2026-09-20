package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.SocketTimeoutException;
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
    private static final String LICENSE_SCOPE = PRODUCT_CODE;
    private static final String SOURCE_DEMO = "demo";
    private static final String SOURCE_DATABASE = "database";
    private static final String SOURCE_ONLINE = "online";

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
                if (!sameDate(databaseStatus.getValidUntil(), verified.getValidUntil())
                        || metadataChanged(databaseStatus, verified)) {
                    refreshDatabaseLicense(databaseStatus.getLicenseKey(), verified);
                }
                return status(true, databaseStatus.getLicenseKey(), verified.getValidUntil(),
                        verified.getReasonCode(), verified.getDetailMessage(), verified.getCustomerName(),
                        verified.getProductCode(), verified.getModules(), verified.getSeatPolicy(),
                        verified.isDeviceBindingEnabled(), SOURCE_ONLINE);
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
                    "select license_key, valid_until, customer_name, product_code, modules, seat_policy, device_binding "
                            + "from erp_licenses "
                            + "where active = 1 and license_scope = ? order by valid_until desc, id desc limit 1"
            );
            statement.setString(1, LICENSE_SCOPE);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return status(false, null, null, "NO_LICENSE", "No active database license.", "", "", "", "", false, SOURCE_DATABASE);
            }
            Date date = resultSet.getDate("valid_until");
            if (date == null) {
                return status(false, resultSet.getString("license_key"), null, "DB_DATE_INVALID",
                        "Active database license has no valid_until date.", string(resultSet, "customer_name"),
                        string(resultSet, "product_code"), string(resultSet, "modules"),
                        string(resultSet, "seat_policy"), resultSet.getBoolean("device_binding"), SOURCE_DATABASE);
            }
            LocalDate validUntil = date.toLocalDate();
            boolean valid = !validUntil.isBefore(LocalDate.now());
            return status(valid, resultSet.getString("license_key"), validUntil,
                    valid ? "DB_ACTIVE" : "EXPIRED", valid ? "Active database license." : "Database license is expired.",
                    string(resultSet, "customer_name"), string(resultSet, "product_code"), string(resultSet, "modules"),
                    string(resultSet, "seat_policy"), resultSet.getBoolean("device_binding"), SOURCE_DATABASE);
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
                return status(false, licenseKey, null, "EMPTY_KEY", "License key is required.", "", "", "", "", false, SOURCE_DEMO);
            }
            LocalDate validUntil = LocalDate.now().plusDays(loadLicenseApiConfig().getCacheDays());
            return registerLocalLicense(normalized, validUntil);
        }

        String normalized = licenseKey == null ? "" : licenseKey.trim();
        if (normalized.length() == 0) {
            return status(false, licenseKey, null, "EMPTY_KEY", "License key is required.", "", "", "", "", false, SOURCE_ONLINE);
        }

        LicenseStatus apiStatus = verifyConfiguredApi(normalized, userCode);
        if (apiStatus.isValid()) {
            return registerDatabaseLicense(normalized, apiStatus, userCode);
        }
        return apiStatus;
    }

    private LicenseStatus registerDatabaseLicense(String licenseKey, LocalDate validUntil) throws SQLException {
        return registerDatabaseLicense(licenseKey, status(true, licenseKey, validUntil, "ONLINE_VALID",
                "License accepted by online API.", "", PRODUCT_CODE, "", "", false, SOURCE_ONLINE), "");
    }

    private LicenseStatus registerDatabaseLicense(String licenseKey, LicenseStatus apiStatus, String userCode) throws SQLException {
        Connection connection = null;
        PreparedStatement deactivate = null;
        PreparedStatement insert = null;
        boolean originalAutoCommit = true;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            deactivate = connection.prepareStatement(
                    "update erp_licenses set active = 0, revoked_at = current_timestamp, updated_by = ? "
                            + "where active = 1 and license_scope = ?"
            );
            deactivate.setString(1, auditUser(userCode));
            deactivate.setString(2, LICENSE_SCOPE);
            deactivate.executeUpdate();
            insert = connection.prepareStatement(
                    "insert into erp_licenses (license_key, license_scope, valid_from, valid_until, active, created_by, updated_by, "
                            + "source_machine, customer_name, product_code, modules, seat_policy, device_binding, "
                            + "last_verified_at, verify_source) values (?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, ?)"
            );
            insert.setString(1, licenseKey.trim());
            insert.setString(2, LICENSE_SCOPE);
            insert.setDate(3, Date.valueOf(LocalDate.now()));
            insert.setDate(4, Date.valueOf(apiStatus.getValidUntil()));
            insert.setString(5, auditUser(userCode));
            insert.setString(6, auditUser(userCode));
            insert.setString(7, localMachineName());
            insert.setString(8, emptyToNull(apiStatus.getCustomerName()));
            insert.setString(9, emptyToNull(apiStatus.getProductCode()));
            insert.setString(10, emptyToNull(apiStatus.getModules()));
            insert.setString(11, emptyToNull(apiStatus.getSeatPolicy()));
            insert.setBoolean(12, apiStatus.isDeviceBindingEnabled());
            insert.setString(13, SOURCE_ONLINE);
            insert.executeUpdate();
            connection.commit();
            return status(true, licenseKey, apiStatus.getValidUntil(), apiStatus.getReasonCode(),
                    apiStatus.getDetailMessage(), apiStatus.getCustomerName(), apiStatus.getProductCode(),
                    apiStatus.getModules(), apiStatus.getSeatPolicy(), apiStatus.isDeviceBindingEnabled(),
                    SOURCE_DATABASE);
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

    private void refreshDatabaseLicense(String licenseKey, LicenseStatus apiStatus) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(
                    "update erp_licenses set valid_until = ?, customer_name = ?, product_code = ?, modules = ?, "
                            + "seat_policy = ?, device_binding = ?, last_verified_at = current_timestamp, "
                            + "verify_source = ?, updated_by = ? where active = 1 and license_scope = ? and license_key = ?"
            );
            statement.setDate(1, Date.valueOf(apiStatus.getValidUntil()));
            statement.setString(2, emptyToNull(apiStatus.getCustomerName()));
            statement.setString(3, emptyToNull(apiStatus.getProductCode()));
            statement.setString(4, emptyToNull(apiStatus.getModules()));
            statement.setString(5, emptyToNull(apiStatus.getSeatPolicy()));
            statement.setBoolean(6, apiStatus.isDeviceBindingEnabled());
            statement.setString(7, SOURCE_ONLINE);
            statement.setString(8, auditUser());
            statement.setString(9, LICENSE_SCOPE);
            statement.setString(10, licenseKey);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    public void revokeCurrentLicense(String userCode) throws SQLException {
        if (!config.isEnabled()) {
            return;
        }
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = Database.connect(config);
            ensureSchema(connection);
            statement = connection.prepareStatement(
                    "update erp_licenses set active = 0, revoked_at = current_timestamp, updated_by = ? "
                            + "where active = 1 and license_scope = ?"
            );
            statement.setString(1, auditUser(userCode));
            statement.setString(2, LICENSE_SCOPE);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    private LicenseStatus verifyConfiguredApi(String licenseKey, String userCode) throws SQLException {
        LicenseApiConfig apiConfig = loadLicenseApiConfig();
        if (!apiConfig.isConfigured()) {
            return status(false, licenseKey, null, "CONFIG_MISSING",
                    "Formal license API is not configured.", "", "", "", "", false, SOURCE_ONLINE);
        }
        if (licenseKey == null || licenseKey.trim().length() == 0) {
            return status(false, licenseKey, null, "EMPTY_KEY", "License key is required.", "", "", "", "", false, SOURCE_ONLINE);
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiConfig.getVerifyApiUrl(licenseKey, userCode, localMachineName()));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(apiConfig.getTimeoutMs());
            connection.setReadTimeout(apiConfig.getTimeoutMs());
            connection.setUseCaches(false);
            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                ApiLicenseResponse response = responseLicense(connection, licenseKey);
                if (response.valid) {
                    return response.toStatus(licenseKey, SOURCE_ONLINE);
                }
                return response.toStatus(licenseKey, SOURCE_ONLINE);
            }
            return status(false, licenseKey, null, httpReason(statusCode),
                    "License API rejected the request with HTTP " + statusCode + ".", "", "", "", "", false,
                    SOURCE_ONLINE);
        } catch (SocketTimeoutException e) {
            return status(false, licenseKey, null, "NETWORK_TIMEOUT", e.getMessage(), "", "", "", "", false, SOURCE_ONLINE);
        } catch (IOException e) {
            return status(false, licenseKey, null, "NETWORK_ERROR", e.getMessage(), "", "", "", "", false, SOURCE_ONLINE);
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
                return ApiLicenseResponse.invalid(null, apiReason(body, "BUSINESS_REJECTED"), jsonString(body, "message"));
            }
            String product = jsonString(body, "product_code");
            if (product != null && !PRODUCT_CODE.equals(product.trim())) {
                return ApiLicenseResponse.invalid(null, "PRODUCT_MISMATCH", "License is for a different product.");
            }
            String responseKey = jsonString(body, "license_key");
            if (responseKey != null && responseKey.trim().length() > 0
                    && !responseKey.trim().equals(licenseKey.trim())) {
                return ApiLicenseResponse.invalid(null, "RESPONSE_MISMATCH", "License API returned a different key.");
            }
            String expiresAt = jsonString(body, "expires_at");
            if (expiresAt == null) {
                return ApiLicenseResponse.invalid(null, "RESPONSE_INVALID", "License API response has no expiry date.");
            }
            LocalDate validUntil = LocalDate.parse(expiresAt.trim());
            if (validUntil.isBefore(LocalDate.now())) {
                return ApiLicenseResponse.invalid(validUntil, "EXPIRED", "License is expired.");
            }
            return ApiLicenseResponse.valid(validUntil, product, jsonString(body, "customer_name"),
                    jsonString(body, "customer"), jsonArrayOrString(body, "modules"), jsonString(body, "seat_policy"),
                    jsonString(body, "seatPolicy"), jsonBoolean(body, "device_binding"));
        } catch (Exception ignored) {
            return ApiLicenseResponse.invalid(null, "RESPONSE_INVALID", "License API response could not be parsed.");
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
            return status(false, null, null, "NO_DEMO_CACHE", "No local demo license cache.", "", "", "", "", false, SOURCE_DEMO);
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
                return status(valid, key, validUntil, valid ? "DEMO_CACHE_VALID" : "DEMO_CACHE_EXPIRED",
                        valid ? "Local demo license cache is valid." : "Local demo license cache is expired.",
                        "Demo", PRODUCT_CODE, "Demo", "Demo", false, SOURCE_DEMO);
            }
            LicenseKeyVerifier.Result verification = LicenseKeyVerifier.verify(key, false);
            return status(verification.isValid(), key, verification.getValidUntil(),
                    verification.isValid() ? "SIGNATURE_VALID" : "SIGNATURE_INVALID",
                    "Offline signature compatibility check.", "", PRODUCT_CODE, "", "", false, SOURCE_DEMO);
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
            return status(true, licenseKey, validUntil, "DEMO_REGISTERED", "Local demo license cache was registered.",
                    "Demo", PRODUCT_CODE, "Demo", "Demo", false, SOURCE_DEMO);
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
                            + "license_key varchar(500) not null,"
                            + "license_scope varchar(120) not null default 'LinovaOneERP',"
                            + "valid_from date not null,"
                            + "valid_until date not null,"
                            + "active tinyint(1) not null default 1,"
                            + "active_scope varchar(120) generated always as (case when active = 1 then license_scope else null end) stored,"
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
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "license_scope", "license_scope varchar(120) not null default 'LinovaOneERP'");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "active_scope", "active_scope varchar(120) generated always as (case when active = 1 then license_scope else null end) stored");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "created_by", "created_by varchar(80)");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "updated_by", "updated_by varchar(80)");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "source_machine", "source_machine varchar(160)");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "customer_name", "customer_name varchar(160)");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "product_code", "product_code varchar(80)");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "modules", "modules varchar(500)");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "seat_policy", "seat_policy varchar(160)");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "device_binding", "device_binding tinyint(1) not null default 0");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "last_verified_at", "last_verified_at timestamp null");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "verify_source", "verify_source varchar(40)");
        DatabaseSchema.ensureColumn(connection, "erp_licenses", "revoked_at", "revoked_at timestamp null");
        collapseDuplicateActiveLicenses(connection);
        DatabaseSchema.ensureIndex(connection, "erp_licenses", "uk_erp_licenses_active_scope", "unique key uk_erp_licenses_active_scope (active_scope)");
        DatabaseSchema.ensureIndex(connection, "erp_licenses", "idx_erp_licenses_scope_active", "index idx_erp_licenses_scope_active (license_scope, active)");
    }

    private void collapseDuplicateActiveLicenses(Connection connection) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "update erp_licenses l "
                            + "join (select license_scope, max(id) as keep_id from erp_licenses "
                            + "where active = 1 group by license_scope having count(*) > 1) d "
                            + "on d.license_scope = l.license_scope and l.id <> d.keep_id "
                            + "set l.active = 0, l.revoked_at = coalesce(l.revoked_at, current_timestamp), l.updated_by = 'system' "
                            + "where l.active = 1"
            );
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
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
                    parseInt(properties.getProperty("license.timeoutMs"), DEFAULT_API_TIMEOUT_MS),
                    Boolean.parseBoolean(properties.getProperty("license.deviceBinding.enabled", "false"))
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
        private final boolean deviceBindingEnabled;

        private LicenseApiConfig(String verifyApiUrl, int cacheDays, int timeoutMs, boolean deviceBindingEnabled) {
            this.verifyApiUrl = verifyApiUrl;
            this.cacheDays = cacheDays <= 0 ? DEFAULT_API_CACHE_DAYS : cacheDays;
            this.timeoutMs = timeoutMs <= 0 ? DEFAULT_API_TIMEOUT_MS : timeoutMs;
            this.deviceBindingEnabled = deviceBindingEnabled;
        }

        private static LicenseApiConfig empty() {
            return new LicenseApiConfig("", DEFAULT_API_CACHE_DAYS, DEFAULT_API_TIMEOUT_MS, false);
        }

        private boolean isConfigured() {
            return verifyApiUrl.length() > 0;
        }

        private String getVerifyApiUrl(String licenseKey, String userCode, String machineCode) throws IOException {
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
                if (deviceBindingEnabled) {
                    params.put("machine_code", machineCode == null ? "" : machineCode.trim());
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

    private static boolean metadataChanged(LicenseStatus left, LicenseStatus right) {
        return !sameText(left.getCustomerName(), right.getCustomerName())
                || !sameText(left.getProductCode(), right.getProductCode())
                || !sameText(left.getModules(), right.getModules())
                || !sameText(left.getSeatPolicy(), right.getSeatPolicy())
                || left.isDeviceBindingEnabled() != right.isDeviceBindingEnabled();
    }

    private static boolean sameText(String left, String right) {
        return value(left).equals(value(right));
    }

    private static boolean jsonBoolean(String body, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*(true|false)").matcher(body);
        return matcher.find() && "true".equals(matcher.group(1));
    }

    private static String jsonString(String body, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
        return matcher.find() ? unescapeJson(matcher.group(1)) : null;
    }

    private static String jsonArrayOrString(String body, String name) {
        String value = jsonString(body, name);
        if (value != null) {
            return value;
        }
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(body);
        if (!matcher.find()) {
            return "";
        }
        String raw = matcher.group(1);
        Matcher item = Pattern.compile("\"([^\"]*)\"").matcher(raw);
        StringBuilder builder = new StringBuilder();
        while (item.find()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(unescapeJson(item.group(1)));
        }
        return builder.toString();
    }

    private static String apiReason(String body, String fallback) {
        String code = jsonString(body, "error_code");
        if (code == null || code.trim().length() == 0) {
            code = jsonString(body, "reason_code");
        }
        return code == null || code.trim().length() == 0 ? fallback : code.trim().toUpperCase(Locale.ROOT);
    }

    private static String unescapeJson(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String auditUser() {
        return auditUser("");
    }

    private static String auditUser(String userCode) {
        if (userCode != null && userCode.trim().length() > 0) {
            return userCode.trim();
        }
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

    private static String string(ResultSet resultSet, String name) throws SQLException {
        String value = resultSet.getString(name);
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().length() == 0 ? null : value.trim();
    }

    private static String value(String text) {
        return text == null ? "" : text.trim();
    }

    private static String httpReason(int statusCode) {
        if (statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
            return "BUSINESS_REJECTED";
        }
        if (statusCode == HttpURLConnection.HTTP_CONFLICT) {
            return "OVER_SEAT";
        }
        if (statusCode == 422) {
            return "EXPIRED";
        }
        return "HTTP_" + statusCode;
    }

    private static LicenseStatus status(boolean valid, String licenseKey, LocalDate validUntil, String reasonCode,
            String detailMessage, String customerName, String productCode, String modules, String seatPolicy,
            boolean deviceBindingEnabled, String source) {
        return new LicenseStatus(valid, licenseKey, validUntil, reasonCode, detailMessage, customerName,
                productCode, modules, seatPolicy, deviceBindingEnabled, source);
    }

    private static final class ApiLicenseResponse {
        private final boolean valid;
        private final LocalDate validUntil;
        private final String reasonCode;
        private final String detailMessage;
        private final String productCode;
        private final String customerName;
        private final String modules;
        private final String seatPolicy;
        private final boolean deviceBindingEnabled;

        private ApiLicenseResponse(boolean valid, LocalDate validUntil, String reasonCode, String detailMessage,
                String productCode, String customerName, String modules, String seatPolicy, boolean deviceBindingEnabled) {
            this.valid = valid;
            this.validUntil = validUntil;
            this.reasonCode = value(reasonCode);
            this.detailMessage = value(detailMessage);
            this.productCode = value(productCode);
            this.customerName = value(customerName);
            this.modules = value(modules);
            this.seatPolicy = value(seatPolicy);
            this.deviceBindingEnabled = deviceBindingEnabled;
        }

        private static ApiLicenseResponse valid(LocalDate validUntil, String productCode, String customerName,
                String fallbackCustomerName, String modules, String seatPolicy, String fallbackSeatPolicy,
                boolean deviceBindingEnabled) {
            String actualCustomer = customerName == null || customerName.trim().length() == 0 ? fallbackCustomerName : customerName;
            String actualSeatPolicy = seatPolicy == null || seatPolicy.trim().length() == 0 ? fallbackSeatPolicy : seatPolicy;
            return new ApiLicenseResponse(true, validUntil, "ONLINE_VALID", "License accepted by online API.",
                    productCode == null || productCode.trim().length() == 0 ? PRODUCT_CODE : productCode,
                    actualCustomer, modules, actualSeatPolicy, deviceBindingEnabled);
        }

        private static ApiLicenseResponse invalid(LocalDate validUntil, String reasonCode, String detailMessage) {
            return new ApiLicenseResponse(false, validUntil, reasonCode, detailMessage, "", "", "", "", false);
        }

        private LicenseStatus toStatus(String licenseKey, String source) {
            return status(valid, licenseKey, validUntil, reasonCode, detailMessage, customerName, productCode,
                    modules, seatPolicy, deviceBindingEnabled, source);
        }
    }
}
