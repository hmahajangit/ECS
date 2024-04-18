package com.siemens.nextwork.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImpactedJobProfiles {
	
	private String trendAndBOId;
	private String jobProfileId;
	private String jobProfileName;
	private String jobClusterId;
	private String jobClusterName;
	private String impactOnHCDemand;
	private String impactOnSkill;

}
