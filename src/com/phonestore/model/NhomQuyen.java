package com.phonestore.model;

public class NhomQuyen {

    private Integer id; // manhomquyen
    private String name; // ten
    private Integer status; // trangthai

    public NhomQuyen() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
