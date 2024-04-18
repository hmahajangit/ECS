package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
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
import com.siemens.nextwork.admin.dto.MemberDTO;
import com.siemens.nextwork.admin.dto.RequestRoleDTO;
import com.siemens.nextwork.admin.dto.RoleDetailDTO;
import com.siemens.nextwork.admin.dto.WorkStreamDTO;
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
 class RolesControllerWebTest extends RestTestEnabler{

	String email = "natasha.patro@siemens.com";
	String dummyToken = Jwts.builder().claim("email", email).compact();
	String authorization = "bearer " + dummyToken;

	
	RequestRoleDTO requestRoleDTO;
	Optional<NextWorkUser> loggedUser;
	Optional<NextWorkUser> remoteUser;
	List<Scoping> scp;
	Scoping scope;
	Roles localRole;
	Optional<Roles> opLocalRole;
	Roles delRole;
	Optional<Roles> opDelRole;
	List<Workstream> gwsList;
	Optional<Workstream> oWorkStream;
	WorkstreamGids workstreamGids;
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@MockBean
	NextWorkUserRepository nextWorkUserRepository;
	
	
	@MockBean
	ScopingRepository scopingRepository; 

	@MockBean
	UserService userService;
	
	@MockBean
	RolesRepository rolesRepository;
	
	@MockBean
	WorkStreamRepository workStreamRepository;
	
	@MockBean
	WorkstreamGidRepository workstreamGidRepository;

	@BeforeEach
	public void setup() {
		requestRoleDTO = getRolesInclusionPutRequestDTO();
		
		NextWorkUser nxtUser = getLoggedUser();
		loggedUser = Optional.of(nxtUser);
		
		NextWorkUser remtUser = getRemoteUser();
		remoteUser = Optional.of(remtUser);
		
		localRole = getRemoteAdminRole();
		opLocalRole = Optional.of(localRole);
		
		delRole = getTestAdminRole();
		opDelRole = Optional.of(delRole);
		
		loadWorkStream();
		loadScoping();
		loadWorkStreamGids();
	}

	private NextWorkUser getLoggedUser() {
		List<Roles> roles = new ArrayList<>();
		Roles role = getTestAdminRole();
		roles.add(role);
		NextWorkUser nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a3960").name("SYAM KUMAR VUTUKURI").email("natasha.yeals@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.creationDate("2022-12-12 11:17:36.279+00").status("Active").rolesDetails(roles).build();
		return nxtUser;
	}
	
	private NextWorkUser getRemoteUser() {
		List<Roles> roles = new ArrayList<>();
		Roles role = getRemoteAdminRole();
		roles.add(role);
		NextWorkUser nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a395d").name("Natasha").email("natasha.patro@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.creationDate("2022-12-12 11:17:36.279+00").status("Active").rolesDetails(roles).build();
		return nxtUser;
	}
	
	private Roles getTestAdminRole() {
		Roles role = Roles.builder().id("64a52b534febce0ce703981b").roleDisplayName("Admin").roleType("ADMIN").haveGIDList(false).isDeleted(true).build();
		return role;
	}
	private Roles getRemoteAdminRole() {
		List<WorkStreamDTO> wsLst = new ArrayList<>();
		WorkStreamDTO wsDto = WorkStreamDTO.builder().name("Test Ws").uid("64f1784c6c96a421b2e7ecaf").build();
		wsLst.add(wsDto);
		Roles role = Roles.builder().id("64a52b534febce0ce703925b").roleDisplayName("GD Local Admin").roleType("LOCAL ADMIN").workstreamList(wsLst)
				.haveGIDList(false).isDeleted(false).build();
		return role;
	}

	private RequestRoleDTO getRolesInclusionPutRequestDTO() {
		List<GidsRequestDTO> gidsLst = new ArrayList<>();
		List<WorkStreamDTO> wsLst = new ArrayList<>();
		List<RoleDetailDTO> rolesLst = new ArrayList<>();
		List<MemberDTO> memberList = new ArrayList<>();
		gidsLst.add(GidsRequestDTO.builder().gid("ABC00001").index(0).build());
		gidsLst.add(GidsRequestDTO.builder().gid("ABC00002").index(1).build());
		WorkStreamDTO wsDto = WorkStreamDTO.builder().name("Test Ws").uid("64f1784c6c96a421b2e7ecaf").build();
		wsLst.add(wsDto);
		RoleDetailDTO rleDto = RoleDetailDTO.builder().roleDisplayName("GD Local Admin").roleId("64cf7542c678de7a7da64f63").roleType("LOCAL ADMIN").build();
		rolesLst.add(rleDto);
		
		MemberDTO member= MemberDTO.builder().uid("64f1784c6c96a421b2e7qaaf").memberEmail("natasha.patro@siemens.com").memberName("Natasha").build();
		memberList.add(member);
		RequestRoleDTO requestRoleDTO = RequestRoleDTO.builder().roleDisplayName("GD Local Admin").roleType("LOCAL ADMIN")
				.roleDescription("GD Local Admin").gids(gidsLst).workstreamList(wsLst).memberList(memberList).build();
		return requestRoleDTO;
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
	 void addWebUserTest() throws JsonProcessingException, Exception {
		when(rolesRepository.findByRoleTypeAndRoleDisplayName(Mockito.anyString(), Mockito.anyString())).thenReturn(opDelRole);
		when(nextWorkUserRepository.findByEmail(Mockito.anyString())).thenReturn(remoteUser.get());
		when( workStreamRepository.findById(Mockito.anyString())).thenReturn(oWorkStream);
		when(rolesRepository.findById(Mockito.anyString())).thenReturn(opLocalRole);
		when(nextWorkUserRepository.findById(Mockito.anyString())).thenReturn(remoteUser);
		when(rolesRepository.save(Mockito.any())).thenReturn(localRole);
		when(scopingRepository.findByRegexGId(Mockito.anyString())).thenReturn(scp);
		when(workstreamGidRepository.findByWorkstreamId(Mockito.anyString())).thenReturn(workstreamGids);
		
		mockMvc.perform(post("/api/v1/roles/").header("Authorization", authorization)
				.content(new ObjectMapper().writeValueAsString(requestRoleDTO))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isCreated())
				.andReturn();
	}
	
	@Test
	 void updateWebUserTest() throws JsonProcessingException, Exception {
		when(rolesRepository.findByRoleTypeAndRoleDisplayName(Mockito.anyString(), Mockito.anyString())).thenReturn(opDelRole);
		when(nextWorkUserRepository.findByEmail(Mockito.anyString())).thenReturn(remoteUser.get());
		when( workStreamRepository.findById(Mockito.anyString())).thenReturn(oWorkStream);
		when(rolesRepository.findById(Mockito.anyString())).thenReturn(opLocalRole);
		when(nextWorkUserRepository.findById(Mockito.anyString())).thenReturn(remoteUser);
		when(rolesRepository.save(Mockito.any())).thenReturn(localRole);
		when(scopingRepository.findByRegexGId(Mockito.anyString())).thenReturn(scp);
		when(workstreamGidRepository.findByWorkstreamId(Mockito.anyString())).thenReturn(workstreamGids);
		
		String id = "64db87e3df9efb70ee773ec0";
		mockMvc.perform(put("/api/v1/roles/"+id).header("Authorization", authorization)
				.param("actionMemberList", "memberExclusion")
				.param("actionWSList", "wsExclusion")
				.content(new ObjectMapper().writeValueAsString(requestRoleDTO))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isCreated())
				.andReturn();
	}
}