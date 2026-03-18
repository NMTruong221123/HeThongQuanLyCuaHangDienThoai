package com.phonestore.util.service.impl;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.ProductDao;
import com.phonestore.dao.jdbc.ProductJdbcDao;
import com.phonestore.model.Product;
import com.phonestore.util.service.ProductService;

import java.util.List;

public class ProductServiceImpl implements ProductService {
    private final ProductDao jdbcDao = new ProductJdbcDao();

    private void requireDb() {
        if (!JDBCUtil.canConnect()) {
            throw new IllegalStateException("Chưa kết nối được DB");
        }
    }

    @Override
    public List<Product> findAll() {
        requireDb();
        return jdbcDao.findAll();
    }

    @Override
    public List<Product> search(String keyword) {
        requireDb();
        return jdbcDao.search(keyword);
    }

    @Override
    public Product create(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        validate(product);
        requireDb();
        return jdbcDao.create(product);
    }

    @Override
    public Product update(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        validate(product);
        requireDb();
        return jdbcDao.update(product);
    }

    @Override
    public void delete(long id) {
        requireDb();
        jdbcDao.delete(id);
    }

    private void validate(Product p) {
        if (isBlank(p.getName())) throw new IllegalArgumentException("Tên sản phẩm là bắt buộc");
        if (p.getOriginId() == null || p.getOriginId() <= 0) throw new IllegalArgumentException("Xuất xứ (ID) là bắt buộc");
        if (p.getBrandId() == null || p.getBrandId() <= 0) throw new IllegalArgumentException("Thương hiệu (ID) là bắt buộc");
        if (p.getOperatingSystemId() == null || p.getOperatingSystemId() <= 0) {
            throw new IllegalArgumentException("Hệ điều hành (ID) là bắt buộc");
        }
        if (isBlank(p.getChipProcessor())) throw new IllegalArgumentException("Chip xử lý là bắt buộc");
        if (p.getBatteryCapacity() == null || p.getBatteryCapacity() <= 0) throw new IllegalArgumentException("Dung lượng pin không hợp lệ");
        if (p.getScreenSize() == null || p.getScreenSize() <= 0) throw new IllegalArgumentException("Kích thước màn hình không hợp lệ");
        if (isBlank(p.getRearCamera())) throw new IllegalArgumentException("Camera sau là bắt buộc");
        if (isBlank(p.getFrontCamera())) throw new IllegalArgumentException("Camera trước là bắt buộc");
        if (p.getOsVersion() == null || p.getOsVersion() <= 0) throw new IllegalArgumentException("Phiên bản HĐH không hợp lệ");
        if (p.getWarrantyMonths() == null || p.getWarrantyMonths() < 0) throw new IllegalArgumentException("Thời gian bảo hành không hợp lệ");
        if (p.getZoneId() == null || p.getZoneId() <= 0) {
            throw new IllegalArgumentException("Khu vực (ID) là bắt buộc");
        }
        if (p.getStock() == null) p.setStock(0);
        if (p.getStock() < 0) throw new IllegalArgumentException("Số lượng tồn không hợp lệ");
        if (p.getStatus() == null) p.setStatus(1);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
