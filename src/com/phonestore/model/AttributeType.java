 
package com.phonestore.model;

public enum AttributeType {
    BRAND("H\u00e3ng"),
    OPERATING_SYSTEM("H\u1ec7 \u0111i\u1ec1u h\u00e0nh"),
    ORIGIN("Xu\u1ea5t x\u1ee9"),
    RAM("RAM"),
    ROM("ROM"),
    COLOR("M\u00e0u s\u1eafc");

    private final String label;

    AttributeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
