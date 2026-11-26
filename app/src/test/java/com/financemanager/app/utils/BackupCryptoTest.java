package com.financemanager.app.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class BackupCryptoTest {

    private static final String PASSWORD = "correct horse battery staple";

    @Test
    public void encryptsAndDecryptsRoundTrip() throws Exception {
        byte[] plaintext = "{\"format\":\"finance-manager-backup\",\"secret\":\"؋ ۱۲۳\"}"
                .getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = BackupCrypto.encrypt(plaintext, PASSWORD);

        assertTrue(BackupCrypto.isEncrypted(encrypted));
        assertFalse(new String(encrypted, StandardCharsets.UTF_8).contains("secret"));
        assertArrayEquals(plaintext, BackupCrypto.decrypt(encrypted, PASSWORD));
    }

    @Test
    public void samePasswordUsesFreshSaltAndIv() throws Exception {
        byte[] plaintext = "backup".getBytes(StandardCharsets.UTF_8);
        byte[] first = BackupCrypto.encrypt(plaintext, PASSWORD);
        byte[] second = BackupCrypto.encrypt(plaintext, PASSWORD);
        assertNotEquals(new String(first, StandardCharsets.UTF_8),
                new String(second, StandardCharsets.UTF_8));
    }

    @Test
    public void wrongPasswordIsReportedAsSuch() throws Exception {
        byte[] encrypted = BackupCrypto.encrypt(
                "backup".getBytes(StandardCharsets.UTF_8), PASSWORD);
        try {
            BackupCrypto.decrypt(encrypted, "not the password");
            fail("Expected a CryptoException");
        } catch (BackupCrypto.CryptoException e) {
            assertTrue(e.isWrongPassword());
        }
    }

    @Test
    public void tamperedCiphertextDoesNotDecrypt() throws Exception {
        byte[] encrypted = BackupCrypto.encrypt(
                "backup".getBytes(StandardCharsets.UTF_8), PASSWORD);
        String envelope = new String(encrypted, StandardCharsets.UTF_8);
        int last = envelope.lastIndexOf('\n', envelope.length() - 2);
        char original = envelope.charAt(last + 1);
        char replacement = original == 'A' ? 'B' : 'A';
        String tampered = envelope.substring(0, last + 1) + replacement
                + envelope.substring(last + 2);
        try {
            BackupCrypto.decrypt(tampered.getBytes(StandardCharsets.UTF_8), PASSWORD);
            fail("Tampered data must not decrypt");
        } catch (BackupCrypto.CryptoException expected) {
            // Either a bad tag or a damaged base64 payload is acceptable.
        }
    }

    @Test
    public void plainJsonIsNotTreatedAsEncrypted() {
        byte[] json = "{\"format\":\"finance-manager-backup\"}"
                .getBytes(StandardCharsets.UTF_8);
        assertFalse(BackupCrypto.isEncrypted(json));
    }

    @Test(expected = BackupCrypto.CryptoException.class)
    public void emptyPasswordIsRejected() throws Exception {
        BackupCrypto.encrypt("backup".getBytes(StandardCharsets.UTF_8), "");
    }

    @Test(expected = BackupCrypto.CryptoException.class)
    public void damagedEnvelopeIsRejected() throws Exception {
        BackupCrypto.decrypt("FMBENC1\nnot-enough".getBytes(StandardCharsets.UTF_8),
                PASSWORD);
    }
}
