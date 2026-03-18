package com.phonestore.dao;

import com.phonestore.model.Supplier;

import java.util.List;

public interface SupplierDao {
    List<Supplier> findAll();

    List<Supplier> search(String keyword);

    Supplier create(Supplier supplier);

    Supplier update(Supplier supplier);

    void delete(long id);
}
