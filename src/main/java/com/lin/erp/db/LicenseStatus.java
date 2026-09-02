package com.lin.erp.db;

import java.time.LocalDate;

public class LicenseStatus {
    private final boolean valid;
    private final String licenseKey;
    private final LocalDate validUntil;

    public LicenseStatus(boolean valid, String licenseKey, LocalDate validUntil) {
        this.valid = valid;
        this.licenseKey = licenseKey;
        this.validUntil = validUntil;
    }

    public boolean isValid() {
        return valid;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }
}
