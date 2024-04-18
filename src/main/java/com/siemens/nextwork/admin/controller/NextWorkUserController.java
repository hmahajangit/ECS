package com.siemens.nextwork.admin.controller;

import static com.siemens.nextwork.admin.util.NextworkConstants.AUTHORIZATION_HEADER;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserByIdResponseDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserPutRequestDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserResponseDTO;
import com.siemens.nextwork.admin.dto.UsersInfoResponseDTO;
import com.siemens.nextwork.admin.service.NextWorkUserService;
import com.siemens.nextwork.admin.util.CommonUtils;
import com.siemens.nextwork.admin.util.NextworkConstants;

@RestController
@RequestMapping("/api/v1")
public class NextWorkUserController {

	private static final Logger LOGGER = LoggerFactory.getLogger(NextWorkUserController.class);

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private NextWorkUserService nextWorkUserService;


	@GetMapping("/users")
	public ResponseEntity<List<NextWorkUserResponseDTO>> getUserDetails(
			@RequestParam(value = "tabType", required = false) String tabType
			) {
		LOGGER.info("Inside NextWork User Controller - GET");
		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		List<NextWorkUserResponseDTO> result = nextWorkUserService.getAllUsers(userEmail,tabType);
		return ResponseEntity.status(HttpStatus.OK).body(result);
	}

	
	
	@GetMapping("/users/{id}")
	public ResponseEntity<NextWorkUserByIdResponseDTO> getUserById(@PathVariable String id) {
		LOGGER.info("Inside NextWork User Controller - GET by ID:");
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		NextWorkUserByIdResponseDTO usersDto = nextWorkUserService.getUserById(id,userEmail);
		return new ResponseEntity<>(usersDto, HttpStatus.OK);
	}
	
	@GetMapping("/userInfo")
	public ResponseEntity<UsersInfoResponseDTO> getUserInfo(
			@RequestParam(value = "userEmail", required = false) String userEmailToSearch
	) {
		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		LOGGER.info("Inside NextWork User Controller - USER INFORMATION");
		UsersInfoResponseDTO result = nextWorkUserService.getUserInfo(userEmail, request, userEmailToSearch);
		return ResponseEntity.status(HttpStatus.OK).body(result);
	}
	
	@PutMapping(path = "/users/{id}")
	public ResponseEntity<IdResponseDTO> updateUser(
			@PathVariable("id") String userId,
			@RequestParam(required = false, value = "actionGidList") String actionGidList,
			@RequestParam(required = false, value = "actionWSList") String actionMemberList,
			@RequestParam(required = false, value = "actionRoleList") String actionRoleList,
			@RequestBody NextWorkUserPutRequestDTO nextWorkUserPutRequestDTO) {
		LOGGER.info("Inside NextWork User Controller - PUT");
		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		var responseDTO= nextWorkUserService.updateUserById(userEmail, userId, nextWorkUserPutRequestDTO, actionGidList,
				actionMemberList,actionRoleList);
		return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
	}
	
	
	
}
