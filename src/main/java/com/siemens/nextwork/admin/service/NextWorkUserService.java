package com.siemens.nextwork.admin.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserByIdResponseDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserPutRequestDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserResponseDTO;
import com.siemens.nextwork.admin.dto.UsersInfoResponseDTO;

public interface NextWorkUserService {

	List<NextWorkUserResponseDTO> getAllUsers(String userEmail, String tabType);

	NextWorkUserByIdResponseDTO getUserById(String id, String userEmail);

	UsersInfoResponseDTO getUserInfo(String userEmail, HttpServletRequest request, String userEmailToSearch);

	IdResponseDTO updateUserById(String userEmail, String userId, NextWorkUserPutRequestDTO workstreamRequestDTO,
			String actionGidList, String actionMemberList, String actionRoleList);

	String checkUserRole(String userId);


}