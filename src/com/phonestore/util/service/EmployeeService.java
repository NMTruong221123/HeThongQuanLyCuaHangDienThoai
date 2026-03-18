package com.phonestore.util.service;

import com.phonestore.model.Employee;

import java.util.List;

public interface EmployeeService {
    List<Employee> findAll();

    List<Employee> search(String keyword);

    Employee create(Employee employee);

    Employee update(Employee employee);

    void delete(long id);

    Employee getById(long id);
}
