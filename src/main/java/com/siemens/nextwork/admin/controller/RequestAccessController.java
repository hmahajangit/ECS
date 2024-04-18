package com.siemens.nextwork.admin.controller;

import static com.siemens.nextwork.admin.util.NextworkConstants.BAD_REQUEST;
import static com.siemens.nextwork.admin.util.NextworkConstants.FORBIDDEN;
import static com.siemens.nextwork.admin.util.NextworkConstants.SUCCESS;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.RequestAccessDTO;
import com.siemens.nextwork.admin.exception.ExceptionResponse;
import com.siemens.nextwork.admin.service.RequestAccessService;
import com.siemens.nextwork.admin.validator.RequestAccessValidator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/accessRequest")
@Tag(description = "API endpoint for Admin Service", name = "Request Access API")
public class RequestAccessController {

	@Autowired
	HttpServletRequest request;

	@Autowired
	RequestAccessService requestAccessService;

	@Autowired
	RequestAccessValidator requestAccessValidator;
	
	@Operation(
			summary = "Raise Access Request to NextWork tool",
			description = "Use this endpoint to raise access request", 
			responses = {
					@ApiResponse(responseCode = "201", description = SUCCESS),
					@ApiResponse(responseCode = "400", description = BAD_REQUEST,
					content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
					@ApiResponse(responseCode = "403", description = FORBIDDEN,
					content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
					@ApiResponse(responseCode = "500", description = "Server Error",
					content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
			})
	@PostMapping
	public ResponseEntity<IdResponseDTO> createRequest(
			@RequestBody RequestAccessDTO requestAccessDTO, BindingResult bindingResult) {
		requestAccessValidator.validate(requestAccessDTO, bindingResult);
		if (bindingResult.hasErrors()) {
			requestAccessValidator.parseErrors(bindingResult);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(requestAccessService.createAccessRequest(requestAccessDTO));
	}
}
