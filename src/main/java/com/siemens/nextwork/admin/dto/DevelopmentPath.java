package com.siemens.nextwork.admin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DevelopmentPath {
	
	private String devPathId;
	private String matrixId;
	
	private String linkId;
	
	private String statusQuoJobProfileId;

	private String statusQuoJobProfileName;

	private String futureStateJobProfileId;

	private String futureStateJobProfileName;
	
	private Boolean futureStateJobProfileIsTwin;

	private String measure;

	private String status;

	private Integer assignedHC;
	
	private Integer headcountEffected;

	private List<Trainings> trainings;

}