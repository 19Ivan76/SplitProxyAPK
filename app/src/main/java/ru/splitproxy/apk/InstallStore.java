package ru.splitproxy.apk;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public final class InstallStore {
    private static final String PREFS = "splitproxy_install";
    private static final String KEY_INSTALL_ID = "install_id";

    private InstallStore() {}

    public static String getOrCreateInstallId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String value = prefs.getString(KEY_INSTALL_ID, null);
        if (value != null && !value.trim().isEmpty()) return value;
        value = UUID.randomUUID().toString();
        prefs.edit().putString(KEY_INSTALL_ID, value).commit();
        return value;
    }
}
