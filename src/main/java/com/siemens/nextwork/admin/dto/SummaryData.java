package com.siemens.nextwork.admin.dto;

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
@Schema(name = "SummaryData", description = "Data object for SummaryData Response", oneOf = SummaryData.class)
public class SummaryData {
	private String fieldName;
	private Integer fieldCount;
	private String filedAction;
}
