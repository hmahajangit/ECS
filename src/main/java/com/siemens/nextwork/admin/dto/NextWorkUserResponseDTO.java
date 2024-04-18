package com.siemens.nextwork.admin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextWorkUserResponseDTO {
	private String id;
	private String name;
	private String email;
	private String orgCode;
	private String creationDate;
	private List<RoleDetailDTO> rolesDetails;
	private String status;
	private String purpose;
}