package com.phonestore.util.service.impl;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.ProductVariantDao;
import com.phonestore.dao.jdbc.ProductVariantJdbcDao;
import com.phonestore.model.ProductVariant;
import com.phonestore.util.service.ProductVariantService;

import java.util.List;

public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantDao jdbcDao = new ProductVariantJdbcDao();

    private void requireDb() {
        if (!JDBCUtil.canConnect()) {
            throw new IllegalStateException("Chưa kết nối được DB");
        }
    }

    @Override
    public List<ProductVariant> findByProductId(long productId) {
        requireDb();
        if (productId <= 0) return List.of();
        return jdbcDao.findByProductId(productId);
    }

    @Override
    public ProductVariant create(ProductVariant variant) {
        requireDb();
        validate(variant, true);
        if (variant.getStock() == null) variant.setStock(0);
        if (variant.getStatus() == null) variant.setStatus(1);
        return jdbcDao.create(variant);
    }

    @Override
    public ProductVariant update(ProductVariant variant) {
        requireDb();
        validate(variant, false);
        return jdbcDao.update(variant);
    }

    @Override
    public void delete(long id) {
        requireDb();
        if (id <= 0) throw new IllegalArgumentException("ID không hợp lệ");
        jdbcDao.delete(id);
    }

    private void validate(ProductVariant v, boolean isCreate) {
        if (v == null) throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        if (!isCreate && v.getId() <= 0) throw new IllegalArgumentException("ID không hợp lệ");
        if (v.getProductId() == null || v.getProductId() <= 0) throw new IllegalArgumentException("Sản phẩm (ID) là bắt buộc");
        if (v.getRomId() == null || v.getRomId() <= 0) throw new IllegalArgumentException("ROM là bắt buộc");
        if (v.getRamId() == null || v.getRamId() <= 0) throw new IllegalArgumentException("RAM là bắt buộc");
        if (v.getColorId() == null || v.getColorId() <= 0) throw new IllegalArgumentException("Màu sắc là bắt buộc");
        if (v.getImportPrice() == null || v.getImportPrice() < 0) throw new IllegalArgumentException("Giá nhập không hợp lệ");
        if (v.getExportPrice() == null || v.getExportPrice() < 0) throw new IllegalArgumentException("Giá xuất không hợp lệ");
    }
}
