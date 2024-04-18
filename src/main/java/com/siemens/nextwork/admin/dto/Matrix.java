package com.siemens.nextwork.admin.dto;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Matrix {
	
	private String uid;
	private String name;
	private String status;
	private Integer totalUpskillCount;
	private Integer totalReskillCount;
	private Integer totalMaintainCount;
	private Integer totalHireCount;
	private Integer totalOtherMeasuresCount;
	private Integer totalFutureHCSupply;
	private Integer totalCurrentHCSupply;
	private Integer totalFutureHCDemand;
	private Integer totalDevelopmentPathIdentified;
	private List<MatrixData> matrixData;
	private Boolean isMigrated = false;
	protected String createdBy;
	protected Date createdOn;
	private String updatedBy;
	protected Date updatedOn;



}
