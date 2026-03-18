package com.phonestore.util.service.impl;

import com.phonestore.dao.WarehouseZoneDao;
import com.phonestore.dao.jdbc.WarehouseZoneJdbcDao;
import com.phonestore.model.WarehouseZone;
import com.phonestore.util.service.WarehouseZoneService;

import java.util.List;

public class WarehouseZoneServiceImpl implements WarehouseZoneService {

    private final WarehouseZoneDao jdbcDao = new WarehouseZoneJdbcDao();

    @Override
    public List<WarehouseZone> findAll() {
        return jdbcDao.findAll();
    }

    @Override
    public List<WarehouseZone> search(String keyword) {
        return jdbcDao.search(keyword);
    }

    @Override
    public WarehouseZone create(WarehouseZone zone) {
        if (zone == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        validate(zone);
        if (zone.getStatus() == null) zone.setStatus(1);
        return jdbcDao.create(zone);
    }

    @Override
    public WarehouseZone update(WarehouseZone zone) {
        if (zone == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        validate(zone);
        if (zone.getStatus() == null) zone.setStatus(1);
        return jdbcDao.update(zone);
    }

    @Override
    public void delete(long id) {
        jdbcDao.delete(id);
    }

    private void validate(WarehouseZone z) {
        if (isBlank(z.getName())) {
            throw new IllegalArgumentException("Tên khu vực là bắt buộc");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
