# SplitProxyAPK 0.1.2

Исправлена последовательность авторизации/активации Android.

## Теперь вход работает так

1. `POST auth/login` (либо регистрация, затем login).
2. Если эта установка ещё не активирована: `POST devices/activation/request` с `platform=android`, без `client_type`.
3. `POST devices/activation/approve` с Bearer-токеном аккаунта.
4. APK подписывает `SP-ACTIVATE-V1\n<install_id>\n<response_code без ACT- и дефисов>` ключом ECDSA P-256/SHA-256.
5. `POST devices/activation/finish`.
6. `key_id` и `device_token` сохраняются защищённо.
7. Только после успешной активации сохраняется авторизованная сессия аккаунта и открывается список приложений.

Гостевой браузерный API APK больше не использует.

## VPN

VPN по-прежнему получает адрес/порт только с API. Текущий серверный `devices/proxy/config` в имеющемся серверном срезе реализован через `ActiveBrowserDeviceAuth` и `BrowserSiteService`, поэтому перед финальной Android-интеграцией его контракт надо проверить/разделить на Android endpoint.

## Сборка

GitHub Actions: `.github/workflows/build-apk.yml` → artifact `SplitProxyAPK-v0.1.2-debug`.


## v0.1.2

Исправлено сохранение токенов через AndroidKeyStore AES-GCM: IV для шифрования теперь генерирует сам AndroidKeyStore; сохранение пары device key/token выполняется атомарно.
