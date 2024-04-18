package com.siemens.nextwork.admin.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.siemens.nextwork.admin.dto.AsyncJobStatusDTO;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.service.AsyncUploadService;
import com.siemens.nextwork.admin.service.FileService;
import com.siemens.nextwork.admin.util.CommonUtils;
import com.siemens.nextwork.admin.util.NextworkConstants;

@RestController
@RequestMapping("/api/v1/upload")
public class AsyncUploadController {
	
	@Autowired
	private FileService fileService;
	
	@Autowired
	private AsyncUploadService asyncUploadService;
	
	@Autowired
	private HttpServletRequest request;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncUploadController.class);

	@PostMapping(value = "/asyncExcelUpload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public ResponseEntity<IdResponseDTO> asyncExcelUploadByType(@RequestParam("target") String type,
			@RequestPart("file") MultipartFile mFile ) {
		LOGGER.info("Inside Async Excel Upload - async POST");
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		IdResponseDTO resID= fileService.saveAsyncFileByType(mFile, userEmail, request.getServerName(), type, type);
		LOGGER.info("After getting resp id : {}", resID.getUid());
		asyncUploadService.processExcelDataByType(resID.getUid(), request.getServerName(), type);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(resID);

	}

	@GetMapping("/asyncJobStatus/{asyncJobId}")
	public ResponseEntity<AsyncJobStatusDTO> asyncJobStatusById(@PathVariable(value = "asyncJobId") String asyncJobId) {
		LOGGER.info("asyncJobStatusById controller method");
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		AsyncJobStatusDTO result = asyncUploadService.getAsyncJobProfileStatus(userEmail, asyncJobId);
		return ResponseEntity.status(HttpStatus.OK).body(result);
	}
	
}
