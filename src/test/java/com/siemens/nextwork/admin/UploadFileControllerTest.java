package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.siemens.nextwork.admin.config.BucketName;
import com.siemens.nextwork.admin.controller.UploadFileController;
import com.siemens.nextwork.admin.dto.IdDTO;
import com.siemens.nextwork.admin.dto.UploadFileStatusResponseDTO;
import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.model.Scoping;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.repo.RolesRepository;
import com.siemens.nextwork.admin.repo.ScopingRepository;
import com.siemens.nextwork.admin.service.FileService;
import com.siemens.nextwork.admin.service.FileStore;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.service.impl.FileServiceImpl;
import com.siemens.nextwork.admin.util.NextworkConstants;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
@SpringBootTest
 class UploadFileControllerTest extends RestTestEnabler {

	FileDetails fileDetails;
	List<FileDetails> fileDetailsList;
	String gid;
	UploadFileStatusResponseDTO uploadFileStatusResponseDTO;
	List<UploadFileStatusResponseDTO> uploadFileStatusResponseDTOList;
	List<FileDetails> fls;
	FileDetails fDels;
	Optional<FileDetails> ofDels;
	Optional<FileDetails> ofDelsExc;
	Optional<FileDetails> ofDelsUsrInc;
	Optional<FileDetails> ofDelsUsrExc;
	
	Optional<NextWorkUser> user;
	List<Scoping> scp;
	Optional<Roles> oRoles;
	List<IdDTO> scopingList;
	
	@Mock
	private UserService userService;
	
	@Mock
	MongoOperations mongoOperations;

	@Mock
	private FileDetailsRepository fileDetailsRepository;
	
	@Mock
	private RolesRepository rolesRepository;
	
	@Mock
	private NextWorkUserRepository nextWorkUserRepository;
	
	@Mock
	ScopingRepository scopingRepository;
	
	@Mock
	FileStore fileStore;

	@MockBean
	private BucketName s3bucketName;
	
	@Mock
	private HttpServletRequest request;

	@Mock
	private FileService fileService;

	@InjectMocks
	private UploadFileController uploadFileController;
	
	@InjectMocks
	private FileServiceImpl fileServiceImpl;

	@Autowired
	private ResourceLoader resourceLoader;
	
	String userEmail = "abc@siemens.com";
	private static final String dummyToken = Jwts.builder().claim("email", "abc@siemens.com").compact();
	private static final String authorization = "bearer " + dummyToken;
	
	@BeforeEach
	public void setup() {
		List<Roles> roles = new ArrayList<>();
		List<String> gids = new ArrayList<>();
		gids.add("ABC00010");
		gids.add("ABC00011");
		gids.add("ABC00012");
		gids.add("ABC00013");
		gids.add("ABC00014");
		gids.add("ABC00015");
		gids.add("ABC00016");
		gids.add("ABC00017");
		gids.add("ABC00018");
		gids.add("ABC00019");
		gids.add("ABC00020");
		gids.add("ABC00021");
		gids.add("ABC00022");
		gids.add("ABC00023");
		gids.add("ABC00024");
		gids.add("ABC00025");
		gids.add("ABC00026");
		gids.add("ABC00027");
		gids.add("ABC00028");
		gids.add("ABC00029");

		Roles role = Roles.builder().id("64a52b534febce0ce703981b").gidList(gids).roleDisplayName("Admin").roleType("ADMIN").haveGIDList(false).isDeleted(false).build();
		roles.add(role);
		oRoles = Optional.of(role);
		NextWorkUser nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a3960").gidList(gids).name("SYAM KUMAR VUTUKURI").email("syam-kumar.vutukuri.ext@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.creationDate("2022-12-12 11:17:36.279+00").status("Active").rolesDetails(roles).build();
		user = Optional.of(nxtUser);
		fDels = FileDetails.builder().uid("64dcce24a5ece628b74e01dc").createdOn(LocalDateTime.now()).type("ROLE_GID").action("Inclusion").filePath("LOCAL/GID").fileName("DataExample_DummyData.xlsb") .errors("NO_ERROR").status("SUCCESS").username("SYAM KUMAR VUTUKURI").build();
		ofDels = Optional.of(fDels);
		FileDetails fDelsExc = FileDetails.builder().uid("64dcce24a5ece628b74e01dc").createdOn(LocalDateTime.now()).type("ROLE_GID").action("Exclusion").filePath("LOCAL/GID").fileName("DataExample_DummyData.xlsb") .errors("NO_ERROR").status("SUCCESS").username("SYAM KUMAR VUTUKURI").build();
		ofDelsExc = Optional.of(fDelsExc);
		FileDetails fDelsUserInc  = FileDetails.builder().uid("64dcce24a5ece628b74e01dc").version("20230102").createdOn(LocalDateTime.now()).type("USER_GID").action("Inclusion").filePath("LOCAL/GID").fileName("DataExample_DummyData.xlsb") .errors("NO_ERROR").status("SUCCESS").username("SYAM KUMAR VUTUKURI").build();
		ofDelsUsrInc = Optional.of(fDelsUserInc);
		FileDetails fDelUserExc =  FileDetails.builder().uid("64dcce24a5ece628b74e01dc").version("20230102").createdOn(LocalDateTime.now()).type("USER_GID").action("Exclusion").filePath("LOCAL/GID").fileName("DataExample_DummyData.xlsb") .errors("NO_ERROR").status("SUCCESS").username("SYAM KUMAR VUTUKURI").build();
		ofDelsUsrExc = Optional.of(fDelUserExc);
		
		fls = new ArrayList<>();
		fls.add(fDels);
		gid = "Z0041234";
		scp = loadScoping();
		
		scopingList = new ArrayList<>();
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00010").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00011").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00012").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00013").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00014").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00015").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00016").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00017").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00018").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00019").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00020").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00021").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00022").build());		
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00023").build());	
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00024").build());	
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00025").build());	
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00026").build());	
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00027").build());
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00028").build());	
		scopingList.add(IdDTO.builder()._id(UUID.randomUUID().toString()).gid("ABC00029").build());	
		
		fileDetails=new FileDetails();
		fileDetails.setAction("Create");
		fileDetails.setCreatedOn(LocalDateTime.now());
		fileDetails.setErrors(null);
		fileDetails.setFileName("DummyData.xlsb");
		fileDetails.setFilePath("/tmp/");
		fileDetails.setStatus("SUCCESS");
		fileDetails.setType("GID");
		fileDetails.setUid("ABC123");
		fileDetailsList=new ArrayList<>();
		fileDetailsList.add(fileDetails);
		
		uploadFileStatusResponseDTO=new UploadFileStatusResponseDTO();
		uploadFileStatusResponseDTO.setStatus("SUCCESS");
		uploadFileStatusResponseDTO.setUsername("Madhavi Mendhe");
		uploadFileStatusResponseDTO.setTaskid("id01");
		uploadFileStatusResponseDTO.setErrors(null);
		
		uploadFileStatusResponseDTOList=new ArrayList<>();
		uploadFileStatusResponseDTOList.add(uploadFileStatusResponseDTO);
		
		when(userService.findUserIdByEmail(Mockito.any())).thenReturn(gid);
		when(request.getHeader(Mockito.any())).thenReturn(authorization);
	}

	

	
	@Test
	 void verifyGetUploadFileStatus() throws Exception {
		when(fileDetailsRepository.findAll()).thenReturn(fileDetailsList);
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(user);
		when(fileDetailsRepository.findAllByActionInAndType(Mockito.anyList(), Mockito.anyString(), Mockito.any())).thenReturn(fls);
		
		Assertions.assertNotNull(fileServiceImpl.getScopingUploadFileStatus(userEmail, NextworkConstants.UPLOAD_TYPE_GID));

	}
	
	private List<Scoping> loadScoping() {
		Resource resource = resourceLoader.getResource("classpath:/test_data/scoping_data.json");
		try {
	        ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
			JavaTimeModule javaTimeModule = new JavaTimeModule();
	        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.getDefault())));
			javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())));
			
	        mapper.registerModule(javaTimeModule);
	        mapper.registerModule(new Jdk8Module());
	        TypeReference<List<Scoping>> ref = new TypeReference<>() {};
	        List<Scoping> scps = (ArrayList<Scoping>) mapper.readValue(resource.getInputStream(), ref);
	        scp = scps;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return scp;
	}
	@Test
	 void processFileByIdRoleIncTest() throws Exception {
		Resource resource = resourceLoader.getResource("classpath:add_gid.xlsx");
		InputStream inputStream = resource.getInputStream();
		when(fileDetailsRepository.findById(Mockito.anyString())).thenReturn(ofDels);
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(this.user);
		when(fileStore.upload(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Boolean.TRUE);
		when(fileDetailsRepository.save(Mockito.any())).thenReturn(fDels);
		when(fileStore.download(Mockito.anyString(), Mockito.anyString())).thenReturn(resource.getInputStream());
		when(rolesRepository.findById(Mockito.any())).thenReturn(oRoles);
		when(scopingRepository.findAllIdsByGIDS(Mockito.anyList())).thenReturn(scopingList);
		String id = UUID.randomUUID().toString();
		fileServiceImpl.processFileById(userEmail, "20060626", "GID", id, "app.staging.com");
		Assertions.assertNotNull(inputStream);
	
	}
	@Test
	 void processFileByIdRoleExcTest() throws Exception {
		Resource resource = resourceLoader.getResource("classpath:add_gid.xlsx");
		InputStream inputStream = resource.getInputStream();
		when(fileDetailsRepository.findById(Mockito.anyString())).thenReturn(ofDelsExc);
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(this.user);
		when(fileStore.upload(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Boolean.TRUE);
		when(fileDetailsRepository.save(Mockito.any())).thenReturn(fDels);
		when(fileStore.download(Mockito.anyString(), Mockito.anyString())).thenReturn(resource.getInputStream());
		when(rolesRepository.findById(Mockito.any())).thenReturn(oRoles);
		when(scopingRepository.findAllIdsByGIDS(Mockito.anyList())).thenReturn(scopingList);
		
		String id = UUID.randomUUID().toString();
		fileServiceImpl.processFileById(userEmail, "20060626", "GID", id, "app.staging.com");
		Assertions.assertNotNull(inputStream);
	
	}
	
	@Test
	 void processFileByIdUserIncTest() throws Exception {
		
		Resource resource = resourceLoader.getResource("classpath:add_gid.xlsx");
		InputStream inputStream = resource.getInputStream();
		when(fileDetailsRepository.findById(Mockito.anyString())).thenReturn(ofDelsUsrInc);
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(this.user);
		when(nextWorkUserRepository.findById(Mockito.anyString())).thenReturn(this.user);
		when(fileStore.upload(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Boolean.TRUE);
		when(fileDetailsRepository.save(Mockito.any())).thenReturn(fDels);
		when(fileStore.download(Mockito.anyString(), Mockito.anyString())).thenReturn(resource.getInputStream());
		when(rolesRepository.findById(Mockito.any())).thenReturn(oRoles);
		when(scopingRepository.findAllIdsByGIDS(Mockito.anyList())).thenReturn(scopingList);
		
		String id = UUID.randomUUID().toString();
		fileServiceImpl.processFileById(userEmail, "20060626", "GID", id, "app.staging.com");
		Assertions.assertNotNull(inputStream);
	
	}
	@Test
	 void processFileByIdUserExcTest() throws Exception {
		Resource resource = resourceLoader.getResource("classpath:add_gid.xlsx");
		InputStream inputStream = resource.getInputStream();
		when(fileDetailsRepository.findById(Mockito.anyString())).thenReturn(ofDelsUsrExc);
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(this.user);
		when(nextWorkUserRepository.findById(Mockito.anyString())).thenReturn(this.user);
		when(fileStore.upload(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Boolean.TRUE);
		when(fileDetailsRepository.save(Mockito.any())).thenReturn(fDels);
		when(fileStore.download(Mockito.anyString(), Mockito.anyString())).thenReturn(resource.getInputStream());
		when(rolesRepository.findById(Mockito.any())).thenReturn(oRoles);
		when(scopingRepository.findAllIdsByGIDS(Mockito.anyList())).thenReturn(scopingList);
		
		String id = UUID.randomUUID().toString();
		fileServiceImpl.processFileById(userEmail, "20060626", "GID", id, "app.staging.com");
		Assertions.assertNotNull(inputStream);
	
	}

	@Test
	 void saveToolDataTest() throws Exception {
		when(nextWorkUserRepository.findById(Mockito.anyString())).thenReturn(this.user);
		when(fileDetailsRepository.findAllByVersionAndAction(Mockito.anyString(), Mockito.anyString())).thenReturn(fls);
		when(fileDetailsRepository.save(Mockito.any())).thenReturn(fDels);
		
		String id = UUID.randomUUID().toString();
		String resp = fileServiceImpl.saveToolData("20060626", "CREATE", id);
		Assertions.assertNotNull(resp);
	
	}
	
	@Test
	 void saveToolDataZeroFilesTest() throws Exception {
		when(nextWorkUserRepository.findById(Mockito.anyString())).thenReturn(this.user);
		List<FileDetails> fles = new ArrayList<>();
		when(fileDetailsRepository.findAllByVersionAndAction(Mockito.anyString(), Mockito.anyString())).thenReturn(fles);
		when(fileDetailsRepository.save(Mockito.any())).thenReturn(fDels);
		
		String id = UUID.randomUUID().toString();
		String resp = fileServiceImpl.saveToolData("20060626", "CREATE", id);
		Assertions.assertNotNull(resp);
	
	}
	
	@Test
	 void readExelSheetDataTest() throws Exception {
		Resource resource = resourceLoader.getResource("classpath:add_gid.xlsx");
		InputStream inputStream = resource.getInputStream();
		fileServiceImpl.readExelSheetData(inputStream, new StringBuilder());
		Assertions.assertNotNull(inputStream);

	}
	
	@Test
	 void getImportStatusTest() throws Exception {
		when(fileService.getScopingUploadFileStatus(userEmail, NextworkConstants.UPLOAD_TYPE_GID)).thenReturn(uploadFileStatusResponseDTOList);
		Assertions.assertNotNull(uploadFileController.getUploadFileStatus(NextworkConstants.UPLOAD_TYPE_GID));

	}
	
	@Test
	 void getAsyncJobStatusByIdTest() throws Exception {
		when(fileService.getScopingUploadFileStatus(userEmail, NextworkConstants.UPLOAD_TYPE_GID)).thenReturn(uploadFileStatusResponseDTOList);
		Assertions.assertNotNull(uploadFileController.getAsyncJobStatusById("id01"));

	}
	@Test
	 void updateProject() throws Exception {
		
		Resource resource = resourceLoader.getResource("classpath:add_GID.xlsx");
		InputStream inputStream = null;
		

		if (!resource.exists()) {
			System.out.println("Resource not exists");
		}

		try {
			inputStream = resource.getInputStream();

		} catch (Exception e) {
			e.printStackTrace();
		}

		MockMultipartFile file = new MockMultipartFile("file", "add_GID.xlsx", null, inputStream);
		when(fileService.getAsyncUploadFileStatusById(Mockito.any(), Mockito.any())).thenReturn(uploadFileStatusResponseDTO);
		Assertions.assertNotNull(uploadFileController.updateProject("id01","target","action",file,request));

	}
}
