package com.siemens.nextwork.admin.service.impl;

import static com.siemens.nextwork.admin.util.NextworkConstants.AUTHORIZATION_HEADER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;

import com.siemens.nextwork.admin.repo.WorkstreamGidRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nimbusds.jwt.JWTClaimsSet;
import com.siemens.nextwork.admin.dto.GidsRequestDTO;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserByIdResponseDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserPutRequestDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserResponseDTO;
import com.siemens.nextwork.admin.dto.RoleDetailDTO;
import com.siemens.nextwork.admin.dto.UsersInfoResponseDTO;
import com.siemens.nextwork.admin.dto.WorkStreamDTO;
import com.siemens.nextwork.admin.enums.ActionForGidList;
import com.siemens.nextwork.admin.enums.ActionForRoleList;
import com.siemens.nextwork.admin.enums.ActionForWorkStreamList;
import com.siemens.nextwork.admin.enums.RoleType;
import com.siemens.nextwork.admin.enums.StatusType;
import com.siemens.nextwork.admin.exception.ResourceNotFoundException;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.exception.RestForbiddenException;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.model.Scoping;
import com.siemens.nextwork.admin.model.Users;
import com.siemens.nextwork.admin.model.Workstream;
import com.siemens.nextwork.admin.model.WorkstreamGids;
import com.siemens.nextwork.admin.repo.RolesRepository;
import com.siemens.nextwork.admin.repo.ScopingRepository;
import com.siemens.nextwork.admin.repo.WorkStreamRepository;
import com.siemens.nextwork.admin.service.NextWorkUserService;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.util.CommonUtils;
import com.siemens.nextwork.admin.util.NextworkConstants;

@Service
public class NextWorkUserServiceImpl implements NextWorkUserService {

	@Autowired
	private UserService userService;

	@Autowired
	private WorkStreamRepository workStreamRepository;

	@Autowired
	private ScopingRepository scopingRepository;

	@Autowired
	private RolesRepository rolesRepository;

	@Autowired
	MongoOperations mongoOperations;

	@Autowired
	private WorkstreamGidRepository workstreamGidRepository;

	private static final Logger LOGGER = LoggerFactory.getLogger(NextWorkUserServiceImpl.class);

	@Override
	@Transactional
	public List<NextWorkUserResponseDTO> getAllUsers(String userEmail, String tabType) {
		LOGGER.info("userEmail :{}", userEmail);
		userService.validateUserAndRole(userEmail);
		List<NextWorkUser> allUser = userService.findAll();
		if (null != tabType && tabType.equalsIgnoreCase("REQUEST")) {
			List<NextWorkUser> newUsers = new ArrayList<>();
			newUsers.addAll(allUser.stream().filter(i -> i.getStatus().equalsIgnoreCase(StatusType.PENDING.toString()))
					.toList());
			newUsers.addAll(allUser.stream().filter(i -> i.getStatus().equalsIgnoreCase(StatusType.REJECTED.toString()))
					.toList());
			allUser = newUsers;
		} else if (null != tabType && tabType.equalsIgnoreCase("USERS")) {
			List<NextWorkUser> newUsers = new ArrayList<>();
			newUsers.addAll(allUser.stream().filter(i -> i.getStatus().equalsIgnoreCase(StatusType.ACTIVE.toString()))
					.toList());
			newUsers.addAll(allUser.stream().filter(i -> i.getStatus().equalsIgnoreCase(StatusType.DEACTIVE.toString()))
					.toList());
			newUsers.addAll(allUser.stream().filter(i -> i.getStatus().equalsIgnoreCase(StatusType.ACCEPTED.toString()))
					.toList());
			allUser = newUsers;
		}
		List<NextWorkUserResponseDTO> listOfUser = new ArrayList<>();
		for (NextWorkUser nextWorkUser : allUser) {
			NextWorkUserResponseDTO nextWorkUserResponseDTO = new NextWorkUserResponseDTO();
			nextWorkUserResponseDTO.setId(nextWorkUser.getId());
			nextWorkUserResponseDTO.setName(nextWorkUser.getName());
			nextWorkUserResponseDTO.setEmail(nextWorkUser.getEmail());
			nextWorkUserResponseDTO.setOrgCode(nextWorkUser.getOrgCode());
			nextWorkUserResponseDTO.setCreationDate(nextWorkUser.getCreationDate());
			nextWorkUserResponseDTO.setPurpose(nextWorkUser.getPurpose());
			List<Roles> rolesDetails = nextWorkUser.getRolesDetails();
			if (rolesDetails != null) {
				List<RoleDetailDTO> roleDetailsList = new ArrayList<>();
				for (Roles role : rolesDetails) {
					RoleDetailDTO roleDetailDTO = new RoleDetailDTO();

					roleDetailDTO.setRoleId(role.getId());
					roleDetailDTO.setRoleDisplayName(role.getRoleDisplayName());
					roleDetailDTO.setRoleType(role.getRoleType());
					roleDetailsList.add(roleDetailDTO);
				}
				nextWorkUserResponseDTO.setRolesDetails(roleDetailsList);
			}
			nextWorkUserResponseDTO.setStatus(nextWorkUser.getStatus());
			listOfUser.add(nextWorkUserResponseDTO);
		}

		return listOfUser;
	}

