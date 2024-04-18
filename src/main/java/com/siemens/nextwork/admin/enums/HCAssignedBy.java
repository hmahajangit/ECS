package com.siemens.nextwork.admin.enums;

public enum HCAssignedBy {
    NONE("None"),
    JOBPROFILE("JobProfile"),
    LEVERS("Levers"),
    YEARS("Years"),
    JOBPROFILE_LEVERS("JobProfile Levers"),
    JOBPROFILE_YEARS("JobProfile Years"),
    LEVERS_YEARS("Levers Years"),
    JOBPROFILE_LEVER_YEAR("JobProfile Lever Year");

    public final String value;

    private HCAssignedBy(String value) {
        this.value = value;
    }
}
