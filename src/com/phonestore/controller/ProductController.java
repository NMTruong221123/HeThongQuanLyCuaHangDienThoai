package com.phonestore.controller;

import com.phonestore.model.Product;
import com.phonestore.util.service.ProductService;
import com.phonestore.util.service.impl.ProductServiceImpl;

import java.util.List;

public class ProductController {

    private final ProductService productService;

    public ProductController() {
        this(new ProductServiceImpl());
    }

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    public List<Product> findAll() {
        return productService.findAll();
    }

    public List<Product> search(String keyword) {
        return productService.search(keyword);
    }

    public Product create(Product product) {
        validate(product);
        return productService.create(product);
    }

    public Product update(Product product) {
        validate(product);
        return productService.update(product);
    }

    public void delete(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID không hợp lệ");
        }
        productService.delete(id);
    }

    private void validate(Product p) {
        if (p == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        if (p.getName() == null || p.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm là bắt buộc");
        }
        if (p.getOriginId() == null || p.getOriginId() <= 0) throw new IllegalArgumentException("Xuất xứ (ID) là bắt buộc");
        if (p.getBrandId() == null || p.getBrandId() <= 0) throw new IllegalArgumentException("Hãng (ID) là bắt buộc");
        if (p.getOperatingSystemId() == null || p.getOperatingSystemId() <= 0) {
            throw new IllegalArgumentException("Hệ điều hành (ID) là bắt buộc");
        }
        if (p.getChipProcessor() == null || p.getChipProcessor().trim().isEmpty()) {
            throw new IllegalArgumentException("Chip xử lý là bắt buộc");
        }
        if (p.getBatteryCapacity() == null || p.getBatteryCapacity() <= 0) {
            throw new IllegalArgumentException("Dung lượng pin không hợp lệ");
        }
        if (p.getScreenSize() == null || p.getScreenSize() <= 0) {
            throw new IllegalArgumentException("Kích thước màn hình không hợp lệ");
        }
        if (p.getRearCamera() == null || p.getRearCamera().trim().isEmpty()) {
            throw new IllegalArgumentException("Camera sau là bắt buộc");
        }
        if (p.getFrontCamera() == null || p.getFrontCamera().trim().isEmpty()) {
            throw new IllegalArgumentException("Camera trước là bắt buộc");
        }
        if (p.getOsVersion() == null || p.getOsVersion() <= 0) {
            throw new IllegalArgumentException("Phiên bản HĐH không hợp lệ");
        }
        if (p.getWarrantyMonths() == null || p.getWarrantyMonths() < 0) {
            throw new IllegalArgumentException("Thời gian bảo hành không hợp lệ");
        }
        if (p.getZoneId() == null || p.getZoneId() <= 0) {
            throw new IllegalArgumentException("Khu vực (ID) là bắt buộc");
        }
        if (p.getStock() == null) p.setStock(0);
        if (p.getStock() < 0) throw new IllegalArgumentException("Số lượng tồn không hợp lệ");
        if (p.getStatus() == null) p.setStatus(1);
        if (p.getStatus() != 0 && p.getStatus() != 1) throw new IllegalArgumentException("Trạng thái chỉ nhận 0/1");
    }
}
