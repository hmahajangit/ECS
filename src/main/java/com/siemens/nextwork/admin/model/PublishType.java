package com.siemens.nextwork.admin.model;

public enum PublishType {
	PUBLISH("Published"),
	UNPUBLISH("Unpublished");
	
	
	 public final String value;

	    private PublishType(String value) {
	        this.value = value;
	    }

}
