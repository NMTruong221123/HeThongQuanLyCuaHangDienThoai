package com.phonestore.controller;

import com.phonestore.model.AttributeItem;
import com.phonestore.model.AttributeType;
import com.phonestore.util.service.AttributeService;
import com.phonestore.util.service.impl.AttributeServiceImpl;

import java.util.List;

public class AttributeController {

    private final AttributeService attributeService;

    public AttributeController() {
        this(new AttributeServiceImpl());
    }

    public AttributeController(AttributeService attributeService) {
        this.attributeService = attributeService;
    }

    public List<AttributeItem> findByType(AttributeType type) {
        if (type == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại thuộc tính");
        }
        return attributeService.findByType(type);
    }

    public AttributeItem create(AttributeType type, String name) {
        validate(type, name);
        return attributeService.create(type, name.trim());
    }

    public AttributeItem update(long id, AttributeType type, String name) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID không hợp lệ");
        }
        validate(type, name);
        return attributeService.update(id, type, name.trim());
    }

    public void delete(long id, AttributeType type) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID không hợp lệ");
        }
        if (type == null) {
            throw new IllegalArgumentException("Loại thuộc tính không hợp lệ");
        }
        attributeService.delete(id, type);
    }

    private void validate(AttributeType type, String name) {
        if (type == null) {
            throw new IllegalArgumentException("Loại thuộc tính không hợp lệ");
        }
        String n = name == null ? "" : name.trim();
        if (n.isBlank()) {
            throw new IllegalArgumentException("Tên thuộc tính là bắt buộc");
        }
    }
}
