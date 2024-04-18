package com.siemens.nextwork.admin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GidData {
	private String gid;
	private String version;
	private String contractStatus;
	private String are;
	private String areDesc;
	private String businessUnit;
	private String orgCodePA;
	private String jobFamily;
	private String subJobFamily;
	private String countryRegionARE;
	private String countryRegionPlaceOfAction;
	private String countryRegionStateOffice;
	private String locationOfficeCity;
	private String locationOffice;
	private String gripPosition;
	private String gripPositionDesc;
	private String gripPositionType;
	private String blueCollarWhiteCollar;

}
