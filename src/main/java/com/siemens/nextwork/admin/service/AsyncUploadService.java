package com.siemens.nextwork.admin.service;

import com.siemens.nextwork.admin.dto.AsyncJobStatusDTO;

public interface AsyncUploadService {

	void processExcelDataByType(String uid, String serverName, String type);

	AsyncJobStatusDTO getAsyncJobProfileStatus(String userEmail, String asyncJobId);

}
