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
public class UsersInfoResponseDTO {

	private String uid;
	private String name;
	private String emailId;
	private String status;
	private String info;
	private String role;
	private List<RoleDetailDTO> rolesDetails;
	private Boolean haveMigratedWs;

}