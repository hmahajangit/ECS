package com.siemens.nextwork.admin.dto;

import com.siemens.nextwork.admin.model.Roles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDetailDTO {
	private String roleId;
	private String roleDisplayName;
	private String roleType;

    public static RoleDetailDTO fromDomain(Roles role) {
        return RoleDetailDTO.builder()
                .roleId(role.getId())
                .roleDisplayName(role.getRoleDisplayName())
                .roleType(role.getRoleType())
                .build();
    }

}