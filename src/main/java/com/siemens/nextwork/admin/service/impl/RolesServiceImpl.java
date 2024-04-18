package com.siemens.nextwork.admin.service.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;

import com.siemens.nextwork.admin.dto.GidsRequestDTO;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.MemberDTO;
import com.siemens.nextwork.admin.dto.RequestRoleDTO;
import com.siemens.nextwork.admin.dto.ResponseRoleDTO;
import com.siemens.nextwork.admin.dto.WorkStreamDTO;
import com.siemens.nextwork.admin.enums.ActionForGidList;
import com.siemens.nextwork.admin.enums.ActionForWorkStreamList;
import com.siemens.nextwork.admin.exception.ResourceNotFoundException;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.model.Member;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.model.Scoping;
import com.siemens.nextwork.admin.model.Workstream;
import com.siemens.nextwork.admin.model.WorkstreamGids;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.repo.RolesRepository;
import com.siemens.nextwork.admin.repo.ScopingRepository;
import com.siemens.nextwork.admin.repo.WorkStreamRepository;
import com.siemens.nextwork.admin.repo.WorkstreamGidRepository;
import com.siemens.nextwork.admin.service.RolesService;
import com.siemens.nextwork.admin.util.NextworkConstants;

@Service
public class RolesServiceImpl implements RolesService {

	@Autowired
	private RolesRepository rolesRepository;

	@Autowired
	private NextWorkUserRepository nextWorkUserRepository;

	@Autowired
	WorkStreamRepository workStreamRepository;

	@Autowired
	private NextWorkUserServiceImpl nextWorkServiceImpl;

	@Autowired
	ScopingRepository scopingRepository;

	@Autowired
	MongoOperations mongoOperations;

	@Autowired
	private WorkstreamGidRepository workstreamGidRepository;

	private static final Logger LOGGER = LoggerFactory.getLogger(RolesServiceImpl.class);

	@Transactional
	@Override
	public Roles createRole(String userEmail, RequestRoleDTO roleDTO) throws ResourceNotFoundException {
		duplicateRoleDisplayNameException(roleDTO);
		LOGGER.info("RequestRoleDTO :{}", roleDTO);
		NextWorkUser user = nextWorkUserRepository.findByEmail(userEmail);
		LOGGER.info("userId :{}", user.getId());
		nextWorkServiceImpl.validateUserAndRole(userEmail);
		Date date = new Date(System.currentTimeMillis());
		var timestamp = new Timestamp(System.currentTimeMillis());
		Roles role = new Roles();
		role.setRoleDisplayName(roleDTO.getRoleDisplayName());
		role.setRoleDescription(roleDTO.getRoleDescription());
		LOGGER.info("role Type :{}", roleDTO.getRoleType());
		if (null != roleDTO.getRoleType() && (roleDTO.getRoleType().equalsIgnoreCase(NextworkConstants.ADMIN)
				|| roleDTO.getRoleType().equalsIgnoreCase("USER"))) {
			throw new RestBadRequestException("Admin/User Role cannot be created");
		}
		if (null != roleDTO.getRoleType() && !roleDTO.getRoleType().equalsIgnoreCase("LOCAL ADMIN")
				&& !roleDTO.getRoleType().equalsIgnoreCase("POWER USER")) {
			throw new RestBadRequestException("Only Power User and Local Admin role can be created");
		}
		role.setRoleType(roleDTO.getRoleType());
		role.setDate(date);
		role.setCreatedBy(userEmail);
		role.setLinkedGIDupdatedBy(null);
		role.setCreatedByEmail(userEmail);
		role.setTime(timestamp);
		role.setEmail(userEmail);
		if (roleDTO.getMemberList() != null) {
			List<Member> memberList = new ArrayList<>();
			addMembersToRole(roleDTO, role, memberList);
		}
		Map<String, Integer> gidMap = new HashMap<>();
		role.setGidMap(gidMap);
		if (roleDTO.getWorkstreamList() != null) {

			if (roleDTO.getWorkstreamList().size() > 25)
				throw new RestBadRequestException("Exceed limit. Maximum 25 workstream data can be added at a time.");
			List<WorkStreamDTO> workstreamsDtoList = new ArrayList<>();

			addWorkstreamToRole(roleDTO, role, workstreamsDtoList);
		}
		setGidDataForRole(roleDTO, role);
		LOGGER.info("role : {} ", role);
		Roles saveRole = rolesRepository.save(role);

		if (roleDTO.getMemberList() != null) {
			List<Member> memberList = new ArrayList<>();
			addRoleToUser(roleDTO, saveRole.getId(), memberList);
		}

		return saveRole;
	}

