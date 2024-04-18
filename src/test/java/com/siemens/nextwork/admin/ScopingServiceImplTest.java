package com.siemens.nextwork.admin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.poi.hpsf.IllegalPropertySetDataException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.bson.Document;
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
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.test.context.ContextConfiguration;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.siemens.nextwork.admin.dto.GIDAggregratorDTO;
import com.siemens.nextwork.admin.dto.GIDRequestDTO;
import com.siemens.nextwork.admin.dto.IdDTO;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.NextWorkExcelMasterScopes;
import com.siemens.nextwork.admin.dto.NotesDTO;
import com.siemens.nextwork.admin.dto.ScopingDTO;
import com.siemens.nextwork.admin.dto.ScopingDataRequestListDTO;
import com.siemens.nextwork.admin.dto.VersionSummary;
import com.siemens.nextwork.admin.exception.ResourceNotFoundException;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.model.Scoping;
import com.siemens.nextwork.admin.model.ScopingVersions;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.repo.ScopingRepository;
import com.siemens.nextwork.admin.repo.ScopingVersionsRepository;
import com.siemens.nextwork.admin.service.FileService;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.service.impl.ScopingServiceImpl;
import com.siemens.nextwork.admin.util.NextworkConstants;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
@SpringBootTest
 class ScopingServiceImplTest extends RestTestEnabler {
	String versionId;
	String gid;
	GIDRequestDTO gidRequestDTO;
	List<ScopingDTO> data;
	Scoping scoping;
	FileDetails fileDetails;
	List<ScopingDataRequestListDTO> scopingDataRequestList;
	ScopingDataRequestListDTO scopingDataRequestListDTO;
	ScopingVersions scopingVersionLatest;
	List<Object> scopingVersionList;
	ScopingVersions scopingVersionOld;
	List<Object> scopingNewdata;
	List<Scoping> scopingPrevdata;
	Scoping scopingPrev;
	List<Object> scopingVersions;
	NotesDTO notesDTO;
	List<NotesDTO> notesList;
	Optional<FileDetails> fileDetailsOpt;
	List<FileDetails> files;
	List<ScopingDataRequestListDTO> scpingDataList;
	List<FileDetails> fls;
	FileDetails fDels;
	Optional<FileDetails> ofDels;
	Optional<NextWorkUser> user;

	@Mock
	private UserService userService;
	
	@Mock
	private NextWorkUserRepository nextWorkUserRepository;

	@Mock
	private ScopingRepository scopingRepository;

	@Mock
	MongoOperations mongoOperations;

	@Mock
	private FileDetailsRepository fileDetailsRepository;

	@Mock
	private ScopingVersionsRepository scopingVersionsRepository;

	@InjectMocks
	private ScopingServiceImpl scopingServiceImpl;

	@Mock
	private FileService fileService;
	
//	@Mock
//	private UserRepository userRepository;
//	
	
	@Autowired
	private ResourceLoader resourceLoader;

	String userEmail = "abc@siemens.com";
	String authorName ="PRIYANKA SAXENA";
	
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
		ofDels = Optional.of(fDels);
		
		versionId = "20230608";
		gid = "Z0041234";
		gidRequestDTO = new GIDRequestDTO();
		data = new ArrayList<>();
		scoping = new Scoping();
		scoping.setAre("ARE");
		scoping.setAreDesc("AREDesc");
		scoping.setBlueCollarWhiteCollar("Blue Collar");
		scoping.setBusinessUnit("BU");
		scoping.setCompany("abc");
		scoping.setCompanyDesc("Company Desc");
		scoping.setContractStatus("Active");
		scoping.setCountryRegionARE("CountryRegionARE");
		scoping.setCountryRegionPlaceOfAction("Country Region Place Of Action");
		scoping.setCountryRegionStateOffice("Country Region State Office");
		scoping.setCountryRegionSubEntity("Country Region Sub Entity");
		scoping.setDepthStructureKey("Depth Structure Key");
		scoping.setDepthStructureKeyDesc("Depth Structure Key Desc");
		scoping.setDivisionExternal("Division External");
		scoping.setDivisionExternalDesc("Div Ext Desc");
		scoping.setDivisionInternal("Division Internal");
		scoping.setDivisionInternalDesc("Div Internal Desc");
		scoping.setEmailId("abc@com");
		scoping.setGid("Z004A123");
		scoping.setGripPosition("Grip Pos");
		scoping.setGripPositionDesc("Grip Pos Desc");
		scoping.setIsVisible(true);
		scoping.setJobFamily("Job Family");
		scoping.setJobFamilyCategory("Job Family Category");
		scoping.setLocationOffice("Loc Office");
		scoping.setLocationOfficeCity("Loc Office City");
		scoping.setOrganizationClass("Org Class");
		scoping.setOrgCodePA("Org Code PA");
		scoping.setPositionType("Pos Type");
		scoping.setRegionalOrganization("Reg. Organization");
		scoping.setRegionalOrganizationDesc("Reg Organization Desc");
		scoping.setSubJobFamily("Sub Job Family");
		scoping.setUnit("Unit");
		scoping.setId("101");
		scoping.setVid("20230809");
		scopingNewdata = new ArrayList<>();
		scopingNewdata.add(scoping);

		scopingPrev = new Scoping();
		scopingPrev.setAre("ARE");
		scopingPrev.setAreDesc("AREDesc");
		scopingPrev.setBlueCollarWhiteCollar("Blue Collar");
		scopingPrev.setBusinessUnit("BU");
		scopingPrev.setCompany("abc");
		scopingPrev.setCompanyDesc("Company Desc");
		scopingPrev.setContractStatus("Active");
		scopingPrev.setId("102");
		scopingPrev.setVid("20230808");
		scopingPrev.setGid("Z00ABCD1");
		scopingPrevdata = new ArrayList<>();
		//scopingPrevdata.add
		scopingPrevdata.add(scopingPrev);

		data = setScopingData(data);
		gidRequestDTO.setAction("Update");
		gidRequestDTO.setData(data);
//		userPlatformRole = new RoleEntity();
//		userPlatformRole.setName("ADMIN");
		fileDetails = new FileDetails();
		fileDetails.setStatus("PASSED");
		fileDetails.setErrors("There are errors in file");
		VersionSummary summary = new VersionSummary();
		Map<String, Integer> addedmap = new HashMap<>();
		addedmap.put("gid", 2);
		addedmap.put("company", 2);
		addedmap.put("unit", 2);
		addedmap.put("businessUnit", 2);
		addedmap.put("are", 2);

		Map<String, Integer> removedmap = new HashMap<>();
		removedmap.put("gid", 2);
		removedmap.put("company", 2);
		removedmap.put("unit", 2);
		removedmap.put("businessUnit", 2);
		removedmap.put("are", 2);

		Map<String, Integer> modifiedmap = new HashMap<>();
		modifiedmap.put("gid", 2);
		modifiedmap.put("company", 2);
		modifiedmap.put("unit", 2);

		summary.setAddedFields(addedmap);
		summary.setModifiedFields(modifiedmap);
		summary.setRemovedFields(removedmap);
		
		notesDTO = new NotesDTO();
		notesDTO.setAuthor("PRIYANKA SAXENA");
		notesDTO.setEmailId("abc@siemens.com");
		notesDTO.setNote("Note");
		notesList = new ArrayList<>();
		notesList.add(notesDTO);

		scopingDataRequestList = new ArrayList<>();
		scopingDataRequestListDTO = new ScopingDataRequestListDTO();
		scopingDataRequestListDTO.setAction("Append");
		scopingDataRequestListDTO.setGid(gid);
		scopingDataRequestListDTO.setIndex(1);
		scopingDataRequestListDTO.setData(data);
		scopingDataRequestList.add(scopingDataRequestListDTO);
		scopingVersionLatest = new ScopingVersions();
		scopingVersionLatest.setLatest(true);
		scopingVersionLatest.setUploadTime(new Date(System.currentTimeMillis()));
		scopingVersionLatest.setPreviousVid("20230602");
		scopingVersionLatest.setSummary(summary);
		scopingVersionLatest.setVid(versionId);
		scopingVersions = new ArrayList<>();
		scopingVersions.add(scopingVersionLatest);

		scopingVersionList = new ArrayList<>();
		scopingVersionOld = new ScopingVersions();
		scopingVersionOld.setLatest(false);
		scopingVersionOld.setUploadTime(new Date(System.currentTimeMillis()));
		scopingVersionOld.setVid(versionId);
		scopingVersionList.add(scopingVersionOld);
		fileDetailsOpt = Optional.of(fileDetails);
		when(userService.findUserIdByEmail(Mockito.any())).thenReturn(gid);
		
		files = new ArrayList<>();
		files.add(fileDetails);		
	}

	private List<ScopingDTO> setScopingData(List<ScopingDTO> data) {

		ScopingDTO scopinggid = new ScopingDTO();
		scopinggid.setFieldName("gid");
		scopinggid.setFieldData("Z0041234");
		data.add(scopinggid);

		ScopingDTO scopingEmailId = new ScopingDTO();
		scopingEmailId.setFieldName("emailId");
		scopingEmailId.setFieldData("abc@com");
		data.add(scopingEmailId);

		ScopingDTO scopingContractStatus = new ScopingDTO();
		scopingContractStatus.setFieldName("contractStatus");
		scopingContractStatus.setFieldData("Active");
		data.add(scopingContractStatus);

		ScopingDTO scopingARE = new ScopingDTO();
		scopingARE.setFieldName("are");
		scopingARE.setFieldData("500E");
		data.add(scopingARE);

		ScopingDTO scopingARETEXT = new ScopingDTO();
		scopingARETEXT.setFieldName("areDesc");
		scopingARETEXT.setFieldData("ARE Description");
		data.add(scopingARETEXT);

		ScopingDTO scopingCompany = new ScopingDTO();
		scopingCompany.setFieldName("company");
		scopingCompany.setFieldData("abc");
		data.add(scopingCompany);

		ScopingDTO scopingCompanyText = new ScopingDTO();
		scopingCompanyText.setFieldName("companyDesc");
		scopingCompanyText.setFieldData("abc Company");
		data.add(scopingCompanyText);

		ScopingDTO scopingDivisionExternal = new ScopingDTO();
		scopingDivisionExternal.setFieldName("divisionExternal");
		scopingDivisionExternal.setFieldData("division External");
		data.add(scopingDivisionExternal);

		ScopingDTO scopingDivisionExternalText = new ScopingDTO();
		scopingDivisionExternalText.setFieldName("divisionExternalDesc");
		scopingDivisionExternalText.setFieldData("division External Description");
		data.add(scopingDivisionExternalText);

		ScopingDTO scopingDivisionInternal = new ScopingDTO();
		scopingDivisionInternal.setFieldName("divisionInternal");
		scopingDivisionInternal.setFieldData("division Internal");
		data.add(scopingDivisionInternal);

		ScopingDTO scopingDivisionInternalText = new ScopingDTO();
		scopingDivisionInternalText.setFieldName("divisionInternalDesc");
		scopingDivisionInternalText.setFieldData("division Internal Description");
		data.add(scopingDivisionInternalText);

		ScopingDTO scopingBusinessUnit = new ScopingDTO();
		scopingBusinessUnit.setFieldName("businessUnit");
		scopingBusinessUnit.setFieldData("Business Unit");
		data.add(scopingBusinessUnit);

		ScopingDTO scopingDepthStructureKey = new ScopingDTO();
		scopingDepthStructureKey.setFieldName("depthStructureKey");
		scopingDepthStructureKey.setFieldData("Depth Structure Key");
		data.add(scopingDepthStructureKey);

		ScopingDTO scopingDepthStructureKeyText = new ScopingDTO();
		scopingDepthStructureKeyText.setFieldName("depthStructureKeyDesc");
		scopingDepthStructureKeyText.setFieldData("Depth Structure Key Description");
		data.add(scopingDepthStructureKeyText);

		ScopingDTO scopingOrgCodePA = new ScopingDTO();
		scopingOrgCodePA.setFieldName("orgCodePA");
		scopingOrgCodePA.setFieldData("Org Code PA");
		data.add(scopingOrgCodePA);

		ScopingDTO scopingOrganizationClass = new ScopingDTO();
		scopingOrganizationClass.setFieldName("organizationClass");
		scopingOrganizationClass.setFieldData("Organization Class");
		data.add(scopingOrganizationClass);

		ScopingDTO scopingUnit = new ScopingDTO();
		scopingUnit.setFieldName("unit");
		scopingUnit.setFieldData("Unit");
		data.add(scopingUnit);

		ScopingDTO scopingJobFamilyCategory = new ScopingDTO();
		scopingJobFamilyCategory.setFieldName("jobFamilyCategory");
		scopingJobFamilyCategory.setFieldData("Job Family Category");
		data.add(scopingJobFamilyCategory);

		ScopingDTO scopingJobFamily = new ScopingDTO();
		scopingJobFamily.setFieldName("jobFamily");
		scopingJobFamily.setFieldData("Job Family");
		data.add(scopingJobFamily);

		ScopingDTO scopingSubJobFamily = new ScopingDTO();
		scopingSubJobFamily.setFieldName("subJobFamily");
		scopingSubJobFamily.setFieldData("Sub Job Family");
		data.add(scopingSubJobFamily);

		ScopingDTO scopingPositionType = new ScopingDTO();
		scopingPositionType.setFieldName("positionType");
		scopingPositionType.setFieldData("SE");
		data.add(scopingPositionType);

		ScopingDTO scopingGripPosition = new ScopingDTO();
		scopingGripPosition.setFieldName("gripPosition");
		scopingGripPosition.setFieldData("Grip Position");
		data.add(scopingGripPosition);

		ScopingDTO scopingGripPositionText = new ScopingDTO();
		scopingGripPositionText.setFieldName("gripPositionDesc");
		scopingGripPositionText.setFieldData("Grip Position Description");
		data.add(scopingGripPositionText);

		ScopingDTO scopingGripRegionalOrganization = new ScopingDTO();
		scopingGripRegionalOrganization.setFieldName("regionalOrganization");
		scopingGripRegionalOrganization.setFieldData("Regional Organization");
		data.add(scopingGripRegionalOrganization);

		ScopingDTO scopingGripRegionalOrganizationText = new ScopingDTO();
		scopingGripRegionalOrganizationText.setFieldName("regionalOrganizationDesc");
		scopingGripRegionalOrganizationText.setFieldData("Regional Organization Description");
		data.add(scopingGripRegionalOrganizationText);

		ScopingDTO scopingCountryRegionAre = new ScopingDTO();
		scopingCountryRegionAre.setFieldName("countryRegionARE");
		scopingCountryRegionAre.setFieldData("Country Region ARE");
		data.add(scopingCountryRegionAre);

		ScopingDTO scopingCountryRegionPlaceOfAction = new ScopingDTO();
		scopingCountryRegionPlaceOfAction.setFieldName("countryRegionPlaceOfAction");
		scopingCountryRegionPlaceOfAction.setFieldData("Country Region Place Of Action");
		data.add(scopingCountryRegionPlaceOfAction);

		ScopingDTO scopingCountryRegionStateOffice = new ScopingDTO();
		scopingCountryRegionStateOffice.setFieldName("countryRegionStateOffice");
		scopingCountryRegionStateOffice.setFieldData("Country Region State Office");
		data.add(scopingCountryRegionStateOffice);

		ScopingDTO scopingLocationOfficeCity = new ScopingDTO();
		scopingLocationOfficeCity.setFieldName("locationOfficeCity");
		scopingLocationOfficeCity.setFieldData("Location Office City");
		data.add(scopingLocationOfficeCity);

		ScopingDTO scopingLocationOffice = new ScopingDTO();
		scopingLocationOffice.setFieldName("locationOffice");
		scopingLocationOffice.setFieldData("Location Office");
		data.add(scopingLocationOffice);

		ScopingDTO scopingCountryRegionSubEntity = new ScopingDTO();
		scopingCountryRegionSubEntity.setFieldName("countryRegionSubEntity");
		scopingCountryRegionSubEntity.setFieldData("Country Region Sub Entity");
		data.add(scopingCountryRegionSubEntity);

		ScopingDTO scopingBlueCollarWhiteCollar = new ScopingDTO();
		scopingBlueCollarWhiteCollar.setFieldName("blueCollarWhiteCollar");
		scopingBlueCollarWhiteCollar.setFieldData("White Color");
		data.add(scopingBlueCollarWhiteCollar);

		return data;
	}

	@Test
	 void updateGID() throws Exception {
		when(fileDetailsRepository.findAllByVersionAndStatus( Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(files);
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scoping);
		
		scopingServiceImpl.updateGID(gidRequestDTO, versionId, userEmail, gid, NextworkConstants.FILE_UPLOAD_PUT_TOOL);
		verify(userService, times(1)).findUserIdByEmail(userEmail);

	}

	@Test
	 void updateGIDActivate() throws Exception {
		gidRequestDTO.setAction("Activate");
		when(fileDetailsRepository.findAllByVersionAndStatus( Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(files);
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scoping);
		scopingServiceImpl.updateGID(gidRequestDTO, versionId, userEmail, gid, NextworkConstants.FILE_UPLOAD_PUT_TOOL);
		verify(userService, times(1)).findUserIdByEmail(userEmail);

	}

	@Test
	 void updateGIDDeactivate() throws Exception {
		gidRequestDTO.setAction("Deactivate");
		when(fileDetailsRepository.findAllByVersionAndStatus( Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(files);
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scoping);
		scopingServiceImpl.updateGID(gidRequestDTO, versionId, userEmail, gid, NextworkConstants.FILE_UPLOAD_PUT_TOOL);
		verify(userService, times(1)).findUserIdByEmail(userEmail);

	}

	@Test
	 void updateGIDException() throws Exception {
		when(fileDetailsRepository.findAllByVersionAndStatus( Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(files);
		Assertions.assertThrows(RestBadRequestException.class, () -> scopingServiceImpl.updateGID(gidRequestDTO, versionId, userEmail, gid, NextworkConstants.FILE_UPLOAD_PUT_TOOL));

	}

	@Test
	 void searchGID() throws Exception {

		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scoping);
		Assertions.assertNotNull(scopingServiceImpl.searchGID(gid, userEmail, data));

	}
	
	@Test
	 void searchGIDNullFields() throws Exception {
		scoping = new Scoping();
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scoping);
		Assertions.assertNotNull(scopingServiceImpl.searchGID(gid, userEmail, data));

	}
	
	@Test
	 void searchGIDScopingNull() throws Exception {
		Assertions.assertThrows(RestBadRequestException.class, () -> scopingServiceImpl.searchGID(gid, userEmail, data));

	}

	@Test
	 void getAsyncJobProfileStatus() throws Exception {
		when(fileDetailsRepository.findById(Mockito.any())).thenReturn(fileDetailsOpt);
		Assertions.assertNotNull(scopingServiceImpl.getAsyncJobProfileStatus(userEmail, gid));

	}
	
	@Test
	 void getAsyncJobProfileStatusException() throws Exception {

		Assertions.assertThrows(ResourceNotFoundException.class, () -> scopingServiceImpl.getAsyncJobProfileStatus(userEmail, gid));

	}

	@Test
	 void getAllVersionsTest() throws Exception {
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scopingVersionLatest);
		when(mongoOperations.find(Mockito.any(), Mockito.any())).thenReturn(scopingVersionList);
		Assertions.assertNotNull(scopingServiceImpl.getAllVersions(userEmail));

	}

	@Test
	 void publishUnpublishScopingMasterDataTest() throws Exception {
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scopingVersionLatest);
		when(mongoOperations.find(Mockito.any(), Mockito.any())).thenReturn(scopingVersionList);
		scopingServiceImpl.publishUnpublishScopingMasterData(versionId, userEmail, "Unpublish");
		verify(userService, times(1)).checkAdminUserRole(userEmail);

	}

	@Test
	 void publishUnpublishScopingMasterDataException() throws Exception {
		Assertions.assertThrows(RestBadRequestException.class, () -> scopingServiceImpl.publishUnpublishScopingMasterData(versionId, userEmail, "Unpublish"));

	}

	@Test
	 void getVersionSummary() throws Exception {
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scopingVersionLatest);
		Assertions.assertNotNull(scopingServiceImpl.getVersionSummaryByVersionId(versionId, userEmail));

	}
	
	@Test
	 void getVersionSummaryScopingDataNull() throws Exception {
		scopingVersionLatest.setSummary(null);
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scopingVersionLatest);
		Assertions.assertThrows(RestBadRequestException.class, () -> scopingServiceImpl.getVersionSummaryByVersionId(versionId, userEmail));

	}
	
	@Test
	 void getVersionSummaryScopingSummaryNull() throws Exception {
		Assertions.assertThrows(RestBadRequestException.class, () -> scopingServiceImpl.getVersionSummaryByVersionId(versionId, userEmail));

	}

	@Test
	 void addNewNote() throws Exception {
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scopingVersionLatest);
		Assertions.assertNotNull(scopingServiceImpl.addNote(notesDTO, versionId, userEmail, authorName));

	}
	
	@Test
	 void addNewNoteInNonEmptyList() throws Exception {
		scopingVersionLatest.setNotes(notesList);
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scopingVersionLatest);
		Assertions.assertNotNull(scopingServiceImpl.addNote(notesDTO, versionId, userEmail, authorName));

	}
	@Test
	 void addExistingNote() throws Exception {
		scopingVersionLatest.setNotes(notesList);
		notesDTO.setIndex(0);
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scopingVersionLatest);
		
		Assertions.assertNotNull(scopingServiceImpl.addNote(notesDTO, versionId, userEmail, authorName));

	}

	@Test
	 void addNoteException() throws Exception {
		Assertions.assertThrows(ResourceNotFoundException.class, () -> scopingServiceImpl.addNote(notesDTO, versionId, userEmail, authorName));

	}
	
	@Test
	 void getNotesException() throws Exception {
		Assertions.assertThrows(ResourceNotFoundException.class, () -> scopingServiceImpl.getNotes(versionId, userEmail));

	}
	
	@Test
	 void getNotes() throws Exception {
		scopingVersionLatest.setNotes(notesList);
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scopingVersionLatest);
		Assertions.assertNotNull(scopingServiceImpl.getNotes(versionId, userEmail));

	}
	
	@Test
	 void getNotesNoteNotAvailable() throws Exception {
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scopingVersionLatest);
		Assertions.assertNotNull(scopingServiceImpl.getNotes(versionId, userEmail));

	}
	
	@Test
	 void publishUnpublishScopingMasterDataPublishTest() throws Exception {
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scopingVersionLatest);
		when(mongoOperations.find(Mockito.any(), Mockito.any())).thenReturn(scopingVersionList);
		versionId = "20230609";
		scopingServiceImpl.publishUnpublishScopingMasterData(versionId, userEmail, "Publish");
		verify(userService, times(1)).checkAdminUserRole(userEmail);

	}
	
	@Test
	 void publishUnpublishScopingMasterDataPublishDateFormatExceptionTest() throws Exception {
		when(mongoOperations.findOne(Mockito.any(), Mockito.any())).thenReturn(scopingVersionLatest);
		when(mongoOperations.find(Mockito.any(), Mockito.any())).thenReturn(scopingVersionList);
		versionId = "2023/06/09";
		scopingServiceImpl.publishUnpublishScopingMasterData(versionId, userEmail, "Publish");
		verify(userService, times(1)).checkAdminUserRole(userEmail);

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
	        scopingPrevdata = scps;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return scopingPrevdata;
	}

	
	@Test
	 void saveExcelPOITOMongoTest() throws IOException, SAXException, OpenXML4JException {
		Resource resource = resourceLoader.getResource("classpath:DataExample_DummyData.xlsb");
		InputStream inputStream = null;
		try {
			inputStream = resource.getInputStream();
		} catch (IOException e) {
			
		}
		NextWorkExcelMasterScopes dto = NextWorkExcelMasterScopes.builder().are("400E").areDesc("SPDL GmbH, Wien").build();
		List<NextWorkExcelMasterScopes> dtoSet = new ArrayList<>();
		dtoSet.add(dto);
		Collection<NextWorkExcelMasterScopes> dtoBeans = dtoSet;
		
		when(mongoOperations.insert(Mockito.<List<NextWorkExcelMasterScopes>>any(), Mockito.anyString())).thenReturn(dtoBeans);
		
		int cnt = scopingServiceImpl.saveExcelPOIToMongo(inputStream, versionId, "mongo_temp", Boolean.TRUE, "");
		boolean chk = cnt > 0;
		Assertions.assertEquals(true, chk);
	}
	
	
	@Test
	 void processScopingDuplicateDataTest() throws IOException, OpenXML4JException, SAXException {
		Resource resource = resourceLoader.getResource("classpath:DataExample_DummyData.xlsb");
		InputStream inputStream = null;
		try { inputStream = resource.getInputStream(); } catch (IOException e) { }
		NextWorkExcelMasterScopes dto = NextWorkExcelMasterScopes.builder().are("400E").areDesc("SPDL GmbH, Wien").build();
		List<NextWorkExcelMasterScopes> dtoSet = new ArrayList<>();
		dtoSet.add(dto);
		Collection<NextWorkExcelMasterScopes> dtoBeans = dtoSet;
		
		String uid = UUID.randomUUID().toString();
		List<GIDAggregratorDTO> gidAgsList = new ArrayList<>();
		GIDAggregratorDTO gidAgr = GIDAggregratorDTO.builder().gid("ACV00001").count(2).build();
		gidAgsList.add(gidAgr);
		AggregationResults<GIDAggregratorDTO> gidAggregate = new AggregationResults<>(gidAgsList, new Document());
		
		when(mongoOperations.insert(Mockito.<List<NextWorkExcelMasterScopes>>any(), Mockito.anyString())).thenReturn(dtoBeans);
		when(mongoOperations.aggregate(Mockito.any(Aggregation.class), Mockito.anyString(), Mockito.eq(GIDAggregratorDTO.class))).thenReturn(gidAggregate);
		
		InputStream iStream = inputStream;
		Assertions.assertThrows(IllegalPropertySetDataException.class, () -> scopingServiceImpl.processScopingData(iStream, versionId, uid, NextworkConstants.FILE_UPLOAD_POST_CREATE));
	}

	@Test
	void processScopingDuplicateRetryDataTest() throws IOException, OpenXML4JException, SAXException {
		Resource resource = resourceLoader.getResource("classpath:DataExample_DummyData.xlsb");
		InputStream inputStream = null;
		try { inputStream = resource.getInputStream(); } catch (IOException e) { }
		NextWorkExcelMasterScopes dto = NextWorkExcelMasterScopes.builder().are("400E").areDesc("SPDL GmbH, Wien").build();
		List<NextWorkExcelMasterScopes> dtoSet = new ArrayList<>();
		dtoSet.add(dto);
		Collection<NextWorkExcelMasterScopes> dtoBeans = dtoSet; 

		String uid = UUID.randomUUID().toString();
		List<GIDAggregratorDTO> gidAgsList = new ArrayList<>();
		AggregationResults<GIDAggregratorDTO> gidAggregate = new AggregationResults<>(gidAgsList, new Document());

		when(mongoOperations.insert(Mockito.<List<NextWorkExcelMasterScopes>>any(), Mockito.anyString())).thenReturn(dtoBeans);
		when(mongoOperations.aggregate(Mockito.any(Aggregation.class), Mockito.anyString(), Mockito.eq(GIDAggregratorDTO.class))).thenReturn(gidAggregate);

		InputStream iStream = inputStream;
		Assertions.assertNotNull(scopingServiceImpl.processScopingData(iStream, versionId, uid, NextworkConstants.FILE_UPLOAD_POST_CREATE));
	}
	
	@Test
	 void processScopingDataTest() throws IOException, OpenXML4JException, SAXException {
		Resource resource = resourceLoader.getResource("classpath:DataExample_DummyData.xlsb");
		InputStream inputStream = null;
		try { inputStream = resource.getInputStream(); } catch (IOException e) { }
		NextWorkExcelMasterScopes dto = NextWorkExcelMasterScopes.builder().are("400E").areDesc("SPDL GmbH, Wien").build();
		List<NextWorkExcelMasterScopes> dtoSet = new ArrayList<>();
		dtoSet.add(dto);
		Collection<NextWorkExcelMasterScopes> dtoBeans = dtoSet;
		
		String uid = UUID.randomUUID().toString();
		List<GIDAggregratorDTO> gidAgsList = new ArrayList<>();
		AggregationResults<GIDAggregratorDTO> gidAggregate = new AggregationResults<>(gidAgsList, new Document());
		
		when(mongoOperations.insert(Mockito.<List<NextWorkExcelMasterScopes>>any(), Mockito.anyString())).thenReturn(dtoBeans);
		when(mongoOperations.aggregate(Mockito.any(Aggregation.class), Mockito.anyString(), Mockito.eq(GIDAggregratorDTO.class))).thenReturn(gidAggregate);
		
		
		scopingServiceImpl.processScopingData(inputStream, versionId, uid, NextworkConstants.FILE_UPLOAD_POST_CREATE);
		Assertions.assertNotNull(versionId);
	}
	
	@Test
	 void searchGIDTest() {
		List<ScopingDataRequestListDTO> scpingDataRqList= loadUIDataScoping();
		List<ScopingDTO> searchGIDResponseDTOList = scpingDataRqList.get(0).getData();
		String id = UUID.randomUUID().toString();
		scopingPrevdata = loadScoping();
		when(mongoOperations.findOne(Mockito.any(), Mockito.eq(Scoping.class))).thenReturn(scopingPrevdata.get(0));
		when(nextWorkUserRepository.findByUserEmail(Mockito.anyString())).thenReturn(user);
		
		List<ScopingDTO> resp = scopingServiceImpl.searchGID(id, userEmail, searchGIDResponseDTOList);
		Assertions.assertNotNull(resp);
	}
	
	private List<ScopingDataRequestListDTO> loadUIDataScoping() {
		Resource resource = resourceLoader.getResource("classpath:/test_data/gid_data.json");
		try {
	        ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
			JavaTimeModule javaTimeModule = new JavaTimeModule();
	        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.getDefault())));
			javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())));
			
	        mapper.registerModule(javaTimeModule);
	        mapper.registerModule(new Jdk8Module());
	        TypeReference<List<ScopingDataRequestListDTO>> ref = new TypeReference<>() {};
	        List<ScopingDataRequestListDTO> scps = (ArrayList<ScopingDataRequestListDTO>) mapper.readValue(resource.getInputStream(), ref);
	        scpingDataList = scps;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return scpingDataList;
	}
	
	@Test
	 void appendScopingDataTest() throws IOException, OpenXML4JException, SAXException {
		List<ScopingDataRequestListDTO> scpingDataRqList= loadUIDataScoping();
		Date d = new Date();
		ScopingVersions scp1 = ScopingVersions.builder().latest(Boolean.TRUE).previousVid("20230102").uploadTime(d).vid("20230101").versionStatus("PUBLISHED").build();
		List<ScopingVersions> scopingVersionList = new ArrayList<>();
		scopingVersionList.add(scp1);
		Scoping ssp = null;
		scopingPrevdata = loadScoping();

		when(fileDetailsRepository.findAllByVersionAndStatus(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(fls);
		when(mongoOperations.find(Mockito.any(), Mockito.eq(ScopingVersions.class))).thenReturn(scopingVersionList);
		when(mongoOperations.findOne(Mockito.any(), Mockito.eq(Scoping.class))).thenReturn(ssp);
		when(scopingRepository.saveAll(Mockito.anyList())).thenReturn(scopingPrevdata);
		when(nextWorkUserRepository.findById(Mockito.anyString())).thenReturn(user);
		when(fileDetailsRepository.findAllByVersionAndAction(Mockito.anyString(), Mockito.anyString())).thenReturn(fls);
		when( fileDetailsRepository.save(Mockito.any())).thenReturn(fls.get(0));
		
		IdResponseDTO resp = scopingServiceImpl.appendScopingData(scpingDataRqList, "20230101", userEmail, NextworkConstants.FILE_UPLOAD_PUT_APPEND, "localhost");
		Assertions.assertNotNull(resp);
	}
	
	
	
	
	@Test
	 void addScopingVersionTest() {
		Date d = new Date();
		ScopingVersions scp1 = ScopingVersions.builder().latest(Boolean.TRUE).previousVid("20230102").uploadTime(d).vid("20230101").versionStatus("PUBLISHED").build();
		List<ScopingVersions> scopingVersionList = new ArrayList<>();
		scopingVersionList.add(scp1);
		
		List<IdDTO> ids = new ArrayList<>();
		IdDTO id1 = IdDTO.builder().gid(gid).nullFields("divisionExternal,depthStructureKeyDesc,positionType").build();
		ids.add(id1);
		
		List<IdDTO> pIds = new ArrayList<>();
		IdDTO id2 = IdDTO.builder().gid("ABC00001").nullFields("depthStructureKeyDesc,positionType").build();
		pIds.add(id2);

		List<GIDAggregratorDTO> gidAgsList = new ArrayList<>();
		AggregationResults<GIDAggregratorDTO> gidAggregate = new AggregationResults<>(gidAgsList, new Document());
		
		scopingPrevdata = loadScoping();
		
		
		when(mongoOperations.findOne(Mockito.any(), Mockito.eq(ScopingVersions.class))).thenReturn(scopingVersionList.get(0));
		when(scopingVersionsRepository.save(Mockito.any(ScopingVersions.class))).thenReturn(scopingVersionList.get(0));
		when(scopingVersionsRepository.save(Mockito.any(ScopingVersions.class))).thenReturn(scopingVersionList.get(0));
		when(mongoOperations.aggregate(Mockito.any(Aggregation.class), Mockito.anyString(), Mockito.eq(GIDAggregratorDTO.class))).thenReturn(gidAggregate);
	
		when(scopingRepository.findAllGIdsByVersion(Mockito.anyString())).thenReturn(ids);
		when(scopingRepository.findAllGidsAndNullFiledsByVersion(Mockito.anyString())).thenReturn(pIds);
		when(scopingRepository.findAllByVidInAndGidIn(Mockito.anyList(), Mockito.anyList(), Mockito.any())).thenReturn(scopingPrevdata);
		
		String version = "20230101";
		scopingServiceImpl.addScopingVersion(version, d);
		Assertions.assertNotNull(version);
	}
	
	
	@Test
	 void appendToolDataTest() {
		scopingPrevdata = loadScoping();
		when(fileDetailsRepository.findById(Mockito.anyString())).thenReturn(ofDels);
		when(fileDetailsRepository.findAllByVersion(Mockito.anyString(), Mockito.any())).thenReturn(fls);
		when(scopingRepository.findAllByVidAndIsCaptured(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(scopingPrevdata);
		when(scopingRepository.saveAll(Mockito.anyList())).thenReturn(scopingPrevdata);
		
		String version = "20230101";
		String id = UUID.randomUUID().toString();
		scopingServiceImpl.appendToolData(id, version, "localhost");
		Assertions.assertNotNull(version);
	}
	
	@Test
	 void retryReplicaDataTest() throws InvalidFormatException, IOException {
		Optional<FileDetails> file = scopingServiceImpl.retryReplicaData(UUID.randomUUID().toString());
		assertEquals(Boolean.TRUE, file.isEmpty());
	}

}

