package com.siemens.nextwork.admin.service;

import com.siemens.nextwork.admin.enums.StageAction;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

public interface DashboardReportDownloadService {

	public byte[] getDashboardReportAsExcel(String workstreamId, String userEmail, Boolean isBulkDownload, HttpServletResponse response) throws IOException;

	public byte[] getDashboardReportAsExcelByListWsIds(List<String> workstreamIds, StageAction stage, String userEmail, Boolean isBulkDownload, HttpServletResponse response) throws IOException, InterruptedException, ExecutionException;

    String migrateWorkstream();
}
