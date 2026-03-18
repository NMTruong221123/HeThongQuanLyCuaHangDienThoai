package com.phonestore.controller;

import com.phonestore.model.Customer;
import com.phonestore.util.service.CustomerService;
import com.phonestore.util.service.impl.CustomerServiceImpl;

import java.util.List;

public class CustomerController {

    private final CustomerService customerService;

    public CustomerController() {
        this(new CustomerServiceImpl());
    }

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    public List<Customer> findAll() {
        return customerService.findAll();
    }

    public Customer findById(long id) {
        if (id <= 0) return null;
        return customerService.findById(id);
    }

    public List<Customer> search(String keyword) {
        return customerService.search(keyword);
    }

    public Customer create(Customer customer) {
        validate(customer);
        return customerService.create(customer);
    }

    public Customer update(Customer customer) {
        validate(customer);
        return customerService.update(customer);
    }

    public void delete(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID không hợp lệ");
        }
        customerService.delete(id);
    }

    private void validate(Customer c) {
        if (c == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        if (isBlank(c.getName())) {
            throw new IllegalArgumentException("Tên khách hàng là bắt buộc");
        }
        if (isBlank(c.getPhone())) {
            throw new IllegalArgumentException("SĐT là bắt buộc");
        }
        if (isBlank(c.getAddress())) {
            throw new IllegalArgumentException("Địa chỉ là bắt buộc");
        }
        if (c.getStatus() != null && c.getStatus() != 0 && c.getStatus() != 1) {
            throw new IllegalArgumentException("Trạng thái chỉ nhận 0/1 hoặc để trống");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
