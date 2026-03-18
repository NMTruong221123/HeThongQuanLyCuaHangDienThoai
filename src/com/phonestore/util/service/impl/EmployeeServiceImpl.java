package com.phonestore.util.service.impl;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.EmployeeDao;
import com.phonestore.dao.jdbc.EmployeeJdbcDao;
import com.phonestore.model.Employee;
import com.phonestore.util.service.EmployeeService;

import java.util.List;

public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeDao jdbcDao = new EmployeeJdbcDao();

    private void requireDb() {
        if (!JDBCUtil.canConnect()) {
            throw new IllegalStateException("Chưa kết nối được DB");
        }
    }

    @Override
    public List<Employee> findAll() {
        requireDb();
        return jdbcDao.findAll();
    }

    @Override
    public List<Employee> search(String keyword) {
        requireDb();
        return jdbcDao.search(keyword);
    }

    @Override
    public Employee create(Employee employee) {
        validate(employee);
        if (employee.getStatus() == null) employee.setStatus(1);
        requireDb();
        return jdbcDao.create(employee);
    }

    @Override
    public Employee update(Employee employee) {
        validate(employee);
        if (employee.getStatus() == null) employee.setStatus(1);
        requireDb();
        Employee updated = jdbcDao.update(employee);

        // Sync account status based on employee status:
        // If employee is suspended (0) -> lock account (0)
        // If employee is active (1) -> unlock account (1)
        try {
            Integer st = updated.getStatus();
            if (st != null) {
                new com.phonestore.util.service.impl.UserAccountServiceImpl().updateStatus(updated.getId(), st);
            }
        } catch (Exception ignored) {
        }

        return updated;
    }

    @Override
    public void delete(long id) {
        requireDb();
        // Try to remove associated user account first to avoid FK constraint blocking
        try {
            new com.phonestore.util.service.impl.UserAccountServiceImpl().delete(id);
        } catch (Exception ex) {
            // If account deletion failed due to foreign key refs, propagate a helpful message
            String msg = ex.getMessage() == null ? "" : ex.getMessage();
            if (msg.toLowerCase().contains("không thể xóa tài khoản") || msg.toLowerCase().contains("foreign key")) {
                throw new IllegalStateException("Không thể xóa nhân viên vì tài khoản liên quan không thể xóa (có dữ liệu tham chiếu). Hãy xóa hoặc khóa các dữ liệu tham chiếu (phiếu nhập/phiếu xuất) trước.", ex);
            }
            // otherwise continue — account may not exist
        }
        try {
            jdbcDao.delete(id);
        } catch (RuntimeException re) {
            String m = re.getMessage() == null ? "" : re.getMessage().toLowerCase();
            if (m.contains("foreign key") || m.contains("cannot delete") || m.contains("integrity")) {
                throw new IllegalStateException("Không thể xóa nhân viên do có dữ liệu tham chiếu trong DB. Xóa các bản ghi liên quan (ví dụ: tài khoản, phiếu) trước.", re);
            }
            throw re;
        }
    }

    @Override
    public Employee getById(long id) {
        requireDb();
        return jdbcDao.getById(id);
    }

    private void validate(Employee e) {
        if (e == null) throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        if (isBlank(e.getFullName())) throw new IllegalArgumentException("Họ tên là bắt buộc");
        if (e.getBirthDate() == null) throw new IllegalArgumentException("Ngày sinh là bắt buộc");
        if (isBlank(e.getPhone())) throw new IllegalArgumentException("SĐT là bắt buộc");
        if (isBlank(e.getEmail())) throw new IllegalArgumentException("Email là bắt buộc");
        if (e.getGender() == null) throw new IllegalArgumentException("Giới tính là bắt buộc (0/1)");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
