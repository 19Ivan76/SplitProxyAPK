package ru.splitproxy.apk;

import android.content.Context;

import org.json.JSONObject;

public final class VpnConfigRepository {
    private final SecureStore store;
    private final ServerGateway gateway = new ServerGateway();

    public VpnConfigRepository(Context context) {
        store = new SecureStore(context.getApplicationContext());
    }

    public VpnConfig fetch(String country) throws Exception {
        if (!store.isLoggedIn()) throw new IllegalStateException("Сначала войдите в аккаунт");
        String keyId = store.getDeviceKeyId();
        String deviceToken = store.getDeviceToken();
        if (keyId == null || deviceToken == null) throw new IllegalStateException("Устройство не активировано");
        JSONObject response = gateway.getProxyConfig(keyId, deviceToken, country);
        return VpnConfig.fromApi(response);
    }
}
