package ru.splitproxy.apk;

import android.content.Context;

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

    public DeviceIdentity ensureDeviceIdentityAndGuestSession() throws Exception {
        DeviceIdentity identity = DeviceIdentity.ensure(context);
        String keyId = store.getDeviceKeyId();
        String token = store.getDeviceToken();
        if (keyId != null && !keyId.isEmpty() && token != null && !token.isEmpty()) return identity;

        JSONObject challengeResponse = gateway.guestChallenge(identity);
        String challenge = challengeResponse.optString("challenge", "");
        if (!challenge.matches("(?i)^[a-f0-9]{64}$")) {
            throw new IllegalStateException("Сервер вернул неверный challenge");
        }
        String signature = identity.signActivationChallenge(challenge);
        JSONObject activation = gateway.guestActivate(identity.installId, challenge, signature);
        String status = activation.optString("status", "");
        String deviceKeyId = activation.optString("key_id", "");
        String deviceToken = activation.optString("device_token", "");
        if (!"guest".equals(status) || deviceKeyId.isEmpty() || deviceToken.isEmpty()) {
            throw new IllegalStateException("Сервер вернул неверные данные активации устройства");
        }
        store.saveDeviceSession(deviceKeyId, deviceToken);
        return identity;
    }

    public AccountResult login(String email, String password) throws Exception {
        ensureDeviceIdentityAndGuestSession();
        JSONObject login = gateway.login(email, password);
        return finishAccountLogin(login);
    }

    public AccountResult register(String name, String email, String password) throws Exception {
        ensureDeviceIdentityAndGuestSession();
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
            gateway.claim(store.getDeviceKeyId(), store.getDeviceToken(), accountToken);
        } catch (ApiException e) {
            try { gateway.logout(accountToken); } catch (Exception ignored) {}
            if (e.status == 422) {
                throw new IllegalStateException("Достигнут лимит устройств. Отзовите старое устройство в личном кабинете и повторите вход.");
            }
            throw e;
        }
        store.saveAccount(accountToken, name, email);
        return new AccountResult(name, email);
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
                store.saveAccount(token, user.optString("name", store.getUserName()),
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
