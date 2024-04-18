package com.siemens.nextwork.admin.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.UploadFileStatusResponseDTO;
import com.siemens.nextwork.admin.service.FileService;
import com.siemens.nextwork.admin.util.CommonUtils;
import com.siemens.nextwork.admin.util.NextworkConstants;

@RestController
@RequestMapping("/api/v1")
public class UploadFileController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UploadFileController.class);
	
	@Autowired
	private HttpServletRequest request;
	
	@Autowired
	FileService fileService;

	@GetMapping("/uploadData/status")
	public List<UploadFileStatusResponseDTO> getUploadFileStatus(@RequestParam("target") String type){
		LOGGER.info("Inside UploadFileController");
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		return fileService.getScopingUploadFileStatus(userEmail, type);
	}
	
	@PutMapping(path = "/asyncExcelUpload/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public ResponseEntity<IdResponseDTO> updateProject(
			@PathVariable("id") String targetId,
			@RequestParam(value = "target") String target,
			@RequestParam(value = "action") String action,
			@RequestParam("file") MultipartFile file, HttpServletRequest request)
	{
		
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		var asyncJobId=fileService.createAsyncJobForFileUpload(userEmail, targetId, target, action , file, request.getServerName());
		
		fileService.processFileById(userEmail, targetId, action,asyncJobId,request.getServerName());
		
		var response=new IdResponseDTO();
		response.setUid(asyncJobId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@GetMapping("/asyncJobStatus/{asyncJobId}")
	public ResponseEntity<UploadFileStatusResponseDTO> getAsyncJobStatusById(@PathVariable("asyncJobId") String asyncJobId){
		
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		
		UploadFileStatusResponseDTO result = fileService.getAsyncUploadFileStatusById(userEmail, asyncJobId);
		return ResponseEntity.status(HttpStatus.OK).body(result);
	}

}