	@Override
	@Transactional
	public NextWorkUserByIdResponseDTO getUserById(String id, String userEmail) {
		userService.validateUserAndRole(userEmail);
		NextWorkUser existingUser = userService.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
		NextWorkUserByIdResponseDTO nextWorkUserByIdResponseDTO = new NextWorkUserByIdResponseDTO();
		nextWorkUserByIdResponseDTO.setId(existingUser.getId());
		nextWorkUserByIdResponseDTO.setName(existingUser.getName());
		nextWorkUserByIdResponseDTO.setEmail(existingUser.getEmail());
		nextWorkUserByIdResponseDTO.setOrgCode(existingUser.getOrgCode());
		nextWorkUserByIdResponseDTO.setCreationDate(existingUser.getCreationDate());
		List<Roles> rolesDetails = existingUser.getRolesDetails();
		if (rolesDetails != null) {
			List<RoleDetailDTO> roleDetailsList = new ArrayList<>();
			for (Roles role : rolesDetails) {
				RoleDetailDTO roleDetailDTO = new RoleDetailDTO();

				roleDetailDTO.setRoleId(role.getId());
				roleDetailDTO.setRoleDisplayName(role.getRoleDisplayName());
				roleDetailDTO.setRoleType(role.getRoleType());
				roleDetailsList.add(roleDetailDTO);
			}
			nextWorkUserByIdResponseDTO.setRolesDetails(roleDetailsList);
		}

		List<WorkStreamDTO> userWorkstream = existingUser.getDirectAssignmentWorkstreamList();
		nextWorkUserByIdResponseDTO.setWorkstreamList(userWorkstream);

		nextWorkUserByIdResponseDTO.setStatus(existingUser.getStatus());
		nextWorkUserByIdResponseDTO.setHaveGIDList(existingUser.isHaveGIDList());

		return nextWorkUserByIdResponseDTO;

	}

	@Override
	@Transactional
	public UsersInfoResponseDTO getUserInfo(String userEmail, HttpServletRequest request, String userEmailToSearch) {

		UsersInfoResponseDTO response = new UsersInfoResponseDTO();
		if (userEmailToSearch != null && !userEmailToSearch.isEmpty()) {
			return settingUserInfoResponseForGivenEmail(userEmailToSearch, response);
		}
		Optional<NextWorkUser> nextWorkUser = userService.findByUserEmail(userEmail);
		if (!nextWorkUser.isPresent()) {
			throw new RestBadRequestException(
					"User doesn't exist.Please raise Access Request if you want to use the system");
		} else if ((nextWorkUser.get().getStatus().equalsIgnoreCase("PENDING")
				|| nextWorkUser.get().getStatus().equalsIgnoreCase("REJECTED")
				|| nextWorkUser.get().getStatus().equalsIgnoreCase("ACCEPTED"))) {
			LOGGER.info("Not an Active Member Yet");
			NextWorkUser user = nextWorkUser.get();
			response = checkInPreOnboardedUserTable(request, user);
			return response;
		}
		NextWorkUser user = nextWorkUser.get();
		response.setUid(user.getId());
		response.setEmailId(user.getEmail());
		response.setName(user.getName());
		response.setStatus(user.getStatus());
		response.setUid(user.getId());
		response.setName(user.getName());
		response.setEmailId(user.getEmail());
		String roleName = "";
		List<RoleDetailDTO> roleDetailsDTOList = new ArrayList<>();
		List<Roles> rolesList = user.getRolesDetails();
		if (null != rolesList && !rolesList.isEmpty()) {
			for (Roles role : rolesList) {
				RoleDetailDTO roleDetailDTO = new RoleDetailDTO();
				roleDetailDTO.setRoleId(role.getId());
				roleDetailDTO.setRoleType(role.getRoleType());
				roleDetailDTO.setRoleDisplayName(role.getRoleDisplayName());
				roleDetailsDTOList.add(roleDetailDTO);
			}
			roleName = rolesList.get(0).getRoleType();
		}
		response.setRole(roleName);

		response.setRolesDetails(roleDetailsDTOList);
		response.setHaveMigratedWs(false);
		if(user.getMigratedWorkStreamList() != null && !user.getMigratedWorkStreamList().isEmpty())
			response.setHaveMigratedWs(true);

		return response;
	}

	private UsersInfoResponseDTO settingUserInfoResponseForGivenEmail(String userEmailToSearch,
			UsersInfoResponseDTO response) {
		Optional<NextWorkUser> nextWorkUser = userService.findByUserEmail(userEmailToSearch);
		LOGGER.info("nextWorkUser :{}", nextWorkUser);
		if (!nextWorkUser.isPresent()) {
			throw new RestBadRequestException(
					"User doesn't exist.Please raise Access Request if you want to use the system");
		}
		NextWorkUser user = nextWorkUser.get();
		response.setUid(user.getId());
		response.setName(user.getName());
		response.setEmailId(user.getEmail());
		response.setStatus(user.getStatus());
		response.setInfo(user.getInfo());
		String roleName = "";
		List<RoleDetailDTO> roleDetailsDTOList = new ArrayList<>();
		List<Roles> rolesList = user.getRolesDetails();
		if (null != rolesList && !rolesList.isEmpty()) {
			for (Roles role : rolesList) {
				RoleDetailDTO roleDetailDTO = new RoleDetailDTO();
				roleDetailDTO.setRoleId(role.getId());
				roleDetailDTO.setRoleType(role.getRoleType());
				roleDetailDTO.setRoleDisplayName(role.getRoleDisplayName());
				roleDetailsDTOList.add(roleDetailDTO);
			}
			roleName = rolesList.get(0).getRoleType();
		}
		response.setRole(roleName);

		response.setRolesDetails(roleDetailsDTOList);
		return response;
	}

