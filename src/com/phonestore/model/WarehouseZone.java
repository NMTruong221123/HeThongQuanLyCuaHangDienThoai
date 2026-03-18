package com.phonestore.model;

public class WarehouseZone {

    // Table: khuvuckho
    private long id; // makhuvuc
    private String name; // tenkhuvuc
    private String note; // ghichu
    private Integer status; // trangthai

    public WarehouseZone() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
