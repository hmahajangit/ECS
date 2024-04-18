package com.siemens.nextwork.admin.service;


import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.UploadFileStatusResponseDTO;
import com.siemens.nextwork.admin.model.FileDetails;

@Service
public interface FileService {

	IdResponseDTO saveFile(MultipartFile mFile, String versionId, String userEmail, String action, String serverName, Boolean isCheckSize);	
    
	List<UploadFileStatusResponseDTO> getScopingUploadFileStatus(String userEmail, String type);

	void copyProcessedFile(String bucketName, FileDetails s3FileDetails, String fileAwsInput, String fileAwsProcessed, String newFile);	
	   
	String createAsyncJobForFileUpload(String userEmail, String targetId, String target, String actionGidList, MultipartFile file,
			String serverName);

	UploadFileStatusResponseDTO getAsyncUploadFileStatusById(String userEmail, String asyncJobId);

	void processFileById(String userEmail, String targetId, String actionGidList, String asyncJobId, String serverName);

	String saveToolData(String versionId, String action, String userId);

	IdResponseDTO saveAsyncFileByType(MultipartFile mFile, String userEmail, String serverName, String type, String action);

	
}
