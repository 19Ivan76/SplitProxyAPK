package ru.splitproxy.apk;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;

public final class DeviceIdentity {
    private static final String KEY_ALIAS = "splitproxy_device_ecdsa_v1";

    public final String installId;
    public final String publicKeyPem;
    public final String fingerprint;

    private DeviceIdentity(String installId, String publicKeyPem, String fingerprint) {
        this.installId = installId;
        this.publicKeyPem = publicKeyPem;
        this.fingerprint = fingerprint;
    }

    public static DeviceIdentity ensure(Context context) {
        try {
            String installId = InstallStore.getOrCreateInstallId(context);
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            if (!store.containsAlias(KEY_ALIAS)) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
                generator.initialize(new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                        .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .build());
                generator.generateKeyPair();
            }
            PublicKey publicKey = store.getCertificate(KEY_ALIAS).getPublicKey();
            byte[] spki = publicKey.getEncoded();
            return new DeviceIdentity(
                    installId,
                    toPem("PUBLIC KEY", spki),
                    base64Url(MessageDigest.getInstance("SHA-256").digest(spki)));
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось создать ключ устройства", e);
        }
    }

    public String signActivationResponseCode(String responseCode) {
        String code = responseCode == null ? "" : responseCode.trim().toUpperCase();
        if (code.startsWith("ACT-")) code = code.substring(4);
        code = code.replace("-", "");
        if (!code.matches("^[A-F0-9]{32}$")) {
            throw new IllegalArgumentException("Неверный код подтверждения активации");
        }
        String canonical = "SP-ACTIVATE-V1\n" + installId + "\n" + code;
        return signP1363(canonical);
    }

    public String signP1363(String text) {
        try {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            PrivateKey privateKey = (PrivateKey) store.getKey(KEY_ALIAS, null);
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(privateKey);
            signer.update(text.getBytes(StandardCharsets.UTF_8));
            byte[] der = signer.sign();
            byte[] p1363 = derToP1363(der);
            if (p1363.length != 64) throw new IllegalStateException("P1363 signature must be 64 bytes");
            return Base64.encodeToString(p1363, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось подписать challenge", e);
        }
    }

    private static String toPem(String label, byte[] data) {
        String base64 = Base64.encodeToString(data, Base64.NO_WRAP);
        StringBuilder builder = new StringBuilder("-----BEGIN ").append(label).append("-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            builder.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }
        return builder.append("-----END ").append(label).append("-----").toString();
    }

    private static String base64Url(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP | Base64.URL_SAFE).replace("=", "");
    }

    private static byte[] derToP1363(byte[] der) {
        if (der.length == 64) return der;
        int[] p = {0};
        if ((der[p[0]++] & 0xff) != 0x30) throw new IllegalArgumentException("Invalid ECDSA DER");
        readLength(der, p);
        if ((der[p[0]++] & 0xff) != 0x02) throw new IllegalArgumentException("Missing R");
        int rLen = readLength(der, p);
        byte[] r = new byte[rLen];
        System.arraycopy(der, p[0], r, 0, rLen); p[0] += rLen;
        if ((der[p[0]++] & 0xff) != 0x02) throw new IllegalArgumentException("Missing S");
        int sLen = readLength(der, p);
        byte[] s = new byte[sLen];
        System.arraycopy(der, p[0], s, 0, sLen);
        byte[] out = new byte[64];
        copyInt32(r, out, 0);
        copyInt32(s, out, 32);
        return out;
    }

    private static int readLength(byte[] der, int[] p) {
        int value = der[p[0]++] & 0xff;
        if ((value & 0x80) == 0) return value;
        int count = value & 0x7f;
        int length = 0;
        for (int i = 0; i < count; i++) length = (length << 8) | (der[p[0]++] & 0xff);
        return length;
    }

    private static void copyInt32(byte[] value, byte[] out, int offset) {
        int start = 0;
        while (start < value.length - 1 && value[start] == 0) start++;
        int length = value.length - start;
        if (length > 32) {
            start += length - 32;
            length = 32;
        }
        System.arraycopy(value, start, out, offset + 32 - length, length);
    }
}
