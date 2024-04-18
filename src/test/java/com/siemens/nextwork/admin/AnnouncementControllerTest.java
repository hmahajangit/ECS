package com.siemens.nextwork.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.BindingResult;

import com.siemens.nextwork.admin.controller.AnnouncementController;
import com.siemens.nextwork.admin.dto.AnnouncementDTO;
import com.siemens.nextwork.admin.service.AnnouncementService;
import com.siemens.nextwork.admin.validator.AnnouncementValidator;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
@SpringBootTest
 class AnnouncementControllerTest extends RestTestEnabler {

	@Mock
	private HttpServletRequest request;
	
	@Mock
	private AnnouncementService announcementService;
	
	@Mock
	private AnnouncementValidator announcementValidator;

	@InjectMocks
	private AnnouncementController announcementController;

	String userEmail = "abc@siemens.com";
	private static final String dummyToken = Jwts.builder().claim("email", "abc@siemens.com").compact();
	private static final String authorization = "bearer " + dummyToken;
	AnnouncementDTO announcementDTO;

	@BeforeEach
	public void setup() {
		announcementDTO = AnnouncementDTO.builder().uid("").title("").description("").uid("").build();
		when(request.getHeader(Mockito.any())).thenReturn(authorization);
	}

	@Test
	 void getAllAnnouncementsTest() throws Exception {
		Assertions.assertNotNull(announcementController.getAllAnnouncements());

	}
	
	@Test
	 void addAnnouncementTest() throws Exception {
		BindingResult br = mock(BindingResult.class);
		Assertions.assertNotNull(announcementController.addAnnouncement(announcementDTO, br));

	}

}
