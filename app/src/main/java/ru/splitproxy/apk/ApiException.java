package ru.splitproxy.apk;

public final class ApiException extends Exception {
    public final int status;
    public final String responseBody;

    public ApiException(int status, String message, String responseBody) {
        super(message);
        this.status = status;
        this.responseBody = responseBody;
    }
}
