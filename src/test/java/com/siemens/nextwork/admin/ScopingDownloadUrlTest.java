package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import com.amazonaws.services.s3.AmazonS3;
import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.service.UserService;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
 class ScopingDownloadUrlTest extends RestTestEnabler {

	List<FileDetails> files;
	FileDetails fileDetails;
	URL myURL;

	@MockBean
	FileDetailsRepository fileDetailsRepository;

	@MockBean
	private UserService userService;

	@MockBean
	AmazonS3 amazonS3;

	String dummyToken = Jwts.builder().claim("email", "abc@siemens.com").compact();
	String authorization = "bearer " + dummyToken;

	@BeforeEach
	public void setup() {
		fileDetails = FileDetails.builder().uid("64dcce24a5ece628b74e01dc").fileName("xyaz.xls").action("SKILL")
				.errors("NO_ERROR").status("SUCCESS").username("SYAM KUMAR VUTUKURI").build();
		files = new ArrayList<>();
		files.add(fileDetails);
		try {
			myURL = new URL("http://example.com/");
		} catch (MalformedURLException e) {
		}
		System.out.println("Url : " + myURL);
	}

	@Test
	 void downloadScope() throws Exception {
		String version = "20210523";
		when(fileDetailsRepository.findAllByVersionAndStatus(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
				.thenReturn(this.files);
		when(amazonS3.generatePresignedUrl(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(this.myURL);
		mockMvc.perform(get("/api/v1/scopes/" + version + "/download").header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$").exists());
	}
}
