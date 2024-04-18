package com.siemens.nextwork.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ReportImpactsDTO", description = "Impacts and Job profile association", oneOf = ReportTrendImpactDTO.class)
public class ReportTrendImpactDTO {

	private String impactName;
	private String impactType;
	private String impactCategory;
	private String jobProfileId;
	private String impactOnHC;
	private String impactOnSkills;
}
