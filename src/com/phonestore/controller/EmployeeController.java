package com.phonestore.controller;

import com.phonestore.model.Employee;
import com.phonestore.util.service.EmployeeService;
import com.phonestore.util.service.impl.EmployeeServiceImpl;

import java.util.List;

public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController() {
        this(new EmployeeServiceImpl());
    }

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    public List<Employee> findAll() {
        return employeeService.findAll();
    }

    public List<Employee> search(String keyword) {
        return employeeService.search(keyword);
    }

    public Employee create(Employee employee) {
        validate(employee);
        return employeeService.create(employee);
    }

    public Employee update(Employee employee) {
        validate(employee);
        return employeeService.update(employee);
    }

    public void delete(long id) {
        if (id <= 0) throw new IllegalArgumentException("ID không hợp lệ");
        employeeService.delete(id);
    }

    private void validate(Employee e) {
        if (e == null) throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        if (e.getFullName() == null || e.getFullName().trim().isEmpty()) throw new IllegalArgumentException("Họ tên là bắt buộc");
        if (e.getBirthDate() == null) throw new IllegalArgumentException("Ngày sinh là bắt buộc");
        if (e.getGender() == null) throw new IllegalArgumentException("Giới tính là bắt buộc (0/1)");
        if (e.getPhone() == null || e.getPhone().trim().isEmpty()) throw new IllegalArgumentException("SĐT là bắt buộc");
        if (e.getEmail() == null || e.getEmail().trim().isEmpty()) throw new IllegalArgumentException("Email là bắt buộc");
        if (e.getStatus() != null && e.getStatus() != 0 && e.getStatus() != 1) {
            throw new IllegalArgumentException("Trạng thái chỉ nhận 0/1 hoặc để trống");
        }
    }
}
