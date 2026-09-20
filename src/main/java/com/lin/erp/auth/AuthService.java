package com.lin.erp.auth;

import com.lin.erp.config.DbConfig;
import com.lin.erp.db.DbUserRepository;
import com.lin.erp.i18n.I18n;
import com.lin.erp.i18n.Language;
import com.lin.erp.logging.AppLogger;

import java.sql.SQLException;
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

        if (dbConfig.isEnabled()) {
            AppLogger.warning("Database login unavailable in database mode.");
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
            if (account.isLocked()) {
                AppLogger.warning("Database user is locked until " + account.getLockedUntil() + ": " + normalizedUsername);
                throw new AuthException(I18n.t(language, "auth.bad.credentials"));
            }

            if (!PasswordHasher.verify(password, account.getPasswordHash())) {
                try {
                    dbUserRepository.recordFailedLogin(account.getUsername());
                } catch (SQLException e) {
                    AppLogger.error("Failed to update failed login counter.", e);
                }
                throw new AuthException(I18n.t(language, "auth.bad.credentials"));
            }

            if (PasswordHasher.needsRehash(account.getPasswordHash())) {
                try {
                    dbUserRepository.updatePasswordHash(account.getUsername(), PasswordHasher.hash(password));
                } catch (SQLException e) {
                    AppLogger.error("Failed to upgrade password hash.", e);
                }
            }
            try {
                dbUserRepository.recordLogin(account.getUsername());
            } catch (SQLException e) {
                AppLogger.error("Failed to update last_login_at.", e);
            }
            return new UserSession(
                    account.getUsername(),
                    account.getDisplayName(),
                    account.getRoleCode(),
                    account.getRoleName(),
                    account.getCompanyName(),
                    language
            );
        } catch (SQLException e) {
            databaseReady = false;
            AppLogger.error("Database authentication failed.", e);
            throw new AuthException(I18n.t(language, "auth.database.failed"));
        }
    }

    private UserSession authenticateWithDemo(String normalizedUsername, char[] password, Language language) throws AuthException {
        Account account = accounts.get(normalizedUsername);
        if (account == null) {
            throw new AuthException(I18n.t(language, "auth.user.notFound"));
        }

        if (!PasswordHasher.verify(password, account.passwordHash)) {
            throw new AuthException(I18n.t(language, "auth.bad.credentials"));
        }

        return new UserSession(
                account.username,
                account.displayNameKey,
                account.roleCode,
                account.roleNameKey,
                account.companyNameKey,
                language
        );
    }

    private void addAccount(String username, String password, String displayNameKey, String roleNameKey, String companyNameKey) {
        String normalizedUsername = normalizeUsername(username);
        accounts.put(normalizedUsername, new Account(
                normalizedUsername,
                PasswordHasher.hash(password),
                displayNameKey,
                roleCodeFromKey(roleNameKey),
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

    private String roleCodeFromKey(String roleNameKey) {
        if ("role.admin".equals(roleNameKey)) {
            return "ADMIN";
        }
        if ("role.planner".equals(roleNameKey)) {
            return "PLANNER";
        }
        return "";
    }

    private static final class Account {
        private final String username;
        private final String passwordHash;
        private final String displayNameKey;
        private final String roleCode;
        private final String roleNameKey;
        private final String companyNameKey;

        private Account(String username, String passwordHash, String displayNameKey, String roleCode, String roleNameKey, String companyNameKey) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.displayNameKey = displayNameKey;
            this.roleCode = roleCode;
            this.roleNameKey = roleNameKey;
            this.companyNameKey = companyNameKey;
        }
    }
}
