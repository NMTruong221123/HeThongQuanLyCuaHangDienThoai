package com.phonestore.model;

public class AttributeItem {

    private long id;
    private AttributeType type;
    private String name;
    private Integer status;

    public AttributeItem() {
    }

    public AttributeItem(long id, AttributeType type, String name) {
        this.id = id;
        this.type = type;
        this.name = name;
    }

    public AttributeItem(long id, AttributeType type, String name, Integer status) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
