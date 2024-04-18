package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.siemens.nextwork.admin.dto.AnnouncementDTO;
import com.siemens.nextwork.admin.exception.ResourceNotFoundException;
import com.siemens.nextwork.admin.model.Announcement;
import com.siemens.nextwork.admin.repo.AnnouncementRepository;
import com.siemens.nextwork.admin.service.NextWorkUserService;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.service.impl.AnnouncementServiceImpl;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
@SpringBootTest
 class AnnouncementServiceTest extends RestTestEnabler {

	@Mock
	private AnnouncementRepository announcementRepository;

	@Mock
	private UserService userService;
	@Mock
	private NextWorkUserService nextWorkUserService;

	@InjectMocks
	private AnnouncementServiceImpl announcementServiceImpl;

	String userEmail = "abc@siemens.com";
	String userId = "us001";
	String announcementId = "a001";
	List<Announcement> announcementList;
	Announcement announcement;
	AnnouncementDTO announcementDTO;
	Optional<Announcement> optionalAnnouncement;
	
	@BeforeEach
	public void setup() {
		announcementList = new ArrayList<>();
		announcement = new Announcement();
		announcement.setDeleted(false);
		announcement.setCreatedOn(new Date(System.currentTimeMillis()));
		announcementList.add(announcement);
		announcementDTO = new AnnouncementDTO();
		optionalAnnouncement = Optional.of(announcement);
		when(userService.findByEmail(Mockito.any())).thenReturn(userId);

	}

	@Test
	 void getAllAnnouncementsTest() {
		when(nextWorkUserService.checkUserRole(userId)).thenReturn("ADMIN");
		when(announcementRepository.findAll()).thenReturn(announcementList);
		Assertions.assertNotNull(announcementServiceImpl.getAllAnnouncements(userEmail));

	}
	
	@Test
	 void getAllAnnouncementsModifiedTest() {
		when(nextWorkUserService.checkUserRole(userId)).thenReturn("ADMIN");
		announcementList.get(0).setCreatedOn(null);
		announcementList.get(0).setUpdatedOn(new Date(System.currentTimeMillis()));
		when(announcementRepository.findAll()).thenReturn(announcementList);
		Assertions.assertNotNull(announcementServiceImpl.getAllAnnouncements(userEmail));

	}
	
	@Test
	 void addAnnouncementTest() {
		when(nextWorkUserService.checkUserRole(userId)).thenReturn("ADMIN");
		when(announcementRepository.save(Mockito.any())).thenReturn(announcement);
		Assertions.assertNotNull(announcementServiceImpl.addOrUpdateAnnouncement(userEmail, announcementDTO));

	}
	
	@Test
	 void updateAnnouncementTest() {
		when(nextWorkUserService.checkUserRole(userId)).thenReturn("ADMIN");
		announcementDTO.setUid(announcementId);
		when(announcementRepository.findById(announcementId)).thenReturn(optionalAnnouncement);
		when(announcementRepository.save(Mockito.any())).thenReturn(announcement);
		Assertions.assertNotNull(announcementServiceImpl.addOrUpdateAnnouncement(userEmail, announcementDTO));

	}
	
	@Test
	 void updateAnnouncementAnnouncementNotFoundExceptionTest() {
		when(nextWorkUserService.checkUserRole(userId)).thenReturn("ADMIN");
		announcementDTO.setUid(announcementId);
		when(announcementRepository.save(Mockito.any())).thenReturn(announcement);
		Assertions.assertThrows(ResourceNotFoundException.class, () -> announcementServiceImpl.addOrUpdateAnnouncement(userEmail, announcementDTO));

	}


}