	private void setGidDataForRole(RequestRoleDTO roleDTO, Roles role) {
		LOGGER.info("gidList : {}", roleDTO.getGids());
		if (roleDTO.getGids() != null) {

			if (roleDTO.getGids().size() > 25)
				throw new RestBadRequestException("Exceed limit. Maximum 25 GID data can be added at a time.");
			addGidToRole(roleDTO, role);
		}
		if (null != role.getGidMap()) {
			addGidList(role);
		}
	}

	private void addRoleToUser(RequestRoleDTO roleDTO, String roleId, List<Member> memberList) {

		List<MemberDTO> roleMembersDTOList = roleDTO.getMemberList();
		List<String> userUids = memberList.stream().map(Member::getUid).toList();

		for (MemberDTO wsMember : roleMembersDTOList) {
			LOGGER.info("wsMember : {} ", wsMember);
			Optional<NextWorkUser> oUser = nextWorkUserRepository.findById(wsMember.getUid());
			if (oUser.isEmpty())
				throw new RestBadRequestException(NextworkConstants.USER_NOT_PRESENT_OR_DEACTIVE);
			NextWorkUser nextWorkUser = oUser.get();
			if (nextWorkUser != null && nextWorkUser.getStatus().equalsIgnoreCase("Active")) {
				if (userUids.contains(wsMember.getUid())) {
					continue;
				}
				List<Roles> rolesList = nextWorkUser.getRolesDetails();
				if (rolesList == null) {
					rolesList = new ArrayList<>();
				}
				Roles roles = new Roles();
				roles.setId(roleId);
				roles.setRoleDisplayName(roleDTO.getRoleDisplayName());
				roles.setRoleType(roleDTO.getRoleType());
				rolesList.add(roles);
				nextWorkUser.setRolesDetails(rolesList);

				nextWorkUserRepository.save(nextWorkUser);
			} else {
				throw new RestBadRequestException(NextworkConstants.USER_NOT_PRESENT_OR_DEACTIVE);
			}
		}

	}

	private void addWorkstreamToRole(RequestRoleDTO roleDTO, Roles roleData, List<WorkStreamDTO> workstreamsDtoList) {
		List<WorkStreamDTO> workstreamsDTOList = roleDTO.getWorkstreamList();
		List<String> workstreamUidList= workstreamsDtoList.stream().map(WorkStreamDTO::getUid).toList();
		
		List<String> workstreamUids = new ArrayList<>();
		
		if(workstreamUidList!=null) {
			workstreamUids.addAll(workstreamUidList);
		}
		
		Map<String, Integer> gidMap = roleData.getGidMap();

		for (WorkStreamDTO ws : workstreamsDTOList) {

			Optional<Workstream> ows = workStreamRepository.findById(ws.getUid());
			if (ows.isEmpty())
				throw new RestBadRequestException("The workStream is not present");
			Workstream workStream = ows.get();
			if (workStream != null) {

				if (!workstreamUids.contains(ws.getUid())) {
					setWorkstreamDetails(workstreamsDtoList, workstreamUids, gidMap, ws, workStream);
				}
			} else {
				throw new RestBadRequestException("The workStream is not present");
			}
			roleData.setGidMap(gidMap);
			roleData.setWorkstreamList(workstreamsDtoList);

		}
	}

	private void setWorkstreamDetails(List<WorkStreamDTO> workstreamsDtoList, List<String> workstreamUids,
			Map<String, Integer> gidMap, WorkStreamDTO ws, Workstream workStream) {
		WorkstreamGids workstreamGids = workstreamGidRepository.findByWorkstreamId(workStream.getUid());
		setWorkstreamData(workstreamsDtoList, workstreamUids, ws, workStream, workstreamGids);

		List<String> gidList = workstreamGids.getGidList();
		if (null != gidList) {
			addGidMap(gidMap, gidList);
		}
	}

