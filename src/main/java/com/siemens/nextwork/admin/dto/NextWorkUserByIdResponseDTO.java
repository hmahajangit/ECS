package com.siemens.nextwork.admin.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NextWorkUserByIdResponseDTO {
	private String id;
	private String name;
	private String email;
	private String orgCode;
	private String creationDate;
	private List<RoleDetailDTO> rolesDetails;
	private String status;
	private boolean haveGIDList;
	private List<WorkStreamDTO> workstreamList;

}
