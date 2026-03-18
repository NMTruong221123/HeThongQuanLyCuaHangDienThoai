package com.phonestore.controller;

import com.phonestore.ui.common.session.UserSession;
import com.phonestore.util.service.AuthService;
import com.phonestore.util.service.impl.AuthServiceImpl;

public class AuthController {

    private final AuthService authService;

    public AuthController() {
        this(new AuthServiceImpl());
    }

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public UserSession login(String username, String password) {
        String u = username == null ? "" : username.trim();
        String p = password == null ? "" : password;
        if (u.isBlank() || p.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tài khoản và mật khẩu");
        }
        return authService.login(u, p);
    }
}
