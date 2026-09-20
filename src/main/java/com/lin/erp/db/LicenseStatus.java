package com.lin.erp.db;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LicenseStatus {
    private final boolean valid;
    private final String licenseKey;
    private final LocalDate validUntil;
    private final String reasonCode;
    private final String detailMessage;
    private final String customerName;
    private final String productCode;
    private final String modules;
    private final String seatPolicy;
    private final boolean deviceBindingEnabled;
    private final String source;

    public LicenseStatus(boolean valid, String licenseKey, LocalDate validUntil) {
        this(valid, licenseKey, validUntil, "", "", "", "", "", "", false, "");
    }

    public LicenseStatus(boolean valid, String licenseKey, LocalDate validUntil, String reasonCode, String detailMessage,
            String customerName, String productCode, String modules, String seatPolicy, boolean deviceBindingEnabled,
            String source) {
        this.valid = valid;
        this.licenseKey = licenseKey;
        this.validUntil = validUntil;
        this.reasonCode = value(reasonCode);
        this.detailMessage = value(detailMessage);
        this.customerName = value(customerName);
        this.productCode = value(productCode);
        this.modules = value(modules);
        this.seatPolicy = value(seatPolicy);
        this.deviceBindingEnabled = deviceBindingEnabled;
        this.source = value(source);
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

    public String getReasonCode() {
        return reasonCode;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getModules() {
        return modules;
    }

    public String getSeatPolicy() {
        return seatPolicy;
    }

    public boolean isDeviceBindingEnabled() {
        return deviceBindingEnabled;
    }

    public String getSource() {
        return source;
    }

    public long getRemainingDays() {
        if (validUntil == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), validUntil);
    }

    private static String value(String text) {
        return text == null ? "" : text.trim();
    }
}
