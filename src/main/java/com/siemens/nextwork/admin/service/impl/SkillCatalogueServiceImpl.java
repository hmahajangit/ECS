package com.siemens.nextwork.admin.service.impl;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.siemens.nextwork.admin.config.BucketName;
import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.service.SkillCatalogueService;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.util.NextworkConstants;

@Service
public class SkillCatalogueServiceImpl implements SkillCatalogueService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SkillCatalogueServiceImpl.class);
	
	@Autowired
	private FileDetailsRepository fileDetailsRepository;
	
	@Autowired
	UserService  userService;
	
	@Autowired
	AmazonS3 amazonS3;
	
	@Autowired
	BucketName bucketName; 

	@Override
	public List<String> getSkillsCataloguePreSignedUrl(String userEmail) {
		
		userService.checkAdminUserRole(userEmail);
		var urls = new ArrayList<String>();
		var actionList = new ArrayList<String>();
		actionList.add(NextworkConstants.UPLOAD_TYPE_SKILL);
		List<FileDetails> skills = fileDetailsRepository.findAllByActionInAndType(actionList, NextworkConstants.UPLOAD_TYPE_SKILL, Sort.by(Sort.Direction.DESC, "updatedOn"));
		if(skills.isEmpty()) throw new IllegalStateException("File is not available.");
		
		FileDetails skill = skills.get(0);
		String bName = skill.getFilePath();
		String keyName = skill.getFileName();
		LOGGER.info("BucketName : {}, FileName or Key Name : {}", bucketName.getBucketName(), skill.getFileName());
		LocalDateTime ldt = LocalDateTime.now();
		ldt = ldt.plusMinutes(60l);
		URL url = amazonS3.generatePresignedUrl(bName, keyName, Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()) );
		if(url == null) {
			throw new IllegalStateException("File is Not Successfully Uploaded.");
		}
		urls.add(url.toString());
		return urls;
	}

}
