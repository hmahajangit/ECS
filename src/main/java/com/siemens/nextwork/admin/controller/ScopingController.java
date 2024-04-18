package com.siemens.nextwork.admin.controller;

import static com.siemens.nextwork.admin.util.NextworkConstants.AUTHORIZATION_HEADER;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.siemens.nextwork.admin.dto.GIDRequestDTO;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.MasterScopingDataSummary;
import com.siemens.nextwork.admin.dto.NotesDTO;
import com.siemens.nextwork.admin.dto.ScopingDTO;
import com.siemens.nextwork.admin.dto.ScopingDataRequestListDTO;
import com.siemens.nextwork.admin.dto.ScopingVersionResponseDTO;
import com.siemens.nextwork.admin.dto.UploadFileStatusResponseDTO;
import com.siemens.nextwork.admin.enums.PublishType;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.service.FileService;
import com.siemens.nextwork.admin.service.ScopingDataDownloadService;
import com.siemens.nextwork.admin.service.ScopingService;
import com.siemens.nextwork.admin.util.CommonUtils;
import com.siemens.nextwork.admin.util.NextworkConstants;

@RestController
@RequestMapping("/api/v1/scopes")
public class ScopingController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScopingController.class);

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private ScopingService scopingService;

	@Autowired
	ScopingDataDownloadService scopingDataDownloadService;

	@Autowired
	FileService fileService;

	@PostMapping(value = "/async/{versionId}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public ResponseEntity<IdResponseDTO> createRequest(@PathVariable("versionId") String versionId,
			@RequestPart("file") MultipartFile mFile, HttpServletRequest request)
			throws InvalidFormatException {

		LOGGER.info("Inside Scoping Controller - async POST");
		if (Boolean.FALSE.equals(CommonUtils.validateVersion(versionId)))
			throw new InvalidFormatException("Version Id must be 8 digits only.");

		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));

		IdResponseDTO resID = fileService.saveFile(mFile, versionId, userEmail,
				NextworkConstants.FILE_UPLOAD_POST_CREATE, request.getServerName(), true);
		LOGGER.info("UID : {}", resID.getUid());
		scopingService.processMasterScopingDataWithVersion(resID.getUid(), userEmail, request.getServerName(),
				versionId, NextworkConstants.FILE_UPLOAD_POST_CREATE);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(resID);

	}

	@GetMapping("/async/{asyncJobId}")
	public ResponseEntity<UploadFileStatusResponseDTO> getStatusForAddJobProfilesProcess(
			@PathVariable(value = "asyncJobId") String asyncJobId) {
		LOGGER.info("getStatusForAddJobProfilesProcess controller method");
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		UploadFileStatusResponseDTO result = scopingService.getAsyncJobProfileStatus(userEmail, asyncJobId);
		return ResponseEntity.status(HttpStatus.OK).body(result);
	}

	@PutMapping(value = "/async/{versionId}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public ResponseEntity<IdResponseDTO> appendScopeRequestviaExcel(@PathVariable("versionId") String versionId,
			@RequestPart("file") MultipartFile mFile, HttpServletRequest request) {

		LOGGER.info("MMXX:Inside PUT API Controller");

		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		IdResponseDTO resID = fileService.saveFile(mFile, versionId, userEmail,
				NextworkConstants.FILE_UPLOAD_PUT_APPEND, request.getServerName(), true);
		scopingService.processMasterScopingDataWithVersion(resID.getUid(), userEmail, request.getServerName(),
				versionId, NextworkConstants.FILE_UPLOAD_PUT_APPEND);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(resID);

	}

	@PutMapping(value = "/{versionId}/data")
	public ResponseEntity<IdResponseDTO> appendScopeRequestviaTool(@PathVariable("versionId") String versionId,
			@RequestBody List<ScopingDataRequestListDTO> scopingDataRequestList, BindingResult bindingResult,
			HttpServletRequest request) {

		LOGGER.info("MMXX:Inside PUT API via Tool Controller");

		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		IdResponseDTO result = scopingService.appendScopingData(scopingDataRequestList, versionId, userEmail,
				NextworkConstants.FILE_UPLOAD_PUT_TOOL, request.getServerName());
		scopingService.appendToolData(result.getUid(), versionId, request.getServerName());
		return ResponseEntity.status(HttpStatus.CREATED).body(result);

	}

	@PutMapping("/{version_id}/{gid}")
	public IdResponseDTO updateGID(@RequestBody GIDRequestDTO gidRequestDTO,
			@PathVariable("version_id") String versionId, @PathVariable("gid") String gid) {
		LOGGER.info("Inside update GID");
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		IdResponseDTO result = scopingService.updateGID(gidRequestDTO, versionId, userEmail, gid,
				NextworkConstants.FILE_UPLOAD_PUT_TOOL);
		scopingService.appendToolData(result.getUid(), versionId, request.getServerName());
		return result;
	}

	@GetMapping("/{version_id}/{gid}")
	public List<ScopingDTO> searchGID(@PathVariable("version_id") String versionId, @PathVariable("gid") String gid) {
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		String id = versionId + "_" + gid;
		List<ScopingDTO> scopingList = new ArrayList<>();
		scopingList = scopingService.searchGID(id, userEmail, scopingList);
		if (!scopingList.isEmpty()) {
			return scopingList;

		} else {
			throw new RestBadRequestException("No data found for given version Id/ gid");
		}
	}

	@GetMapping("/versionList")
	public ScopingVersionResponseDTO getAllVersions() {
		LOGGER.info("Inside get All versions");

		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		return scopingService.getAllVersions(userEmail);
	}

	@PutMapping("/{version_id}/action")
	public IdResponseDTO publishUnpublishScopingMasterData(@RequestBody String publishUnpublishFlag,
			@PathVariable("version_id") String versionId) {
		LOGGER.info("Inside publishUnpublishScopingMasterData");
		// Validation for flag
		if (!(publishUnpublishFlag.equals(PublishType.PUBLISH.value)
				|| publishUnpublishFlag.equals(PublishType.UNPUBLISH.value))) {
			throw new RestBadRequestException("The request value should be either Publish or Unpublish");
		}
		IdResponseDTO idResponseDTO = new IdResponseDTO();

		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		scopingService.publishUnpublishScopingMasterData(versionId, userEmail, publishUnpublishFlag);
		idResponseDTO.setUid(versionId);

		return idResponseDTO;

	}

	@GetMapping("/{versionId}/summary")
	public List<MasterScopingDataSummary> getVersionSummary(@PathVariable("versionId") String versionId) {

		LOGGER.info("Inside controller summary");
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		return scopingService.getVersionSummaryByVersionId(versionId, userEmail);
	}

	@PutMapping("/{version_id}/notes")
	public ResponseEntity<IdResponseDTO> addNote(@RequestBody NotesDTO notesDTO,
			@PathVariable("version_id") String versionId) {
		LOGGER.info("Inside Add Note");
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		String authorName = CommonUtils.getAuthorName(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		LOGGER.info("author_name : {} ",authorName);
		IdResponseDTO idResponseDTO = scopingService.addNote(notesDTO, versionId, userEmail,authorName);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(idResponseDTO);
	}

	@GetMapping("{version_id}/notes")
	public ResponseEntity<List<NotesDTO>> getNotes(@PathVariable("version_id") String versionId) {
		LOGGER.info("Inside Get Notes");
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		List<NotesDTO> notesList = scopingService.getNotes(versionId, userEmail);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(notesList);
	}

	@GetMapping("/{versionId}/download")
	public List<String> downloadScopingVersion(@PathVariable("versionId") String versionId) {
		String userEmail = CommonUtils.getEmailId(request.getHeader(NextworkConstants.AUTHORIZATION_HEADER));
		LOGGER.info("User Email : {} ", userEmail);
		return scopingDataDownloadService.getPreSignedUrl(versionId,userEmail);
	}

}
