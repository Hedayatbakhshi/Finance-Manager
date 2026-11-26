package com.financemanager.app.utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Authenticated password encryption for backup files.
 *
 * <p>The encrypted file is a small text envelope:</p>
 * <pre>
 * FMBENC1
 * &lt;iterations&gt;
 * &lt;base64 salt&gt;
 * &lt;base64 iv&gt;
 * &lt;base64 ciphertext + tag&gt;
 * </pre>
 *
 * <p>AES-256-GCM both hides the records and detects tampering. The key is derived
 * from the password with PBKDF2-HMAC-SHA256 and a random 128-bit salt, so the same
 * password never produces the same key twice. Nothing here depends on Android APIs,
 * which keeps the format unit-testable.</p>
 */
public final class BackupCrypto {

    private static final String MAGIC = "FMBENC1";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final String KDF = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int KEY_BITS = 256;
    private static final int TAG_BITS = 128;

    private BackupCrypto() {
    }

    /** Thrown for unreadable files and for a wrong password (see {@link #isWrongPassword()}). */
    public static class CryptoException extends Exception {
        private final boolean wrongPassword;

        CryptoException(String message, boolean wrongPassword, Throwable cause) {
            super(message, cause);
            this.wrongPassword = wrongPassword;
        }

        public boolean isWrongPassword() {
            return wrongPassword;
        }
    }

    /** True when the bytes start with the encrypted-backup marker. */
    public static boolean isEncrypted(byte[] data) {
        if (data == null || data.length < MAGIC.length()) {
            return false;
        }
        for (int i = 0; i < MAGIC.length(); i++) {
            if (data[i] != (byte) MAGIC.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static byte[] encrypt(byte[] plaintext, String password) throws CryptoException {
        requirePassword(password);
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_BYTES];
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(salt);
            random.nextBytes(iv);

            byte[] key = deriveKey(password, salt, ITERATIONS);
            try {
                Cipher cipher = Cipher.getInstance(CIPHER);
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                        new GCMParameterSpec(TAG_BITS, iv));
                byte[] ciphertext = cipher.doFinal(plaintext);
                Base64.Encoder encoder = Base64.getEncoder();
                String envelope = MAGIC + "\n"
                        + ITERATIONS + "\n"
                        + encoder.encodeToString(salt) + "\n"
                        + encoder.encodeToString(iv) + "\n"
                        + encoder.encodeToString(ciphertext) + "\n";
                return envelope.getBytes(StandardCharsets.UTF_8);
            } finally {
                java.util.Arrays.fill(key, (byte) 0);
            }
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Could not encrypt the backup", false, e);
        }
    }

    public static byte[] decrypt(byte[] envelope, String password) throws CryptoException {
        requirePassword(password);
        if (!isEncrypted(envelope)) {
            throw new CryptoException("This file is not an encrypted backup", false, null);
        }
        String[] lines = new String(envelope, StandardCharsets.UTF_8).split("\n");
        if (lines.length < 5) {
            throw new CryptoException("The encrypted backup is damaged", false, null);
        }
        int iterations;
        byte[] salt;
        byte[] iv;
        byte[] ciphertext;
        try {
            iterations = Integer.parseInt(lines[1].trim());
            if (iterations <= 0) {
                throw new NumberFormatException("iterations");
            }
            Base64.Decoder decoder = Base64.getDecoder();
            salt = decoder.decode(lines[2].trim());
            iv = decoder.decode(lines[3].trim());
            ciphertext = decoder.decode(lines[4].trim());
        } catch (IllegalArgumentException e) {
            throw new CryptoException("The encrypted backup is damaged", false, e);
        }
        if (salt.length == 0 || iv.length == 0 || ciphertext.length == 0) {
            throw new CryptoException("The encrypted backup is damaged", false, null);
        }
        byte[] key = null;
        try {
            key = deriveKey(password, salt, iterations);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (AEADBadTagException e) {
            throw new CryptoException("That password is incorrect", true, e);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Could not decrypt the backup", false, e);
        } finally {
            if (key != null) {
                java.util.Arrays.fill(key, (byte) 0);
            }
        }
    }

    private static void requirePassword(String password) throws CryptoException {
        if (password == null || password.isEmpty()) {
            throw new CryptoException("A password is required", false, null);
        }
    }

    private static byte[] deriveKey(String password, byte[] salt, int iterations)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(KDF).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
