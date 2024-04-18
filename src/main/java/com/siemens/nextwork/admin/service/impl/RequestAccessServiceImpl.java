package com.siemens.nextwork.admin.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.RequestAccessDTO;
import com.siemens.nextwork.admin.enums.StatusType;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.exception.RestForbiddenException;
import com.siemens.nextwork.admin.exception.RestOkException;
import com.siemens.nextwork.admin.exception.RestTooManyRequestsException;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.service.RequestAccessService;
import com.siemens.nextwork.admin.service.UserService;

@Service
@Transactional
public class RequestAccessServiceImpl implements RequestAccessService {


	@Autowired
	UserService userService;

	@Autowired
	private NextWorkUserRepository nextworkUserRepository;

	@Override
	public IdResponseDTO createAccessRequest(RequestAccessDTO requestAccessDTO) {
		NextWorkUser newUser = getPreOnboardedUserEntity(requestAccessDTO);

		return IdResponseDTO.builder().uid(newUser.getId()).build();
	}

	private NextWorkUser getPreOnboardedUserEntity(RequestAccessDTO requestAccessDTO) {
		Optional<NextWorkUser> user = nextworkUserRepository.findByUserEmail(requestAccessDTO.getEmail());
		if (user.isPresent() && (user.get().getStatus().equalsIgnoreCase(StatusType.ACCEPTED.toString())
				|| user.get().getStatus().equalsIgnoreCase(StatusType.ACTIVE.toString()))) {
			throw new RestOkException("You can acccess the system by login");
		} else if (user.isPresent() && user.get().getStatus().equalsIgnoreCase(StatusType.PENDING.toString())) {
			throw new RestTooManyRequestsException("Already Access Request submitted");
		} else if (user.isPresent() && user.get().getStatus().equalsIgnoreCase(StatusType.DEACTIVE.toString())) {
			throw new RestForbiddenException(
					"Your access has been revoked.Please contact Support : futureofwork@siemens.com");
		} else if (user.isPresent() && user.get().getStatus().equalsIgnoreCase(StatusType.REJECTED.toString())) {
			user.get().setStatus(StatusType.PENDING.toString());
			return user.get();
		} else if (user.isPresent()) {
			throw new RestBadRequestException("Invalid input");
		}
		Date date = new Date(System.currentTimeMillis());
		NextWorkUser requestAccessNewUser = new NextWorkUser();
		requestAccessNewUser.setEmail(requestAccessDTO.getEmail());
		requestAccessNewUser.setPurpose(requestAccessDTO.getPurpose());
		requestAccessNewUser.setCreationDate(date.toString());
		requestAccessNewUser.setRolesDetails(new ArrayList<>());
		requestAccessNewUser.setStatus(StatusType.PENDING.toString());
		nextworkUserRepository.save(requestAccessNewUser);
		return requestAccessNewUser;
	}

}
