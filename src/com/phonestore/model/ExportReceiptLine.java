package com.phonestore.model;

public class ExportReceiptLine {

    private Long productId;
    private String productName;

    private Long variantId;
    private String variantLabel;

    private Integer quantity;
    private Long unitPrice; // VND

    // Transient (not persisted): IMEI list for exported devices
    private java.util.List<String> imeis;

    public ExportReceiptLine() {}

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getVariantLabel() {
        return variantLabel;
    }

    public void setVariantLabel(String variantLabel) {
        this.variantLabel = variantLabel;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public java.util.List<String> getImeis() {
        return imeis;
    }

    public void setImeis(java.util.List<String> imeis) {
        this.imeis = imeis;
    }

    public long getLineTotal() {
        long qty = quantity == null ? 0 : Math.max(0, quantity);
        long price = unitPrice == null ? 0 : unitPrice;
        return qty * price;
    }
}
