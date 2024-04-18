package com.siemens.nextwork.admin.service;

import java.util.List;

public interface ScopingDataDownloadService {

	List<String> getPreSignedUrl(String versionId, String userEmail);
}
