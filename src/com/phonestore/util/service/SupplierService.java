package com.phonestore.util.service;

import com.phonestore.model.Supplier;

import java.util.List;

public interface SupplierService {
    List<Supplier> findAll();

    List<Supplier> search(String keyword);

    Supplier create(Supplier supplier);

    Supplier update(Supplier supplier);

    void delete(long id);
}
