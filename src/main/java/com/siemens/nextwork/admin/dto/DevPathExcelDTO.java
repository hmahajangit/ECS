package com.siemens.nextwork.admin.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Schema(
		name = "DevPathExcelDTO",
		description = "Data object to generate excel",
		oneOf = DevelopmentPathResponseDTO.class)
public class DevPathExcelDTO {

	private String statusQuoJobProfile;

	private String futureStateJobProfile;
	
	private String gripCodeStatusQuoJobProfile;
	
	private String gripCodeFutureStateJobProfile;

	private String measure;

	private Integer assignedHC;
	
	private List<String> trainings;
}

