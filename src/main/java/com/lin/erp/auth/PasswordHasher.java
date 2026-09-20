package com.lin.erp.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2_sha256";
    private static final int ITERATIONS = 120000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS);
        try {
            return PREFIX + "$" + ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } finally {
            Arrays.fill(hash, (byte) 0);
            Arrays.fill(salt, (byte) 0);
        }
    }

    public static String hash(String password) {
        char[] chars = password == null ? new char[0] : password.toCharArray();
        try {
            return hash(chars);
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    public static boolean verify(char[] password, String storedHash) {
        if (storedHash == null || storedHash.trim().length() == 0) {
            return false;
        }
        if (storedHash.startsWith(PREFIX + "$")) {
            return verifyPbkdf2(password, storedHash);
        }
        if (isLegacySha256Hex(storedHash)) {
            return MessageDigest.isEqual(legacySha256(password), hexToBytes(storedHash));
        }
        return false;
    }

    public static boolean needsRehash(String storedHash) {
        return storedHash == null || !storedHash.startsWith(PREFIX + "$");
    }

    private static boolean verifyPbkdf2(char[] password, String storedHash) {
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password, salt, iterations);
            try {
                return MessageDigest.isEqual(expected, actual);
            } finally {
                Arrays.fill(actual, (byte) 0);
                Arrays.fill(expected, (byte) 0);
                Arrays.fill(salt, (byte) 0);
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password == null ? new char[0] : password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(PBKDF2_ALGORITHM + " is not available.", e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("Password hash parameters are invalid.", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] legacySha256(char[] password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(new String(password == null ? new char[0] : password).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private static boolean isLegacySha256Hex(String value) {
        return value != null && value.matches("(?i)[0-9a-f]{64}");
    }

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return bytes;
    }
}
