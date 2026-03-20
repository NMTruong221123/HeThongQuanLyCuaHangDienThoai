package com.phonestore.ui.common.session;

public final class SessionContext {

    private static volatile UserSession session;

    private SessionContext() {}

    public static UserSession getSession() {
        return session;
    }

    public static void setSession(UserSession session) {
        SessionContext.session = session;
    }

    public static void clear() {
        SessionContext.session = null;
    }
}
