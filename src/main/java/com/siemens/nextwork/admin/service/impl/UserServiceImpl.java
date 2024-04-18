package com.siemens.nextwork.admin.service.impl;

import static com.siemens.nextwork.admin.util.NextworkConstants.ADMIN;
import static com.siemens.nextwork.admin.util.NextworkConstants.USER_NOT_EXIST;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siemens.nextwork.admin.enums.RoleType;
import com.siemens.nextwork.admin.exception.ResourceNotFoundException;
import com.siemens.nextwork.admin.exception.RestForbiddenException;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.util.NextworkConstants;

@Service
@Transactional
public class UserServiceImpl implements UserService {

	@Autowired
	UserService userService;
	
	@Autowired
	private NextWorkUserRepository nextWorkUserRepository;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

	@Override
	public String findUserIdByEmail(String userEmail) {
		LOGGER.info("User email in findUserIdByEmail method : {}", userEmail);
		return findByEmail(userEmail);
	}
	
	@Override
	public String findByEmail(String email) {
		Optional<NextWorkUser> nextWorkUser = nextWorkUserRepository.findUserEntityByEmail(email);
		LOGGER.info("User email in findByEmail method : {}  and user id check : {} ", email, nextWorkUser.isPresent());
		if (nextWorkUser.isPresent() && nextWorkUser.get().getStatus().equalsIgnoreCase(NextworkConstants.ACTIVE)) {
			LOGGER.info("Inside nextwork user and id : {}", nextWorkUser.get().getId());
			return nextWorkUser.get().getId();
		} else {
			throw new ResourceNotFoundException(USER_NOT_EXIST);
		}
	}

	public void checkAdminUserRole(String email) {
		Optional<NextWorkUser> userPlatformRole = nextWorkUserRepository.findByUserEmail(email);
		if (userPlatformRole.isPresent() && userPlatformRole.get().getStatus().equalsIgnoreCase(NextworkConstants.ACTIVE)) {
			List<Roles> roles= userPlatformRole.get().getRolesDetails();
			if(roles == null || roles.isEmpty()) {
				throw new ResourceNotFoundException("User roles doesn't exist.");
			}
			Optional<Roles> role = roles.stream().filter(r -> r.getRoleType().equalsIgnoreCase(ADMIN)).findFirst();
			if (!role.isPresent()) {
				throw new RestForbiddenException("Only Admin can access this functionality.");
			}
		}
		else
		{
			throw new RestForbiddenException("User doesn't exists/Deactive User");
		}
		
	}

	@Override
	public void validateUserAndRole(String userEmail) {
		Optional<NextWorkUser> findUserByEmail = nextWorkUserRepository.findByUserEmail(userEmail);
		LOGGER.info("user Email:{}", userEmail);
		if (findUserByEmail.isPresent() && findUserByEmail.get().getStatus().equalsIgnoreCase(NextworkConstants.ACTIVE)) {
			NextWorkUser user = findUserByEmail.get();
			String userRole = "";
			if (null != user.getRolesDetails() && !user.getRolesDetails().isEmpty()) {
				userRole = user.getRolesDetails().get(0).getRoleType();
			}
			LOGGER.info("NextWORK userRole :{}", userRole);
			if (!userRole.equalsIgnoreCase(RoleType.ADMIN.toString())) {
				throw new RestForbiddenException(
						"User does't have privilege to access this request. Please contact #Nextwork Support Team or ADMIN");
			}
		} else
			throw new ResourceNotFoundException(USER_NOT_EXIST);

	}

	@Override
	public List<NextWorkUser> findAll() {
		return nextWorkUserRepository.findAll();
	}

	@Override
	public Optional<NextWorkUser> findById(String id) {
		return nextWorkUserRepository.findById(id);
	}

	@Override
	public Optional<NextWorkUser> findByUserEmail(String userEmail) {
		return nextWorkUserRepository.findByUserEmail(userEmail);
	}

	@Override
	public NextWorkUser saveNextWorkUser(NextWorkUser nextworkUserData) {
		return nextWorkUserRepository.save(nextworkUserData);
	}

	@Override
	public void validateUserRole(String userEmail) {
		Optional<NextWorkUser> userPlatformRole = nextWorkUserRepository.findByUserEmail(userEmail);
		if (userPlatformRole.isPresent() && userPlatformRole.get().getStatus().equalsIgnoreCase(NextworkConstants.ACTIVE)) {
			List<Roles> roles= userPlatformRole.get().getRolesDetails();
			if(roles == null || roles.isEmpty()) {
				throw new ResourceNotFoundException("User roles doesn't exist.");
			}
		} else {
			throw new ResourceNotFoundException(USER_NOT_EXIST);
		}
	}
	
}
