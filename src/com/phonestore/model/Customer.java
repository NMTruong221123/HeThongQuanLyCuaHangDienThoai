package com.phonestore.model;

public class Customer {

    private long id; // makh
    private String name; // tenkhachhang
    private String address; // diachi
    private String email; // email (optional depending on DB schema)
    private String phone; // sdt
    private Integer status; // trangthai (0/1)

    public Customer() {}

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        String n = name == null ? "" : name.trim();
        String p = phone == null ? "" : phone.trim();
        if (!n.isBlank() && !p.isBlank()) return n + " - " + p;
        if (!n.isBlank()) return n;
        if (!p.isBlank()) return p;
        return "Khach hang";
    }
}
