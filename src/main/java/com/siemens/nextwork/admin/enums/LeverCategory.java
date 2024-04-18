package com.siemens.nextwork.admin.enums;

public enum LeverCategory {
    SUPPLY("Supply"),
    DEMAND("Demand");

    public final String value;

    private LeverCategory(String value) {
        this.value = value;
    }

}