	private void setWorkstreamData(List<WorkStreamDTO> workstreamsDtoList, List<String> workstreamUids,
								   WorkStreamDTO ws, Workstream workStream, WorkstreamGids workstreamGids) {
		workstreamUids.add(ws.getUid());
		WorkStreamDTO workstreamDTO = new WorkStreamDTO();
		workstreamDTO.setUid(ws.getUid());
		if (!ws.getName().equalsIgnoreCase(workStream.getName())) {
			throw new RestBadRequestException("Workstream Names does not match");
		}
		workstreamDTO.setName(ws.getName());
		workstreamDTO.setGidList(workstreamGids.getGidList());
		workstreamsDtoList.add(workstreamDTO);
	}

	private void addGidMap(Map<String, Integer> gidMap, List<String> gidList) {
		for (String gid : gidList) {
			if (!gidMap.containsKey(gid)) {
				gidMap.put(gid, 1);
			} else {
				gidMap.put(gid, gidMap.get(gid) + 1);
			}
		}
	}

	public ResponseRoleDTO getRoleById(String id, String userEmail) {
		nextWorkServiceImpl.validateUserAndRole(userEmail);
		Roles roles = rolesRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found with id " + id));
		return convertToDTO(roles);
	}

	@Override
	public List<ResponseRoleDTO> getAllRoles(String userEmail) {
		nextWorkServiceImpl.validateUserAndRole(userEmail);
		List<Roles> rolesList = rolesRepository.findAll().stream().filter(i -> i.getIsDeleted().equals(Boolean.FALSE))
				.toList();
		return rolesList.stream().map(this::convertToDTO).toList();
	}

	private ResponseRoleDTO convertToDTO(Roles roles) {
		ResponseRoleDTO roleDTOs = new ResponseRoleDTO();
		roleDTOs.setId(roles.getId());
		roleDTOs.setRoleDisplayName(roles.getRoleDisplayName());
		roleDTOs.setRoleDescription(roles.getRoleDescription());
		roleDTOs.setRoleType(roles.getRoleType());
		if (roles.getWorkstreamList() != null) {
			roleDTOs.setWorkstreamList(roles.getWorkstreamList());
		}
		if (roles.getMemberList() != null) {
			roleDTOs.setMemberList(roles.getMemberList().stream().map(this::convertToDTO).toList());
		}
		if (roles.getGidList() != null && !roles.getGidList().isEmpty()) {
			roleDTOs.setHaveGIDList(true);
		}
		return roleDTOs;
	}

	private MemberDTO convertToDTO(Member member) {
		MemberDTO memberDTO = new MemberDTO();
		memberDTO.setMemberName(member.getMemberName());
		memberDTO.setMemberEmail(member.getMemberEmail());
		memberDTO.setUid(member.getUid());
		return memberDTO;
	}

