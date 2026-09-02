package com.lin.erp.auth;

import com.lin.erp.config.DbConfig;
import com.lin.erp.db.DbUserRepository;
import com.lin.erp.i18n.I18n;
import com.lin.erp.i18n.Language;
import com.lin.erp.logging.AppLogger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AuthService {
    private final Map<String, Account> accounts = new HashMap<String, Account>();
    private final DbConfig dbConfig;
    private final DbUserRepository dbUserRepository;
    private boolean databaseReady;

    public AuthService() {
        addAccount("admin", "admin123", "user.admin.name", "role.admin", "app.company");
        addAccount("planner", "plan123", "user.planner.name", "role.planner", "app.company");
        dbConfig = DbConfig.loadDefault();
        dbUserRepository = new DbUserRepository(dbConfig);
        databaseReady = dbConfig.isEnabled();
    }

    public UserSession authenticate(String username, char[] password, Language language) throws AuthException {
        Language actualLanguage = language == null ? I18n.DEFAULT_LANGUAGE : language;
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.length() == 0) {
            throw new AuthException(I18n.t(actualLanguage, "auth.user.required"));
        }

        if (password == null || password.length == 0) {
            throw new AuthException(I18n.t(actualLanguage, "auth.password.required"));
        }

        if (dbConfig.isEnabled() && databaseReady) {
            AppLogger.info("Authenticating with MySQL for user: " + normalizedUsername);
            return authenticateWithDatabase(normalizedUsername, password, actualLanguage);
        }

        if (dbConfig.isEnabled() && !dbConfig.isFallbackToDemo()) {
            AppLogger.warning("Database login unavailable and demo fallback disabled.");
            throw new AuthException(I18n.t(actualLanguage, "auth.database.unavailable"));
        }

        AppLogger.warning("Using demo authentication fallback for user: " + normalizedUsername);
        return authenticateWithDemo(normalizedUsername, password, actualLanguage);
    }

    private UserSession authenticateWithDatabase(String normalizedUsername, char[] password, Language language) throws AuthException {
        try {
            DbUserRepository.DbAccount account = dbUserRepository.findActiveUser(normalizedUsername);
            if (account == null) {
                throw new AuthException(I18n.t(language, "auth.user.notFound"));
            }

            String candidateHash = hashPasswordHex(new String(password));
            if (!candidateHash.equalsIgnoreCase(account.getPasswordHash())) {
                throw new AuthException(I18n.t(language, "auth.bad.credentials"));
            }

            try {
                dbUserRepository.recordLogin(account.getUsername());
            } catch (SQLException e) {
                AppLogger.error("Failed to update last_login_at.", e);
            }
            return new UserSession(
                    account.getUsername(),
                    account.getDisplayName(),
                    account.getRoleName(),
                    account.getCompanyName(),
                    language
            );
        } catch (SQLException e) {
            databaseReady = false;
            AppLogger.error("Database authentication failed.", e);
            if (!dbConfig.isFallbackToDemo()) {
                throw new AuthException(I18n.t(language, "auth.database.failed"));
            }
            return authenticateWithDemo(normalizedUsername, password, language);
        }
    }

    private UserSession authenticateWithDemo(String normalizedUsername, char[] password, Language language) throws AuthException {
        Account account = accounts.get(normalizedUsername);
        if (account == null) {
            throw new AuthException(I18n.t(language, "auth.user.notFound"));
        }

        byte[] candidateHash = hashPassword(new String(password));
        try {
            if (!MessageDigest.isEqual(account.passwordHash, candidateHash)) {
                throw new AuthException(I18n.t(language, "auth.bad.credentials"));
            }
        } finally {
            Arrays.fill(candidateHash, (byte) 0);
        }

        return new UserSession(
                account.username,
                account.displayNameKey,
                account.roleNameKey,
                account.companyNameKey,
                language
        );
    }

    private void addAccount(String username, String password, String displayNameKey, String roleNameKey, String companyNameKey) {
        String normalizedUsername = normalizeUsername(username);
        accounts.put(normalizedUsername, new Account(
                normalizedUsername,
                hashPassword(password),
                displayNameKey,
                roleNameKey,
                companyNameKey
        ));
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private byte[] hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private String hashPasswordHex(String password) {
        byte[] hash = hashPassword(password);
        StringBuilder builder = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            builder.append(String.format("%02x", b & 0xff));
        }
        Arrays.fill(hash, (byte) 0);
        return builder.toString();
    }

    private static final class Account {
        private final String username;
        private final byte[] passwordHash;
        private final String displayNameKey;
        private final String roleNameKey;
        private final String companyNameKey;

        private Account(String username, byte[] passwordHash, String displayNameKey, String roleNameKey, String companyNameKey) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.displayNameKey = displayNameKey;
            this.roleNameKey = roleNameKey;
            this.companyNameKey = companyNameKey;
        }
    }
}
