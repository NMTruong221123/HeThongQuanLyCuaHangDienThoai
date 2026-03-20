package com.phonestore.dao;

import com.phonestore.model.Product;

import java.util.List;

public interface ProductDao {
    List<Product> findAll();

    List<Product> search(String keyword);

    Product create(Product product);

    Product update(Product product);

    void delete(long id);
}