	@Override
	public IdResponseDTO updateRoleById(String userEmail, String roleId, RequestRoleDTO roleDTO, String actionGidList,
			String actionWorkStreamList) {
		LOGGER.info("Inside updateRoleById service");
		nextWorkServiceImpl.validateUserAndRole(userEmail);
		Roles existingRole = rolesRepository.findById(roleId)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));
		LOGGER.info("Role id found in db");
		if (existingRole.getRoleType().equalsIgnoreCase(NextworkConstants.ADMIN)
				|| existingRole.getRoleType().equalsIgnoreCase("USER")) {
			throw new RestBadRequestException("ADMIN/USER role cannot be updated");
		}
		Date date = new Date(System.currentTimeMillis());
		var timestamp = new Timestamp(System.currentTimeMillis());
		if (!existingRole.getRoleDisplayName().equalsIgnoreCase(roleDTO.getRoleDisplayName())) {
			duplicateRoleDisplayNameException(roleDTO);
		}
		existingRole.setRoleDisplayName(roleDTO.getRoleDisplayName());
		existingRole.setRoleDescription(roleDTO.getRoleDescription());
		if (null != roleDTO.getRoleType() && (roleDTO.getRoleType().equalsIgnoreCase(NextworkConstants.ADMIN)
				|| roleDTO.getRoleType().equalsIgnoreCase("USER"))) {
			throw new RestBadRequestException("Admin/User Role cannot be created/updated");
		}
		if (null != roleDTO.getRoleType()
				&& !roleDTO.getRoleType().equalsIgnoreCase(NextworkConstants.ROLE_TYPE_LOCAL_ADMIN)
				&& !roleDTO.getRoleType().equalsIgnoreCase("POWER USER")) {
			throw new RestBadRequestException("Only Power User and Local Admin role can be created");
		}

		existingRole.setRoleType(roleDTO.getRoleType());
		existingRole.setDate(date);
		existingRole.setLinkedGIDupdatedBy(null);
		existingRole.setTime(timestamp);

		validateAndUpdateMemberList(roleDTO, existingRole);

		validateAndUpdateGidList(roleDTO, actionGidList, existingRole);

		validateAndUpdateWorkStreamList(roleDTO, actionWorkStreamList, existingRole);
		if (null != existingRole.getGidMap()) {
			addGidList(existingRole);
		}
		boolean haveGidList = false;
		if (null != existingRole.getGidList() && !existingRole.getGidList().isEmpty()) {
			haveGidList = true;
		}
		existingRole.setHaveGIDList(haveGidList);
		LOGGER.info("saving to database");
		rolesRepository.save(existingRole);
		IdResponseDTO responseDTO = new IdResponseDTO();
		responseDTO.setUid(existingRole.getId());
		return responseDTO;

	}

	private void addGidList(Roles existingRole) {
		List<String> gidList = new ArrayList<>();

		for (Map.Entry<String, Integer> gid : existingRole.getGidMap().entrySet()) {
			if (gid.getValue() > 0) {
				gidList.add(gid.getKey());
			}
		}
		existingRole.setGidList(gidList);
	}

	private void duplicateRoleDisplayNameException(RequestRoleDTO roleDTO) {
		Optional<Roles> rolesOpt = rolesRepository.findByRoleTypeAndRoleDisplayNameAndIsDeleted(roleDTO.getRoleType(),
				roleDTO.getRoleDisplayName(), false);
		if (rolesOpt.isPresent()) {
			Roles role = rolesOpt.get();
			if (null != role) {
				throw new RestBadRequestException(
						"A role with the same display name and same Role Type already exists");
			}
		}
	}

	private void validateAndUpdateGidList(RequestRoleDTO roleDTO, String actionGidList, Roles existingRole) {
		if (actionGidList != null) {
			LOGGER.info("non empty gid list");

			if (actionGidList.equalsIgnoreCase(ActionForGidList.GIDINCLUSION.toString())) {
				LOGGER.info("GIDINCLUSION");

				if (roleDTO.getGids() != null && roleDTO.getGids().size() > 25)
					throw new RestBadRequestException("Exceed limit. Maximum 25 GID data can be added at a time.");
				addGidToRole(roleDTO, existingRole);
			}

			else if (actionGidList.equalsIgnoreCase(ActionForGidList.GIDEXCLUSION.toString())) {
				LOGGER.info("GIDEXCLUSION");
				if (roleDTO.getGids() != null && roleDTO.getGids().size() > 25)
					throw new RestBadRequestException("Exceed limit. Maximum 25 GID data can be removed at a time.");
				removeGIDFromRole(roleDTO.getGids(), existingRole);
			} else {
				throw new RestBadRequestException("actionGidList should be either gidInclusion or gidExclusion");
			}
		}
	}

	private void validateAndUpdateMemberList(RequestRoleDTO roleDTO, Roles roleData) {

		LOGGER.info("MEMBER UPDATE");
		List<Member> memberList = roleData.getMemberList();
		if (null == memberList || memberList.isEmpty()) {
			memberList = new ArrayList<>();

		}
		addMembersToRole(roleDTO, roleData, memberList);

	}

	private void validateAndUpdateWorkStreamList(RequestRoleDTO roleDTO, String actionWorkStreamList,
			Roles existingRole) {

		if (actionWorkStreamList != null) {
			LOGGER.info("non empty member list, actionWokstreamList= {}", actionWorkStreamList);

			if (actionWorkStreamList.equalsIgnoreCase(ActionForWorkStreamList.WSINCLUSION.toString())) {
				workstreamInclusion(roleDTO, existingRole);
			}

			else if (actionWorkStreamList.equalsIgnoreCase(ActionForWorkStreamList.WSEXCLUSION.toString())) {
				workstreamExclusion(roleDTO, existingRole);
			} else {
				throw new RestBadRequestException("actionWokstreamList should be either wsInclusion or wsExclusion");
			}

		}

	}

	private void workstreamExclusion(RequestRoleDTO roleDTO, Roles existingRole) {
		LOGGER.info("WORKSTREAM EXCLUSION");
		if (roleDTO.getWorkstreamList() != null && roleDTO.getWorkstreamList().size() > 25)
			throw new RestBadRequestException("Exceed limit. Maximum 25 workstream data can be removed at a time.");
		removeWorkstreamFromRole(roleDTO.getWorkstreamList(), existingRole);
	}

	private void workstreamInclusion(RequestRoleDTO roleDTO, Roles existingRole) {
		LOGGER.info("WORKSTREAM INCLUSION");
		List<WorkStreamDTO> workstreamsDTOList = existingRole.getWorkstreamList();
		if (workstreamsDTOList == null) {
			workstreamsDTOList = new ArrayList<>();
		}
		if (roleDTO.getWorkstreamList() != null && roleDTO.getWorkstreamList().size() > 25)
			throw new RestBadRequestException("Exceed limit. Maximum 25 workstream data can be added at a time.");
		addWorkstreamToRole(roleDTO, existingRole, workstreamsDTOList);
	}

	private void removeWorkstreamFromRole(List<WorkStreamDTO> workstreamList, Roles existingRole) {

		List<WorkStreamDTO> workstreamsDTOList = existingRole.getWorkstreamList();
		if (null != workstreamsDTOList) {

			List<String> roleWorkStreams = workstreamsDTOList.stream().map(WorkStreamDTO::getUid).toList();

			List<WorkStreamDTO> removeWorkstreamsDTOs = workstreamList.stream()
					.filter(e -> roleWorkStreams.contains(e.getUid())).toList();
			LOGGER.info("removeWorkstreamsDTOs : {} ", removeWorkstreamsDTOs);

			List<String> removeWorkstreamsId = workstreamList.stream().map(WorkStreamDTO::getUid).toList();
			List<String> gids = new ArrayList<>();
			for (String wsId : removeWorkstreamsId) {
				settingGidData(roleWorkStreams, gids, wsId);

			}
			Map<String, Integer> gidMap = existingRole.getGidMap();
			for (String gid : gids) {
				removeGidFromGidMap(gidMap, gid);
			}
			workstreamsDTOList.removeAll(removeWorkstreamsDTOs);
			LOGGER.info("workstreamsDTOList after Removal : {} ", workstreamsDTOList);
			existingRole.setWorkstreamList(workstreamsDTOList);
			existingRole.setGidMap(gidMap);
		} else {
			throw new RestBadRequestException("The required role doesn't have any associated workstream");
		}

	}

	private void settingGidData(List<String> roleWorkStreams, List<String> gids, String wsId) {
		if (!roleWorkStreams.contains(wsId)) {
			throw new RestBadRequestException("Workstream " + wsId + " not found in role");
		}
		Optional<Workstream> ows = workStreamRepository.findById(wsId);
		if (ows.isEmpty())
			throw new RestBadRequestException("The Workstream is not available.");
		Workstream workStream = ows.get();
		WorkstreamGids workstreamGids = workstreamGidRepository.findByWorkstreamId(workStream.getUid());
		if (null != workStream && CollectionUtils.isNotEmpty(workstreamGids.getGidList())){
			gids.addAll(workstreamGids.getGidList());
		}
	}

	private void removeGidFromGidMap(Map<String, Integer> gidMap, String gid) {
		if (gidMap != null && gid != null && gidMap.containsKey(gid)) {
			gidMap.computeIfPresent(gid, (key, value) -> value - 1);
			gidMap.remove(gid, 0);
		}
	}

	private void addMembersToRole(RequestRoleDTO roleDTO, Roles roleData, List<Member> memberList) {
		List<MemberDTO> roleMembersDTOList = roleDTO.getMemberList();
		if (null != roleMembersDTOList) {
			List<String> userUids = new ArrayList<>();
			if (!memberList.isEmpty()) {
				userUids = memberList.stream().map(Member::getUid).toList();
			}

			for (MemberDTO wsMember : roleMembersDTOList) {
				addMember(roleDTO, memberList, userUids, wsMember);
			}
		}
		roleData.setMemberList(memberList);

	}

	private void addMember(RequestRoleDTO roleDTO, List<Member> memberList, List<String> userUids, MemberDTO wsMember) {
		LOGGER.info("wsMember : {} ", wsMember);
		Optional<NextWorkUser> oUser = nextWorkUserRepository.findById(wsMember.getUid());
		if (oUser.isEmpty())
			throw new RestBadRequestException("The user is either not present or Deactive");
		NextWorkUser nextWorkUser = oUser.get();
		if (nextWorkUser != null && nextWorkUser.getStatus().equalsIgnoreCase("Active")) {

			if (!userUids.contains(wsMember.getUid())) {

				userUids.add(wsMember.getUid());
				Member member = new Member();
				member.setUid(wsMember.getUid());
				validateMemberData(wsMember, nextWorkUser);
				member.setMemberName(wsMember.getMemberName());
				member.setMemberEmail(wsMember.getMemberEmail());
				String memberRoleType = "";
				memberRoleType = setMemberRoleType(nextWorkUser, memberRoleType);
				addMemberIfRoleTypeMatches(roleDTO, memberList, member, memberRoleType);
			}
		} else {
			throw new RestBadRequestException("The user is either not present or Deactive");
		}
	}

	private String setMemberRoleType(NextWorkUser nextWorkUser, String memberRoleType) {
		if (null != nextWorkUser.getRolesDetails() && !nextWorkUser.getRolesDetails().isEmpty()) {
			memberRoleType = nextWorkUser.getRolesDetails().get(0).getRoleType();

		}
		return memberRoleType;
	}

	private void addMemberIfRoleTypeMatches(RequestRoleDTO roleDTO, List<Member> memberList, Member member,
			String memberRoleType) {
		if (!memberRoleType.isBlank() && memberRoleType.equalsIgnoreCase(roleDTO.getRoleType())) {
			memberList.add(member);

		} else {
			throw new RestBadRequestException("Role " + roleDTO.getRoleType() + " cannot be assigned to given member");
		}
	}

	private void validateMemberData(MemberDTO wsMember, NextWorkUser nextWorkUser) {
		LOGGER.info("Nextwork User : {}", nextWorkUser);
		LOGGER.info("wsMember : {}", wsMember);
		if (!nextWorkUser.getName().equalsIgnoreCase(wsMember.getMemberName())) {
			throw new RestBadRequestException("Member Name doesn't match");
		}

		if (!nextWorkUser.getEmail().equalsIgnoreCase(wsMember.getMemberEmail())) {
			throw new RestBadRequestException("Member Email doesn't match");
		}
	}

	private void removeGIDFromRole(List<GidsRequestDTO> gidRequestList, Roles roleData) {
		List<String> existingGidDataList = roleData.getGidList();
		List<String> error = new ArrayList<>();
		Map<String, Integer> gidMap = roleData.getGidMap();
		List<String> directGidList = roleData.getDirectGidList();
		if (null != existingGidDataList) {
			for (GidsRequestDTO gidToRemove : gidRequestList) {
				removeGidData(existingGidDataList, error, gidMap, gidToRemove,directGidList);

			}
			if (!error.isEmpty()) {
				throw new RestBadRequestException(error.toString());
			}
			roleData.setDirectGidList(directGidList);
			roleData.setGidMap(gidMap);

		} else {
			throw new RestBadRequestException("Role doesn't have any associated gid");
		}

	}

	private void removeGidData(List<String> existingGidDataList, List<String> error, Map<String, Integer> gidMap,
			GidsRequestDTO gidToRemove,List<String> directGidList) {
		int index = gidToRemove.getIndex();
		String prefix = "{'" + index;
		boolean gidFound = false;

		if (gidToRemove.getGid().length() != 8) {
			error.add(prefix + "': 'GID must be 8 alphanumeric characters'}");
			return;
		}
		for (String removeGid : existingGidDataList) {
			if (removeGid.equalsIgnoreCase(gidToRemove.getGid())) {
				gidFound = true;
				removeGidFromGidMap(gidMap, removeGid);
				if(directGidList.contains(removeGid))
				{
					directGidList.remove(removeGid);
				}
			}

		}
		if (!gidFound)
			error.add(prefix + "': 'GID not found for user'}");
	}

	private void addGidToRole(RequestRoleDTO roleRequestDTO, Roles roleData) {
		LOGGER.info("Gid Inclusion");
		List<String> userGidList = roleData.getGidList();
		if (userGidList == null) {
			userGidList = new ArrayList<>();
		}
		List<String> gidDataList = new ArrayList<>();

		List<String> error = new ArrayList<>();
		List<String> directGidList = roleData.getDirectGidList();
		if (directGidList == null) {
			directGidList = new ArrayList<>();

		}
		Map<String, Integer> gidMap = roleData.getGidMap();
		if (null == gidMap) {
			gidMap = new HashMap<>();
		}

		boolean hasError = false;
		for (GidsRequestDTO gidReq : roleRequestDTO.getGids()) {
			hasError = setGidstoRole(userGidList, gidDataList, error, directGidList, gidMap, hasError, gidReq);
		}

		if (hasError) {
			throw new RestBadRequestException(error.toString());
		}
		roleData.setDirectGidList(directGidList);
		LOGGER.info("gidMap : {} ", gidMap);
		roleData.setGidMap(gidMap);

	}

	private boolean setGidstoRole(List<String> userGidList, List<String> gidDataList, List<String> error,
			List<String> directGidList, Map<String, Integer> gidMap, boolean hasError, GidsRequestDTO gidReq) {
		int index = gidReq.getIndex();
		String prefix = "{'" + index;

		if (gidReq.getGid().length() != 8) {
			error.add(prefix + "': 'GID must be 8 alphanumeric characters'}");
			hasError = true;
		} else {
			List<Scoping> scps = scopingRepository.findByRegexGId(gidReq.getGid().toLowerCase());

			if (scps.isEmpty()) {
				error.add(prefix + "': 'GID doesn't exists'}");
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
		if (gidMap == null) {
			gidMap = new HashMap<>();
		}
		if (!gidMap.containsKey(gidReq.getGid())) {
			gidMap.put(gidReq.getGid(), 1);
		} else {
			gidMap.put(gidReq.getGid(), gidMap.get(gidReq.getGid()) + 1);
		}

	}

	@Override
	public List<ResponseRoleDTO> getRolesBySearchQuery(String userEmail, String searchQuery) {
		List<ResponseRoleDTO> rolesResponseDTOList = new ArrayList<>();
		nextWorkServiceImpl.validateUserAndRole(userEmail);
		if (searchQuery.isEmpty()) {
			throw new RestBadRequestException("Please provide some data to search");
		}
		List<Roles> rolesList = rolesRepository.findByNameIsLike(searchQuery).stream()
				.filter(i -> i.getIsDeleted().equals(Boolean.FALSE)).toList();
		if (rolesList.isEmpty()) {
			throw new ResourceNotFoundException("Request Role was not found in system");
		}
		LOGGER.info("rolesList size= {}", rolesList.size());
		for (Roles role : rolesList) {
			ResponseRoleDTO roleResponseDTO = new ResponseRoleDTO();
			roleResponseDTO.setRoleDisplayName(role.getRoleDisplayName());
			roleResponseDTO.setRoleType(role.getRoleType());
			roleResponseDTO.setId(role.getId());
			rolesResponseDTOList.add(roleResponseDTO);
		}
		return rolesResponseDTOList;
	}

	@Override
	public List<IdResponseDTO> deleteRoles(String userEmail, List<IdResponseDTO> idResponseDTOList) {
		List<IdResponseDTO> responseList = new ArrayList<>();
		nextWorkServiceImpl.validateUserAndRole(userEmail);
		List<Roles> rolesList = rolesRepository.findAll().stream().filter(i -> i.getIsDeleted().equals(Boolean.FALSE))
				.toList();
		List<String> error = new ArrayList<>();

		if (null != rolesList) {
			for (IdResponseDTO id : idResponseDTOList) {

				boolean found = false;
				for (Roles role : rolesList) {

					found = deleteRoleIfIdFound(responseList, id, found, role);
				}
				if (!found) {
					error.add("id : " + id.getUid() + " not found in Roles data ");
				}
			}
		}
		if (!error.isEmpty()) {
			throw new RestBadRequestException(error.toString());
		}
		if (null != rolesList) {
			rolesRepository.saveAll(rolesList);

		}
		return responseList;
	}

	private boolean deleteRoleIfIdFound(List<IdResponseDTO> responseList, IdResponseDTO id, boolean found, Roles role) {
		if (id.getUid().equals(role.getId())) {
			found = true;
			if (role.getRoleType().equalsIgnoreCase(NextworkConstants.ADMIN)
					|| (role.getRoleType().equalsIgnoreCase("USER"))) {
				throw new RestBadRequestException("Role ADMIN/ USER cannot be deleted");
			}
			role.setIsDeleted(true);
			IdResponseDTO response = new IdResponseDTO();
			response.setUid(role.getId());
			responseList.add(response);

		}
		return found;
	}

}
