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
public class TrendsAndBizOutlook {
	
	@Id
	private String uid;	
	private String impactType;
	private String impactCategory;
	private String impactSubCategory;
	private String description;
	private Boolean isDeleted =false;
	private List<String> tags;
	private List<ImpactedJobProfiles> impactedJobProfiles;

}