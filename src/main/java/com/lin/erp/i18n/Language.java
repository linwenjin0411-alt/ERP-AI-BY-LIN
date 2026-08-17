package com.lin.erp.i18n;

public enum Language {
    EN("English"),
    ZH("简体中文"),
    JA("日本語");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
