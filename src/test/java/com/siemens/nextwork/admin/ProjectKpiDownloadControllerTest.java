package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.siemens.nextwork.admin.dto.IdDTO;
import com.siemens.nextwork.admin.dto.WorkStreamDTO;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.model.Workstream;
import com.siemens.nextwork.admin.model.WorkstreamGids;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.repo.RolesRepository;
import com.siemens.nextwork.admin.repo.WorkStreamRepository;
import com.siemens.nextwork.admin.repo.WorkstreamGidRepository;
import com.siemens.nextwork.admin.util.NextworkConstants;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
 class ProjectKpiDownloadControllerTest extends RestTestEnabler {
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@MockBean
	WorkStreamRepository workStreamRepository;
	
	@MockBean
	NextWorkUserRepository nextWorkUserRepository;
	
	@MockBean
	RolesRepository rolesRepository;
	
	@MockBean
	WorkstreamGidRepository workstreamGidRepository;
	

	String email = "natasha.patro@siemens.com";
	String dummyToken = Jwts.builder().claim("email", email).compact();
	String authorization = "bearer " + dummyToken;

	
	String[] ids;
	Optional<Workstream> pws;
	Optional<NextWorkUser> user;
	Optional<NextWorkUser> olaUser;
	List<Roles> rolesList;
	List<IdDTO> idDTOs;
	List<Workstream> wsList;
	WorkstreamGids workstreamGids;
	
	private Optional<Workstream> getWorkStream(String id) {
		List<Workstream> ws = getListWorkStreamsByJson();
		pws = ws.stream().filter(w -> w.getUid().equalsIgnoreCase(id)).findFirst();
		return pws;
	}

	private List<Workstream> getListWorkStreamsByJson(){
		List<Workstream> ws = null;
		try {
		Resource resource = resourceLoader.getResource("classpath:/test_data/workStreamTest_Matrix.json");
        ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
		JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())));
		javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())));
		
        mapper.registerModule(javaTimeModule);
        mapper.registerModule(new Jdk8Module());
        TypeReference<List<Workstream>> ref = new TypeReference<>() {};
        ws = (ArrayList<Workstream>) mapper.readValue(resource.getInputStream(), ref);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ws;
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

	@BeforeEach
	public void setup() {
		String id = "6555ea91dd30a757fd28c91f";
		ids = new String[] {id};
		this.pws = getWorkStream(id);
		this.wsList = getListWorkStreamsByJson();
		this.workstreamGids = loadWorkStreamGids();
		
		List<Roles> roles = new ArrayList<>();
		Roles role = Roles.builder().id("64a52b534febce0ce703981b").roleDisplayName("Admin").roleType("ADMIN").haveGIDList(false).isDeleted(false).build();
		roles.add(role);
		NextWorkUser nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a3960").name("SYAM KUMAR VUTUKURI").email("syam-kumar.vutukuri.ext@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.creationDate("2022-12-12 11:17:36.279+00").status("Active").rolesDetails(roles).build();
		user = Optional.of(nxtUser);
		
		List<Roles> laRoles = new ArrayList<>();
		Roles laRole = Roles.builder().id("64a52b534febce0ce703981b").roleDisplayName(NextworkConstants.ROLE_TYPE_LOCAL_ADMIN).roleType(NextworkConstants.ROLE_TYPE_LOCAL_ADMIN).haveGIDList(false).isDeleted(false).build();
		laRoles.add(laRole);
		rolesList = laRoles;
		idDTOs = new ArrayList<>();
		IdDTO dt = IdDTO.builder()._id(id).build();
		idDTOs.add(dt);
		List<WorkStreamDTO> wsDtoList = new ArrayList<>();
		WorkStreamDTO wsDto = WorkStreamDTO.builder().uid(id).name(id).build();
		wsDtoList.add(wsDto);
		NextWorkUser laUser = NextWorkUser.builder().id("64b0e557e3280a909b3a3960").name("SYAM KUMAR VUTUKURI").email("syam-kumar.vutukuri.ext@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.workStreamList(wsDtoList).creationDate("2022-12-12 11:17:36.279+00").status("Active").rolesDetails(laRoles).build();
		olaUser = Optional.of(laUser);
	}


	@Test
	 void verifyGetProjectKPIAsExcel() throws Exception {
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(user);
		when(workStreamRepository.findById(Mockito.anyString())).thenReturn(this.pws);
		when(workstreamGidRepository.findByWorkstreamId(Mockito.anyString())).thenReturn(workstreamGids);
		mockMvc.perform(get("/api/v1/projectKpiDownload")
				.header("Authorization", authorization)
				.param("id", ids)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}
	
	@Test
	 void verifyGetProjectKPIAsExcelForLocalAdmin() throws Exception {
		when(workStreamRepository.findAllWorkStreamIdByGids(Mockito.anyList())).thenReturn(idDTOs);
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(olaUser);
		when(workStreamRepository.findById(Mockito.anyString())).thenReturn(this.pws);
		when(rolesRepository.findAllActiveRolesByIds(Mockito.anyList())).thenReturn(rolesList);
		when(workStreamRepository.findAllWorkStreamIdsByUids(Mockito.anyList())).thenReturn(idDTOs);
		when(workstreamGidRepository.findByWorkstreamId(Mockito.anyString())).thenReturn(workstreamGids);
		mockMvc.perform(get("/api/v1/projectKpiDownload")
				.header("Authorization", authorization)
				.param("id", ids)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}
}
