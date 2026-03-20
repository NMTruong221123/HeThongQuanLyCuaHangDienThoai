package com.phonestore.controller;

import com.phonestore.model.WarehouseZone;
import com.phonestore.util.service.WarehouseZoneService;
import com.phonestore.util.service.impl.WarehouseZoneServiceImpl;

import java.util.List;

public class WarehouseZoneController {

    private final WarehouseZoneService zoneService;

    public WarehouseZoneController() {
        this(new WarehouseZoneServiceImpl());
    }

    public WarehouseZoneController(WarehouseZoneService zoneService) {
        this.zoneService = zoneService;
    }

    public List<WarehouseZone> findAll() {
        return zoneService.findAll();
    }

    public List<WarehouseZone> search(String keyword) {
        return zoneService.search(keyword);
    }

    public WarehouseZone create(WarehouseZone zone) {
        validate(zone);
        return zoneService.create(zone);
    }

    public WarehouseZone update(WarehouseZone zone) {
        validate(zone);
        return zoneService.update(zone);
    }

    public void delete(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID không hợp lệ");
        }
        zoneService.delete(id);
    }

    private void validate(WarehouseZone z) {
        if (z == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        if (z.getName() == null || z.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên khu vực là bắt buộc");
        }
    }
}
