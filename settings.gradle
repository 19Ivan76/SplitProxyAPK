package ru.splitproxy.apk;

import android.content.Context;
import android.content.SharedPreferences;

public final class VpnConfigStore {
    private static final String PREFS = "splitproxy_vpn_config";

    private VpnConfigStore() {}

    public static boolean save(Context context, VpnConfig config) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("host", config.host)
                .putInt("port", config.port)
                .putString("scheme", config.scheme)
                .putString("display", config.display())
                .putString("country", config.exitCountry)
                .commit();
    }

    public static Endpoint load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String host = prefs.getString("host", "");
        int port = prefs.getInt("port", -1);
        if (host == null || host.isEmpty() || port < 1) return null;
        return new Endpoint(
                host, port,
                prefs.getString("scheme", "http"),
                prefs.getString("display", host + ":" + port),
                prefs.getString("country", AppConstants.DEFAULT_COUNTRY));
    }

    public static final class Endpoint {
        public final String host;
        public final int port;
        public final String scheme;
        public final String display;
        public final String country;

        Endpoint(String host, int port, String scheme, String display, String country) {
            this.host = host;
            this.port = port;
            this.scheme = scheme == null || scheme.isEmpty() ? "http" : scheme;
            this.display = display;
            this.country = country;
        }

        public String url() { return scheme + "://" + host + ":" + port; }
    }
}
