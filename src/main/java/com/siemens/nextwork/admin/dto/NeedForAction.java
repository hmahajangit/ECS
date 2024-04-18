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
public class NeedForAction {
	
	private String jobProfileId;
	private String jobProfileName;
	private Integer futureHCDemand;
	private Integer futureHCSupply;
	private Integer currentHCSupply;
	private List<Skills> skills;
    private Boolean isOriginFuture;
    private Boolean isTwin;

}