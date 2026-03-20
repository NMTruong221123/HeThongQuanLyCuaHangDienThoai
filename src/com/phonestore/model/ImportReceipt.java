package com.phonestore.model;

import java.time.LocalDateTime;
import java.util.List;

public class ImportReceipt {

    private long id; // maphieunhap
    private LocalDateTime time; // thoigian
    private Long supplierId; // manhacungcap
    private String supplierName; // nhacungcap.tennhacungcap
    private Long createdBy; // nguoitao
    private String createdByName; // nhanvien.hoten (via taikhoan.manv)
    private Double total; // tongtien
    private Integer status; // trangthai

    // Transient (best-effort persisted if caller provides lines)
    private List<ImportReceiptLine> lines;

    public ImportReceipt() {}

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

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
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

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<ImportReceiptLine> getLines() {
        return lines;
    }

    public void setLines(List<ImportReceiptLine> lines) {
        this.lines = lines;
    }
}
