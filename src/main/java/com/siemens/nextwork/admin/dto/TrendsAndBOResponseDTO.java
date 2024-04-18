package com.siemens.nextwork.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@EqualsAndHashCode()
@Schema(name = "TrendsAndBOResponseDTO", description = "Data object for trend and BO Response", oneOf = TrendsAndBOResponseDTO.class)
@Getter
@Setter
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class TrendsAndBOResponseDTO {

	private String id;

	private String name;

	private Integer count;

}
