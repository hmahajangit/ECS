package com.siemens.nextwork.admin.enums;

public enum Business {

	SI("SI"),
	SMO("SMO"),
	DI("DI"),
	FUNCTIONS("Functions"),
	REGIONS("Regions");
	
	public final String value;

    private Business(String value) {
        this.value = value;
    }
}
