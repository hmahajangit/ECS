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
@Schema(name = "IdResponseDTO", description = "Data object for Project Controller Request", oneOf = IdResponseDTO.class)
public class IdResponseDTO {

	private String uid;

}
