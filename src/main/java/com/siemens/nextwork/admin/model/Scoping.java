package com.siemens.nextwork.admin.model;



import java.util.Date;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@With
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection="master_scoping_data")
public class Scoping {
	
	@Id
	private String id;
	
	@NotNull(message = "gid cannot be null")
	@Size(min=8, max=8, message = "gid must be 8 characters")
	private String gid;
	@Size(min=8, max=8, message = "vid must be 8 characters")
	private String vid;
	private String vGid;
	private Date uploadTime;
	private Date modifyTime;
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
	private String actionType;
	private String nullFields;
	private Boolean isCaptured;
	
	
}