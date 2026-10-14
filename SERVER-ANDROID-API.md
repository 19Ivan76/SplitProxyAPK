# Android API — состояние для SplitProxyAPK 0.1.1

## Реализовано клиентом по фактическому Laravel-контракту

### 1. Вход
`POST auth/login` → `access_token`.

### 2. Запрос активации
`POST devices/activation/request`

```json
{
  "install_id": "uuid",
  "platform": "android",
  "device_name": "Manufacturer Model",
  "public_key": "-----BEGIN PUBLIC KEY-----..."
}
```

`client_type` для Android не передаётся.

### 3. Подтверждение владельцем аккаунта
`POST devices/activation/approve`

Bearer account token.

```json
{ "request_code": "SP-...." }
```

### 4. Подпись
Каноническая строка UTF-8 без завершающего LF:

`SP-ACTIVATE-V1\n<install_id>\n<CODE>`

`CODE` = `response_code` без `ACT-` и дефисов, uppercase.

ECDSA P-256 / SHA-256. В API отправляется Base64 от 64 байт P1363 `R||S`.

### 5. Завершение
`POST devices/activation/finish`

```json
{
  "install_id": "uuid",
  "response_code": "ACT-....",
  "signature": "Base64 P1363"
}
```

Ожидается `status=active`, `key_id`, `device_token`.

## Следующий серверный вопрос

Нужен подтверждённый Android-контракт получения VPN-конфигурации. Имеющийся `GET devices/proxy/config` использует browser-specific auth/sites. APK не должен зависеть от списка сайтов браузера.
