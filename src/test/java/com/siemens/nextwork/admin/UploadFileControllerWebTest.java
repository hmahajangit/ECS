package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.model.Scoping;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.repo.RolesRepository;
import com.siemens.nextwork.admin.repo.ScopingRepository;
import com.siemens.nextwork.admin.service.FileStore;
import com.siemens.nextwork.admin.util.NextworkConstants;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
 class UploadFileControllerWebTest extends RestTestEnabler{

	private static final String dummyToken = Jwts.builder().claim("email", "natasha.patro@siemens.com").compact();
	private static final String authorization = "bearer " + dummyToken;
	Optional<NextWorkUser> user;
	List<FileDetails> fls;
	FileDetails fDels;
	Optional<FileDetails> ofDels;
	Optional<Roles> oRoles;
	InputStream inputStream;
	String tempPath;
	Scoping scope;
	List<Scoping> scp;
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@MockBean
	NextWorkUserRepository nextWorkUserRepository;
	
	@MockBean
	RolesRepository rolesRepository;

	@MockBean
	FileStore fileStore;
	
	@MockBean
	FileDetailsRepository fileRepository;
	
	@MockBean
	ScopingRepository scopingRepository;
	
	@Mock
	MongoOperations mongoOperations;
	
	@Mock
	Resource resource;
	
	@BeforeEach
	public void setup() {
		List<Roles> roles = new ArrayList<>();
		Roles role = Roles.builder().id("64a52b534febce0ce703981b").roleDisplayName("Admin").roleType("ADMIN").haveGIDList(false).isDeleted(false).build();
		roles.add(role);
		oRoles = Optional.of(role);
		NextWorkUser nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a3960").name("SYAM KUMAR VUTUKURI").email("syam-kumar.vutukuri.ext@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.creationDate("2022-12-12 11:17:36.279+00").status("Active").rolesDetails(roles).build();
		user = Optional.of(nxtUser);
		
		fDels = FileDetails.builder().uid("64dcce24a5ece628b74e01dc").type("GID").action("GID_GID").filePath("LOCAL/GID").fileName("add_gid.xlsx") .errors("NO_ERROR").status("SUCCESS").username("SYAM KUMAR VUTUKURI").build();
		
		fls = new ArrayList<>();
		fls.add(fDels);
		resource = resourceLoader.getResource("classpath:add_gid.xlsx");
		try {
			File f = resource.getFile();
			Path resourceDirectory = Paths.get("src","test","resources");
			tempPath = resourceDirectory.toAbsolutePath().toString();
			inputStream = resource.getInputStream();
			fDels.setFilePath(tempPath);
			fDels.setFileName(f.getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		ofDels = Optional.of(fDels);
		scope = Scoping.builder().build();
		this.scp = loadScoping();
		
	}
	
	private List<Scoping> loadScoping() {
		resource = resourceLoader.getResource("classpath:/test_data/scoping_data.json");
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
	 void asyncAppendTest() throws Exception {
		resource = resourceLoader.getResource("classpath:add_gid.xlsx");
		inputStream = resource.getInputStream();
		when(fileRepository.findById(Mockito.any())).thenReturn(ofDels);
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(this.user);
		when(fileStore.upload(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Boolean.TRUE);
		when(fileRepository.save(Mockito.any())).thenReturn(fDels);
		when(fileStore.download(Mockito.anyString(), Mockito.anyString())).thenReturn(resource.getInputStream());
		when(rolesRepository.findById(Mockito.anyString())).thenReturn(oRoles);
		when(scopingRepository.findByRegexGId(Mockito.anyString())).thenReturn(scp);

		MockMultipartFile file = new MockMultipartFile("file", "add_gid.xlsx", null, inputStream);
		 MockMultipartHttpServletRequestBuilder builder =
		            MockMvcRequestBuilders.multipart("/api/v1/asyncExcelUpload/20060626");
		    builder.with(new RequestPostProcessor() {
		        @Override
		        public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
		            request.setMethod("PUT");
		            return request;
		        }
		    });
		
		mockMvc.perform(builder.file(file)
				.header("Authorization", authorization)
				.param("target", NextworkConstants.UPLOAD_TYPE_GID)
				.param("action", NextworkConstants.UPLOAD_TYPE_GID)
				.contentType(MediaType.MULTIPART_FORM_DATA)).andExpect(status().isCreated()).andReturn();
	}
}
