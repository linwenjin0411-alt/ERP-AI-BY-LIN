package com.lin.erp.db;

import com.lin.erp.config.DbConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Properties;

public class DbLicenseRepository {
    private final DbConfig config;

    public DbLicenseRepository(DbConfig config) {
        this.config = config;
    }

    public LicenseStatus currentStatus() throws SQLException {
        if (!config.isEnabled()) {
            return currentLocalStatus();
        }
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
        LocalDate validUntil = parseValidUntil(licenseKey);
        if (validUntil == null || validUntil.isBefore(LocalDate.now())) {
            return new LicenseStatus(false, licenseKey, validUntil);
        }
        if (!config.isEnabled()) {
            return registerLocalLicense(licenseKey, validUntil);
        }

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
            LocalDate validUntil = parseValidUntil(key);
            return new LicenseStatus(validUntil != null && !validUntil.isBefore(LocalDate.now()), key, validUntil);
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

    private LocalDate parseValidUntil(String licenseKey) {
        if (licenseKey == null) {
            return null;
        }
        String normalized = licenseKey.trim().toUpperCase();
        if (!normalized.startsWith("LINOVA-")) {
            return null;
        }
        String datePart = normalized.substring("LINOVA-".length()).replace("-", "");
        if (datePart.length() != 8) {
            return null;
        }
        try {
            int year = Integer.parseInt(datePart.substring(0, 4));
            int month = Integer.parseInt(datePart.substring(4, 6));
            int day = Integer.parseInt(datePart.substring(6, 8));
            return LocalDate.of(year, month, day);
        } catch (RuntimeException e) {
            return null;
        }
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
}
