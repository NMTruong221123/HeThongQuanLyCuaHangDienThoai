package com.phonestore.controller;

import com.phonestore.model.UserAccount;
import com.phonestore.util.service.UserAccountService;
import com.phonestore.util.service.impl.UserAccountServiceImpl;

import java.util.List;

public class UserAccountController {

    private final UserAccountService accountService;

    public UserAccountController() {
        this(new UserAccountServiceImpl());
    }

    public UserAccountController(UserAccountService accountService) {
        this.accountService = accountService;
    }

    public List<UserAccount> findAll() {
        return accountService.findAll();
    }

    public List<UserAccount> search(String keyword) {
        return accountService.search(keyword);
    }

    public UserAccount create(UserAccount account) {
        validate(account);
        return accountService.create(account);
    }

    public UserAccount update(UserAccount account) {
        validate(account);
        return accountService.update(account);
    }

    public void delete(long employeeId) {
        if (employeeId <= 0) throw new IllegalArgumentException("Mã NV không hợp lệ");
        accountService.delete(employeeId);
    }

    private void validate(UserAccount a) {
        if (a == null) throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        if (a.getEmployeeId() <= 0) throw new IllegalArgumentException("Mã NV không hợp lệ");
        if (a.getPassword() == null || a.getPassword().trim().isEmpty()) throw new IllegalArgumentException("Mật khẩu là bắt buộc");
        if (a.getUsername() == null || a.getUsername().trim().isEmpty()) throw new IllegalArgumentException("Tên đăng nhập là bắt buộc");
        if (a.getRoleId() == null || a.getRoleId() <= 0) throw new IllegalArgumentException("Nhóm quyền (ID) là bắt buộc");
        if (a.getStatus() != null && a.getStatus() != 0 && a.getStatus() != 1) {
            throw new IllegalArgumentException("Trạng thái chỉ nhận 0/1 hoặc để trống");
        }
    }
}
