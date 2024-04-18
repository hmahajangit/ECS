package com.siemens.nextwork.admin;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;

import com.siemens.nextwork.admin.dto.GidsRequestDTO;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.MemberDTO;
import com.siemens.nextwork.admin.dto.RequestRoleDTO;
import com.siemens.nextwork.admin.dto.ResponseRoleDTO;
import com.siemens.nextwork.admin.dto.RoleDetailDTO;
import com.siemens.nextwork.admin.dto.WorkStreamDTO;
import com.siemens.nextwork.admin.exception.ResourceNotFoundException;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.model.GidData;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.model.Scoping;
import com.siemens.nextwork.admin.model.ScopingVersions;
import com.siemens.nextwork.admin.model.Workstream;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.repo.RolesRepository;
import com.siemens.nextwork.admin.repo.ScopingVersionsRepository;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.service.impl.NextWorkUserServiceImpl;
import com.siemens.nextwork.admin.service.impl.RolesServiceImpl;

@ContextConfiguration(classes = AdminManagementServiceApplication.class)
@SpringBootTest
 class RolesServiceTest extends RestTestEnabler {

	@Mock
	MongoOperations mongoOperations;

	@Mock
	private ScopingVersionsRepository scopingVersionsRepository;

	@Mock
	private NextWorkUserRepository nextWorkUserRepository;

	@Mock
	private RolesRepository rolesRepository;

	@InjectMocks
	private RolesServiceImpl rolesServiceImpl;

	@Mock
	private NextWorkUserServiceImpl nextWorkServiceImpl;

	@Mock
	private UserService userService;


	String userEmail = "abc@siemens.com";
	RequestRoleDTO requestRoleDTO;
	ResponseRoleDTO responseRoleDTO;
	List<ResponseRoleDTO> rolesResponseDTOList;
	String roleName = "ADMIN";
	String id = "id01";
	String actionGidList = "gidInclusion";
	String actionWSList = "WSINCLUSION";
	String actionMemberList = "memberInclusion";
	String versionId = "20230503";
	String gidString = "z00A1234";
	NextWorkUser nextWorkUser;
	NextWorkUser nextWorkUser1;
	Optional<NextWorkUser> nextWorkUserOpt;
	Optional<Roles> rolesOpt;
	List<Scoping> scopingDataList = new ArrayList<>();;
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
	List<Roles> rolesList = new ArrayList<>();
	List<Roles> rolesList1 = new ArrayList<>();
	Roles role;
	Roles role1;

	@BeforeEach
	public void setup() {

		requestRoleDTO = new RequestRoleDTO();
		requestRoleDTO.setRoleDisplayName("PUSer");
		requestRoleDTO.setRoleType("LOCAL ADMIN");
		requestRoleDTO.setRoleDescription(actionGidList);
		List<RoleDetailDTO> roleDetailList = new ArrayList<>();
		RoleDetailDTO roleDetailDTO = new RoleDetailDTO();
		roleDetailDTO.setRoleId("id01");
		roleDetailDTO.setRoleDisplayName("PUSer");
		roleDetailDTO.setRoleType("ADMIN");
		roleDetailList.add(roleDetailDTO);

		List<GidsRequestDTO> gidReqList = new ArrayList<>();
		GidsRequestDTO gid = new GidsRequestDTO();
		gid.setGid("z00A1234");
		gid.setIndex(1);
		gidReqList.add(gid);

		requestRoleDTO.setGids(gidReqList);
		List<WorkStreamDTO> ws = new ArrayList<>();
		WorkStreamDTO workStreamDTO = new WorkStreamDTO();
		workStreamDTO.setUid("ws01");
		workStreamDTO.setName("ws1");
		ws.add(workStreamDTO);

		requestRoleDTO.setWorkstreamList(ws);

		List<MemberDTO> rolesMembers = new ArrayList<>();
		MemberDTO memberDTO = new MemberDTO();
		memberDTO.setUid(id);
		memberDTO.setMemberName("TestUser");
		memberDTO.setMemberEmail(userEmail);
		rolesMembers.add(memberDTO);
		requestRoleDTO.setMemberList(rolesMembers);
		nextWorkUser = new NextWorkUser();
		nextWorkUser.setStatus("Active");
		nextWorkUser.setEmail(userEmail);
		nextWorkUser.setId(id);
		nextWorkUser.setName("TestUser");

		role = new Roles();
		role.setRoleDescription("Test");
		role.setRoleType("ADMIN");
		role.setRoleDisplayName("PUSer");
		List<String> gidlist = new ArrayList<String>();
		gidlist.add("z00A1234");
		gidlist.add("z00AB123");
		role.setGidList(gidlist);
		role.setWorkstreamList(ws);
		role.setId("id01");
		// roleDetailList.add(role);
		rolesList.add(role);
		nextWorkUser.setRolesDetails(rolesList);
		// nextWorkUser.setRolesDetails(roleDetailList);
		// nextWorkUser.setWorkstreamList(workstreamIds);
		nextWorkUserList = new ArrayList<>();
		workStreamList = new ArrayList<>();
		nextWorkUserOpt = Optional.of(nextWorkUser);
		rolesOpt = Optional.of(role);

		nextWorkUser1 = new NextWorkUser();
		nextWorkUser1.setStatus("Active");
		nextWorkUser1.setEmail(userEmail);
		nextWorkUser1.setId(id);
		nextWorkUser1.setName("TestUser");
		nextWorkUser1.setRolesDetails(rolesList1);
		role1 = new Roles();
		role1.setRoleDescription("Test");
		role1.setId("id01");
		role1.setRoleType("LOCAL ADMIN");
		role1.setRoleDisplayName("PUSer");
		role1.setWorkstreamList(ws);
		role1.setGidList(Arrays.asList("z00A1236"));
		rolesList.add(role1);
		workstream = new Workstream();
		workstream.setUid("ws01");
		workstream.setName("ws1");
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
		scopingDataList.add(scoping);
		scopingVersion = new ScopingVersions();
		scopingVersion.setVersionStatus("PUBLISH");
		scopeVersionOpt = Optional.of(scopingVersion);
		nextWorkUserList.add(nextWorkUser);
		workstreamOpt = Optional.of(workstream);
		
		
		when(nextWorkUserRepository.findByUserEmail(userEmail)).thenReturn(nextWorkUserOpt);
		when(userService.findByEmail(Mockito.any())).thenReturn(id);

	}

	@Test
	 void getRolesBySearchQueryTest() throws Exception {
		String searchQuery = "search";
		when(rolesRepository.findByNameIsLike(searchQuery)).thenReturn(rolesList);
		Assertions.assertNotNull(rolesServiceImpl.getRolesBySearchQuery(userEmail, searchQuery));
	}

	@Test
	 void updateRoleByIdTest() throws Exception {
		Query q = new Query();
		q.addCriteria(Criteria.where("id").is("id01"));
		when(mongoOperations.findOne(q, NextWorkUser.class)).thenReturn(nextWorkUser1);
		//when(nextWorkUserRepository.findByUserEmail(userEmail)).thenReturn(nextWorkUserOpt);
		when(rolesRepository.findById(Mockito.any())).thenReturn(Optional.of(role1));
		when(scopingVersionsRepository.findById(Mockito.any())).thenReturn(scopeVersionOpt);
		q = new Query();
		q.addCriteria(Criteria.where("uid").is("ws01"));
		when(mongoOperations.findOne(q, Workstream.class)).thenReturn(workstream);
		GidsRequestDTO gidReq = new GidsRequestDTO();
		gidReq.setGid("z00A1235");
		gidReq.setIndex(1);
		Scoping scoping = mongoOperations.findOne(q, Scoping.class);
		q = new Query();
		q.addCriteria(Criteria.where("gid").regex("^" + gidReq.getGid().toLowerCase(), "i"));
		when(mongoOperations.findOne(q, Scoping.class)).thenReturn(scoping);
		Assertions.assertThrows(RestBadRequestException.class, () -> rolesServiceImpl.updateRoleById(userEmail, id, requestRoleDTO, actionGidList, actionWSList));
	}

	@Test
	 void updateRoleByIdTestExclusion() throws Exception {
		Query q = new Query();
		q.addCriteria(Criteria.where("id").is("id01"));
		when(mongoOperations.findOne(q, NextWorkUser.class)).thenReturn(nextWorkUser);
		when(nextWorkUserRepository.findByUserEmail(userEmail)).thenReturn(nextWorkUserOpt);
		when(rolesRepository.findById(Mockito.any())).thenReturn(rolesOpt);

		q = new Query();
		q.addCriteria(Criteria.where("id").is("id01"));
		when(mongoOperations.findOne(q, Roles.class)).thenReturn(role);
		userEmail = "abc.com";
		when(scopingVersionsRepository.findById(Mockito.any())).thenReturn(scopeVersionOpt);
		GidsRequestDTO gidReq = new GidsRequestDTO();
		gidReq.setGid("z00A1234");
		gidReq.setIndex(1);

		q = new Query();
		q.addCriteria(Criteria.where("gid").regex("^" + gidReq.getGid().toLowerCase(), "i"));
		when(mongoOperations.findOne(q, Scoping.class)).thenReturn(scoping);

		Assertions.assertThrows(RestBadRequestException.class, () -> rolesServiceImpl.updateRoleById(userEmail, id, requestRoleDTO, "wsExclusion", "gidExclusion"));

	}

	@Test
	 void getRoleByIdTest() throws Exception {
		when(rolesRepository.findById(id)).thenReturn(rolesOpt);
		Assertions.assertNotNull(rolesServiceImpl.getRoleById( id,userEmail));

	}

	@Test
	 void getRoleByIdDeletedExceptionTest() {
		rolesOpt.get().setIsDeleted(true);
		when(rolesRepository.findById(id)).thenReturn(rolesOpt);
		Assertions.assertNotNull(rolesServiceImpl.getRoleById(id, userEmail));

	}

	@Test
	 void getRoleByIdNotPresentExceptionTest() throws Exception {
		Assertions.assertThrows(ResourceNotFoundException.class, () -> rolesServiceImpl.getRoleById(id, userEmail));

	}

	@Test
	 void deleteRolesTest() {
		Query q = new Query();
		q.addCriteria(Criteria.where("id").is("id01"));
		when(mongoOperations.findOne(q, NextWorkUser.class)).thenReturn(nextWorkUser);
		when(nextWorkUserRepository.findByUserEmail(userEmail)).thenReturn(nextWorkUserOpt);
		List<IdResponseDTO> idResponseDTOList = new ArrayList<>();
		IdResponseDTO idResponseDTO = new IdResponseDTO();
		idResponseDTO.setUid("id01");
		idResponseDTOList.add(idResponseDTO);
		Assertions.assertThrows(RestBadRequestException.class, () -> rolesServiceImpl.deleteRoles(userEmail, idResponseDTOList));
	}

	@Test
	 void getAllRolesTest() throws Exception {
		when(nextWorkUserRepository.findByUserEmail(userEmail)).thenReturn(nextWorkUserOpt);
		when(rolesRepository.findAll()).thenReturn(rolesList);
		when(nextWorkUserRepository.findById(Mockito.any())).thenReturn(nextWorkUserOpt);
		Assertions.assertNotNull(rolesServiceImpl.getAllRoles(userEmail));

	}

	@Test
	 void createRolesDuplicateNameExceptionTest() throws Exception {
		when(rolesRepository.findByRoleTypeAndRoleDisplayNameAndIsDeleted(Mockito.anyString(), Mockito.anyString(),Mockito.anyBoolean())).thenReturn(rolesOpt);
		when(nextWorkUserRepository.findByEmail(Mockito.any())).thenReturn(nextWorkUser);
		when(rolesRepository.findAll()).thenReturn(rolesList);
		requestRoleDTO.setRoleDisplayName("PUSer");
		Assertions.assertThrows(RestBadRequestException.class, () -> rolesServiceImpl.createRole(userEmail, requestRoleDTO));
		
	}

	@Test
	 void createRolesUserNotFoundExceptionTest() throws Exception {
		when(rolesRepository.findByRoleTypeAndRoleDisplayNameAndIsDeleted(Mockito.anyString(), Mockito.anyString(),Mockito.anyBoolean())).thenReturn(rolesOpt);
		when(nextWorkUserRepository.findByEmail(Mockito.any())).thenReturn(nextWorkUser);
		Assertions.assertThrows(RestBadRequestException.class, () -> rolesServiceImpl.createRole(userEmail, requestRoleDTO));

	}
}
