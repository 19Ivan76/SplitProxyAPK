# SplitProxyAPK

Android-клиент Split Proxy. Первый этап новой версии на основе рабочего браузерного расширения `splitproxy-chromium-1.1.3` и проверенного VPN-движка старого SplitProxyMobile.

## Что уже реализовано

- регистрация и вход через `https://split-proxy.ru/api/v1/`;
- без входа список приложений и кнопка VPN недоступны;
- отдельная установка Android получает постоянный `install_id`;
- ECDSA P-256 ключ хранится в Android Keystore;
- публичный ключ отправляется серверу как PEM/SPKI;
- подпись активации — SHA-256 ECDSA, Base64 P1363 `r||s` (64 байта), как в браузерном расширении;
- токены устройства и аккаунта шифруются AES-GCM ключом из Android Keystore;
- установка сначала создаёт техническую guest-сессию устройства, но **никаких гостевых VPN-функций в UI нет**;
- после входа выполняется `guest/claim`, то есть устройство привязывается к аккаунту;
- список установленных приложений становится доступен только после входа;
- VPN строится через Android `VpnService` + `tun2proxy 0.8.1`;
- через VPN идут только отмеченные приложения (`addAllowedApplication`);
- IP/порт прокси в APK не прошиты: приложение читает `proxy.server` / `proxy.port` из серверной конфигурации;
- при выходе из аккаунта VPN останавливается.

## Важная серверная зависимость

Текущий браузерный API должен разрешить Android-клиенту регистрацию установки:

- `platform = android`
- `client_type = android`

Также `devices/proxy/config` должен для Android возвращать структурированный объект `proxy` с `server` и `port`, независимо от списка браузерных сайтов. Подробности: `SERVER-ANDROID-API.md`.

## Сборка

GitHub Actions: `.github/workflows/build-apk.yml`.

После push в `main`:

1. открыть GitHub → Actions → `Build Android APK`;
2. дождаться `Build debug APK`;
3. скачать artifact `SplitProxyAPK-v0.1.0-debug`;
4. внутри будет `app-debug.apk`.

## Текущая версия

`0.1.0`, package: `ru.splitproxy.apk`.
