package com.siemens.nextwork.admin.dto;

import java.util.List;

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
public class ResponseRoleDTO {
	private String id;
	private String roleDisplayName;
	private String roleDescription;
	private String roleType;
	private List<MemberDTO> memberList;
	private List<WorkStreamDTO> workstreamList;
	private boolean haveGIDList;

}
