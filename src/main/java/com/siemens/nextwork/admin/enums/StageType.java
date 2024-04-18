package com.siemens.nextwork.admin.enums;

public enum StageType {
	TEST("Test"),
	INITIAL_ANALYSIS("Initial Analysis"),
	IMPLEMENTATION("Implementation & Continuous Review"),
	MIGRATED("Migrated Workstream");
	
	public final String value;

    private StageType(String value) {
        this.value = value;
    }

}
