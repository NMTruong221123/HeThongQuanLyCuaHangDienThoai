package com.phonestore.util.service;

import com.phonestore.model.ProductVariant;

import java.util.List;

public interface ProductVariantService {
    List<ProductVariant> findByProductId(long productId);

    ProductVariant create(ProductVariant variant);

    ProductVariant update(ProductVariant variant);

    void delete(long id);
}
