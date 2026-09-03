package com.lin.erp.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public final class LicenseKeyVerifier {
    private static final String PREFIX = "LINOVA-";
    private static final String SIGNATURE_PAYLOAD_PREFIX = "LINOVA|";
    private static final String PUBLIC_KEY_RESOURCE = "/com/lin/erp/license/license-public-key.txt";
    private static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private LicenseKeyVerifier() {
    }

    public static Result verify(String licenseKey, boolean allowLegacyDateOnly) {
        LocalDate validUntil = parseDatePart(licenseKey);
        if (validUntil == null) {
            return Result.invalid(null);
        }
        if (validUntil.isBefore(LocalDate.now())) {
            return Result.invalid(validUntil);
        }

        String normalized = licenseKey.trim();
        String datePart = COMPACT_DATE.format(validUntil);
        String expectedUnsigned = PREFIX + datePart;
        if (normalized.equalsIgnoreCase(expectedUnsigned)) {
            return allowLegacyDateOnly ? Result.valid(validUntil, true) : Result.invalid(validUntil);
        }

        String signatureText = extractSignature(normalized, datePart);
        if (signatureText == null || signatureText.length() == 0) {
            return Result.invalid(validUntil);
        }
        return verifySignature(validUntil, signatureText) ? Result.valid(validUntil, false) : Result.invalid(validUntil);
    }

    private static LocalDate parseDatePart(String licenseKey) {
        if (licenseKey == null) {
            return null;
        }
        String normalized = licenseKey.trim().toUpperCase();
        if (!normalized.startsWith(PREFIX)) {
            return null;
        }
        String remainder = normalized.substring(PREFIX.length());
        String compactDate = remainder.length() >= 10 && remainder.charAt(4) == '-' && remainder.charAt(7) == '-'
                ? remainder.substring(0, 10).replace("-", "")
                : firstToken(remainder);
        if (compactDate.length() != 8) {
            return null;
        }
        try {
            int year = Integer.parseInt(compactDate.substring(0, 4));
            int month = Integer.parseInt(compactDate.substring(4, 6));
            int day = Integer.parseInt(compactDate.substring(6, 8));
            return LocalDate.of(year, month, day);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String firstToken(String value) {
        int separator = value.indexOf('-');
        return separator < 0 ? value : value.substring(0, separator);
    }

    private static String extractSignature(String normalized, String compactDate) {
        String compactPrefix = PREFIX + compactDate + "-";
        if (normalized.regionMatches(true, 0, compactPrefix, 0, compactPrefix.length())) {
            return normalized.substring(compactPrefix.length());
        }
        String dashedDate = compactDate.substring(0, 4) + "-" + compactDate.substring(4, 6) + "-" + compactDate.substring(6, 8);
        String dashedPrefix = PREFIX + dashedDate + "-";
        if (normalized.regionMatches(true, 0, dashedPrefix, 0, dashedPrefix.length())) {
            return normalized.substring(dashedPrefix.length());
        }
        return null;
    }

    private static boolean verifySignature(LocalDate validUntil, String signatureText) {
        try {
            PublicKey publicKey = loadPublicKey();
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update((SIGNATURE_PAYLOAD_PREFIX + validUntil).getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getUrlDecoder().decode(signatureText));
        } catch (Exception e) {
            return false;
        }
    }

    private static PublicKey loadPublicKey() throws Exception {
        InputStream input = LicenseKeyVerifier.class.getResourceAsStream(PUBLIC_KEY_RESOURCE);
        if (input == null) {
            throw new IOException("Missing license public key resource.");
        }
        try {
            byte[] encoded = Base64.getMimeDecoder().decode(readAll(input));
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
        } finally {
            input.close();
        }
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    public static final class Result {
        private final boolean valid;
        private final boolean legacy;
        private final LocalDate validUntil;

        private Result(boolean valid, boolean legacy, LocalDate validUntil) {
            this.valid = valid;
            this.legacy = legacy;
            this.validUntil = validUntil;
        }

        private static Result valid(LocalDate validUntil, boolean legacy) {
            return new Result(true, legacy, validUntil);
        }

        private static Result invalid(LocalDate validUntil) {
            return new Result(false, false, validUntil);
        }

        public boolean isValid() {
            return valid;
        }

        public boolean isLegacy() {
            return legacy;
        }

        public LocalDate getValidUntil() {
            return validUntil;
        }
    }
}
