package com.siemens.nextwork.admin.service;

import java.util.List;

import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.RequestRoleDTO;
import com.siemens.nextwork.admin.dto.ResponseRoleDTO;
import com.siemens.nextwork.admin.model.Roles;

public interface RolesService {

	Roles createRole(String userEmail, RequestRoleDTO roleDTO);

	List<ResponseRoleDTO> getAllRoles(String userEmail);

	ResponseRoleDTO getRoleById(String id,String userEmail);

	IdResponseDTO updateRoleById(String userEmail, String roleId, RequestRoleDTO requestRoleDTO,
			String actionGidList,String actionWorkStreamList);
	
	List<ResponseRoleDTO> getRolesBySearchQuery(String userEmail, String searchQuery);

	List<IdResponseDTO> deleteRoles(String userEmail, List<IdResponseDTO> idResponseDTOList);
}
