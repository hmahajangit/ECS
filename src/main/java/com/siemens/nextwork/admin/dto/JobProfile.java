package com.siemens.nextwork.admin.dto;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document
public class JobProfile {

	
	@Id
	private String uid;
	private String migratedOldUid;
	private Boolean isDeleted = false;
	private String name;
	private String description;
	private String gripName;
	private List<String> gripPositionType;
	private List<String> gripPostion;
	private List<String> gripCodeCatalog;
	private List<GripTaskDTO> gripTask;
	private Integer currentHeadCountSupply;
	private Integer futureHeadCountSupply;
	private Integer futureHeadCountDemand;
	private Integer assignedFutureHCDemand;
	private Integer assignedFutureHCSupply;
	private ValueStream valueStream;
	private String valueStreamDescription;
	private String cluster;
	private Boolean isOriginFuture;
	private Boolean isTwin;
	private List<String> jobFamily;
	private List<String> gidList;
	private String gidMappingStatus;
	private List<SkillAssignment> skillAssignments;
	private String statusQuoJcId;
	private String statusQuoJcName;
	private String futureStateJcId;
	private String futureStateJcName;
	private List<String> tags;
	private Boolean isMigrated = false;
}