	private UsersInfoResponseDTO checkInPreOnboardedUserTable(HttpServletRequest request,
			NextWorkUser preOnboardedUser) {
		UsersInfoResponseDTO response = new UsersInfoResponseDTO();

		if (preOnboardedUser.getStatus().equalsIgnoreCase(StatusType.PENDING.toString())) {
			throw new RestForbiddenException("Pending for approval");
		} else if (preOnboardedUser.getStatus().equalsIgnoreCase(StatusType.REJECTED.toString())) {
			throw new RestForbiddenException("Request is rejected");
		} else if (preOnboardedUser.getStatus().equalsIgnoreCase(StatusType.ACCEPTED.toString())) {
			migrateUser(request, preOnboardedUser, response);
		}
		return response;
	}

	@Override
	@Transactional
	public IdResponseDTO updateUserById(String userEmail, String usersId, NextWorkUserPutRequestDTO userRequestDTO,
			String actionGidList, String actionWorkStreamList, String actionRoleList) {
		LOGGER.info("Inside updateUserById service");
		String userId = userService.findByEmail(userEmail);
		var roleName = checkUserRole(userId);

		LOGGER.info("roleName={}", roleName);
		if (roleName.equalsIgnoreCase(NextworkConstants.ADMIN)) {
			Optional<NextWorkUser> userOpt = userService.findById(usersId);
			if (userOpt.isPresent()) {
				NextWorkUser nextworkUserData = userOpt.get();
				if (nextworkUserData.getStatus().equalsIgnoreCase(StatusType.ACCEPTED.toString())) {
					throw new RestBadRequestException("User has to login first");
				}
				nextworkUserData.setStatus(userRequestDTO.getStatus());
				LOGGER.info("nextworkUserData={}", nextworkUserData);
				validateAndUpdateRoleList(userRequestDTO, actionRoleList, nextworkUserData);
				validateAndUpdateGidList(userRequestDTO, actionGidList, roleName, nextworkUserData);
				validateAndUpdateWorkStreamList(userRequestDTO, actionWorkStreamList, nextworkUserData);
				settingUserGidList(nextworkUserData);

				resetGidAndDirectAssignmentListForAdminAndUser(nextworkUserData);

				LOGGER.info("saving to db");
				userService.saveNextWorkUser(nextworkUserData);
				IdResponseDTO responseDTO = new IdResponseDTO();
				responseDTO.setUid(nextworkUserData.getId());
				return responseDTO;
			} else {
				throw new RestBadRequestException("No user found with the given id");
			}
		} else {
			throw new RestBadRequestException("This operation can be done by ADMIN only");
		}
	}

	private void resetGidAndDirectAssignmentListForAdminAndUser(NextWorkUser nextworkUserData) {
		if (null != nextworkUserData.getRolesDetails() && !nextworkUserData.getRolesDetails().isEmpty()) {
			String role = nextworkUserData.getRolesDetails().get(0).getRoleType();
			if (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("USER")) {
				nextworkUserData.setDirectGidList(null);
				nextworkUserData.setGidList(null);
				nextworkUserData.setDirectAssignmentWorkstreamList(null);
				nextworkUserData.setHaveGIDList(false);
				nextworkUserData.setUserGidMap(null);
			}
		}
	}

	private void settingUserGidList(NextWorkUser nextworkUserData) {
		LOGGER.info("User GID MAP : {}", nextworkUserData.getGidList());
		if (null != nextworkUserData.getUserGidMap()) {
			List<String> gidList = new ArrayList<>();

			for (Map.Entry<String, Integer> gid : nextworkUserData.getUserGidMap().entrySet()) {
				if (gid.getValue() > 0) {
					gidList.add(gid.getKey());
				}
			}
			nextworkUserData.setGidList(gidList);
		}
		boolean haveGidList = false;
		if (null != nextworkUserData.getGidList() && !nextworkUserData.getGidList().isEmpty()) {
			haveGidList = true;
		}
		nextworkUserData.setHaveGIDList(haveGidList);
	}

	private void validateAndUpdateGidList(NextWorkUserPutRequestDTO userRequestDTO, String actionGidList,
			String roleName, NextWorkUser nextworkUserData) {
		if (actionGidList != null) {
			LOGGER.info("non empty gid list");

			if (!(roleName.equalsIgnoreCase("ADMIN") || roleName.equalsIgnoreCase("LOCAL_ADMIN")
					|| roleName.equalsIgnoreCase("POWER_USER")))
				throw new RestBadRequestException("User not authorized to add/remove gids");

			if (actionGidList.equalsIgnoreCase(ActionForGidList.GIDINCLUSION.toString())) {
				gidInclusion(userRequestDTO, nextworkUserData);
			}

			else if (actionGidList.equalsIgnoreCase(ActionForGidList.GIDEXCLUSION.toString())) {
				gidExclusion(userRequestDTO, nextworkUserData);
			} else {
				throw new RestBadRequestException("actionGidList should be either gidInclusion or gidExclusion");
			}
		}
	}

	private void gidInclusion(NextWorkUserPutRequestDTO userRequestDTO, NextWorkUser nextworkUserData) {
		LOGGER.info("GIDINCLUSION");
		if (userRequestDTO.getGids() == null) {
			throw new RestBadRequestException("Please provide gids to be included");

		}
		if (userRequestDTO.getGids().size() > 25)
			throw new RestBadRequestException("Exceed limit. Maximum 25 gid data can be added at a time.");

		addingScopingDataForUser(userRequestDTO, nextworkUserData);
	}

	private void gidExclusion(NextWorkUserPutRequestDTO userRequestDTO, NextWorkUser nextworkUserData) {
		LOGGER.info("GIDEXCLUSION");
		if (userRequestDTO.getGids() == null) {
			throw new RestBadRequestException("Please provide gids to be excluded");

		}
		if (userRequestDTO.getGids().size() > 25)
			throw new RestBadRequestException("Exceed limit. Maximum 25 gid data can be removed at a time.");

		removeGIDFromUser(userRequestDTO.getGids(), nextworkUserData);
	}

