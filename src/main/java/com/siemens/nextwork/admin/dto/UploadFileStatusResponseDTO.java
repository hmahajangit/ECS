package com.siemens.nextwork.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

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
@JsonInclude(Include.NON_NULL)
public class UploadFileStatusResponseDTO {
	
	private String taskid;
	private String username;
	private String originalfileName;
	private String status;
	private String date;
	private String type;
	private String errors;
	private Boolean isSummary;
	private String action;

}
