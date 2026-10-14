package ru.splitproxy.apk;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

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
        putSecret(KEY_DEVICE_ID, keyId);
        putSecret(KEY_DEVICE_TOKEN, deviceToken);
    }

    public void saveAccount(String token, String name, String email) {
        putSecret(KEY_ACCOUNT_TOKEN, token);
        prefs.edit()
                .putString(KEY_USER_NAME, name == null ? "" : name)
                .putString(KEY_USER_EMAIL, email == null ? "" : email)
                .commit();
    }

    public void clearAccount() {
        prefs.edit()
                .remove(KEY_ACCOUNT_TOKEN)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .commit();
    }

    private void putSecret(String key, String value) {
        if (value == null) {
            prefs.edit().remove(key).commit();
            return;
        }
        try {
            SecretKey secretKey = getOrCreateKey();
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[1 + iv.length + encrypted.length];
            packed[0] = 1;
            System.arraycopy(iv, 0, packed, 1, iv.length);
            System.arraycopy(encrypted, 0, packed, 1 + iv.length, encrypted.length);
            prefs.edit().putString(key, Base64.encodeToString(packed, Base64.NO_WRAP)).commit();
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сохранить защищённые данные", e);
        }
    }

    private String getSecret(String key) {
        String encoded = prefs.getString(key, null);
        if (encoded == null) return null;
        try {
            byte[] packed = Base64.decode(encoded, Base64.NO_WRAP);
            if (packed.length < 14 || packed[0] != 1) return null;
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[packed.length - 13];
            System.arraycopy(packed, 1, iv, 0, 12);
            System.arraycopy(packed, 13, encrypted, 0, encrypted.length);
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
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
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
