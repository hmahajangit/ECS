package com.siemens.nextwork.admin.dto;

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
		name = "DevelopmentPathResponseDTO",
		description = "Data object to view all Development Paths",
		oneOf = DevelopmentPathResponseDTO.class)
public class DevelopmentPathResponseDTO {

	private String devPathId;

	private String statusQuoJobProfileId;

	private String statusQuoJobProfileName;

	private String futureStateJobProfileId;

	private String futureStateJobProfileName;

	private String measure;

	private String status;

	private Integer assignedHC;
}
