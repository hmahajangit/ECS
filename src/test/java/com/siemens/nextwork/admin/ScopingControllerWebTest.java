package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.model.Scoping;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
 class ScopingControllerWebTest extends RestTestEnabler{
	
	private static final String dummyToken = Jwts.builder().claim("email", "natasha.patro@siemens.com").compact();
	private static final String authorization = "bearer " + dummyToken;
	Optional<NextWorkUser> user;
	List<FileDetails> fls;
	FileDetails fDels;
	Optional<FileDetails> ofDels;
	InputStream inputStream;
	String tempPath;
	Scoping scope;
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@MockBean
	NextWorkUserRepository nextWorkUserRepository;
	
	@MockBean
	FileDetailsRepository fileDetailsRepository;
	
	@Mock
	MongoOperations mongoOperations;
	
	@BeforeEach
	public void setup() {
		List<Roles> roles = new ArrayList<>();
		Roles role = Roles.builder().id("64a52b534febce0ce703981b").roleDisplayName("Admin").roleType("ADMIN").haveGIDList(false).isDeleted(false).build();
		roles.add(role);
		NextWorkUser nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a3960").name("SYAM KUMAR VUTUKURI").email("syam-kumar.vutukuri.ext@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.creationDate("2022-12-12 11:17:36.279+00").status("Active").rolesDetails(roles).build();
		user = Optional.of(nxtUser);
		
		fDels = FileDetails.builder().uid("64dcce24a5ece628b74e01dc").action("GID").filePath("LOCAL/GID").fileName("DataExample_DummyData.xlsb") .errors("NO_ERROR").status("SUCCESS").username("SYAM KUMAR VUTUKURI").build();
		
		fls = new ArrayList<>();
		fls.add(fDels);
		Resource resource = resourceLoader.getResource("classpath:DataExample_DummyData.xlsb");
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
		
	}
	
	@Test
	 void asyncCreateTest() throws Exception {
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(this.user);
		when(fileDetailsRepository.findAllByVersionAndStatus(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(this.fls);
		when(fileDetailsRepository.save(Mockito.any())).thenReturn(this.fDels);
		when(fileDetailsRepository.findById(Mockito.anyString())).thenReturn(this.ofDels);
		MockMultipartFile file = new MockMultipartFile("file", "DataExample_DummyData.xlsb", null, inputStream);
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/scopes/async/20060626").file(file)
				.header("Authorization", authorization)
				.contentType(MediaType.MULTIPART_FORM_DATA)).andExpect(status().isAccepted()).andReturn();
	}
	
	
	
	@Test
	 void asyncCreateVersionValidationTest() throws Exception {
		
		MockMultipartFile file = new MockMultipartFile("file", "DataExample_DummyData.xlsb", null, inputStream);
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/scopes/async/200626").file(file)
				.header("Authorization", authorization)
				.contentType(MediaType.MULTIPART_FORM_DATA)).andExpect(status().is5xxServerError()).andReturn();
	}
	
	@Test
	 void asyncAppendTest() throws Exception {
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(this.user);
		when(fileDetailsRepository.findAllByVersionAndStatus(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(this.fls);
		when(fileDetailsRepository.save(Mockito.any())).thenReturn(this.fDels);
		when(fileDetailsRepository.findById(Mockito.anyString())).thenReturn(this.ofDels);
		
		MockMultipartFile file = new MockMultipartFile("file", "DataExample_DummyData.xlsb", null, inputStream);
		 MockMultipartHttpServletRequestBuilder builder =
		            MockMvcRequestBuilders.multipart("/api/v1/scopes/async/20060626");
		    builder.with(new RequestPostProcessor() {
		        @Override
		        public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
		            request.setMethod("PUT");
		            return request;
		        }
		    });
		
		mockMvc.perform(builder.file(file)
				.header("Authorization", authorization)
				.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isAccepted()).andReturn();
	}
	
	
}
