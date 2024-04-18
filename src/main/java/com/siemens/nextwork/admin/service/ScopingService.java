package com.siemens.nextwork.admin.service;

import java.util.List;
import com.siemens.nextwork.admin.dto.GIDRequestDTO;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.MasterScopingDataSummary;
import com.siemens.nextwork.admin.dto.NotesDTO;
import com.siemens.nextwork.admin.dto.ScopingDTO;
import com.siemens.nextwork.admin.dto.ScopingDataRequestListDTO;
import com.siemens.nextwork.admin.dto.ScopingVersionResponseDTO;
import com.siemens.nextwork.admin.dto.UploadFileStatusResponseDTO;

public interface ScopingService {
	
	UploadFileStatusResponseDTO	 getAsyncJobProfileStatus(String userEmail, String id);

	IdResponseDTO appendScopingData(List<ScopingDataRequestListDTO> scopingDataRequestList, String versionId, String userEmail,
			String action, String server);

	IdResponseDTO updateGID(GIDRequestDTO gidRequestDTO,String id, String userEmail, String gid, String action);

	List<ScopingDTO> searchGID(String id, String userEmail,List<ScopingDTO> scopingList);

	ScopingVersionResponseDTO getAllVersions(String userEmail);

	void publishUnpublishScopingMasterData(String versionId, String userEmail, String publishUnpublishFlag);

	boolean invalidateVersionId(String versionId);

	void processMasterScopingDataWithVersion(String jobId, String userEmail, String server, String versionId, String action);
	
	public void appendToolData(String uid, String versionId, String server) ;

	List<MasterScopingDataSummary> getVersionSummaryByVersionId(String versionId, String userEmail);
	
	IdResponseDTO addNote(NotesDTO notesDTO, String versionId, String userEmail, String authorName);

	List<NotesDTO> getNotes(String versionId, String userEmail);

}
