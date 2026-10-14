package ru.splitproxy.apk;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecureStore {
    private static final String PREFS = "splitproxy_secure";
    private static final String AES_ALIAS = "splitproxy_storage_aes_v1";
    private static final String KEY_DEVICE_ID = "device_key_id";
    private static final String KEY_DEVICE_TOKEN = "device_token";
    private static final String KEY_ACCOUNT_TOKEN = "account_token";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";

    private final SharedPreferences prefs;

    public SecureStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getDeviceKeyId() { return getSecret(KEY_DEVICE_ID); }
    public String getDeviceToken() { return getSecret(KEY_DEVICE_TOKEN); }
    public String getAccountToken() { return getSecret(KEY_ACCOUNT_TOKEN); }
    public String getUserName() { return prefs.getString(KEY_USER_NAME, ""); }
    public String getUserEmail() { return prefs.getString(KEY_USER_EMAIL, ""); }

    public boolean isLoggedIn() {
        String token = getAccountToken();
        return token != null && !token.isEmpty();
    }

    public void saveDeviceSession(String keyId, String deviceToken) {
        if (keyId == null || deviceToken == null) {
            throw new IllegalArgumentException("Пустые учётные данные устройства");
        }
        try {
            String encryptedKeyId = encrypt(keyId);
            String encryptedToken = encrypt(deviceToken);
            boolean saved = prefs.edit()
                    .putString(KEY_DEVICE_ID, encryptedKeyId)
                    .putString(KEY_DEVICE_TOKEN, encryptedToken)
                    .commit();
            if (!saved) throw new IllegalStateException("SharedPreferences commit failed");
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сохранить защищённые данные", e);
        }
    }

    public void saveAccount(String token, String name, String email) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Пустой токен аккаунта");
        }
        try {
            String encryptedToken = encrypt(token);
            boolean saved = prefs.edit()
                    .putString(KEY_ACCOUNT_TOKEN, encryptedToken)
                    .putString(KEY_USER_NAME, name == null ? "" : name)
                    .putString(KEY_USER_EMAIL, email == null ? "" : email)
                    .commit();
            if (!saved) throw new IllegalStateException("SharedPreferences commit failed");
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сохранить защищённые данные", e);
        }
    }

    public void clearAccount() {
        prefs.edit()
                .remove(KEY_ACCOUNT_TOKEN)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .commit();
    }

    private String encrypt(String value) throws Exception {
        SecretKey secretKey = getOrCreateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // AndroidKeyStore with randomized encryption rejects a caller-supplied IV
        // for ENCRYPT_MODE. Let the provider generate a secure random IV.
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();
        if (iv == null || iv.length < 12) {
            throw new IllegalStateException("AndroidKeyStore did not provide a GCM IV");
        }

        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] packed = new byte[2 + iv.length + encrypted.length];
        packed[0] = 2;
        packed[1] = (byte) iv.length;
        System.arraycopy(iv, 0, packed, 2, iv.length);
        System.arraycopy(encrypted, 0, packed, 2 + iv.length, encrypted.length);
        return Base64.encodeToString(packed, Base64.NO_WRAP);
    }

    private String getSecret(String key) {
        String encoded = prefs.getString(key, null);
        if (encoded == null) return null;
        try {
            byte[] packed = Base64.decode(encoded, Base64.NO_WRAP);
            if (packed.length < 15) return null;

            int version = packed[0] & 0xff;
            final int ivOffset;
            final int ivLength;
            final int encryptedOffset;

            if (version == 1) {
                // Read compatibility with v0.1.1, if any value was successfully written.
                ivOffset = 1;
                ivLength = 12;
                encryptedOffset = 13;
            } else if (version == 2) {
                ivLength = packed[1] & 0xff;
                if (ivLength < 12 || ivLength > 32) return null;
                ivOffset = 2;
                encryptedOffset = 2 + ivLength;
            } else {
                return null;
            }

            if (packed.length <= encryptedOffset) return null;
            byte[] iv = new byte[ivLength];
            byte[] encrypted = new byte[packed.length - encryptedOffset];
            System.arraycopy(packed, ivOffset, iv, 0, ivLength);
            System.arraycopy(packed, encryptedOffset, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(AES_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(AES_ALIAS, null)).getSecretKey();
        }

        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                AES_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
