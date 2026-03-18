package com.phonestore.util.service;

import com.phonestore.model.UserAccount;

import java.util.List;

public interface UserAccountService {
    List<UserAccount> findAll();

    List<UserAccount> search(String keyword);

    UserAccount create(UserAccount account);

    UserAccount update(UserAccount account);

    void delete(long employeeId);

    void updateStatus(long employeeId, int status);
}
