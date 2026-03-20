package com.phonestore.util.service;

import com.phonestore.ui.common.session.UserSession;

public interface AuthService {
    UserSession login(String username, String password);
}
