package com.siemens.nextwork.admin;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import com.siemens.nextwork.admin.controller.RolesController;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.RequestRoleDTO;
import com.siemens.nextwork.admin.dto.ResponseRoleDTO;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.repo.RolesRepository;
import com.siemens.nextwork.admin.service.RolesService;
import com.siemens.nextwork.admin.service.UserService;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
 class RolesControllerTest extends RestTestEnabler {

	@MockBean
	private RolesService roleService;

	@Mock
	private RolesService roleService1;

	@MockBean
	private RolesRepository rolesRepository;

	@Mock
	private UserService userService;

	@InjectMocks
	private RolesController rolesController;

	@Mock
	private HttpServletRequest request;
	Roles roles;


	IdResponseDTO idResponseDTO;

	String dummyToken = Jwts.builder().claim("email", "ujjwal.kushwaha@siemens.com").compact();
	String authorization = "bearer " + dummyToken;
	String myToken = Jwts.builder().claim("email", "syam-kumar.vutukuri.ext@siemens.com").compact();
	String myAuthorization = "bearer " + myToken;
	String incorrectDummyToken = Jwts.builder().claim("email", "unknonw@user.com").compact();
	String incorrectAuthorization = "bearer " + incorrectDummyToken;
	String forbiddenDummyToken = Jwts.builder().claim("email", "piyushkumar@siemens.com").compact();
	String forbiddenAuthorization = "bearer " + forbiddenDummyToken;

	RequestRoleDTO requestRoleDTO;
	String id = "id01";
	String actionGidList = "gidInclusion";
	String actionMemberList = "memberInclusion";
	List<IdResponseDTO> idResponseDTOList = new ArrayList<>();
	
	@BeforeEach
	public void setup() {

		roles = new Roles();
		roles.setId("12");
		roles.setRoleDisplayName("Dummy");
		idResponseDTO = new IdResponseDTO();
		idResponseDTO.setUid("12");
		idResponseDTOList.add(idResponseDTO);
		requestRoleDTO = new RequestRoleDTO();
		
		when(request.getHeader(Mockito.any())).thenReturn(authorization);

	}

	@Test
	 void testtotalgetroles() throws Exception {
		List<ResponseRoleDTO> result = new ArrayList<>();
		ResponseRoleDTO responseRoleDTO1 = new ResponseRoleDTO();
		responseRoleDTO1.setRoleDisplayName("hello1");
		responseRoleDTO1.setRoleDescription("hellodescription1");
		ResponseRoleDTO responseRoleDTO2 = new ResponseRoleDTO();
		responseRoleDTO2.setRoleDisplayName("hello2");
		responseRoleDTO2.setRoleDescription("hellodescription2");
		result.add(responseRoleDTO1);
		result.add(responseRoleDTO2);
		when(roleService.getAllRoles(anyString())).thenReturn(result);
		mockMvc.perform(get("/api/v1/roles").header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andReturn();

	}

	@Test
	 void testgetRoles() throws Exception {
		String roleId = "role";
		ResponseRoleDTO responseRoleDTO = new ResponseRoleDTO();
		responseRoleDTO.setRoleDescription(roleId);
		responseRoleDTO.setRoleDisplayName("ujwal");

		when(roleService.getRoleById(anyString(), anyString())).thenReturn(responseRoleDTO);

		mockMvc.perform(get("/api/v1/roles").header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andReturn();

	}
	
	@Test
	 void patchRequestTest() throws Exception {
		Assertions.assertNotNull(rolesController.deleteRoles(idResponseDTOList));

	}
	
	@Test
	 void testgetRoleById() throws Exception {

		Assertions.assertNotNull(rolesController.getRoleById(id));

	}
	
	@Test
	 void updateRoleByIdTest() throws Exception {

		Assertions.assertNotNull(
				rolesController.updateRole(id, actionMemberList, actionGidList, requestRoleDTO));

	}

	@Test
	 void createRoleTest() throws Exception {

		Assertions.assertThrows(NullPointerException.class, () -> rolesController.createRole(requestRoleDTO, request));

	}

	@Test
	 void getRoleDataBasedOnSearchQueryTest() throws Exception {
		String searchQuery = "im";
		Assertions.assertNotNull(rolesController.getRoleDataBasedOnSearchQuery(searchQuery));

	}

}