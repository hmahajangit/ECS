package com.siemens.nextwork.admin.controller;

import static com.siemens.nextwork.admin.util.NextworkConstants.AUTHORIZATION_HEADER;
import static com.siemens.nextwork.admin.util.NextworkConstants.BAD_REQUEST;
import static com.siemens.nextwork.admin.util.NextworkConstants.CONTENT_TYPE;
import static com.siemens.nextwork.admin.util.NextworkConstants.CONTENT_TYPE_VALUE;
import static com.siemens.nextwork.admin.util.NextworkConstants.FORBIDDEN;
import static com.siemens.nextwork.admin.util.NextworkConstants.SERVER_ERROR;
import static com.siemens.nextwork.admin.util.NextworkConstants.SUCCESS;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.siemens.nextwork.admin.exception.ExceptionResponse;
import com.siemens.nextwork.admin.service.ProjectKpiDownloadService;
import com.siemens.nextwork.admin.util.CommonUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/projectKpiDownload")
@Tag(description = "API endpoint for Admin Service", name = "Project KPI Download API")
public class ProjectKpiDownloadController {

	@Autowired
	HttpServletRequest request;

	@Autowired
	ProjectKpiDownloadService projectKpiDownloadService;

	@Operation(summary = "Download project KPIs as excel", description = "Use this endpoint to download project KPIs as excel", responses = {
			@ApiResponse(responseCode = "200", description = SUCCESS),
			@ApiResponse(responseCode = "400", description = BAD_REQUEST,
			content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
			@ApiResponse(responseCode = "403", description = FORBIDDEN, content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
			@ApiResponse(responseCode = "500", description = SERVER_ERROR, content = @Content(schema = @Schema(implementation = ExceptionResponse.class))) })
	@GetMapping
	public ResponseEntity<byte[]> getProjectKPIAsExcel(@RequestParam(value = "id", required = false) String[] ids,
			HttpServletResponse response) {
		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		byte[] projectKPIAsExcel = projectKpiDownloadService.getProjectKPIAsExcel(ids, userEmail, response);
		return ResponseEntity.ok().header(CONTENT_TYPE, CONTENT_TYPE_VALUE).body(projectKPIAsExcel);
	}

}
