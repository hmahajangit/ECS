package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.nextwork.admin.dto.RequestAccessDTO;
import com.siemens.nextwork.admin.enums.StatusType;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
 class RequestAccessControllerTest extends RestTestEnabler {

	String email = "natasha.patro@siemens.com";
	String dummyToken = Jwts.builder().claim("email", email).compact();
	String authorization = "bearer " + dummyToken;
	Optional<NextWorkUser> activeUser;
	Optional<NextWorkUser> pendingUser;
	Optional<NextWorkUser> rejectedUser;
	Optional<NextWorkUser> newUser;
	Optional<NextWorkUser> deActiveUser;

	NextWorkUser nxtUser;
	NextWorkUser pUser;
	NextWorkUser rUser;
	NextWorkUser nUser;
	NextWorkUser dUser;

	RequestAccessDTO requestAccessDTO;

	@MockBean
	NextWorkUserRepository nextWorkUserRepository;

	@BeforeEach
	public void setup() {
		requestAccessDTO = RequestAccessDTO.builder().email(email).purpose("To access the app").build();
		List<Roles> roles = new ArrayList<>();
		Roles role = Roles.builder().id("64a52b534febce0ce703981b").roleDisplayName("Admin").roleType("ADMIN")
				.haveGIDList(false).isDeleted(false).build();
		roles.add(role);
		nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a3960").name("SYAM KUMAR VUTUKURI")
				.email("syam-kumar.vutukuri.ext@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.creationDate("2022-12-12 11:17:36.279+00").status(StatusType.ACTIVE.toString()).rolesDetails(roles)
				.build();
		activeUser = Optional.of(nxtUser);
		pUser = new NextWorkUser();
		BeanUtils.copyProperties(nxtUser, pUser);
		pUser.setStatus(StatusType.PENDING.toString());
		pendingUser = Optional.of(pUser);

		rUser = new NextWorkUser();
		BeanUtils.copyProperties(nxtUser, rUser);
		rUser.setStatus(StatusType.REJECTED.toString());
		rejectedUser = Optional.of(rUser);

		nUser = new NextWorkUser();
		BeanUtils.copyProperties(nxtUser, nUser);
		nUser.setStatus("Intial");
		newUser = Optional.of(nUser);

		dUser = new NextWorkUser();
		BeanUtils.copyProperties(nxtUser, dUser);
		dUser.setStatus("Deactive");
		deActiveUser = Optional.of(dUser);

	}

	@Test
	 void acccessRequestForActive() throws Exception {
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(activeUser);
		mockMvc.perform(post("/api/v1/accessRequest").header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(requestAccessDTO)).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	 void acccessRequestForPending() throws Exception {
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(pendingUser);

		mockMvc.perform(post("/api/v1/accessRequest").header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(requestAccessDTO)).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	 void acccessRequestForRejected() throws Exception {
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(rejectedUser);

		mockMvc.perform(post("/api/v1/accessRequest").header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(requestAccessDTO)).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());
	}

	@Test
	 void acccessRequestForNew() throws Exception {
		mockMvc.perform(post("/api/v1/accessRequest").header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(requestAccessDTO)).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());
	}

	@Test
	 void acccessRequestForDeActive() throws Exception {
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(deActiveUser);

		mockMvc.perform(post("/api/v1/accessRequest").header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(requestAccessDTO)).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	 void acccessRequestForUserWithInvalidStatus() throws Exception {
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(newUser);

		mockMvc.perform(post("/api/v1/accessRequest").header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(requestAccessDTO)).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

}
