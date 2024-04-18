package com.siemens.nextwork.admin.service;

import java.util.List;

import com.siemens.nextwork.admin.dto.AnnouncementDTO;
import com.siemens.nextwork.admin.dto.IdResponseDTO;

public interface AnnouncementService {

	IdResponseDTO addOrUpdateAnnouncement(String userEmail, AnnouncementDTO announcementDTO);

	List<AnnouncementDTO> getAllAnnouncements(String userEmail);

}
