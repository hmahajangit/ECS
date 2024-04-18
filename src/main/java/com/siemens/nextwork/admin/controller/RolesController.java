package com.siemens.nextwork.admin.controller;

import static com.siemens.nextwork.admin.util.NextworkConstants.AUTHORIZATION_HEADER;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.RequestRoleDTO;
import com.siemens.nextwork.admin.dto.ResponseRoleDTO;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.service.RolesService;
import com.siemens.nextwork.admin.util.CommonUtils;
import com.siemens.nextwork.admin.util.NextworkConstants;

@RestController
@RequestMapping("/api/v1/roles")
public class RolesController {

	private static final Logger LOGGER = LoggerFactory.getLogger(RolesController.class);

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private RolesService roleService;

	@PostMapping
	public ResponseEntity<IdResponseDTO> createRole(@RequestBody RequestRoleDTO requestRoleDto,
			HttpServletRequest request) {
		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		LOGGER.info("Inside Role Controller - Role POST");
		Roles createdRole = roleService.createRole(userEmail, requestRoleDto);
		var responseDTO = new IdResponseDTO();
		responseDTO.setUid(createdRole.getId());
		return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
	}

	@PutMapping(path = "/{roleId}")
	public ResponseEntity<IdResponseDTO> updateRole(
			@PathVariable("roleId") String roleId,
			@RequestParam(required = false, value = "actionGidList") String actionGidList,
			@RequestParam(required = false, value = "actionWSList") String actionWorkStreamList,
			@RequestBody RequestRoleDTO requestRoleDTO) {
		LOGGER.info("Inside Role Controller - Role PUT");
		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		var responseDTO= roleService.updateRoleById(userEmail, roleId, requestRoleDTO, actionGidList, actionWorkStreamList);
		return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
	}


	@GetMapping
	public ResponseEntity<List<ResponseRoleDTO>> getAllRoles() {
		LOGGER.info("Inside Role Controller - Role Get All roles");
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		List<ResponseRoleDTO> rolesDtoList = roleService.getAllRoles(userEmail);
		return new ResponseEntity<>(rolesDtoList, HttpStatus.OK);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ResponseRoleDTO> getRoleById(@PathVariable String id) {
		LOGGER.info("Inside Role Controller - Role Get role by id");
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		ResponseRoleDTO rolesDto = roleService.getRoleById(id,userEmail);
		return new ResponseEntity<>(rolesDto, HttpStatus.OK);
	}
	

	@GetMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<ResponseRoleDTO>> getRoleDataBasedOnSearchQuery(
			@RequestParam(required = true, value = "searchQuery") String searchQuery) {
		LOGGER.info("Get all role data based on search query");
		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		List<ResponseRoleDTO> result =roleService.getRolesBySearchQuery(userEmail,searchQuery);
		return ResponseEntity.status(HttpStatus.OK).body(result);
	}
	
	@PatchMapping
	public List<IdResponseDTO> deleteRoles(@RequestBody List<IdResponseDTO> idResponseDTOList) {
		LOGGER.info("Inside Role Controller - DELETE Roles");
		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		return roleService.deleteRoles(userEmail,idResponseDTOList);
	}
	
}
