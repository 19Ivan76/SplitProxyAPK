package ru.splitproxy.apk;

public final class NativeBridge {
    private static volatile boolean loaded;

    private NativeBridge() {}

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        System.loadLibrary("splitproxy_jni");
        loaded = true;
    }

    public static int run(String commandLine, int mtu) {
        ensureLoaded();
        return nativeRun(commandLine, mtu);
    }

    public static int stop() {
        if (!loaded) return 0;
        return nativeStop();
    }

    private static native int nativeRun(String commandLine, int mtu);
    private static native int nativeStop();
}
