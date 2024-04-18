package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.siemens.nextwork.admin.controller.ScopingController;
import com.siemens.nextwork.admin.dto.GIDRequestDTO;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.NotesDTO;
import com.siemens.nextwork.admin.dto.ScopingDTO;
import com.siemens.nextwork.admin.dto.ScopingDataRequestListDTO;
import com.siemens.nextwork.admin.dto.UploadFileStatusResponseDTO;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.service.ScopingService;
import com.siemens.nextwork.admin.util.NextworkConstants;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
@SpringBootTest
 class ScopingControllerTest extends RestTestEnabler {
	String versionId;
	String gid;
	GIDRequestDTO gidRequestDTO;
	List<ScopingDTO> data;
	ScopingDTO scopingARE;
	UploadFileStatusResponseDTO result;
	List<ScopingDataRequestListDTO> scopingDataRequestList;
	 NotesDTO notesDTO;
	 IdResponseDTO rsp;


	@Mock
	private HttpServletRequest request;

	@Mock
	private ScopingService scopingService;

	@InjectMocks
	private ScopingController scopingController;

	String dummyToken = Jwts.builder().claim("email", "abc@siemens.com").compact();
	String authorization = "bearer " + dummyToken;
	String userEmail = "abc@siemens.com";
	String publishFlag = "Publish";


	@BeforeEach
	public void setup() {
		versionId = "20230608";
		gid = "Z0041234";
		gidRequestDTO = new GIDRequestDTO();
		// gidRequestDTO.setAction("Update");
		data = new ArrayList<>();

		scopingARE = new ScopingDTO();
		scopingARE.setFieldData("ARE");
		scopingARE.setFieldName("700E");
		data.add(scopingARE);

		gidRequestDTO.setData(data);
		result = new UploadFileStatusResponseDTO();
		result.setTaskid("id");
		result.setErrors("errors");
		result.setStatus("PASSED");
		notesDTO = new NotesDTO();
		
		String uid = UUID.randomUUID().toString();
		rsp = IdResponseDTO.builder().uid(uid).build();

		scopingDataRequestList = new ArrayList<>();
		when(request.getHeader(Mockito.any())).thenReturn(authorization);

	}

	@Test
	 void updateGIDTest() throws Exception {
		when(scopingService.updateGID(gidRequestDTO, versionId, userEmail, gid, NextworkConstants.FILE_UPLOAD_PUT_TOOL)).thenReturn(rsp);
		Assertions.assertNotNull(scopingController.updateGID(gidRequestDTO, versionId, gid));

	}

	@Test
	 void searchGIDTest() throws Exception {
		System.out.println("data : " + data);
		when(scopingService.searchGID(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(data);
		Assertions.assertNotNull(scopingController.searchGID(versionId, gid));

	}

	@Test
	 void searchGIDExceptionTest() throws Exception {

		Assertions.assertThrows(RestBadRequestException.class, () -> scopingController.searchGID(versionId, gid));

	}
	
	@Test
	 void getStatusForAddJobProfilesProcessTest() throws Exception {
		when(scopingService.getAsyncJobProfileStatus(Mockito.any(), Mockito.any())).thenReturn(result);
		Assertions.assertNotNull(scopingController.getStatusForAddJobProfilesProcess(gid));

	}
	
	@Test
	 void appendScopeRequestviaTool() throws Exception {
		when(scopingService.appendScopingData(Mockito.any(), Mockito.any(), Mockito.any(), 
				Mockito.any(), Mockito.any())).thenReturn(rsp);
		Assertions.assertNotNull(scopingController.appendScopeRequestviaTool(versionId, scopingDataRequestList, null,request));

	}
	
	@Test
	 void getAllVersionsTest() throws Exception {
		Assertions.assertNull(scopingController.getAllVersions());

	}
	@Test
	 void publishUnpublishScopingMasterDataTest() throws Exception {
		
		Assertions.assertNotNull(scopingController.publishUnpublishScopingMasterData(publishFlag, versionId));


	}
	
	@Test
	 void publishUnpublishScopingMasterDataExceptionTest() throws Exception {
		
		Assertions.assertThrows(RestBadRequestException.class, () -> scopingController.publishUnpublishScopingMasterData("published", versionId));


	}
	
	@Test
     void verifyGetSummaryForVersionId() throws Exception {
		Assertions.assertNotNull(scopingController.getVersionSummary(versionId));
	}

	  
	  @Test
	   void getNotes() throws Exception {
	  
	  Assertions.assertNotNull(scopingController.getNotes(versionId));
	  
	  
	  }
	
}
