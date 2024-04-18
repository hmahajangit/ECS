package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import com.amazonaws.services.s3.AmazonS3;
import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
 class SkillCatalogueControllerTest extends RestTestEnabler {

	@MockBean
	FileDetailsRepository fileDetailsRepository;

	@MockBean
	AmazonS3 amazonS3;
	
	@MockBean
	NextWorkUserRepository nextWorkUserRepository;

	List<FileDetails> fls;
	FileDetails fDels;
	URL myURL;
	Optional<NextWorkUser> user;
	String dummyToken = Jwts.builder().claim("email", "abc@siemens.com").compact();
	String authorization = "bearer " + dummyToken;

	@BeforeEach
	public void setup() {
		List<Roles> roles = new ArrayList<>();
		Roles role = Roles.builder().id("64a52b534febce0ce703981b").roleDisplayName("Admin").roleType("ADMIN").haveGIDList(false).isDeleted(false).build();
		roles.add(role);
		NextWorkUser nxtUser = NextWorkUser.builder().id("64b0e557e3280a909b3a3960").name("SYAM KUMAR VUTUKURI").email("syam-kumar.vutukuri.ext@siemens.com").orgCode("ADV GD DEV SE DH DT1")
				.creationDate("2022-12-12 11:17:36.279+00").status("Active").rolesDetails(roles).build();
		user = Optional.of(nxtUser);
		
		fDels = FileDetails.builder().uid("64dcce24a5ece628b74e01dc").action("SKILL").filePath("LOCAL/SKILL").fileName("xyz.xlsx") .errors("NO_ERROR").status("SUCCESS").username("SYAM KUMAR VUTUKURI").build();
		fls = new ArrayList<>();
		fls.add(fDels);
		try {
			myURL = new URL("http://example.com/");
		} catch (MalformedURLException e) {	e.printStackTrace();	}
		System.out.println("URL Object "+this.myURL);
	}

	@Test
	 void getSkillCatalogueDownload() throws Exception {
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(this.user);
		when(fileDetailsRepository.findAllByActionInAndType(Mockito.anyList(), Mockito.anyString(), Mockito.any())).thenReturn(this.fls);
		when(amazonS3.generatePresignedUrl(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(this.myURL);
		
		mockMvc.perform(get("/api/v1/skillsCatalogue/download")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andReturn();
	}

}
