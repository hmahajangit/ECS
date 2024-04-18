package com.siemens.nextwork.admin.dto;

import java.util.List;

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
@Schema(name = "RequestRoleDTO", description = "Data object for Role Controller Request", oneOf = RequestRoleDTO.class)
public class RequestRoleDTO {
	
	private String roleDisplayName;
	
	private String roleDescription;

	private String roleType;

	private List<GidsRequestDTO> gids;
	
	private List<MemberDTO> memberList;
	
	private List<WorkStreamDTO> workstreamList;
}
