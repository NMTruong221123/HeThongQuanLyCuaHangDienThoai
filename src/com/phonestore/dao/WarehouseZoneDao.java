package com.phonestore.dao;

import com.phonestore.model.WarehouseZone;

import java.util.List;

public interface WarehouseZoneDao {
    List<WarehouseZone> findAll();

    List<WarehouseZone> search(String keyword);

    WarehouseZone create(WarehouseZone zone);

    WarehouseZone update(WarehouseZone zone);

    void delete(long id);
}
