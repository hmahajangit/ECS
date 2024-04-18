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
@Schema(name = "RequestAccessResponseDTO", description = "Data object for AccessRequest's Responses", oneOf = RequestAccessResponseDTO.class)

public class RequestAccessResponseDTO {
	
	private String uid;

	private String email;

	private String purpose;

}
