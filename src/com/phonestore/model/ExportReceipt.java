package com.phonestore.model;

import java.time.LocalDateTime;

public class ExportReceipt {

    private long id; // maphieuxuat
    private LocalDateTime time; // thoigian
    private Double total; // tongtien
    private Long createdBy; // nguoitaophieuxuat
    private String createdByName; // nhanvien.hoten (via taikhoan.manv)
    private Long customerId; // makh
    private String customerName; // khachhang.tenkhachhang
    private Integer status; // trangthai

    // Transient (not persisted): line items used for invoice preview/email.
    private java.util.List<ExportReceiptLine> lines;

    // Transient (not persisted): transfer reference/content used for reconciliation.
    private String paymentRef;

    // Transient (not persisted): business receipt code shown to user/email.
    private String receiptCode;

    public ExportReceipt() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public java.util.List<ExportReceiptLine> getLines() {
        return lines;
    }

    public void setLines(java.util.List<ExportReceiptLine> lines) {
        this.lines = lines;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public void setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
    }
}
