package com.siemens.nextwork.admin.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "JobProfilesMeasureDTO", description = "Measure for Job profile", oneOf = JobProfilesMeasureDTO.class)
public class JobProfilesMeasureDTO {

	private String stausQuoJobProfileName;
	private String stausQuoJobProfileId;
	private String futureStateJobProfileName;
	private String futureStateJobProfileId;
	private String devPath;
	private String measureLevel1;
	private String measureLevel2;
	private Map<String, Integer> measure2NameAssignedHC = new HashMap<>();
	private Integer measure2AssignedHC;
}
