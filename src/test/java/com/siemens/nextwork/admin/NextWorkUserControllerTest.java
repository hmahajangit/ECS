
package com.siemens.nextwork.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.nextwork.admin.controller.NextWorkUserController;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserByIdResponseDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserPutRequestDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserResponseDTO;
import com.siemens.nextwork.admin.dto.UsersInfoResponseDTO;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.service.NextWorkUserService;
import com.siemens.nextwork.admin.service.RolesService;
import com.siemens.nextwork.admin.util.DTOUtil;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
 class NextWorkUserControllerTest extends RestTestEnabler {

	@MockBean
	private NextWorkUserService nextWorkUserService;

	@MockBean
	private NextWorkUserRepository nextWorkUserRepository;

	@InjectMocks
	private NextWorkUserController nextWorkUserController;

	@Mock
	private HttpServletRequest request;

	@MockBean
	private RolesService roleService;

	@Mock
	private NextWorkUserService nextWorkUserService1;
	Roles roles;
	NextWorkUser users;

	IdResponseDTO idResponseDTO;
	String id = "id01";
	String actionGidList = "gidInclusion";
	String actionMemberList = "memberInclusion";
	UsersInfoResponseDTO userinfodetails;
	NextWorkUserPutRequestDTO nextWorkUserPutRequestDTO;
	String dummyToken = Jwts.builder().claim("email", "ujjwal.kushwaha@siemens.com").compact();
	String authorization = "bearer " + dummyToken;
	String myToken = Jwts.builder().claim("email", "syam-kumar.vutukuri.ext@siemens.com").compact();
	String myAuthorization = "bearer " + myToken;
	String incorrectDummyToken = Jwts.builder().claim("email", "unknonw@user.com").compact();
	String incorrectAuthorization = "bearer " + incorrectDummyToken;
	String forbiddenDummyToken = Jwts.builder().claim("email", "piyushkumar@siemens.com").compact();
	String forbiddenAuthorization = "bearer " + forbiddenDummyToken;

	@BeforeEach
	public void setup() {
		users = new NextWorkUser();
		users.setId("12");
		users.setName("Dummy");
		userinfodetails = new UsersInfoResponseDTO();
		userinfodetails.setEmailId("rajesh.@siemens.com");
		userinfodetails.setName("Rajesh");
		userinfodetails.setInfo("rajesh");

		roles = new Roles();
		roles.setId("12");
		roles.setRoleDisplayName("Dummy");
		idResponseDTO = new IdResponseDTO();
		idResponseDTO.setUid("12");
		nextWorkUserPutRequestDTO = new NextWorkUserPutRequestDTO();
		when(request.getHeader(any())).thenReturn(authorization);
	}

	@Test
	 void totalUsers() throws Exception {
		List<NextWorkUserResponseDTO> result = new ArrayList<>();
		NextWorkUserResponseDTO response1 = new NextWorkUserResponseDTO();
		response1.setEmail("rajesh");
		response1.setStatus("Active");
		NextWorkUserResponseDTO response2 = new NextWorkUserResponseDTO();
		response2.setEmail("rajesh");
		response2.setStatus("Active");

		result.add(response1);
		result.add(response2);

		when(nextWorkUserService.getAllUsers(anyString(), anyString())).thenReturn(result);
		mockMvc.perform(get("/api/v1/users").header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andReturn();

	}

	@Test
	 void getUserByIdTest() throws Exception {
		NextWorkUserByIdResponseDTO result = new NextWorkUserByIdResponseDTO();
		result.setId("12");
		result.setEmail("ujjwal");
		when(nextWorkUserService.getUserById(anyString(), anyString())).thenReturn(result);
		String id = "12";
		mockMvc.perform(get("/api/v1/users/" + id).header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andReturn();
	}

	@Test
	 void updatetUserStatusAndRoleTest() throws Exception {
		NextWorkUserPutRequestDTO response = new NextWorkUserPutRequestDTO();
		response.setId("12");
		response.setStatus("Active");
		when(nextWorkUserService.updateUserById(anyString(), anyString(),
				ArgumentMatchers.any(NextWorkUserPutRequestDTO.class), anyString(), anyString(), anyString()))
				.thenReturn(idResponseDTO);
		String id = "12";
		mockMvc.perform(get("/api/v1/users/" + id).header("Authorization", authorization)
				.content(new ObjectMapper().writeValueAsString(DTOUtil.getRequestUserDTO()))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andReturn();
	}

	@Test
	 void getUserInfoTest() throws Exception {
		String email = "test@example.com";
		Assertions.assertNotNull(nextWorkUserController.getUserInfo(email));

	}

	@Test
	 void updateUserTest() throws Exception {

		Assertions.assertNotNull(nextWorkUserController.updateUser(id, actionMemberList, actionMemberList, actionGidList,
				nextWorkUserPutRequestDTO));

	}
}
