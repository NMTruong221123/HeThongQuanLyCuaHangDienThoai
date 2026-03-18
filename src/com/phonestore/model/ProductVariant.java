package com.phonestore.model;

public class ProductVariant {
    // Table: phienbansanpham
    private long id; // maphienbansp
    private Integer productId; // masp

    private Integer romId; // rom -> dungluongrom.madlrom
    private String romName; // dungluongrom.kichthuocrom

    private Integer ramId; // ram -> dungluongram.madlram
    private String ramName; // dungluongram.kichthuocram

    private Integer colorId; // mausac -> mausac.mamau
    private String colorName; // mausac.tenmau

    private Integer importPrice; // gianhap
    private Integer exportPrice; // giaxuat

    private Integer stock; // soluongton
    private Integer status; // trangthai

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getRomId() {
        return romId;
    }

    public void setRomId(Integer romId) {
        this.romId = romId;
    }

    public String getRomName() {
        return romName;
    }

    public void setRomName(String romName) {
        this.romName = romName;
    }

    public Integer getRamId() {
        return ramId;
    }

    public void setRamId(Integer ramId) {
        this.ramId = ramId;
    }

    public String getRamName() {
        return ramName;
    }

    public void setRamName(String ramName) {
        this.ramName = ramName;
    }

    public Integer getColorId() {
        return colorId;
    }

    public void setColorId(Integer colorId) {
        this.colorId = colorId;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }

    public Integer getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(Integer importPrice) {
        this.importPrice = importPrice;
    }

    public Integer getExportPrice() {
        return exportPrice;
    }

    public void setExportPrice(Integer exportPrice) {
        this.exportPrice = exportPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
