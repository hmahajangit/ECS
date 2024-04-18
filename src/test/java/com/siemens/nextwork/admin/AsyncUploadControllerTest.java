package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.siemens.nextwork.admin.dto.AsyncDetailsDTO;
import com.siemens.nextwork.admin.dto.AsyncJobStatusDTO;
import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.util.NextworkConstants;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
 class AsyncUploadControllerTest extends RestTestEnabler{
	
	String email = "syam-kumar.vutukuri.ext@siemens.com";
	String token = Jwts.builder().claim("email", email).compact();
	String authorization = "bearer " + token;
	String uid = null;
	AsyncDetailsDTO details = null;
	AsyncJobStatusDTO jobStatus = null;
	FileDetails fileDetails = null;
	Optional<FileDetails> ofd = null;
	Optional<NextWorkUser> user;
	

	@Autowired
	private ResourceLoader resourceLoader;
	
	@MockBean
	NextWorkUserRepository nextWorkUserRepository;
	
	@MockBean
	private FileDetailsRepository fileDetailsRepository;
	
	@BeforeEach
	public void setup() {
		uid = UUID.randomUUID().toString();
		details = AsyncDetailsDTO.builder().keyName("SKILL").errorMessage("NO_ERROR").build();
		jobStatus = AsyncJobStatusDTO.builder().message(NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS).details(details).build();
		List<Roles> roles = new ArrayList<>();
		Roles role = Roles.builder().id("64a52b534febce0ce703981b").roleDisplayName("Admin").roleType("ADMIN").haveGIDList(false).isDeleted(false).build();
		roles.add(role);
		NextWorkUser nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a3960").name("SYAM KUMAR VUTUKURI").email("syam-kumar.vutukuri.ext@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.creationDate("2022-12-12 11:17:36.279+00").status("Active").rolesDetails(roles).build();
		user = Optional.of(nxtUser);
		
		fileDetails = FileDetails.builder().uid("64dcce24a5ece628b74e01dc").action("SKILL").errors("NO_ERROR").status("SUCCESS").username("SYAM KUMAR VUTUKURI").build();
		ofd = Optional.of(fileDetails);
	}
	
	

	
	@Test
	 void asyncSkillUploadTest() throws Exception {
		Resource resource = resourceLoader.getResource("classpath:Skills_Report.xlsx");
		InputStream inputStream = null;
		try {
			inputStream = resource.getInputStream();
		} catch (Exception e) { System.err.println(e.getMessage()); }
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(this.user);
		when(fileDetailsRepository.save(Mockito.any())).thenReturn(fileDetails);
		when(fileDetailsRepository.findById(Mockito.anyString())).thenReturn(ofd);
		
		MockMultipartFile file = new MockMultipartFile("file", "Skills_Report.xlsx", null, inputStream);
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/upload/asyncExcelUpload").file(file)
				.header("Authorization", authorization).param("target", NextworkConstants.UPLOAD_TYPE_SKILL)
				.contentType(MediaType.MULTIPART_FORM_DATA)).andExpect(status().isAccepted()).andReturn();
	}
	
	@Test
	 void asyncAppendTest() throws Exception {
		

		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(this.user);
		when(fileDetailsRepository.findById(Mockito.anyString())).thenReturn(this.ofd);
		
	    mockMvc.perform(get("/api/v1/upload/asyncJobStatus/"+uid)
        		.header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
	}
	
	
}