	private void validateAndUpdateRoleList(NextWorkUserPutRequestDTO userRequestDTO, String actionRoleList,
			NextWorkUser nextworkUserData) {
		if (actionRoleList != null) {
			LOGGER.info("non empty member list, actionRoleList= {}", actionRoleList);

			if (userRequestDTO.getRolesDetails() != null && userRequestDTO.getRolesDetails().size() > 25) {
				throw new RestBadRequestException("Exceed limit. Maximum 25 role data can be added at a time.");
			}
			if (actionRoleList.equalsIgnoreCase(ActionForRoleList.ROLEINCLUSION.toString())) {
				LOGGER.info("ROLE INCLUSION");
				if (userRequestDTO.getRolesDetails() == null) {
					throw new RestBadRequestException("Please provide role Details to be included");
				}
				addRolesToUser(userRequestDTO, nextworkUserData);
			}

			else if (actionRoleList.equalsIgnoreCase(ActionForRoleList.ROLEEXCLUSION.toString())) {
				LOGGER.info("ROLE EXCLUSION");
				if (null != userRequestDTO.getRolesDetails()) {
					removeRolesFromUser(userRequestDTO.getRolesDetails(), nextworkUserData);
				} else {
					throw new RestBadRequestException("Please provide role Details to be excluded");

				}
			} else {
				throw new RestBadRequestException(
						"actionMemberList should be either memberInclusion or memberExclusion");
			}

		}
	}

	private void validateAndUpdateWorkStreamList(NextWorkUserPutRequestDTO userRequestDTO, String actionWorkStreamList,
			NextWorkUser nextworkUserData) {

		if (actionWorkStreamList != null) {
			LOGGER.info("non empty member list, actionWokstreamList= {}", actionWorkStreamList);
			if (userRequestDTO.getWorkstreamList() != null && userRequestDTO.getWorkstreamList().size() > 25) {
				throw new RestBadRequestException("Exceed limit. Maximum 25 workstream data can be added at a time.");
			}
			if (actionWorkStreamList.equalsIgnoreCase(ActionForWorkStreamList.WSINCLUSION.toString())) {
				LOGGER.info("WORKSTREAM INCLUSION");
				if (userRequestDTO.getWorkstreamList() == null) {
					throw new RestBadRequestException("Please provide workstreams to be included");
				}
				addWorkstreamToUser(userRequestDTO, nextworkUserData);
			}

			else if (actionWorkStreamList.equalsIgnoreCase(ActionForWorkStreamList.WSEXCLUSION.toString())) {
				LOGGER.info("WORKSTREAM EXCLUSION");
				if (null != userRequestDTO.getWorkstreamList()) {
					removeWorkstreamFromUser(userRequestDTO.getWorkstreamList(), nextworkUserData);
				} else {
					throw new RestBadRequestException("Please provide workstreams to be excluded");

				}
			} else {
				throw new RestBadRequestException("actionWokstreamList should be either wsInclusion or wsExclusion");
			}

		}

	}

	private void addWorkstreamToUser(NextWorkUserPutRequestDTO userRequestDTO, NextWorkUser nextworkUserData) {
		List<WorkStreamDTO> userDirectAssignedWorkStreamList = nextworkUserData.getDirectAssignmentWorkstreamList();
		List<WorkStreamDTO> userWorkStreamList = nextworkUserData.getWorkStreamList();

		if (null == userDirectAssignedWorkStreamList) {
			userDirectAssignedWorkStreamList = new ArrayList<>();
		}
		if (null == userWorkStreamList) {
			userWorkStreamList = new ArrayList<>();
		}
		List<WorkStreamDTO> workstreamsIdList = userRequestDTO.getWorkstreamList();
		LOGGER.info("workStreams workstreamsIdList :{}", workstreamsIdList);

		List<String> workstreamsIds = userDirectAssignedWorkStreamList.stream().map(WorkStreamDTO::getUid)
				.toList();
		Map<String, Integer> gidMap = nextworkUserData.getUserGidMap();
		if (null == gidMap) {
			gidMap = new HashMap<>();
		}
		List<Workstream> workStreamList = new ArrayList<>();
		if (!workstreamsIdList.isEmpty()) {
			for (WorkStreamDTO ws : workstreamsIdList) {
				settingWorkStreamsForUser(nextworkUserData, userDirectAssignedWorkStreamList, userWorkStreamList,
						workstreamsIds, gidMap, workStreamList, ws);
			}
		}
	}

