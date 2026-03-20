package com.phonestore.controller;

import com.phonestore.model.Employee;
import com.phonestore.util.service.PasswordResetService;
import com.phonestore.util.service.impl.PasswordResetServiceImpl;

public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController() {
        this(new PasswordResetServiceImpl());
    }

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    public Employee findEmployeeById(long employeeId) {
        return service.findEmployeeById(employeeId);
    }

    public String sendOtp(long employeeId, String email) {
        return service.sendOtp(employeeId, email);
    }

    public void resetPassword(long employeeId, String otp, String newPassword) {
        service.resetPassword(employeeId, otp, newPassword);
    }
}
