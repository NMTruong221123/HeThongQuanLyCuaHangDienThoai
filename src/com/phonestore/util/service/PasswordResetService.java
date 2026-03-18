package com.phonestore.util.service;

import com.phonestore.model.Employee;

public interface PasswordResetService {

    Employee findEmployeeById(long employeeId);

    /**
     * Verifies employee + email match, sends OTP to employee email, and returns masked recipient for UI.
     */
    String sendOtp(long employeeId, String email);

    void resetPassword(long employeeId, String otp, String newPassword);
}
