package ru.splitproxy.apk;

import android.content.Context;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

public final class SessionManager {
    private final Context context;
    private final SecureStore store;
    private final ServerGateway gateway;

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        this.store = new SecureStore(this.context);
        this.gateway = new ServerGateway();
    }

    public AccountResult login(String email, String password) throws Exception {
        JSONObject login = gateway.login(email, password);
        return finishAccountLogin(login);
    }

    public AccountResult register(String name, String email, String password) throws Exception {
        gateway.register(name, email, password);
        JSONObject login = gateway.login(email, password);
        return finishAccountLogin(login);
    }

    private AccountResult finishAccountLogin(JSONObject login) throws Exception {
        String accountToken = login.optString("access_token", "");
        if (accountToken.isEmpty()) throw new IllegalStateException("Сервер не вернул токен аккаунта");

        JSONObject user = login.optJSONObject("user");
        String name = user == null ? "" : user.optString("name", "");
        String email = user == null ? "" : user.optString("email", "");

        try {
            DeviceIdentity identity = DeviceIdentity.ensure(context);
            ensureActivatedForAccount(identity, accountToken);
            store.saveAccount(accountToken, name, email);
            return new AccountResult(name, email);
        } catch (Exception error) {
            try { gateway.logout(accountToken); } catch (Exception ignored) {}
            throw error;
        }
    }

    private void ensureActivatedForAccount(DeviceIdentity identity, String accountToken) throws Exception {
        String keyId = store.getDeviceKeyId();
        String deviceToken = store.getDeviceToken();

        if (keyId != null && !keyId.isEmpty() && deviceToken != null && !deviceToken.isEmpty()) {
            verifyExistingDeviceOwnership(identity.installId, accountToken);
            return;
        }

        String deviceName = buildDeviceName();
        JSONObject requested = gateway.activationRequest(identity, deviceName);
        String requestCode = requested.optString("request_code", "");
        if (!requestCode.matches("(?i)^SP-([a-f0-9]{4}-){4}[a-f0-9]{4}$")) {
            throw new IllegalStateException("Сервер вернул неверный код запроса активации");
        }

        JSONObject approved = gateway.activationApprove(requestCode, accountToken);
        String responseCode = approved.optString("response_code", "");
        if (!responseCode.matches("(?i)^ACT-(?:[a-f0-9]{4}-){7}[a-f0-9]{4}$")) {
            throw new IllegalStateException("Сервер вернул неверный код подтверждения активации");
        }

        String signature = identity.signActivationResponseCode(responseCode);
        JSONObject finished = gateway.activationFinish(identity.installId, responseCode, signature);
        if (!"active".equals(finished.optString("status", ""))) {
            throw new IllegalStateException("Сервер не активировал устройство");
        }

        String activatedKeyId = finished.optString("key_id", "");
        String activatedToken = finished.optString("device_token", "");
        if (!activatedKeyId.matches("^SP-[A-F0-9]{32}$")
                || !activatedToken.matches("^[a-f0-9]{64}$")) {
            throw new IllegalStateException("Сервер вернул неверные учётные данные устройства");
        }

        store.saveDeviceSession(activatedKeyId, activatedToken);
    }

    private void verifyExistingDeviceOwnership(String installId, String accountToken) throws Exception {
        JSONObject response = gateway.devices(accountToken);
        JSONArray devices = response.optJSONArray("devices");
        if (devices == null) {
            throw new IllegalStateException("Сервер не вернул список устройств аккаунта");
        }

        for (int i = 0; i < devices.length(); i++) {
            JSONObject device = devices.optJSONObject(i);
            if (device == null) continue;
            if (installId.equals(device.optString("install_id", ""))) {
                if ("active".equals(device.optString("status", ""))) return;
                throw new IllegalStateException("Эта установка Android отозвана или неактивна");
            }
        }

        throw new IllegalStateException("Эта установка Android уже привязана к другому аккаунту или недоступна");
    }

    private static String buildDeviceName() {
        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.trim();
        String model = Build.MODEL == null ? "" : Build.MODEL.trim();
        String value = (manufacturer + " " + model).trim();
        if (value.isEmpty()) value = "Android";
        if (value.length() > 150) value = value.substring(0, 150);
        return value;
    }

    public AccountResult restoreCachedAccount() {
        return new AccountResult(store.getUserName(), store.getUserEmail());
    }

    public boolean isLoggedIn() {
        return store.isLoggedIn();
    }

    public void validateAccountIfPossible() throws Exception {
        String token = store.getAccountToken();
        if (token == null || token.isEmpty()) return;
        try {
            JSONObject response = gateway.me(token);
            JSONObject user = response.optJSONObject("user");
            if (user == null && response.has("email")) user = response;
            if (user != null) {
                store.saveAccount(token,
                        user.optString("name", store.getUserName()),
                        user.optString("email", store.getUserEmail()));
            }
        } catch (ApiException e) {
            if (e.status == 401) store.clearAccount();
            else throw e;
        }
    }

    public void logout() throws Exception {
        String token = store.getAccountToken();
        try {
            if (token != null && !token.isEmpty()) gateway.logout(token);
        } catch (ApiException e) {
            if (e.status != 401) throw e;
        } finally {
            // Device credentials remain: this installation stays activated.
            // A subsequent login is accepted only for the owning account.
            store.clearAccount();
        }
    }

    public static final class AccountResult {
        public final String name;
        public final String email;

        public AccountResult(String name, String email) {
            this.name = name == null ? "" : name;
            this.email = email == null ? "" : email;
        }
    }
}
