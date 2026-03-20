package com.phonestore.util.service.impl;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.SupplierDao;
import com.phonestore.dao.jdbc.SupplierJdbcDao;
import com.phonestore.model.Supplier;
import com.phonestore.util.service.SupplierService;

import java.util.List;

public class SupplierServiceImpl implements SupplierService {
    private final SupplierDao jdbcDao = new SupplierJdbcDao();

    private void requireDb() {
        if (!JDBCUtil.canConnect()) {
            throw new IllegalStateException("Chưa kết nối được DB");
        }
    }

    @Override
    public List<Supplier> findAll() {
        requireDb();
        return jdbcDao.findAll();
    }

    @Override
    public List<Supplier> search(String keyword) {
        requireDb();
        return jdbcDao.search(keyword);
    }

    @Override
    public Supplier create(Supplier supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        validate(supplier);

        if (supplier.getStatus() == null) {
            supplier.setStatus(1);
        }

        requireDb();
        return jdbcDao.create(supplier);
    }

    @Override
    public Supplier update(Supplier supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        validate(supplier);

        if (supplier.getStatus() == null) {
            supplier.setStatus(1);
        }

        requireDb();
        return jdbcDao.update(supplier);
    }

    @Override
    public void delete(long id) {
        requireDb();
        jdbcDao.delete(id);
    }

    private void validate(Supplier s) {
        if (isBlank(s.getName())) {
            throw new IllegalArgumentException("Tên nhà cung cấp là bắt buộc");
        }
        if (isBlank(s.getPhone())) {
            throw new IllegalArgumentException("SĐT là bắt buộc");
        }
        if (isBlank(s.getEmail())) {
            throw new IllegalArgumentException("Email là bắt buộc");
        }
        if (isBlank(s.getAddress())) {
            throw new IllegalArgumentException("Địa chỉ là bắt buộc");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
