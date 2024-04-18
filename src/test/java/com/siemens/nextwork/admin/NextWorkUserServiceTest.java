package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;

import com.siemens.nextwork.admin.dto.GidsRequestDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserPutRequestDTO;
import com.siemens.nextwork.admin.dto.RoleDetailDTO;
import com.siemens.nextwork.admin.dto.WorkStreamDTO;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.model.GidData;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.model.Scoping;
import com.siemens.nextwork.admin.model.ScopingVersions;
import com.siemens.nextwork.admin.model.Workstream;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.repo.ScopingVersionsRepository;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.service.impl.NextWorkUserServiceImpl;

import io.jsonwebtoken.Jwts;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
 class NextWorkUserServiceTest extends RestTestEnabler {

	@Mock
	MongoOperations mongoOperations;

	@Mock
	private ScopingVersionsRepository scopingVersionsRepository;

	@Mock
	private NextWorkUserRepository nextWorkUserRepository;

	@Mock
	private UserService userService;

	@InjectMocks
	private NextWorkUserServiceImpl nextWorkServiceImpl;

	@Mock
	private HttpServletRequest request;

	String dummyToken = Jwts.builder().claim("email", "ujjwal.kushwaha@siemens.com").compact();
	String authorization = "bearer " + dummyToken;
	String myToken = Jwts.builder().claim("email", "syam-kumar.vutukuri.ext@siemens.com").compact();
	String myAuthorization = "bearer " + myToken;
	String incorrectDummyToken = Jwts.builder().claim("email", "unknonw@user.com").compact();
	String incorrectAuthorization = "bearer " + incorrectDummyToken;
	String forbiddenDummyToken = Jwts.builder().claim("email", "piyushkumar@siemens.com").compact();
	String forbiddenAuthorization = "bearer " + forbiddenDummyToken;

	String userEmail = "abc@siemens.com";
	NextWorkUserPutRequestDTO userRequestDTO;
	NextWorkUserPutRequestDTO userRequestDTO1;
	String roleName = "ADMIN";
	String id = "id01";
	String actionGidList = "gidInclusion";
	String actionRoleList = "roleInclusion";;
	String actionWSList = "wsInclusion";
	String versionId = "20230503";
	String gidString = "z00A1234";
	NextWorkUser nextWorkUser;
	NextWorkUser nextWorkUser1;
	Optional<NextWorkUser> nextWorkUserOpt;
	List<Scoping> scopingDataList;
	Scoping scoping;
	List<Workstream> workStreamList;
	List<NextWorkUser> nextWorkUserList;
	Workstream workstream;
	Optional<Workstream> workstreamOpt;
	Optional<Workstream> workstreamOpt1;
	Optional<ScopingVersions> scopeVersionOpt;
	ScopingVersions scopingVersion;
	List<WorkStreamDTO> workstreamIds = new ArrayList<>();
	WorkStreamDTO workstreamDTO;
	List<GidData> gidList;
	List<String> gidList1;
	GidData gidData;
	List<String> workstreamIdList = new ArrayList<>();
	List<Roles> roleDetailList1 = new ArrayList<>();
	Roles role;

	@BeforeEach
	public void setup() {
		userRequestDTO = new NextWorkUserPutRequestDTO();
		userRequestDTO.setId("id01");
		userRequestDTO.setStatus("Active");
		userRequestDTO1 = new NextWorkUserPutRequestDTO();
		userRequestDTO1.setId("id01");
		userRequestDTO1.setStatus("Active");
		List<RoleDetailDTO> roleDetailList = new ArrayList<>();
		RoleDetailDTO roleDetailDTO = new RoleDetailDTO();
		roleDetailDTO.setRoleId("id01");
		roleDetailDTO.setRoleDisplayName("PUSer");
		roleDetailDTO.setRoleType("ADMIN");
		roleDetailList.add(roleDetailDTO);
		userRequestDTO.setRolesDetails(roleDetailList);
		userRequestDTO1.setRolesDetails(roleDetailList);
		List<GidsRequestDTO> gidReqList = new ArrayList<>();
		GidsRequestDTO gid = new GidsRequestDTO();
		gid.setGid("z00A1234");
		gid.setIndex(1);
		gidReqList.add(gid);
		userRequestDTO.setGids(gidReqList);

		List<WorkStreamDTO> ws = new ArrayList<>();
		WorkStreamDTO workStreamDTO = new WorkStreamDTO();
		workStreamDTO.setUid(id);
		workStreamDTO.setName("ws1");
		ws.add(workStreamDTO);
		userRequestDTO.setWorkstreamList(ws);
		userRequestDTO.setScopingVersion(actionGidList);

		nextWorkUser = new NextWorkUser();
		nextWorkUser.setStatus("Active");
		nextWorkUser.setEmail(userEmail);
		nextWorkUser.setId(id);

		nextWorkUser1 = new NextWorkUser();
		nextWorkUser1.setStatus("ACCEPTED");
		nextWorkUser1.setEmail(userEmail);
		nextWorkUser1.setId(id);

		role = new Roles();
		role.setRoleDescription("Test");
		role.setRoleType("ADMIN");
		role.setRoleDisplayName("PUSer");
		roleDetailList1.add(role);
		nextWorkUser.setRolesDetails(roleDetailList1);
		nextWorkUser.setWorkStreamList(ws);
		nextWorkUserList = new ArrayList<>();
		workStreamList = new ArrayList<>();
		nextWorkUserOpt = Optional.of(nextWorkUser);

		scoping = new Scoping();
		scoping.setAre("are");
		scoping.setAreDesc("areDesc");
		scoping.setBlueCollarWhiteCollar("White collar");
		scoping.setBusinessUnit("Business Unit");
		scoping.setContractStatus("Active");
		scoping.setCountryRegionARE("Country Region ARE");
		scoping.setCountryRegionPlaceOfAction("Country Region Place Of Action");
		scoping.setCountryRegionStateOffice("Country Region State Office");
		scoping.setGid("z00A1234");
		scoping.setGripPosition("grip01");
		scoping.setGripPositionDesc("gripDesc");
		scoping.setJobFamily("job Family");
		scoping.setLocationOffice("location office");
		scoping.setLocationOfficeCity("location office city");
		scoping.setOrgCodePA("org code");
		scoping.setSubJobFamily("sub job family");
		scoping.setVid("20230503");
		scoping.setGid("z00A1234");
		//scopingDataList.add(scoping);
		nextWorkUserList.add(nextWorkUser);

		when(userService.findByEmail(Mockito.any())).thenReturn(id);
		when(nextWorkUserRepository.findUserEntityByEmail(userEmail)).thenReturn(nextWorkUserOpt);

	}

	//@Test
	public void updateNextWorkTest() {
		Query q = new Query();
		q.addCriteria(Criteria.where("id").is("id01"));
		when(mongoOperations.findOne(q, NextWorkUser.class)).thenReturn(nextWorkUser);
		when(userService.findById(Mockito.any())).thenReturn(nextWorkUserOpt);

		q = new Query();
		q.addCriteria(Criteria.where("id").is("id01"));
		when(mongoOperations.findOne(q, Roles.class)).thenReturn(role);
		userEmail = "abc.com";
		q = new Query();
		q.addCriteria(Criteria.where("uid").is("id01"));
		when(mongoOperations.findOne(q, Workstream.class)).thenReturn(workstream);
		GidsRequestDTO gidReq = new GidsRequestDTO();
		gidReq.setGid("z00A1234");
		gidReq.setIndex(1);

		q = new Query();
		q.addCriteria(Criteria.where("gid").regex("^" + gidReq.getGid().toLowerCase(), "i"));
		when(mongoOperations.findOne(q, Scoping.class)).thenReturn(scoping);

		Assertions.assertNotNull(nextWorkServiceImpl.updateUserById(userEmail, id, userRequestDTO, actionGidList,
				actionWSList, actionRoleList));

	}

//	@Test(expected = RestBadRequestException.class)
	public void updateNextWork2Test() {
		Query q = new Query();
		q.addCriteria(Criteria.where("id").is("id01"));
		when(mongoOperations.findOne(q, NextWorkUser.class)).thenReturn(nextWorkUser);
		when(userService.findById(Mockito.any())).thenReturn(nextWorkUserOpt);
		q = new Query();
		q.addCriteria(Criteria.where("id").is("id01"));
		when(mongoOperations.findOne(q, Roles.class)).thenReturn(role);
		userEmail = "abc.com";
		q = new Query();
		q.addCriteria(Criteria.where("uid").is("ws01"));
		when(mongoOperations.findOne(q, Workstream.class)).thenReturn(workstream);
		GidsRequestDTO gidReq = new GidsRequestDTO();
		gidReq.setGid("z00A1234");
		gidReq.setIndex(1);
		q = new Query();
		q.addCriteria(Criteria.where("gid").regex("^" + gidReq.getGid().toLowerCase(), "i"));
		when(mongoOperations.findOne(q, Scoping.class)).thenReturn(scoping);
		nextWorkServiceImpl.updateUserById(userEmail, id, userRequestDTO, actionGidList, actionWSList, "roleExclusion");

	}

	//@Test
	public void updateNextWorkExclusionSuccessTest() {
		Query q = new Query();
		q.addCriteria(Criteria.where("id").is("id01"));
		when(mongoOperations.findOne(q, NextWorkUser.class)).thenReturn(nextWorkUser);
		q = new Query();
		q.addCriteria(Criteria.where("id").is("id01"));
		when(mongoOperations.findOne(q, Roles.class)).thenReturn(role);
		q = new Query();
		q.addCriteria(Criteria.where("uid").is("ws01"));
		when(mongoOperations.findOne(q, Workstream.class)).thenReturn(workstream);
		when(nextWorkUserRepository.findById(Mockito.any())).thenReturn(nextWorkUserOpt);
		GidsRequestDTO gidReq = new GidsRequestDTO();
		gidReq.setGid("z00A1234");
		gidReq.setIndex(1);
		q = new Query();
		q.addCriteria(Criteria.where("vid").is(versionId));
		q.addCriteria(Criteria.where("gid").regex("^" + gidReq.getGid().toLowerCase(), "i"));
		when(mongoOperations.findOne(q, Scoping.class)).thenReturn(scoping);
		Assertions.assertNotNull(nextWorkServiceImpl.updateUserById(userEmail, id, userRequestDTO, "gidExclusion",
				"wsExclusion", actionRoleList));

	}

//	@Test(expected = RestBadRequestException.class)
	public void updateNextWorkExclusionTest() {
		Query q = new Query();
		q.addCriteria(Criteria.where("id").is("id01"));
		when(mongoOperations.findOne(q, NextWorkUser.class)).thenReturn(nextWorkUser);
		when(userService.findById(Mockito.any())).thenReturn(nextWorkUserOpt);
		q = new Query();
		q.addCriteria(Criteria.where("uid").is("ws01"));
		when(mongoOperations.findOne(q, Workstream.class)).thenReturn(workstream);
		GidsRequestDTO gidReq = new GidsRequestDTO();
		gidReq.setGid("z00A1234");
		gidReq.setIndex(1);

		q = new Query();
		q.addCriteria(Criteria.where("vid").is(versionId));
		q.addCriteria(Criteria.where("gid").regex("^" + gidReq.getGid().toLowerCase(), "i"));
		when(mongoOperations.findOne(q, Scoping.class)).thenReturn(scoping);

		nextWorkServiceImpl.updateUserById(userEmail, id, userRequestDTO, actionGidList, "wsExclusion", actionRoleList);

	}

	@Test
	 void getUserByIdTest() throws Exception {
		userEmail = "abc@siemens.com";
		when(userService.findByUserEmail(userEmail)).thenReturn(nextWorkUserOpt);
		when(userService.findById(Mockito.any())).thenReturn(nextWorkUserOpt);
		Assertions.assertNotNull(nextWorkServiceImpl.getUserById(id, userEmail));

	}

	@Test
	 void getAllUsersTest() throws Exception {
		when(userService.findByUserEmail(userEmail)).thenReturn(nextWorkUserOpt);
		when(userService.findAll()).thenReturn(nextWorkUserList);
		when(userService.findById(Mockito.any())).thenReturn(nextWorkUserOpt);
		Assertions.assertNotNull(nextWorkServiceImpl.getAllUsers(userEmail, null));

	}

	@Test
	 void getUserInfoTest() throws Exception {
		userEmail = "abc@siemens.com";
		String userEmailToSearch = "user@siemens.com";
		when(userService.findByUserEmail(userEmailToSearch)).thenReturn(nextWorkUserOpt);
		when(userService.findByUserEmail(userEmail)).thenReturn(nextWorkUserOpt);
		when(userService.findAll()).thenReturn(nextWorkUserList);
		when(userService.findById(Mockito.any())).thenReturn(nextWorkUserOpt);
		Assertions.assertNotNull(nextWorkServiceImpl.getUserInfo(userEmail, request, userEmailToSearch));

	}

	@Test
	 void getUserInfo2Test() throws Exception {
		userEmail = "abc@siemens.com";
		String userEmailToSearch = "user@siemens.com";
		when(userService.findByUserEmail(userEmailToSearch)).thenReturn(nextWorkUserOpt);
		when(userService.findByUserEmail(userEmail)).thenReturn(nextWorkUserOpt);
		when(userService.findAll()).thenReturn(nextWorkUserList);
		when(userService.findById(Mockito.any())).thenReturn(nextWorkUserOpt);
		Assertions.assertNotNull(nextWorkServiceImpl.getUserInfo(userEmail, request, Mockito.any()));

	}

	@Test
	 void getUserInfo3Test() throws Exception {
		String userEmailToSearch = "user@siemens.com";
		when(nextWorkUserRepository.findByUserEmail(userEmailToSearch)).thenReturn(Optional.of(nextWorkUser1));
		when(userService.findByUserEmail(userEmail)).thenReturn(Optional.of(nextWorkUser1));
		when(userService.findById(Mockito.any())).thenReturn(nextWorkUserOpt);
		Assertions.assertThrows(RestBadRequestException.class, () -> nextWorkServiceImpl.getUserInfo(userEmail, request, userEmailToSearch));

	}
}
