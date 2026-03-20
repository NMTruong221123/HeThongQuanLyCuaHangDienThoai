package com.phonestore.ui.common.session;

import java.util.Collections;
import java.util.Set;

public class UserSession {
    private final Long userId;
    private final String username;
    private final Set<String> permissions;

    public UserSession(Long userId, String username, Set<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.permissions = permissions == null ? Collections.emptySet() : Collections.unmodifiableSet(permissions);
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(String permissionKey) {
        return permissions.contains(permissionKey);
    }
}
