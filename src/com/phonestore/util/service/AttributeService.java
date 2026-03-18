package com.phonestore.util.service;

import com.phonestore.model.AttributeItem;
import com.phonestore.model.AttributeType;

import java.util.List;

public interface AttributeService {

    List<AttributeItem> findByType(AttributeType type);

    AttributeItem create(AttributeType type, String name);

    AttributeItem update(long id, AttributeType type, String name);

    void delete(long id, AttributeType type);
}
