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
@Schema(name = "RequestAccessDTO", description = "Data object for PreOnboardedUsers Controller Request", oneOf = RequestAccessDTO.class)
public class RequestAccessDTO {

	private String email;

	private String purpose;

}
