package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.siemens.nextwork.admin.model.WorkstreamGids;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.repo.RolesRepository;
import com.siemens.nextwork.admin.repo.WorkStreamRepository;
import com.siemens.nextwork.admin.repo.WorkstreamGidRepository;
import com.siemens.nextwork.admin.service.ProjectKpiDownloadService;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.service.impl.DashboardReportDownloadServiceImpl;
import com.siemens.nextwork.admin.util.NextworkDateUtils;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
@SpringBootTest
 class DashboardReportDownloadControllerTest extends RestTestEnabler {

	@Autowired
	private ResourceLoader resourceLoader;
	
	@Mock
	private UserService userService;
	
	@Mock
	MongoOperations mongoOperations;
	
	@Mock
	private RolesRepository rolesRepository;
	
	@Mock
	private NextworkDateUtils nextworkDateUtils;
	
	@Mock
	private NextWorkUserRepository nextWorkUserRepository;
	@Mock
	private WorkStreamRepository workStreamRepository;
	
	@Mock
	private ProjectKpiDownloadService projectKpiDownloadService;
	@Mock
	private HttpServletRequest request;
	
	@Mock
	private HttpServletResponse response;
	@Mock
	private WorkstreamGidRepository workstreamGidRepository;
	
	@InjectMocks
	private DashboardReportDownloadServiceImpl dashboardReportDownloadServiceImpl;

	String userEmail = "abc@siemens.com";
	WorkstreamGids workstreamGids;
	@BeforeEach
	public void setup() {
		loadWorkStreamGids();
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
	 void verifyDashboardReportDownloadasExcel() throws Exception {
	
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(user);
		when(workStreamRepository.findById(Mockito.anyString())).thenReturn(pws);
		when(projectKpiDownloadService.claculateSkillGap(Mockito.any())).thenReturn(2);
		when(nextworkDateUtils.getFormattedDate(Mockito.any())).thenReturn("5/8/2023");
		when(workstreamGidRepository.findByWorkstreamId(Mockito.anyString())).thenReturn(workstreamGids);
		Assertions.assertNotNull(dashboardReportDownloadServiceImpl.getDashboardReportAsExcel(pws.get().getUid(), userEmail, false, response));

	}
	

}
