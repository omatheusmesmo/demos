package dev.omatheusmesmo.demo.threading.pinning;

/**
 * A class whose static initializer blocks. Class initialization runs through native code, which is one of the
 * cases where a virtual thread still cannot unmount on Java 25.
 */
final class SlowInitializer {

    static final String VALUE = initialize();

    private SlowInitializer() {
    }

    private static String initialize() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "initialized";
    }
}
