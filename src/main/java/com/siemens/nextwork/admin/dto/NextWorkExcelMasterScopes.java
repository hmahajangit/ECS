package com.siemens.nextwork.admin.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextWorkExcelMasterScopes {

	private String id;
	private String vid;
	private String vGid;
	private Date uploadTime;
	private Date modifyTime;
	private String gid;
	private Boolean isVisible;
	private String emailId;
	private String contractStatus;
	private String are;
	private String areDesc;
	private String company;
	private String companyDesc;
	private String divisionExternal;
	private String divisionExternalDesc;
	private String divisionInternal;
	private String divisionInternalDesc;
	private String businessUnit;
	private String depthStructureKey;
	private String depthStructureKeyDesc;
	private String orgCodePA;
	private String organizationClass;
	private String unit;
	private String jobFamilyCategory;
	private String jobFamily;
	private String subJobFamily;	
	private String positionType;
	private String gripPosition;
	private String gripPositionDesc;
	private String regionalOrganization;
	private String regionalOrganizationDesc;
	private String countryRegionARE;
	private String countryRegionPlaceOfAction;
	private String countryRegionStateOffice;
	private String locationOfficeCity;
	private String locationOffice;	
	private String countryRegionSubEntity;
	private String blueCollarWhiteCollar;
	
}
