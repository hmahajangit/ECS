package com.siemens.nextwork.admin.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siemens.nextwork.admin.dto.AnnouncementDTO;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.exception.ResourceNotFoundException;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.model.Announcement;
import com.siemens.nextwork.admin.repo.AnnouncementRepository;
import com.siemens.nextwork.admin.service.AnnouncementService;
import com.siemens.nextwork.admin.service.NextWorkUserService;
import com.siemens.nextwork.admin.service.UserService;

@Service
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {

	@Autowired
	private AnnouncementRepository announcementRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private NextWorkUserService nextWorkUserService;

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnouncementServiceImpl.class);

	@Override
	public IdResponseDTO addOrUpdateAnnouncement(String userEmail, AnnouncementDTO announcementDTO) {
		String userId = userService.findByEmail(userEmail);
		LOGGER.info("userId : {}", userId);
		String role = nextWorkUserService.checkUserRole(userId);
		IdResponseDTO responseDTO = new IdResponseDTO();
		LOGGER.info("user Role : {}", role);

			Date date = new Date(System.currentTimeMillis());
			if (Objects.isNull(announcementDTO.getUid())) {
				List<Announcement> announcementList = announcementRepository.findAll();
				int count = announcementList.size();
				if(count >= 10)
				{
					throw new RestBadRequestException("Max announcement count exceeded. Please delete existing announcement before adding any new");
				}
				Announcement announcement = new Announcement();
				announcement.setTitle(announcementDTO.getTitle());
				announcement.setDescription(announcementDTO.getDescription());
				announcement.setCreatedOn(date);
				announcement.setCreatedBy(userEmail);
				announcement.setDeleted(false);
				Announcement announcementCreated = announcementRepository.save(announcement);
				responseDTO.setUid(announcementCreated.getId());
				return responseDTO;
			} else {
				String announcementId = announcementDTO.getUid();
				Optional<Announcement> optionalAnnouncement = announcementRepository.findById(announcementId);
				if (!optionalAnnouncement.isPresent())
					throw new ResourceNotFoundException("Announcement not found");
				Announcement announcement = optionalAnnouncement.get();
				announcement.setTitle(announcementDTO.getTitle());
				announcement.setDescription(announcementDTO.getDescription());
				announcement.setUpdatedOn(date);
				announcement.setUpdatedBy(userEmail);
				Announcement announcementUpdated = announcementRepository.save(announcement);
				responseDTO.setUid(announcementUpdated.getId());
				return responseDTO;
			}
	}

	@Override
	public List<AnnouncementDTO> getAllAnnouncements(String userEmail) {
		userService.findByEmail(userEmail);
		List<Announcement> data = announcementRepository.findAll();
		data = data.stream().filter(i -> i.getDeleted().equals(Boolean.FALSE)).toList();
		
		return getAnnouncementResponse(data);

	}

	private List<AnnouncementDTO> getAnnouncementResponse(List<Announcement> announcementList) {
		List<AnnouncementDTO> responseList = new ArrayList<>();
		for (Announcement announcement : announcementList) {
			LocalDate date;
			if (Objects.nonNull(announcement.getUpdatedOn()))
				date = announcement.getUpdatedOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			else
				date = announcement.getCreatedOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			responseList.add(AnnouncementDTO.builder().uid(announcement.getId()).title(announcement.getTitle())
					.description(announcement.getDescription()).date(date).build());
		}
		return responseList;
	}
}
