package ru.splitproxy.apk;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public final class SelectionStore {
    private static final String PREFS = "splitproxy_selection";
    private static final String KEY_PACKAGES = "selected_packages";

    private SelectionStore() {}

    public static Set<String> load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(KEY_PACKAGES, new HashSet<>()));
    }

    public static boolean save(Context context, Set<String> packages) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putStringSet(KEY_PACKAGES, new HashSet<>(packages))
                .commit();
    }
}
