package com.uniondesk.auth.core;

import java.util.Optional;

public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CURRENT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserContext context) {
        if (context == null) {
            CURRENT.remove();
            return;
        }
        CURRENT.set(context);
    }

    public static Optional<UserContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UserContext requireCurrent() {
        return current().orElseThrow(() -> new IllegalStateException("user context is not available"));
    }

    public static void clear() {
        CURRENT.remove();
    }
}
