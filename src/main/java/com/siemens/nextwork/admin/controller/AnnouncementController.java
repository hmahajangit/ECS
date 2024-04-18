package com.siemens.nextwork.admin.controller;

import static com.siemens.nextwork.admin.util.NextworkConstants.AUTHORIZATION_HEADER;
import static com.siemens.nextwork.admin.util.NextworkConstants.BAD_REQUEST;
import static com.siemens.nextwork.admin.util.NextworkConstants.FORBIDDEN;
import static com.siemens.nextwork.admin.util.NextworkConstants.SERVER_ERROR;
import static com.siemens.nextwork.admin.util.NextworkConstants.SUCCESS;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siemens.nextwork.admin.dto.AnnouncementDTO;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.exception.ExceptionResponse;
import com.siemens.nextwork.admin.service.AnnouncementService;
import com.siemens.nextwork.admin.util.CommonUtils;
import com.siemens.nextwork.admin.validator.AnnouncementValidator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/announcement")
@Tag(description = "API endpoint for Admin Management Service", name = "Announcement API")
public class AnnouncementController {

	@Autowired
	HttpServletRequest request;
	
	@Autowired
	AnnouncementService announcementService;
	
    @Autowired
    AnnouncementValidator announcementValidator;
	
	@Operation(summary = "Add an announcement",
			description = "Use this endpoint to add an announcement", 
			responses = {
			@ApiResponse(responseCode = "201", description = SUCCESS),
			@ApiResponse(responseCode = "400", description = BAD_REQUEST,
					content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
			@ApiResponse(responseCode = "403", description = FORBIDDEN,
					content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
			@ApiResponse(responseCode = "500", description = SERVER_ERROR,
					content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
			})
	@PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdResponseDTO> addAnnouncement(@RequestBody AnnouncementDTO announcementDTO,
			BindingResult bindingResult) {
		announcementValidator.validate(announcementDTO, bindingResult);
		if (bindingResult.hasErrors()) {
			CommonUtils.parseErrors(bindingResult);
		}
		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(announcementService.addOrUpdateAnnouncement(userEmail, announcementDTO));
	}
	
	@Operation(summary = "View list of Announcements", description = "Use this endpoint to view list of Announcements", responses = {
			@ApiResponse(responseCode = "200", description = SUCCESS),
			@ApiResponse(responseCode = "403", description = FORBIDDEN,
					content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
			@ApiResponse(responseCode = "500", description = SERVER_ERROR,
					content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
	})
	@GetMapping(path = "")
	public ResponseEntity<List<AnnouncementDTO>> getAllAnnouncements() {
		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		return ResponseEntity.ok().body(announcementService.getAllAnnouncements(userEmail));
	}
}
