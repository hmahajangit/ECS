package com.siemens.nextwork.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
@Getter
@Setter
@Schema(name = "AsyncJobStatusDTO", description = "To provide the job status", oneOf = AsyncJobStatusDTO.class)
public class AsyncJobStatusDTO {

	private String message;
	private AsyncDetailsDTO details;
	
}