	private void settingWorkStreamsForUser(NextWorkUser nextworkUserData,
			List<WorkStreamDTO> userDirectAssignedWorkStreamList, List<WorkStreamDTO> userWorkStreamList,
			List<String> workstreamsIds, Map<String, Integer> gidMap, List<Workstream> workStreamList,
			WorkStreamDTO ws) {
		Query q = new Query();
		q.addCriteria(Criteria.where("uid").is(ws.getUid()));
		Optional<Workstream> workStream = workStreamRepository.findById(ws.getUid());
		if (workStream.isEmpty())
			throw new RestBadRequestException("The workStream is not present");

		Workstream ows = workStream.get();
		if (ows != null) {
			if (workstreamsIds.contains(ws.getUid())) {
				return;
			}
			if (!ws.getName().equals(ows.getName())) {
				throw new RestBadRequestException("Workstream name doesn't match");
			}
			WorkstreamGids workstreamGids = workstreamGidRepository.findByWorkstreamId(ws.getUid());
			WorkStreamDTO workStreamDTO = new WorkStreamDTO();
			workStreamDTO.setUid(ws.getUid());
			workStreamDTO.setName(ws.getName());
			workStreamDTO.setGidList(workstreamGids.getGidList());
			userDirectAssignedWorkStreamList.add(workStreamDTO);
			if (null != nextworkUserData.getRolesDetails() && !nextworkUserData.getRolesDetails().isEmpty()
					&& nextworkUserData.getRolesDetails().get(0).getRoleType().equalsIgnoreCase("POWER USER")) {
				settingDataForPowerUser(nextworkUserData, userWorkStreamList, workStreamList, ows, workStreamDTO);
				nextworkUserData.setWorkStreamList(userWorkStreamList);

			}
			setGidMapForWorkstream(gidMap, ows, workstreamGids);

		} else {
			throw new RestBadRequestException("The workStream is not present");
		}
		workStreamRepository.saveAll(workStreamList);
		nextworkUserData.setUserGidMap(gidMap);
		nextworkUserData.setDirectAssignmentWorkstreamList(userDirectAssignedWorkStreamList);
	}

	private void setGidMapForWorkstream(Map<String, Integer> gidMap, Workstream workStream, WorkstreamGids workstreamGids) {
		List<String> gidList = workstreamGids.getGidList();
		if (null != gidList) {
			for (String gid : gidList) {
				if (!gidMap.containsKey(gid)) {
					gidMap.put(gid, 1);
				} else {
					gidMap.put(gid, gidMap.get(gid) + 1);
				}
			}
		}
	}

	private void settingDataForPowerUser(NextWorkUser nextworkUserData, List<WorkStreamDTO> userWorkStreamList,
			List<Workstream> workStreamList, Workstream workStream, WorkStreamDTO workStreamDTO) {
		List<Users> workStreamMembers = workStream.getUsers();
		if (null == workStreamMembers) {
			workStreamMembers = new ArrayList<>();
		}
		List<String> workStreamMembersIds = workStreamMembers.stream().map(Users::getUid).toList();
		if (workStreamMembersIds.contains(nextworkUserData.getId())) {
			throw new RestBadRequestException("User is already a member of given workstream");
		}
		Users newUser = new Users();
		newUser.setUid(nextworkUserData.getId());
		newUser.setName(nextworkUserData.getName());
		newUser.setEmail(nextworkUserData.getEmail());
		newUser.setRole(NextworkConstants.POWER_USER);
		newUser.setProjectRole(NextworkConstants.PROJECT_MEMBER);
		workStreamMembers.add(newUser);
		workStream.setUsers(workStreamMembers);
		workStreamList.add(workStream);
		userWorkStreamList.add(workStreamDTO);
	}

	private void removeWorkstreamFromUser(List<WorkStreamDTO> workstreamList, NextWorkUser nextworkUserData) {
		List<WorkStreamDTO> directWorkstreamsDTOList = nextworkUserData.getDirectAssignmentWorkstreamList();

		if (null != directWorkstreamsDTOList) {

			List<String> userWorkStreams = directWorkstreamsDTOList.stream().map(WorkStreamDTO::getUid)
					.toList();

			List<WorkStreamDTO> removeWorkstreamsDTOs = workstreamList.stream()
					.filter(e -> userWorkStreams.contains(e.getUid())).toList();
			List<String> removeWorkstreamsId = workstreamList.stream().map(WorkStreamDTO::getUid)
					.toList();
			List<String> gids = new ArrayList<>();
			for (String wsId : removeWorkstreamsId) {
				settingGidsForWorkstream(userWorkStreams, gids, wsId);

			}
			Map<String, Integer> gidMap = nextworkUserData.getUserGidMap();
			if (!gids.isEmpty()) {
				updateGidMapForWorkstreamRemoval(gids, gidMap);
			}
			directWorkstreamsDTOList.removeAll(removeWorkstreamsDTOs);
			LOGGER.info("workstreamsDTOList after Removal : {} ", directWorkstreamsDTOList);
			nextworkUserData.setDirectAssignmentWorkstreamList(directWorkstreamsDTOList);
			nextworkUserData.setUserGidMap(gidMap);
		} else {
			throw new RestBadRequestException("The required role doesn't have any associated workstream");
		}

	}

	private void settingGidsForWorkstream(List<String> userWorkStreams, List<String> gids, String wsId) {
		if (!userWorkStreams.contains(wsId)) {
			throw new RestBadRequestException("Workstream " + wsId + " not found in role");
		}
		Workstream ws = null;
		WorkstreamGids workstresmGids = null;
		Optional<Workstream> workStream = workStreamRepository.findById(wsId);
		if (!workStream.isEmpty()) {
			ws = workStream.get();
			workstresmGids = workstreamGidRepository.findByWorkstreamId(ws.getUid());
		}
		if (null != ws && CollectionUtils.isNotEmpty(workstresmGids.getGidList())) {
			gids.addAll(workstresmGids.getGidList());
		}
	}

	private void updateGidMapForWorkstreamRemoval(List<String> gids, Map<String, Integer> gidMap) {
		for (String gid : gids) {
			if (gidMap != null && gid != null && gidMap.containsKey(gid)) {
				gidMap.computeIfPresent(gid, (key, value) -> value - 1);
				gidMap.remove(gid, 0);
			}
		}
	}

