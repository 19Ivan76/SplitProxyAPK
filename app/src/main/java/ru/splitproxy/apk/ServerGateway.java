package ru.splitproxy.apk;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class ServerGateway {
    private final ApiClient client = new ApiClient();

    public JSONObject guestChallenge(DeviceIdentity identity) throws Exception {
        JSONObject body = new JSONObject();
        body.put("install_id", identity.installId);
        body.put("platform", AppConstants.PLATFORM);
        body.put("client_type", AppConstants.CLIENT_TYPE);
        body.put("public_key", identity.publicKeyPem);
        return client.post("guest/devices/challenge", body, null, null);
    }

    public JSONObject guestActivate(String installId, String challenge, String signature) throws Exception {
        JSONObject body = new JSONObject();
        body.put("install_id", installId);
        body.put("challenge", challenge);
        body.put("signature", signature);
        return client.post("guest/devices/activate", body, null, null);
    }

    public JSONObject register(String name, String email, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        body.put("password_confirmation", password);
        return client.post("auth/register", body, null, null);
    }

    public JSONObject login(String email, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);
        return client.post("auth/login", body, null, null);
    }

    public JSONObject me(String accountToken) throws Exception {
        return client.get("auth/me", accountToken, null);
    }

    public void logout(String accountToken) throws Exception {
        client.post("auth/logout", new JSONObject(), accountToken, null);
    }

    public JSONObject claim(String deviceKeyId, String deviceToken, String accountToken) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Guest-Device-Key", deviceKeyId);
        headers.put("X-Guest-Device-Token", deviceToken);
        return client.post("guest/claim", new JSONObject(), accountToken, headers);
    }

    public JSONObject getProxyConfig(String deviceKeyId, String deviceToken, String country) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Device-Key", deviceKeyId);
        String path = "devices/proxy/config?country=" + URLEncoder.encode(country, StandardCharsets.UTF_8.name());
        return client.get(path, deviceToken, headers);
    }
}
