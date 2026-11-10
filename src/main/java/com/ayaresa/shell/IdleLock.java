package com.ayaresa.shell;

import javax.swing.Timer;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.time.Duration;

/**
 * Signs the user out after a period without keyboard or mouse activity, so an
 * unattended clinic PC doesn't stay open on patient records.
 */
final class IdleLock {

    private static final long EVENTS = AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK
            | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK;

    private final long timeoutMs;
    private final Runnable onTimeout;
    private final AWTEventListener listener = e -> touch();
    private final Timer check;
    private volatile long lastActivity = System.currentTimeMillis();

    IdleLock(Duration timeout, Runnable onTimeout) {
        this.timeoutMs = timeout.toMillis();
        this.onTimeout = onTimeout;
        this.check = new Timer(15_000, e -> {
            if (System.currentTimeMillis() - lastActivity >= timeoutMs) {
                stop();
                onTimeout.run();
            }
        });
    }

    void start() {
        Toolkit.getDefaultToolkit().addAWTEventListener(listener, EVENTS);
        touch();
        check.start();
    }

    /** Must be called when the window closes, or the global listener leaks. */
    void stop() {
        check.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
    }

    private void touch() {
        lastActivity = System.currentTimeMillis();
    }
}
