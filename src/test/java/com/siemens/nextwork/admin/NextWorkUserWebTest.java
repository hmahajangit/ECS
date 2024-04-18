package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.siemens.nextwork.admin.dto.GidsRequestDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserPutRequestDTO;
import com.siemens.nextwork.admin.dto.RoleDetailDTO;
import com.siemens.nextwork.admin.dto.WorkStreamDTO;
import com.siemens.nextwork.admin.enums.ActionForGidList;
import com.siemens.nextwork.admin.enums.ActionForRoleList;
import com.siemens.nextwork.admin.enums.ActionForWorkStreamList;
import com.siemens.nextwork.admin.enums.StatusType;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.model.Scoping;
import com.siemens.nextwork.admin.model.Workstream;
import com.siemens.nextwork.admin.model.WorkstreamGids;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.repo.RolesRepository;
import com.siemens.nextwork.admin.repo.ScopingRepository;
import com.siemens.nextwork.admin.repo.WorkStreamRepository;
import com.siemens.nextwork.admin.repo.WorkstreamGidRepository;
import com.siemens.nextwork.admin.service.UserService;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
 class NextWorkUserWebTest extends RestTestEnabler {


	String email = "natasha.patro@siemens.com";
	String dummyToken = Jwts.builder().claim("email", email).claim("org_code", "ADV GD DEV SE DH DT1").claim("given_name", "natasha").claim("family_name", "patro").compact();
	String authorization = "bearer " + dummyToken;

	
	NextWorkUserPutRequestDTO nextWorkUserInclusionPutRequestDTO;
	NextWorkUserPutRequestDTO nextWorkUserExclusionPutRequestDTO;
	Optional<NextWorkUser> loggedUser;
	Optional<NextWorkUser> remoteUser;
	Optional<NextWorkUser> remveUser;
	List<Scoping> scp;
	Scoping scope;
	Roles adminRole;
	Optional<Roles> remoteRole;
	List<Workstream> gwsList;
	Optional<Workstream> oWorkStream;
	
	Optional<NextWorkUser> pendingUser;
	Optional<NextWorkUser> rejectedUser;
	Optional<NextWorkUser> acceptedUser;
	WorkstreamGids workstreamGids;
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@MockBean
	NextWorkUserRepository nextWorkUserRepository;
	
	@MockBean
	UserService userService;
	
	@MockBean
	ScopingRepository scopingRepository; 
	
	@MockBean
	RolesRepository rolesRepository;
	
	@MockBean
	WorkStreamRepository workStreamRepository;
	
	@Mock
	MongoOperations mongoOperations;
	
	@MockBean
	WorkstreamGidRepository workstreamGidRepository;
	
	
//	@InjectMocks
//	Query query;

	@BeforeEach
	public void setup() {
		nextWorkUserInclusionPutRequestDTO = getNextWorkUserInclusionPutRequestDTO();
		nextWorkUserExclusionPutRequestDTO =  getNextWorkUserExclusionPutRequestDTO();
		
		NextWorkUser nxtUser = getLoggedUser();
		loggedUser = Optional.of(nxtUser);
		
		NextWorkUser remtUser = getRemoteUser();
		remoteUser = Optional.of(remtUser);
		
		Roles roles = getRemoteAdminRole();
		remoteRole = Optional.of(roles);
		
		NextWorkUser rUser = getRemoveUser();
		remveUser = Optional.of(rUser);
		
		NextWorkUser pUser = getUserByStatus(StatusType.PENDING.toString());
		pendingUser = Optional.of(pUser);
		
		NextWorkUser rejUser = getUserByStatus(StatusType.REJECTED.toString());
		rejectedUser = Optional.of(rejUser);
		
		NextWorkUser aUser = getUserByStatus(StatusType.ACCEPTED.toString());
		acceptedUser = Optional.of(aUser);
	
		loadScoping();
		loadWorkStream();
		loadWorkStreamGids();
//		workstreamGids = new WorkstreamGids();
//		workstreamGids.setId("WSGidId");
//		workstreamGids.setGidDataList(new ArrayList<>());
//		workstreamGids.setGidList(new ArrayList<>());
//		workstreamGids.setWorkstreamId("64dcce3f82ee21797fdc2533");
	}

	private NextWorkUser getLoggedUser() {
		List<Roles> roles = new ArrayList<>();
		Roles role = getTestAdminRole();
		roles.add(role);
		NextWorkUser nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a3960").name("SYAM KUMAR VUTUKURI").email("syam-kumar.vutukuri.ext@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.creationDate("2022-12-12 11:17:36.279+00").status("Active").rolesDetails(roles).build();
		return nxtUser;
	}
	
	private NextWorkUser getRemoteUser() {
		List<Roles> roles = new ArrayList<>();
		Roles role = getTestAdminRole();
		roles.add(role);
		NextWorkUser nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a395d").name("RAJA KUM").email("raj@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.creationDate("2022-12-12 11:17:36.279+00").status("Active").rolesDetails(roles).build();
		return nxtUser;
	}
	
	private NextWorkUser getRemoveUser() {
		List<Roles> roles = new ArrayList<>();
		Roles role = getTestAdminRole();
		roles.add(role);
		List<String> gids = new ArrayList<>();
		gids.add("ABC00001");
		List<WorkStreamDTO> wsDtosList = new ArrayList<>();
		WorkStreamDTO wsDto = WorkStreamDTO.builder().name("Test Ws").uid("64f1784c6c96a421b2e7ecaf").build();
		wsDtosList.add(wsDto);
		
		NextWorkUser nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a395d").name("RAJA KUM").email("raj@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.creationDate("2022-12-12 11:17:36.279+00").status("Active").rolesDetails(roles).gidList(gids).directAssignmentWorkstreamList(wsDtosList).build();
		return nxtUser;
	}
	
	private NextWorkUser getUserByStatus(String status) {
		List<Roles> roles = new ArrayList<>();
		Roles role = getTestAdminRole();
		roles.add(role);
		List<String> gids = new ArrayList<>();
		gids.add("ABC00001");
		List<WorkStreamDTO> wsDtosList = new ArrayList<>();
		WorkStreamDTO wsDto = WorkStreamDTO.builder().name("Test Ws").uid("64f1784c6c96a421b2e7ecaf").build();
		wsDtosList.add(wsDto);
		
		NextWorkUser nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a395d").name("RAJA KUM").email("natasha.patro@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				 .creationDate("2022-12-12 11:17:36.279+00").status(status).rolesDetails(roles).gidList(gids).directAssignmentWorkstreamList(wsDtosList).build();
		return nxtUser;
	}

	private Roles getTestAdminRole() {
		Roles role = Roles.builder().id("64a52b534febce0ce703981b").roleDisplayName("Admin").roleType("ADMIN").haveGIDList(false).isDeleted(false).build();
		return role;
	}
	private Roles getRemoteAdminRole() {
		Roles role = Roles.builder().id("64a52b534febce0ce703925b").roleDisplayName("GD Local Admin").roleType("LOCAL ADMIN").haveGIDList(false).isDeleted(false).build();
		return role;
	}

	private NextWorkUserPutRequestDTO getNextWorkUserInclusionPutRequestDTO() {
		UUID id = UUID.randomUUID();
		List<GidsRequestDTO> gidsLst = new ArrayList<>();
		List<WorkStreamDTO> wsLst = new ArrayList<>();
		List<RoleDetailDTO> rolesLst = new ArrayList<>();
		gidsLst.add(GidsRequestDTO.builder().gid("ABC00001").index(0).build());
		gidsLst.add(GidsRequestDTO.builder().gid("ABC00002").index(1).build());
		WorkStreamDTO wsDto = WorkStreamDTO.builder().name("Test Ws").uid("64f1784c6c96a421b2e7ecaf").build();
		wsLst.add(wsDto);
		RoleDetailDTO rleDto = RoleDetailDTO.builder().roleDisplayName("GD Local Admin").roleId("64cf7542c678de7a7da64f63").roleType("LOCAL ADMIN").build();
		rolesLst.add(rleDto);
		NextWorkUserPutRequestDTO nextWorkUserPutRequestDTO1 = NextWorkUserPutRequestDTO.builder().Id(id.toString()).gids(gidsLst).workstreamList(wsLst).rolesDetails(rolesLst).build();
		return nextWorkUserPutRequestDTO1;
	}
	
	private NextWorkUserPutRequestDTO getNextWorkUserExclusionPutRequestDTO() {
		UUID id = UUID.randomUUID();
		List<GidsRequestDTO> gidsLst = new ArrayList<>();
		List<WorkStreamDTO> wsLst = new ArrayList<>();
		List<RoleDetailDTO> rolesLst = new ArrayList<>();
		gidsLst.add(GidsRequestDTO.builder().gid("ABC00001").index(0).build());
		WorkStreamDTO wsDto = WorkStreamDTO.builder().name("Test Ws").uid("64f1784c6c96a421b2e7ecaf").build();
		wsLst.add(wsDto);
		RoleDetailDTO rleDto = RoleDetailDTO.builder().roleDisplayName("GD Local Admin").roleId("64cf7542c678de7a7da64f63").roleType("LOCAL ADMIN").build();
		rolesLst.add(rleDto);
		NextWorkUserPutRequestDTO nextWorkUserPutRequestDTO1 = NextWorkUserPutRequestDTO.builder().Id(id.toString()).gids(gidsLst).workstreamList(wsLst).rolesDetails(rolesLst).build();
		return nextWorkUserPutRequestDTO1;
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
	
	private List<Workstream> loadWorkStream() {
		Resource resource = resourceLoader.getResource("classpath:/test_data/workStreamTest.json");
		try {
	        ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
			JavaTimeModule javaTimeModule = new JavaTimeModule();
	        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.getDefault())));
			javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())));
			
	        mapper.registerModule(javaTimeModule);
	        mapper.registerModule(new Jdk8Module());
	        TypeReference<List<Workstream>> ref = new TypeReference<>() {};
	        List<Workstream> wsList = (ArrayList<Workstream>) mapper.readValue(resource.getInputStream(), ref);
	        gwsList = wsList;
	        Workstream ows = wsList.get(0);
	        this.oWorkStream = Optional.of(ows);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return gwsList;
	}
	
	private WorkstreamGids loadWorkStreamGids() {
		Resource resource = resourceLoader.getResource("classpath:/test_data/workstreamGidData.json");
		try {
	        ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
			JavaTimeModule javaTimeModule = new JavaTimeModule();
	        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.getDefault())));
			javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())));
			
	        mapper.registerModule(javaTimeModule);
	        mapper.registerModule(new Jdk8Module());
	        TypeReference<WorkstreamGids> ref = new TypeReference<>() {};
	        WorkstreamGids wsGids = mapper.readValue(resource.getInputStream(), ref);
	        this.workstreamGids = wsGids;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return workstreamGids;
	}
	
	
	@Test
	 void updateWebUserTestInclusion() throws JsonProcessingException, Exception {
		when(nextWorkUserRepository.findUserEntityByEmail(Mockito.anyString())).thenReturn(loggedUser);
		when(userService.findById(Mockito.any())).thenReturn(remoteUser);
		when(nextWorkUserRepository.findById(Mockito.anyString())).thenReturn(remoteUser);
		when(scopingRepository.findByRegexGId(Mockito.anyString())).thenReturn(scp);
		when(userService.saveNextWorkUser(Mockito.any())).thenReturn(remoteUser.get());
		when(rolesRepository.findById(Mockito.anyString())).thenReturn(remoteRole);
		when(workStreamRepository.findById(Mockito.anyString())).thenReturn(oWorkStream);
		when(workstreamGidRepository.findByWorkstreamId(Mockito.anyString())).thenReturn(workstreamGids);
		
		String usId = "64b0e557e3280a909b3a394f";
		mockMvc.perform(put("/api/v1/users/"+usId).header("Authorization", authorization)
				.param("actionGidList", ActionForGidList.GIDINCLUSION.toString())
				.param("actionWSList", ActionForWorkStreamList.WSINCLUSION.toString())
				.param("actionRoleList", ActionForRoleList.ROLEINCLUSION.toString())
				.content(new ObjectMapper().writeValueAsString(nextWorkUserInclusionPutRequestDTO))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isCreated())
				.andReturn();
	}
	
	@Test
	 void updateWebUserTestExclusion() throws JsonProcessingException, Exception {
		when(nextWorkUserRepository.findUserEntityByEmail(Mockito.anyString())).thenReturn(loggedUser);
		when(userService.findById(Mockito.any())).thenReturn(remveUser);
		when(nextWorkUserRepository.findById(Mockito.anyString())).thenReturn(remveUser);
		when(scopingRepository.findByRegexGId(Mockito.anyString())).thenReturn(scp);
		when(userService.saveNextWorkUser(Mockito.any())).thenReturn(remoteUser.get());
		when(rolesRepository.findById(Mockito.anyString())).thenReturn(remoteRole);
		when(workStreamRepository.findById(Mockito.anyString())).thenReturn(oWorkStream);
		when(workstreamGidRepository.findByWorkstreamId(Mockito.anyString())).thenReturn(workstreamGids);
		
		String usId = "64b0e557e3280a909b3a394f";
		mockMvc.perform(put("/api/v1/users/"+usId).header("Authorization", authorization)
				.param("actionGidList", ActionForGidList.GIDEXCLUSION.toString())
				.param("actionWSList", ActionForWorkStreamList.WSEXCLUSION.toString())
				.param("actionRoleList", ActionForRoleList.ROLEEXCLUSION.toString())
				.content(new ObjectMapper().writeValueAsString(nextWorkUserExclusionPutRequestDTO))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isCreated()).andDo(print())
				.andReturn();
	}
	
	@Test
	 void getUserInforAcceptTest() throws JsonProcessingException, Exception {
		when(userService.findByUserEmail(Mockito.any())).thenReturn(acceptedUser);
		when(userService.saveNextWorkUser(Mockito.any())).thenReturn(acceptedUser.get());
		
		mockMvc.perform(get("/api/v1/userInfo").header("Authorization", authorization)
				.content(new ObjectMapper().writeValueAsString(nextWorkUserExclusionPutRequestDTO))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk())
				.andReturn();
	}
	
	@Test
	 void getUserInforRejectedTest() throws JsonProcessingException, Exception {

		when(userService.findByUserEmail(Mockito.any())).thenReturn(rejectedUser);
		when(userService.saveNextWorkUser(Mockito.any())).thenReturn(rejectedUser.get());
		
		mockMvc.perform(get("/api/v1/userInfo").header("Authorization", authorization)
				.content(new ObjectMapper().writeValueAsString(nextWorkUserExclusionPutRequestDTO))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isForbidden())
				.andReturn();
	}
	
	@Test
	 void getUserInforPendingTest() throws JsonProcessingException, Exception {

		when(userService.findByUserEmail(Mockito.any())).thenReturn(pendingUser);
		when(userService.saveNextWorkUser(Mockito.any())).thenReturn(pendingUser.get());
		
		mockMvc.perform(get("/api/v1/userInfo").header("Authorization", authorization)
				.content(new ObjectMapper().writeValueAsString(nextWorkUserExclusionPutRequestDTO))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isForbidden())
				.andReturn();
	}
	

}
