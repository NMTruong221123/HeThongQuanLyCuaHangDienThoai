package com.phonestore.dao;

import com.phonestore.model.UserAccount;

import java.util.List;

public interface UserAccountDao {
    List<UserAccount> findAll();

    List<UserAccount> search(String keyword);

    UserAccount create(UserAccount account);

    UserAccount update(UserAccount account);

    void delete(long employeeId);

    void updateStatus(long employeeId, int status);
}
