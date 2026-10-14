# Что требуется серверу для SplitProxyAPK 0.1.0

APK не подключается к PostgreSQL напрямую. Единственная постоянная точка — `https://split-proxy.ru/api/v1/`.

## Уже используемые контракты из браузерного расширения

### Регистрация / вход

- `POST auth/register`
  - `name`
  - `email`
  - `password`
  - `password_confirmation`
- `POST auth/login`
  - ожидается `access_token`
  - ожидается `user.name`, `user.email`
- `GET auth/me`
- `POST auth/logout`

### Техническая активация установки

APK использует тот же криптографический протокол, что Chromium 1.1.3:

1. `POST guest/devices/challenge`
   - `install_id`
   - `platform: android`
   - `client_type: android`
   - `public_key`: PEM/SPKI P-256
2. Каноническая строка подписи:

   `SP-GUEST-ACTIVATE-V1\n{install_id}\n{CHALLENGE_UPPERCASE}`

3. `POST guest/devices/activate`
   - `install_id`
   - `challenge`
   - `signature`: Base64 от 64 байт P1363 `r||s`
4. ожидаются:
   - `status: guest`
   - `key_id`
   - `device_token`

Это только техническая регистрация установки. До входа пользователя Android-клиент не запрашивает рабочую VPN-конфигурацию и не позволяет выбрать приложения.

### Привязка после входа

`POST guest/claim`

- `Authorization: Bearer <account token>`
- `X-Guest-Device-Key: <key_id>`
- `X-Guest-Device-Token: <device_token>`

Сервер должен разрешить claim для `platform=android`.

## Конфигурация VPN

На первом этапе APK вызывает уже существующий:

`GET devices/proxy/config?country=NL`

Headers:

- `Authorization: Bearer <device_token>`
- `X-Device-Key: <key_id>`

Для Android ответ обязан содержать минимум:

```json
{
  "proxy": {
    "server": "proxy.example.net",
    "port": 28443,
    "scheme": "http",
    "entry_node": "rf2",
    "exit_node": "nd"
  },
  "exit_country": "NL",
  "available_exit_countries": ["NL"]
}
```

`server` и `port` не должны быть зашиты в APK.

### Критично

Браузерный `devices/proxy/config` сейчас связан с PAC и сайтами. Для Android VPN список браузерных сайтов не должен определять доступность прокси. Если существующий route невозможно безопасно сделать общим, нужен отдельный серверный route для Android. Его URL нужно согласовать до изменения APK — не придумывать клиентом самостоятельно.

## Лимиты устройств

Нужно отдельно определить серверное правило для Android-установок. Сейчас APK при HTTP 422 на `guest/claim` сообщает пользователю, что достигнут лимит и старое устройство нужно отозвать в личном кабинете. Автоматическую замену устройства в 0.1.0 не реализуем до точного контракта сервера.
