package com.phonestore.controller;

import com.phonestore.model.ProductVariant;
import com.phonestore.util.service.ProductVariantService;
import com.phonestore.util.service.impl.ProductVariantServiceImpl;

import java.util.List;

public class ProductVariantController {

    private final ProductVariantService service;

    public ProductVariantController() {
        this(new ProductVariantServiceImpl());
    }

    public ProductVariantController(ProductVariantService service) {
        this.service = service;
    }

    public List<ProductVariant> findByProductId(long productId) {
        return service.findByProductId(productId);
    }

    public ProductVariant create(ProductVariant variant) {
        return service.create(variant);
    }

    public ProductVariant update(ProductVariant variant) {
        return service.update(variant);
    }

    public void delete(long id) {
        service.delete(id);
    }
}
