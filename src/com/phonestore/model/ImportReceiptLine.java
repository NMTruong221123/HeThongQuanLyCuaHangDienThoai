package com.phonestore.model;

import java.util.List;

public class ImportReceiptLine {

    private Long productId;
    private String productName;

    private Long variantId;
    private String variantLabel;

    private Integer quantity;
    private Long unitPrice; // VND
    private List<String> imeis;

    public ImportReceiptLine() {}

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

    public List<String> getImeis() {
        return imeis;
    }

    public void setImeis(List<String> imeis) {
        this.imeis = imeis;
    }

    public long getLineTotal() {
        long qty = quantity == null ? 0 : Math.max(0, quantity);
        long price = unitPrice == null ? 0 : unitPrice;
        return qty * price;
    }
}