	private void removeRolesFromUser(List<RoleDetailDTO> roleDetails, NextWorkUser nextworkUserData) {

		List<Roles> roles = nextworkUserData.getRolesDetails();
		LOGGER.info("WORKSTREAM INCLUSION   {}", roles);
		List<String> removeUsersId = roleDetails.stream().map(RoleDetailDTO::getRoleId).toList();

		List<Roles> removeUsers = roles.stream().filter(e -> removeUsersId.contains(e.getId()))
				.toList();
		roles.removeAll(removeUsers);
		nextworkUserData.setRolesDetails(roles);
	}

	private void addRolesToUser(NextWorkUserPutRequestDTO userRequestDTO, NextWorkUser nextworkUserData) {
		List<Roles> usersroles = nextworkUserData.getRolesDetails();
		if (usersroles == null) {
			usersroles = new ArrayList<>();
		}
		List<RoleDetailDTO> roleDetailList = userRequestDTO.getRolesDetails();
		List<String> userUids = usersroles.stream().map(Roles::getId).collect(Collectors.toList());
		Boolean firstObject = true;
		String requestRole = "";

		for (RoleDetailDTO roleDetailsDTO : roleDetailList) {
			Optional<Roles> nextWorkUserRole = rolesRepository.findById(roleDetailsDTO.getRoleId());
			if (nextWorkUserRole.isEmpty())
				throw new RestBadRequestException("The user is either not present or Deactive");
			Roles nextRole = nextWorkUserRole.get();
			if (nextRole != null) {

				if (userUids.contains(roleDetailsDTO.getRoleId())) {
					LOGGER.info("role Id already presents");

					continue;
				}
				userUids.add(roleDetailsDTO.getRoleId());
				Roles userRole = setUserRole(roleDetailsDTO, nextRole);
				String roleType = getRoleTypeOfUser(nextworkUserData, usersroles);
				requestRole = getRoleTypeForFirstObject(firstObject, requestRole, roleDetailsDTO);
				if (!roleType.isBlank() && !roleType.equals(roleDetailsDTO.getRoleType())
						&& Boolean.TRUE.equals(firstObject)) {
					usersroles = setUserRolesToNewListIfRoleTypeIsDifferent();

				}

				roleTypeNotMatchesException(firstObject, requestRole, roleDetailsDTO);
				usersroles.add(userRole);
				firstObject = false;
				LOGGER.info("roleDetails Role Type : {}", roleDetailsDTO.getRoleType());
				addRoleWorkstreamAndGidsToUserWorkstreamGids(nextRole, nextworkUserData);
			} else {
				throw new RestBadRequestException("The user is either not present or Deactive");
			}
		}

		nextworkUserData.setRolesDetails(usersroles);

	}
	private void addRoleWorkstreamAndGidsToUserWorkstreamGids(Roles role, NextWorkUser nextworkUserData) {
		List<WorkStreamDTO> workstreamsAddedToRole = addWorkstreamToUserWsList(role, nextworkUserData);
		addRoleWSAndGidsToUserWSAndGids(role, nextworkUserData, workstreamsAddedToRole);
		
	}

	private List<WorkStreamDTO> addWorkstreamToUserWsList(Roles role, NextWorkUser nextworkUserData) {
		final List<WorkStreamDTO> workstreamsAddedToRole = role.getWorkstreamList()!=null? role.getWorkstreamList(): new ArrayList<>();
		List<WorkStreamDTO> userExistingWorkstreams = nextworkUserData.getWorkStreamList();
		if (null == userExistingWorkstreams) {
			userExistingWorkstreams = new ArrayList<>();
		}
		final Map<String, WorkStreamDTO> workstreamMap = userExistingWorkstreams.stream()
				.collect(Collectors.toMap(WorkStreamDTO::getUid, Function.identity()));
		workstreamsAddedToRole.forEach(newWorkstream -> workstreamMap.put(newWorkstream.getUid(), newWorkstream));
		userExistingWorkstreams = new ArrayList<>(workstreamMap.values());
		nextworkUserData.setWorkStreamList(userExistingWorkstreams);
		return workstreamsAddedToRole;
	}

	private void addRoleWSAndGidsToUserWSAndGids(Roles role, NextWorkUser nextworkUserData, List<WorkStreamDTO> workstreamsAddedToRole) {
		final List<String> workstreamIds = workstreamsAddedToRole.stream().map(WorkStreamDTO::getUid).toList();
		final List<Workstream> workstreamList = StreamSupport.stream(workStreamRepository.findAllById(workstreamIds).spliterator(), false)
				.toList();
		workstreamList.stream().forEach(workstream -> {
			final List<Users> workStreamMembers = Optional.of(workstream.getUsers()).orElse(new ArrayList<>());
			final List<String> workStreamMembersIds = workStreamMembers.stream().map(Users::getUid).toList();
			if (!workStreamMembersIds.contains(nextworkUserData.getId())) {
				final Users newUser = new Users();
				newUser.setUid(nextworkUserData.getId());
				newUser.setName(nextworkUserData.getName());
				newUser.setEmail(nextworkUserData.getEmail());
				newUser.setRole(NextworkConstants.POWER_USER);
				newUser.setProjectRole(NextworkConstants.PROJECT_MEMBER);
				workStreamMembers.add(newUser);
				workstream.setUsers(workStreamMembers);
			}
			addWorkstreamGidsAndDirectRoleGidsToUser(role, nextworkUserData, workstream);
		});
		workStreamRepository.saveAll(workstreamList);
	}

