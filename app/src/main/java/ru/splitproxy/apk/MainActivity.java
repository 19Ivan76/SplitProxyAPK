package ru.splitproxy.apk;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private static final int VPN_REQUEST = 7001;
    private static final int NOTIFICATION_REQUEST = 7002;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler statusHandler = new Handler(Looper.getMainLooper());
    private final Set<String> selectedPackages = new HashSet<>();

    private SessionManager sessionManager;
    private VpnConfigRepository configRepository;
    private AppListAdapter adapter;

    private View authPanel;
    private View loggedPanel;
    private View vpnPanel;
    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginTab;
    private Button registerTab;
    private Button authSubmit;
    private TextView authMessage;
    private TextView accountText;
    private TextView vpnStatus;
    private TextView routeText;
    private EditText search;
    private Button startButton;
    private Button stopButton;
    private Button refreshConfigButton;
    private Spinner countrySpinner;
    private ProgressBar progress;

    private boolean registerMode;
    private boolean appsLoaded;
    private boolean spinnerUpdating;
    private String selectedCountry = AppConstants.DEFAULT_COUNTRY;
    private VpnConfig currentConfig;

    private final Runnable statusPoll = new Runnable() {
        @Override public void run() {
            updateVpnStatus();
            statusHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        configRepository = new VpnConfigRepository(this);
        bindViews();
        setupAuth();
        setupApps();
        setupCountrySpinner();

        selectedPackages.addAll(SelectionStore.load(this));
        requestNotificationsIfNeeded();
        renderAuthState();

        if (sessionManager.isLoggedIn()) {
            loadApplicationsIfNeeded();
            refreshProxyConfig(false, null);
            executor.execute(() -> {
                try { sessionManager.validateAccountIfPossible(); }
                catch (Exception ignored) {}
                runOnUiThread(() -> {
                    renderAuthState();
                    if (!sessionManager.isLoggedIn()) stopVpn();
                });
            });
        }
    }

    private void bindViews() {
        authPanel = findViewById(R.id.authPanel);
        loggedPanel = findViewById(R.id.loggedPanel);
        vpnPanel = findViewById(R.id.vpnPanel);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginTab = findViewById(R.id.loginTab);
        registerTab = findViewById(R.id.registerTab);
        authSubmit = findViewById(R.id.authSubmit);
        authMessage = findViewById(R.id.authMessage);
        accountText = findViewById(R.id.accountText);
        vpnStatus = findViewById(R.id.vpnStatus);
        routeText = findViewById(R.id.routeText);
        search = findViewById(R.id.search);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        refreshConfigButton = findViewById(R.id.refreshConfigButton);
        countrySpinner = findViewById(R.id.countrySpinner);
        progress = findViewById(R.id.progress);
    }

    private void setupAuth() {
        loginTab.setOnClickListener(v -> setRegisterMode(false));
        registerTab.setOnClickListener(v -> setRegisterMode(true));
        authSubmit.setOnClickListener(v -> authenticate());
        findViewById(R.id.logoutButton).setOnClickListener(v -> logout());
        setRegisterMode(false);
    }

    private void setRegisterMode(boolean register) {
        registerMode = register;
        nameInput.setVisibility(register ? View.VISIBLE : View.GONE);
        authSubmit.setText(register ? "Зарегистрироваться" : "Войти");
        loginTab.setEnabled(register);
        registerTab.setEnabled(!register);
        authMessage.setText("");
    }

    private void authenticate() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (email.isEmpty() || password.isEmpty() || (registerMode && name.isEmpty())) {
            authMessage.setText("Заполните все поля");
            return;
        }
        setBusy(true, registerMode ? "Регистрация…" : "Вход…");
        executor.execute(() -> {
            try {
                SessionManager.AccountResult account = registerMode
                        ? sessionManager.register(name, email, password)
                        : sessionManager.login(email, password);
                runOnUiThread(() -> {
                    passwordInput.setText("");
                    authMessage.setText("");
                    setBusy(false, null);
                    renderAuthState();
                    loadApplicationsIfNeeded();
                    refreshProxyConfig(false, null);
                    Toast.makeText(this, "Вход выполнен" + (account.name.isEmpty() ? "" : ": " + account.name), Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setBusy(false, null);
                    authMessage.setText(friendlyError(e));
                });
            }
        });
    }

    private void logout() {
        stopVpn();
        setBusy(true, "Выход…");
        executor.execute(() -> {
            try { sessionManager.logout(); }
            catch (Exception ignored) {}
            runOnUiThread(() -> {
                setBusy(false, null);
                currentConfig = null;
                appsLoaded = false;
                adapter.clear();
                renderAuthState();
            });
        });
    }

    private void renderAuthState() {
        boolean loggedIn = sessionManager.isLoggedIn();
        authPanel.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        loggedPanel.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        vpnPanel.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        if (loggedIn) {
            SessionManager.AccountResult account = sessionManager.restoreCachedAccount();
            String label = account.name.isEmpty() ? account.email
                    : account.name + (account.email.isEmpty() ? "" : " · " + account.email);
            accountText.setText(label.isEmpty() ? "Аккаунт Split Proxy" : label);
        } else {
            routeText.setText("Войдите в аккаунт");
        }
        updateVpnStatus();
    }

    private void setupApps() {
        RecyclerView list = findViewById(R.id.appList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppListAdapter((app, selected) -> {
            if (!sessionManager.isLoggedIn()) return;
            if (selected) selectedPackages.add(app.packageName);
            else selectedPackages.remove(app.packageName);
            SelectionStore.save(this, selectedPackages);
            updateVpnStatus();
        });
        list.setAdapter(adapter);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { adapter.filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        startButton.setOnClickListener(v -> prepareStart());
        stopButton.setOnClickListener(v -> stopVpn());
        refreshConfigButton.setOnClickListener(v -> refreshProxyConfig(true, null));
    }

    private void loadApplicationsIfNeeded() {
        if (appsLoaded || !sessionManager.isLoggedIn()) return;
        appsLoaded = true;
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<AppEntry> entries = new ArrayList<>();
            for (ApplicationInfo info : installed) {
                if (getPackageName().equals(info.packageName)) continue;
                if (pm.getLaunchIntentForPackage(info.packageName) == null) continue;
                entries.add(new AppEntry(
                        String.valueOf(pm.getApplicationLabel(info)),
                        info.packageName,
                        pm.getApplicationIcon(info),
                        selectedPackages.contains(info.packageName)));
            }
            entries.sort(Comparator.comparing(a -> a.label.toLowerCase()));
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                adapter.submit(entries);
                updateVpnStatus();
            });
        });
    }

    private void setupCountrySpinner() {
        ArrayAdapter<String> initial = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{AppConstants.DEFAULT_COUNTRY});
        initial.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(initial);
        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerUpdating || !sessionManager.isLoggedIn()) return;
                String country = String.valueOf(parent.getItemAtPosition(position));
                if (!country.equals(selectedCountry)) {
                    selectedCountry = country;
                    currentConfig = null;
                    refreshProxyConfig(false, null);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshProxyConfig(boolean showToast, @Nullable Runnable afterSuccess) {
        if (!sessionManager.isLoggedIn()) return;
        routeText.setText("Получение конфигурации…");
        refreshConfigButton.setEnabled(false);
        executor.execute(() -> {
            try {
                VpnConfig config = configRepository.fetch(selectedCountry);
                VpnConfigStore.save(this, config);
                runOnUiThread(() -> {
                    currentConfig = config;
                    selectedCountry = config.exitCountry;
                    routeText.setText(config.display());
                    applyCountries(config);
                    refreshConfigButton.setEnabled(true);
                    updateVpnStatus();
                    if (showToast) Toast.makeText(this, "Конфигурация обновлена", Toast.LENGTH_SHORT).show();
                    if (afterSuccess != null) afterSuccess.run();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    currentConfig = null;
                    routeText.setText("Ошибка конфигурации: " + friendlyError(e));
                    refreshConfigButton.setEnabled(true);
                    updateVpnStatus();
                });
            }
        });
    }

    private void applyCountries(VpnConfig config) {
        spinnerUpdating = true;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, config.availableCountries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(adapter);
        int index = config.availableCountries.indexOf(config.exitCountry);
        countrySpinner.setSelection(Math.max(0, index), false);
        spinnerUpdating = false;
    }

    private void prepareStart() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Сначала войдите в аккаунт", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedPackages.isEmpty()) {
            Toast.makeText(this, "Выберите хотя бы одно приложение", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!SelectionStore.save(this, selectedPackages)) {
            Toast.makeText(this, "Не удалось сохранить список приложений", Toast.LENGTH_SHORT).show();
            return;
        }
        refreshProxyConfig(false, this::requestVpnPermission);
    }

    private void requestVpnPermission() {
        if (currentConfig == null) return;
        Intent permission = VpnService.prepare(this);
        if (permission != null) startActivityForResult(permission, VPN_REQUEST);
        else startProxyService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST && resultCode == Activity.RESULT_OK) startProxyService();
    }

    private void startProxyService() {
        Intent intent = new Intent(this, ProxyVpnService.class);
        intent.setAction(ProxyVpnService.ACTION_START);
        intent.putStringArrayListExtra(ProxyVpnService.EXTRA_PACKAGES, new ArrayList<>(selectedPackages));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
        vpnStatus.postDelayed(this::updateVpnStatus, 500L);
    }

    private void stopVpn() {
        Intent intent = new Intent(this, ProxyVpnService.class).setAction(ProxyVpnService.ACTION_STOP);
        startService(intent);
        updateVpnStatus();
    }

    private void updateVpnStatus() {
        boolean loggedIn = sessionManager != null && sessionManager.isLoggedIn();
        ProxyStateStore.State state = ProxyStateStore.read(this);
        boolean active = state.isActive();
        if (!loggedIn && active) {
            Intent stop = new Intent(this, ProxyVpnService.class).setAction(ProxyVpnService.ACTION_STOP);
            startService(stop);
            active = false;
        }
        if (ProxyStateStore.RUNNING.equals(state.name)) vpnStatus.setText("VPN включён — " + state.message);
        else if (ProxyStateStore.STARTING.equals(state.name)) vpnStatus.setText(state.message);
        else if (ProxyStateStore.ERROR.equals(state.name)) vpnStatus.setText("Ошибка: " + state.message);
        else vpnStatus.setText("VPN выключен");

        adapter.setLocked(active || !loggedIn);
        search.setEnabled(loggedIn && !active);
        countrySpinner.setEnabled(loggedIn && !active);
        refreshConfigButton.setEnabled(loggedIn && !active);
        startButton.setEnabled(loggedIn && !active && !selectedPackages.isEmpty() && currentConfig != null);
        stopButton.setEnabled(active);
    }

    private void setBusy(boolean busy, String message) {
        loginTab.setEnabled(!busy && registerMode);
        registerTab.setEnabled(!busy && !registerMode);
        authSubmit.setEnabled(!busy);
        nameInput.setEnabled(!busy);
        emailInput.setEnabled(!busy);
        passwordInput.setEnabled(!busy);
        if (message != null) authMessage.setText(message);
    }

    private void requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        }
    }

    private static String friendlyError(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) return error.getClass().getSimpleName();
        return message.replace("[\"", "").replace("\"]", "");
    }

    @Override protected void onResume() {
        super.onResume();
        statusHandler.removeCallbacks(statusPoll);
        statusHandler.post(statusPoll);
    }

    @Override protected void onPause() {
        statusHandler.removeCallbacks(statusPoll);
        super.onPause();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
