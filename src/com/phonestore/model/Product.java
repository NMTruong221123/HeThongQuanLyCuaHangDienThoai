package com.phonestore.model;

public class Product {

    // Table: sanpham
    private long id; // masp
    private String name; // tensp
    private String imagePath; // hinhanh
    private Integer originId; // xuatxu (FK)
    private String originName; // xuatxu.tenxuatxu

    private String chipProcessor; // chipxuly
    private Integer batteryCapacity; // dungluongpin
    private Double screenSize; // kichthuocman
    private Integer operatingSystemId; // hedieuhanh (FK)
    private String operatingSystemName; // hedieuhanh.tenhedieuhanh
    private Integer osVersion; // phienbanhdh
    private String rearCamera; // camerasau
    private String frontCamera; // cameratruoc
    private Integer warrantyMonths; // thoigianbaohanh

    private Integer brandId; // thuonghieu (FK)
    private String brandName; // thuonghieu.tenthuonghieu
    private Integer zoneId; // khuvuckho (FK)
    private String zoneName; // khuvuckho.tenkhuvuc
    private Integer stock; // soluongton
    private Integer status; // trangthai

    public Product() {}

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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Integer getOriginId() {
        return originId;
    }

    public void setOriginId(Integer originId) {
        this.originId = originId;
    }

    public String getOriginName() {
        return originName;
    }

    public void setOriginName(String originName) {
        this.originName = originName;
    }

    public String getChipProcessor() {
        return chipProcessor;
    }

    public void setChipProcessor(String chipProcessor) {
        this.chipProcessor = chipProcessor;
    }

    public Integer getBatteryCapacity() {
        return batteryCapacity;
    }

    public void setBatteryCapacity(Integer batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }

    public Double getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(Double screenSize) {
        this.screenSize = screenSize;
    }

    public Integer getOperatingSystemId() {
        return operatingSystemId;
    }

    public void setOperatingSystemId(Integer operatingSystemId) {
        this.operatingSystemId = operatingSystemId;
    }

    public String getOperatingSystemName() {
        return operatingSystemName;
    }

    public void setOperatingSystemName(String operatingSystemName) {
        this.operatingSystemName = operatingSystemName;
    }

    public Integer getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(Integer osVersion) {
        this.osVersion = osVersion;
    }

    public String getRearCamera() {
        return rearCamera;
    }

    public void setRearCamera(String rearCamera) {
        this.rearCamera = rearCamera;
    }

    public String getFrontCamera() {
        return frontCamera;
    }

    public void setFrontCamera(String frontCamera) {
        this.frontCamera = frontCamera;
    }

    public Integer getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(Integer warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    public Integer getBrandId() {
        return brandId;
    }

    public void setBrandId(Integer brandId) {
        this.brandId = brandId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public Integer getZoneId() {
        return zoneId;
    }

    public void setZoneId(Integer zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
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
