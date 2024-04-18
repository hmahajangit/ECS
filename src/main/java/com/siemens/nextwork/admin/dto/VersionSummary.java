package com.siemens.nextwork.admin.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class VersionSummary {
	private Map<String,Integer> addedFields;
	private Map<String,Integer> modifiedFields;
	private Map<String,Integer> removedFields;
	
}
