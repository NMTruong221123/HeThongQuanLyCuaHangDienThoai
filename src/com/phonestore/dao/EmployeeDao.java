package com.phonestore.dao;

import com.phonestore.model.Employee;

import java.util.List;

public interface EmployeeDao {
    List<Employee> findAll();

    List<Employee> search(String keyword);

    Employee getById(long id);

    Employee create(Employee employee);

    Employee update(Employee employee);

    void delete(long id);
}
