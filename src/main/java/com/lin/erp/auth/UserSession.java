package com.lin.erp.auth;

import com.lin.erp.i18n.Language;

public class UserSession {
    private final String username;
    private final String displayNameKey;
    private final String roleCode;
    private final String roleNameKey;
    private final String companyNameKey;
    private Language language;

    public UserSession(String username, String displayNameKey, String roleCode, String roleNameKey, String companyNameKey, Language language) {
        this.username = username;
        this.displayNameKey = displayNameKey;
        this.roleCode = roleCode;
        this.roleNameKey = roleNameKey;
        this.companyNameKey = companyNameKey;
        this.language = language;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayNameKey() {
        return displayNameKey;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleNameKey() {
        return roleNameKey;
    }

    public String getCompanyNameKey() {
        return companyNameKey;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }
}
