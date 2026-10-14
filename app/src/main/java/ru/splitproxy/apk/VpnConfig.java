package ru.splitproxy.apk;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class VpnConfig {
    public final String host;
    public final int port;
    public final String scheme;
    public final String entryNode;
    public final String exitNode;
    public final String exitCountry;
    public final List<String> availableCountries;

    public VpnConfig(String host, int port, String scheme, String entryNode,
                     String exitNode, String exitCountry, List<String> availableCountries) {
        this.host = host;
        this.port = port;
        this.scheme = scheme == null || scheme.isEmpty() ? "http" : scheme;
        this.entryNode = entryNode == null ? "" : entryNode;
        this.exitNode = exitNode == null ? "" : exitNode;
        this.exitCountry = exitCountry == null || exitCountry.isEmpty() ? AppConstants.DEFAULT_COUNTRY : exitCountry;
        this.availableCountries = availableCountries;
    }

    public String url() {
        return scheme + "://" + host + ":" + port;
    }

    public String display() {
        String route = (!entryNode.isEmpty() || !exitNode.isEmpty())
                ? (entryNode + " → " + exitNode + " (" + exitCountry + ")")
                : exitCountry;
        return route + " · " + host + ":" + port;
    }

    public static VpnConfig fromApi(JSONObject response) {
        JSONObject proxy = response.optJSONObject("proxy");
        if (proxy == null) throw new IllegalStateException("Сервер не вернул объект proxy");
        String host = proxy.optString("server", proxy.optString("host", ""));
        int port = proxy.optInt("port", -1);
        if (host.isEmpty() || port < 1 || port > 65535) {
            throw new IllegalStateException("Сервер пока не выдал proxy.server/port для Android");
        }
        String country = response.optString("exit_country", proxy.optString("exit_country", AppConstants.DEFAULT_COUNTRY));
        List<String> countries = new ArrayList<>();
        JSONArray array = response.optJSONArray("available_exit_countries");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                Object item = array.opt(i);
                if (item instanceof String) countries.add(((String) item).toUpperCase());
                else if (item instanceof JSONObject) {
                    String code = ((JSONObject) item).optString("code", "").toUpperCase();
                    if (!code.isEmpty()) countries.add(code);
                }
            }
        }
        if (countries.isEmpty()) countries.add(country.toUpperCase());
        return new VpnConfig(
                host, port, proxy.optString("scheme", "http"),
                proxy.optString("entry_node", ""),
                proxy.optString("exit_node", ""),
                country.toUpperCase(), countries);
    }
}
