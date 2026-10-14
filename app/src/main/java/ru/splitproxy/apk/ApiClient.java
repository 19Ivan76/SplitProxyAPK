package ru.splitproxy.apk;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public final class ApiClient {
    public JSONObject request(String method, String path, JSONObject body,
                              String bearerToken, Map<String, String> extraHeaders) throws Exception {
        URL url = new URL(AppConstants.API_BASE_URL + path.replaceFirst("^/", ""));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "SplitProxyAPK/" + AppConstants.VERSION + " Android");
        if (bearerToken != null && !bearerToken.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }
        for (Map.Entry<String, String> entry : (extraHeaders == null ? Collections.<String, String>emptyMap() : extraHeaders).entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
        String raw = readAll(stream);
        if (status < 200 || status >= 300) {
            String message = "HTTP " + status;
            try {
                JSONObject error = raw.isEmpty() ? new JSONObject() : new JSONObject(raw);
                if (error.has("message")) message = error.optString("message", message);
                JSONObject errors = error.optJSONObject("errors");
                if (errors != null && errors.keys().hasNext()) {
                    Object first = errors.opt(errors.keys().next());
                    if (first != null) message = String.valueOf(first).replace("[\"", "").replace("\"]", "");
                }
            } catch (Exception ignored) {}
            throw new ApiException(status, message, raw);
        }
        if (raw.trim().isEmpty()) return new JSONObject();
        return new JSONObject(raw);
    }

    public JSONObject get(String path, String token, Map<String, String> headers) throws Exception {
        return request("GET", path, null, token, headers);
    }

    public JSONObject post(String path, JSONObject body, String token, Map<String, String> headers) throws Exception {
        return request("POST", path, body, token, headers);
    }

    private static String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line).append('\n');
        }
        return builder.toString().trim();
    }
}
