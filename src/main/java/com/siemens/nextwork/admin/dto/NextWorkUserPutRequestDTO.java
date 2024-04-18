package com.siemens.nextwork.admin.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NextWorkUserPutRequestDTO {
	private String Id;
	private String status;
	private List<GidsRequestDTO> gids;
	private List<WorkStreamDTO> workstreamList;
	private List<RoleDetailDTO> rolesDetails;
	private String scopingVersion;
}
