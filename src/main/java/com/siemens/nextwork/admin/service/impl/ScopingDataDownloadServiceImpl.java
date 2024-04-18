package com.siemens.nextwork.admin.service.impl;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.siemens.nextwork.admin.config.BucketName;
import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.service.ScopingDataDownloadService;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.util.NextworkConstants;

@Service
public class ScopingDataDownloadServiceImpl implements ScopingDataDownloadService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ScopingDataDownloadServiceImpl.class);
	
	@Autowired
	AmazonS3 amazonS3;

	@Autowired
	private FileDetailsRepository fileRepository;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	BucketName bucketName; 

	@Override
	public List<String> getPreSignedUrl(String versionId, String userEmail) {
		
		userService.checkAdminUserRole(userEmail);

		List<String> urls = new ArrayList<>();
		Sort sort = Sort.by(Sort.Direction.DESC, "updatedOn");
		List<FileDetails> files= fileRepository.findAllByVersionAndStatus(versionId, "SUCCESS" ,sort);
		LOGGER.info("total version records : {} ", files.size());
		URL url = null;
		Map<String, String> s3FileMap = new LinkedHashMap<>();
		s3FileMap.put(NextworkConstants.FILE_UPLOAD_POST_CREATE, null);
		s3FileMap.put(NextworkConstants.FILE_UPLOAD_PUT_APPEND, null);
		String filePath = null;
		if(!files.isEmpty()) {
			for(FileDetails fileDetail : files) {
				filePath = fileDetail.getFilePath();
				if(fileDetail.getAction().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_POST_CREATE)) {
					s3FileMap.put(NextworkConstants.FILE_UPLOAD_POST_CREATE, fileDetail.getFileName());
					break;
				}
				s3FileMap.put(NextworkConstants.FILE_UPLOAD_PUT_APPEND, fileDetail.getFileName());
			}
		}
	
		for(String action : s3FileMap.keySet()) {
			String keyName = s3FileMap.getOrDefault(action, null);
			if(keyName == null) continue;
			String bName = filePath;
			LOGGER.info("Version : {}, BucketName : {}, FileName : {}", versionId, bucketName, keyName);
			LocalDateTime ldt = LocalDateTime.now();
			ldt = ldt.plusMinutes(60l);
			url = amazonS3.generatePresignedUrl(bName, keyName, Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()) );
			if(url == null) {
				throw new IllegalStateException("File is Not Successfully Uploaded.");
			}
			urls.add(url.toString());
		
		}
		
		return urls;
	}
	

}
