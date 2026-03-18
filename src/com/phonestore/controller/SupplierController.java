package com.phonestore.controller;

import com.phonestore.model.Supplier;
import com.phonestore.util.service.SupplierService;
import com.phonestore.util.service.impl.SupplierServiceImpl;

import java.util.List;

public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController() {
        this(new SupplierServiceImpl());
    }

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    public List<Supplier> findAll() {
        return supplierService.findAll();
    }

    public List<Supplier> search(String keyword) {
        return supplierService.search(keyword);
    }

    public Supplier create(Supplier supplier) {
        validate(supplier);
        return supplierService.create(supplier);
    }

    public Supplier update(Supplier supplier) {
        validate(supplier);
        return supplierService.update(supplier);
    }

    public void delete(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID không hợp lệ");
        }
        supplierService.delete(id);
    }

    private void validate(Supplier s) {
        if (s == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
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
        if (s.getStatus() != null && s.getStatus() != 0 && s.getStatus() != 1) {
            throw new IllegalArgumentException("Trạng thái chỉ nhận 0/1 hoặc để trống");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
