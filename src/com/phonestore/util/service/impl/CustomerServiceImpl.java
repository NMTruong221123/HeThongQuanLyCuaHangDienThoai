package com.phonestore.util.service.impl;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.CustomerDao;
import com.phonestore.dao.jdbc.CustomerJdbcDao;
import com.phonestore.model.Customer;
import com.phonestore.util.service.CustomerService;

import java.util.List;

public class CustomerServiceImpl implements CustomerService {
    private final CustomerDao jdbcDao = new CustomerJdbcDao();

    private void requireDb() {
        if (!JDBCUtil.canConnect()) {
            throw new IllegalStateException("Chưa kết nối được DB");
        }
    }

    @Override
    public List<Customer> findAll() {
        requireDb();
        return jdbcDao.findAll();
    }

    @Override
    public Customer findById(long id) {
        requireDb();
        return jdbcDao.findById(id);
    }

    @Override
    public List<Customer> search(String keyword) {
        requireDb();
        return jdbcDao.search(keyword);
    }

    @Override
    public Customer create(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        validate(customer);

        if (customer.getStatus() == null) {
            customer.setStatus(1);
        }

        requireDb();
        return jdbcDao.create(customer);
    }

    @Override
    public Customer update(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        validate(customer);

        if (customer.getStatus() == null) {
            customer.setStatus(1);
        }

        requireDb();
        return jdbcDao.update(customer);
    }

    @Override
    public void delete(long id) {
        requireDb();
        jdbcDao.delete(id);
    }

    private void validate(Customer c) {
        if (isBlank(c.getName())) {
            throw new IllegalArgumentException("Tên khách hàng là bắt buộc");
        }
        if (isBlank(c.getPhone())) {
            throw new IllegalArgumentException("SĐT là bắt buộc");
        }
        if (isBlank(c.getAddress())) {
            throw new IllegalArgumentException("Địa chỉ là bắt buộc");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