	private void addWorkstreamGidsAndDirectRoleGidsToUser(Roles role, NextWorkUser nextworkUserData, Workstream workstream) {
		final Set<String> userGids = new HashSet<>(Optional.ofNullable(nextworkUserData.getGidList()).orElse(new ArrayList<>()));
		final Set<String> userDirectGids = new HashSet<>(Optional.ofNullable(nextworkUserData.getDirectGidList()).orElse(new ArrayList<>()));
		WorkstreamGids workstreamGids = workstreamGidRepository.findByWorkstreamId(workstream.getUid());
		if (workstreamGids.getGidList() != null) {
			userGids.addAll(workstreamGids.getGidList());
		}
		if (role.getDirectGidList() != null) {
			userDirectGids.addAll(role.getDirectGidList());
		}
		nextworkUserData.setGidList(new ArrayList<>(userGids));
		nextworkUserData.setDirectGidList(new ArrayList<>(userDirectGids));
	}

	private String getRoleTypeForFirstObject(Boolean firstObject, String requestRole, RoleDetailDTO roleDetailsDTO) {
		if (Boolean.TRUE.equals(firstObject)) {
			requestRole = roleDetailsDTO.getRoleType();
		}
		return requestRole;
	}

	private void roleTypeNotMatchesException(Boolean firstObject, String requestRole, RoleDetailDTO roleDetailsDTO) {
		if (Boolean.FALSE.equals(firstObject) && !(requestRole.equals(roleDetailsDTO.getRoleType()))) {
			throw new RestBadRequestException("The roleTypes in requests doesn't matches");
		}
	}

	private String getRoleTypeOfUser(NextWorkUser nextworkUserData, List<Roles> usersroles) {
		String roleType = "";
		if (!usersroles.isEmpty()) {
			LOGGER.info("User Roles are not empty");

			roleType = nextworkUserData.getRolesDetails().get(0).getRoleType();
		}
		return roleType;
	}

	private List<Roles> setUserRolesToNewListIfRoleTypeIsDifferent() {
		List<Roles> usersroles;
		LOGGER.info("Role Type are different");
		usersroles = new ArrayList<>();
		LOGGER.info("First Object is Different");
		return usersroles;
	}

	private Roles setUserRole(RoleDetailDTO roleDetailsDTO, Roles nextWorkUserRole) {
		Roles userRole = new Roles();
		userRole.setId(roleDetailsDTO.getRoleId());
		if (null != nextWorkUserRole.getRoleDisplayName()
				&& !nextWorkUserRole.getRoleDisplayName().equalsIgnoreCase(roleDetailsDTO.getRoleDisplayName())) {
			throw new RestBadRequestException("Role Display name doesn't matches");
		}
		userRole.setRoleDisplayName(roleDetailsDTO.getRoleDisplayName());
		if (null != nextWorkUserRole.getRoleType()
				&& !nextWorkUserRole.getRoleType().equalsIgnoreCase(roleDetailsDTO.getRoleType())) {
			throw new RestBadRequestException("Role Type doesn't matches");
		}
		userRole.setRoleType(roleDetailsDTO.getRoleType());
		userRole.setHaveGIDList(nextWorkUserRole.isHaveGIDList());
		return userRole;
	}

	private void removeGIDFromUser(List<GidsRequestDTO> gidRequest, NextWorkUser userData) {
		LOGGER.info("removeGIDFromUser User : {}", userData);
		List<String> existingGidDataList = userData.getGidList();
		LOGGER.info("removeGIDFromUser GID LIST : {}", existingGidDataList);
		List<String> error = new ArrayList<>();
		Map<String, Integer> gidMap = userData.getUserGidMap();
		List<String> directGidList = userData.getDirectGidList();
		if (null != existingGidDataList) {
			for (GidsRequestDTO gidToRemove : gidRequest) {
				int index = gidToRemove.getIndex();
				String prefix = "{'" + index;
				boolean gidFound = false;

				if (gidToRemove.getGid().length() != 8) {
					error.add(prefix + "': 'Gid must be 8 alphanumeric characters'}");
					continue;
				}
				for (String removeGid : existingGidDataList) {
					LOGGER.info("removeGIDFromUser remove GID and gidToRemove : {} -> {} ", removeGid, gidToRemove);
					gidFound = updateGIDMapForGidRemoval(gidMap, gidToRemove.getGid(), gidFound, removeGid,directGidList);

				}
				if (!gidFound)
					error.add(prefix + "': 'Gid not found for user'}");

			}
			LOGGER.info("Error check : {}", error.isEmpty());
			if (!error.isEmpty()) {
				LOGGER.info("Error check message : {}", error);
				throw new RestBadRequestException(error.toString());
			}
			userData.setDirectGidList(directGidList);
			userData.setUserGidMap(gidMap);

		} else {
			throw new RestBadRequestException("The user doesn't have any gid associated with it");
		}

	}

	private boolean updateGIDMapForGidRemoval(Map<String, Integer> gidMap, String gidToRemove, boolean gidFound,
			String removeGid, List<String> gidDirectList) {
		LOGGER.info("updateGIDMapForGidRemoval remove GID : {}", removeGid);
		LOGGER.info("updateGIDMapForGidRemoval GID to remove : {}", gidToRemove);
		if (removeGid.equalsIgnoreCase(gidToRemove)) {
			gidFound = true;
			if (gidMap != null && gidMap.containsKey(removeGid)) {
				gidMap.computeIfPresent(removeGid, (key, value) -> value - 1);
				gidMap.remove(removeGid, 0);
				if(gidDirectList.contains(removeGid))
				{
					gidDirectList.remove(removeGid);
				}
			}

		}
		return gidFound;
	}

