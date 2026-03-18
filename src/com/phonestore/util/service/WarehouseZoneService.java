package com.phonestore.util.service;

import com.phonestore.model.WarehouseZone;

import java.util.List;

public interface WarehouseZoneService {
    List<WarehouseZone> findAll();

    List<WarehouseZone> search(String keyword);

    WarehouseZone create(WarehouseZone zone);

    WarehouseZone update(WarehouseZone zone);

    void delete(long id);
}
