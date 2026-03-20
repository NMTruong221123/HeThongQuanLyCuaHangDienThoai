package com.phonestore.util.service;

import com.phonestore.model.Customer;

import java.util.List;

public interface CustomerService {
    List<Customer> findAll();

    Customer findById(long id);

    List<Customer> search(String keyword);

    Customer create(Customer customer);

    Customer update(Customer customer);

    void delete(long id);
}