	private void addingScopingDataForUser(NextWorkUserPutRequestDTO userRequestDTO, NextWorkUser userData) {
		List<String> userGidList = userData.getGidList();
		if (userGidList == null) {
			userGidList = new ArrayList<>();
		}
		List<String> directGidList = userData.getDirectGidList();
		if (directGidList == null) {
			directGidList = new ArrayList<>();

		}
		List<String> gidDataList = new ArrayList<>();

		List<String> error = new ArrayList<>();

		Map<String, Integer> gidMap = userData.getUserGidMap();
		if (null == gidMap) {
			gidMap = new HashMap<>();
		}

		boolean hasError = false;
		for (GidsRequestDTO gidReq : userRequestDTO.getGids()) {
			hasError = addGidDataForUser(userGidList, directGidList, gidDataList, error, gidMap, hasError, gidReq);
		}

		if (hasError) {
			throw new RestBadRequestException(error.toString());
		}
		userData.setDirectGidList(directGidList);
		LOGGER.info("gidMap : {} ", gidMap);
		userData.setUserGidMap(gidMap);

	}

	private boolean addGidDataForUser(List<String> userGidList, List<String> directGidList, List<String> gidDataList,
			List<String> error, Map<String, Integer> gidMap, boolean hasError, GidsRequestDTO gidReq) {
		int index = gidReq.getIndex();
		String prefix = "{'" + index;

		if (gidReq.getGid().length() != 8) {
			error.add(prefix + "': 'GID must be 8 alphanumeric characters'}");
			hasError = true;
		} else {
			List<Scoping> scp = scopingRepository.findByRegexGId(gidReq.getGid());

			if (scp == null || scp.isEmpty()) {
				error.add(prefix + "': 'GID doesn't exist'}");
				hasError = true;
			} else if (userGidList.contains(gidReq.getGid())) {
				error.add(prefix + "': 'GID already exists'}");
				hasError = true;
			} else if (!gidDataList.contains(gidReq.getGid())) {
				setGidMap(gidDataList, gidMap, gidReq);
				if (!directGidList.contains(gidReq.getGid())) {
					directGidList.add(gidReq.getGid());
				}
			} else {
				error.add(prefix + "': 'GID is duplicate'}");
				hasError = true;
			}
		}
		return hasError;
	}

	private void setGidMap(List<String> gidDataList, Map<String, Integer> gidMap, GidsRequestDTO gidReq) {
		gidDataList.add(gidReq.getGid());
		if (!gidMap.containsKey(gidReq.getGid())) {
			gidMap.put(gidReq.getGid(), 1);
		} else {
			gidMap.put(gidReq.getGid(), gidMap.get(gidReq.getGid()) + 1);
		}
	}

	@Override
	@Transactional
	public String checkUserRole(String userId) {
		var userPlatformRoleName = "";
		Query q = new Query();
		q.addCriteria(Criteria.where("id").is(userId));
		LOGGER.info("userID : {}", userId);
		Optional<NextWorkUser> nextWorkUser = userService.findById(userId);
		if (nextWorkUser.isEmpty())
			throw new RestBadRequestException("No role assigned to USER");
		NextWorkUser nUser = nextWorkUser.get();
		if (nUser != null) {
			if (null != nUser.getRolesDetails() && !nUser.getRolesDetails().isEmpty()) {
				userPlatformRoleName = nUser.getRolesDetails().get(0).getRoleType();
			} else {
				throw new RestBadRequestException("No role assigned to USER");
			}
			if (!userPlatformRoleName.equalsIgnoreCase(RoleType.ADMIN.toString())) {
				throw new RestForbiddenException(
						"User does't have privilege to access this request. Please contact #Nextwork Support Team or ADMIN");
			}
		} else {
			throw new RestBadRequestException("User not exists/ Deactive User");
		}
		return userPlatformRoleName;
	}

	private void migrateUser(HttpServletRequest request, NextWorkUser preOnboardedUser, UsersInfoResponseDTO response) {

		JWTClaimsSet jwtClaimSet = CommonUtils.getJwtClaimSet(request.getHeader(AUTHORIZATION_HEADER));
		String userEmail = jwtClaimSet.getClaims().get(NextworkConstants.EMAIL).toString();
		String orgCode = jwtClaimSet.getClaims().get(NextworkConstants.ORG_CODE).toString();
		String firstName = jwtClaimSet.getClaims().get(NextworkConstants.GIVEN_NAME).toString();
		String lastName = jwtClaimSet.getClaims().get(NextworkConstants.FAMILY_NAME).toString();

		preOnboardedUser.setStatus("Active");
		preOnboardedUser.setEmail(userEmail.toLowerCase());
		preOnboardedUser.setOrgCode(orgCode);
		preOnboardedUser.setName(firstName + " " + lastName);
		userService.saveNextWorkUser(preOnboardedUser);

		response.setUid(preOnboardedUser.getId());
		response.setEmailId(preOnboardedUser.getEmail());
		response.setName(preOnboardedUser.getName());
		response.setStatus(preOnboardedUser.getStatus());
		String roleName = "";
		List<RoleDetailDTO> roleDetailsDTOList = new ArrayList<>();
		List<Roles> rolesList = preOnboardedUser.getRolesDetails();
		if (null != rolesList && !rolesList.isEmpty()) {
			for (Roles role : rolesList) {
				RoleDetailDTO roleDetailDTO = new RoleDetailDTO();
				roleDetailDTO.setRoleId(role.getId());
				roleDetailDTO.setRoleType(role.getRoleType());
				roleDetailDTO.setRoleDisplayName(role.getRoleDisplayName());
				roleDetailsDTOList.add(roleDetailDTO);
			}
			roleName = rolesList.get(0).getRoleType();
		}
		response.setRole(roleName);

		response.setRolesDetails(roleDetailsDTOList);
	}

	public void validateUserAndRole(String userEmail) {
		userService.validateUserAndRole(userEmail);
	}

}
