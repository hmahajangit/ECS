package com.siemens.nextwork.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillAssignment {
	
	private String jobProfileId;
	private String migratedJobProfileId;
	private String skillAssignmentId;
	private String migratedSkillId;
	private String skillId;
	private String statusQuoJcId;
	private String statusQuoJcName;
	private String futureStateJcId;
	private String futureStateJcName;
	private String skillName;
	private String currentSkillLevel;
	private String futureSkillLevel;
	private String skillCategory;


}