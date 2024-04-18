package com.siemens.nextwork.admin.controller;

import static com.siemens.nextwork.admin.util.NextworkConstants.AUTHORIZATION_HEADER;
import static com.siemens.nextwork.admin.util.NextworkConstants.BAD_REQUEST;
import static com.siemens.nextwork.admin.util.NextworkConstants.CONTENT_TYPE;
import static com.siemens.nextwork.admin.util.NextworkConstants.CONTENT_TYPE_VALUE;
import static com.siemens.nextwork.admin.util.NextworkConstants.FORBIDDEN;
import static com.siemens.nextwork.admin.util.NextworkConstants.SERVER_ERROR;
import static com.siemens.nextwork.admin.util.NextworkConstants.SUCCESS;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.siemens.nextwork.admin.enums.StageAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.siemens.nextwork.admin.exception.ExceptionResponse;
import com.siemens.nextwork.admin.service.DashboardReportDownloadService;
import com.siemens.nextwork.admin.util.CommonUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/dashboardData")
@Tag(description = "API endpoint for Admin Service", name = "Project KPI Download API")
public class DashboardReportDownloadController {

	@Autowired
	HttpServletRequest request;

	@Autowired
	DashboardReportDownloadService dashboardReportDownloadService;

	@Operation(summary = "Download dashboard data report as excel", description = "Use this endpoint to download dashboard data report as excel", responses = {
			@ApiResponse(responseCode = "200", description = SUCCESS),
			@ApiResponse(responseCode = "400", description = BAD_REQUEST,
			content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
			@ApiResponse(responseCode = "403", description = FORBIDDEN, content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
			@ApiResponse(responseCode = "500", description = SERVER_ERROR, content = @Content(schema = @Schema(implementation = ExceptionResponse.class))) })
	@GetMapping("/{workstreamId}")
	public ResponseEntity<byte[]> getDashboradReportAsExcel(@PathVariable("workstreamId") String workstreamId,
			HttpServletResponse response) throws IOException {
		String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		byte[] projectKPIAsExcel = dashboardReportDownloadService.getDashboardReportAsExcel(workstreamId, userEmail, false, response);
		return ResponseEntity.ok().header(CONTENT_TYPE, CONTENT_TYPE_VALUE).body(projectKPIAsExcel);
	}

	@Operation(summary = "Download dashboard data report as excel", description = "Use this endpoint to download dashboard data report as excel", responses = {
		       @ApiResponse(responseCode = "200", description = SUCCESS),
		       @ApiResponse(responseCode = "400", description = BAD_REQUEST,
		             content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
		       @ApiResponse(responseCode = "403", description = FORBIDDEN, content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
		       @ApiResponse(responseCode = "500", description = SERVER_ERROR, content = @Content(schema = @Schema(implementation = ExceptionResponse.class))) })
		@PostMapping
		public ResponseEntity<byte[]> getDashboradReportAsExcelByListWsIds(@RequestBody(required = false) List<String> workstreamIds,
																		   @RequestParam(required = false) StageAction stage,
		                                           HttpServletResponse response) throws IOException, InterruptedException, ExecutionException {
		    String userEmail = CommonUtils.getEmailId(request.getHeader(AUTHORIZATION_HEADER));
		    byte[] projectKPIAsExcel = dashboardReportDownloadService.getDashboardReportAsExcelByListWsIds(workstreamIds, stage, userEmail, true, response);
		    return ResponseEntity.ok().header(CONTENT_TYPE, CONTENT_TYPE_VALUE).body(projectKPIAsExcel);
		}

	@PostMapping(path = "/migrate")
	public ResponseEntity<String> migrateWorkstream()
	{
		String migrationMsg = dashboardReportDownloadService.migrateWorkstream();
		return ResponseEntity.status(HttpStatus.CREATED).body(migrationMsg);

	}

}
