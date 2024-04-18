package com.siemens.nextwork.admin.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siemens.nextwork.admin.service.SkillCatalogueService;
import com.siemens.nextwork.admin.util.CommonUtils;
import com.siemens.nextwork.admin.util.NextworkConstants;

@RestController
@RequestMapping("/api/v1/")
public class SkillCatalogueController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SkillCatalogueController.class);

	@Autowired
	private HttpServletRequest request;
	
	@Autowired
	private SkillCatalogueService skillCatalogueService; 

	@GetMapping("/skillsCatalogue/download")
	public List<String> downloadSkillsCatalogue(){
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		LOGGER.info("User Email : {} ", userEmail);
		return skillCatalogueService.getSkillsCataloguePreSignedUrl(userEmail);
	}
	
}
