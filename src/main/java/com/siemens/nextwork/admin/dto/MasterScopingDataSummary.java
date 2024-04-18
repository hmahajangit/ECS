package com.siemens.nextwork.admin.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "MasterScopingDataSummary", description = "Data object for MasterScopingDataSummary Response", oneOf = MasterScopingDataSummary.class)
public class MasterScopingDataSummary {
	
	private String vid;
	private Integer recCount;
	private List<SummaryData> summaryData;
	
	

}
