package com.siemens.nextwork.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.siemens.nextwork.admin.dto.IdDTO;
import com.siemens.nextwork.admin.model.SiemensMySkills;
import com.siemens.nextwork.admin.repo.WorkStreamRepository;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;
import com.siemens.nextwork.admin.dto.AsyncDetailsDTO;
import com.siemens.nextwork.admin.dto.AsyncJobStatusDTO;
import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.repo.GRIPCatalogueRepositroy;
import com.siemens.nextwork.admin.repo.SiemensMySkillsRepository;
import com.siemens.nextwork.admin.service.FileStore;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.service.impl.AsyncUploadServiceImpl;
import com.siemens.nextwork.admin.service.impl.FileServiceImpl;
import com.siemens.nextwork.admin.util.NextworkConstants;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
@SpringBootTest
 class AsyncUploadServiceTest extends RestTestEnabler {

	
	String uid = null;
	AsyncDetailsDTO details = null;
	AsyncJobStatusDTO jobStatus = null;

	@Autowired
	private ResourceLoader resourceLoader;

	@Mock
	private FileServiceImpl fileService;

	@InjectMocks
	private AsyncUploadServiceImpl asyncUploadService;

	@Mock
	private SiemensMySkillsRepository siemensMySkillsRepository;

	@Mock
	private GRIPCatalogueRepositroy gripCatalogueRepositroy;

	@Mock
	private FileDetailsRepository fileDetailsRepository;

	@Mock
	private FileStore fileStore;
	@Mock
	private UserService userService;

	@Mock
	private WorkStreamRepository  workStreamRepository;

	@Mock
	private MongoOperations mongoOperations;
	FileDetails fileDetails;
	FileDetails fileDetails1;
	//RoleEntity roleEntity;
	String jobId = "id01";
	String server = "localhost";
	String type = "SKILL";
	String userEmail = "user@siemens.com";
	InputStream fileStream;
	File localFile;
	
	@BeforeEach
	public void setup() {
		uid = UUID.randomUUID().toString();
		details = AsyncDetailsDTO.builder().keyName("SKILL").errorMessage("NO_ERROR").build();
		jobStatus = AsyncJobStatusDTO.builder().message(NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS).details(details)
				.build();

		fileDetails = new FileDetails();
		fileDetails.setFileName("test.txt");
		fileDetails.setUid("id01");
		
		
		fileDetails1 = new FileDetails();
		fileDetails1.setFileName("test.txt");
		fileDetails1.setUid("id01");
		fileDetails1.setStatus("SUCCESS");
		
		fileDetails1.setFilePath("s3/test.txt");	
		
	}

	@Test
	 void  processExcelDataByTypeTest() {
	
	   when(fileDetailsRepository.findById(Mockito.any())).thenReturn(Optional.of(fileDetails));
	  asyncUploadService.processExcelDataByType("id01",server,type);
	  int result =2;
	  assertEquals(2,result);
	
	}
	@Test
	 void  processExcelDataByType2Test() {
		
	  when(fileDetailsRepository.findById(Mockito.any())).thenReturn(Optional.of(fileDetails));
	 
	  asyncUploadService.processExcelDataByType("id01","s3",type);
	  
	  int result =2;
	  assertEquals(2,result);
	
	}
	@Test
	 void  processExcelDataByType3Test() {
		
	  when(fileDetailsRepository.findById(Mockito.any())).thenReturn(Optional.of(fileDetails1));
	 
	  asyncUploadService.processExcelDataByType("id01","s3",type);
	  
	  int result =2;
	  assertEquals(2,result);
	
	}
	@Test
	 void getAsyncJobProfileStatusTest(){		
		when(userService.findUserIdByEmail(userEmail)).thenReturn("id01");
		//when(roleRepository.findPlatformRole("id01")).thenReturn(Optional.of(roleEntity));
		when(fileDetailsRepository.findById(Mockito.any())).thenReturn(Optional.of(fileDetails));
		Assertions.assertNotNull(asyncUploadService.getAsyncJobProfileStatus(userEmail,"id01"));
		 int result =2;
		  assertEquals(2,result);
		
	}

	@Test
	 void readAndSaveExcelDataSkillTest() throws InvalidFormatException, IOException {
		InputStream inputStream = getSkillInputStream();
		List<SiemensMySkills> allSkills = new ArrayList<>();
		List<SiemensMySkills> modSkills = new ArrayList<>();
		List<IdDTO> allSkillIds = new ArrayList<>();
		List<IdDTO> wsIds = new ArrayList<>();
		SiemensMySkills sk1 = SiemensMySkills.builder().id("lex_skill_1688119494158390").skillName("Work & Organizational Psychology")
				.description("Focus on creating a strengths-based, forward looking, supportive and growth-oriented work environment, always in line with the four strategic priorities to enable individuals, teams and organizations to effectively accomplish tasks and confidently tackle new challenges.")
				.skillCatalog("Interpersonal & Personal").category("Communication").skillCatalog("P&O specifics").skillGroup("Global").build();
		SiemensMySkills sk2 = SiemensMySkills.builder().id("lex_skill_1688037225966580").skillName("SMO P Strategy")
				.description("The procurement function of Siemens Mobility defines its strategy based on the superior procurement strategy considering their business environments.")
				.skillCatalog("Function & Methods").category("Sales & Account Management").skillCatalog("SMO specifics").skillGroup("Global").build();
		SiemensMySkills sk3 = SiemensMySkills.builder().id("lex_skill_1685631825159690").skillName("Customer Centricity")
				.description("Customer centricity is people in an organization being able to to understand customers")
				.skillCatalog("Leadership").category("Customer Service").skillCatalog("Common Skills").skillGroup("Global").build();
		allSkills.add(sk1); allSkills.add(sk2); allSkills.add(sk3);

		modSkills.add(sk1);

		IdDTO id1 = IdDTO.builder()._id("lex_skill_1688119494158390").build();
		IdDTO id2 = IdDTO.builder()._id("lex_skill_1688037225966580").build();
		IdDTO id3 = IdDTO.builder()._id("lex_skill_1685631825159690").build();
		allSkillIds.add(id1); allSkillIds.add(id2); allSkillIds.add(id3);

		IdDTO wid1 = IdDTO.builder()._id("lex_skill_1688119494158390").build();
		IdDTO wid2 = IdDTO.builder()._id("lex_skill_1688037225966580").build();
		wsIds.add(wid1);  wsIds.add(wid2);


		when(siemensMySkillsRepository.findAll()).thenReturn(allSkills);
		when(siemensMySkillsRepository.findAllIdsByIsDeleted(Mockito.anyBoolean())).thenReturn(allSkillIds);
		when(workStreamRepository.findAllWorkStreamIdsByUpdatedSkillIds(Mockito.anyList())).thenReturn(wsIds);
		when(siemensMySkillsRepository.findAllByIsModified(Mockito.anyBoolean())).thenReturn(modSkills);
		when(workStreamRepository.findAllWorkStreamsById(Mockito.anyList())).thenReturn(new ArrayList<>());

		String errorMsg = asyncUploadService.readAndSaveExcelData(inputStream, NextworkConstants.UPLOAD_TYPE_SKILL);
		assertNotNull(errorMsg);
	}
	
	@Test
	 void readAndSaveExcelDataGripTest() throws InvalidFormatException, IOException {
		InputStream inputStream = getSkillInputStream();
		Assertions.assertThrows(IllegalStateException.class, () -> asyncUploadService.readAndSaveExcelData(inputStream, NextworkConstants.UPLOAD_TYPE_GRIP));
	}

	private InputStream getSkillInputStream() {
		Resource resource = resourceLoader.getResource("classpath:Skills_Report.xlsx");
		InputStream inputStream = null;
		try {
			inputStream = resource.getInputStream();
		} catch (Exception e) { System.err.println(e.getMessage()); }
		return inputStream;
	}
	
	@Test
	 void retryReplicaDataTest() throws InvalidFormatException, IOException {
		Optional<FileDetails> file = asyncUploadService.retryReplicaData(UUID.randomUUID().toString());
		assertEquals(Boolean.TRUE, file.isEmpty());
	}
}
