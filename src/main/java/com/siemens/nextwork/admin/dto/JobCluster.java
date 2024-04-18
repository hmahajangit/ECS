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
public class JobCluster {
	
	private String uid;
	private String name;
	private Boolean isOriginFuture;
	private List<JobProfile> jobProfile;

}