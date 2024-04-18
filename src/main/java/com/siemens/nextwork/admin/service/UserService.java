package com.siemens.nextwork.admin.service;

import java.util.List;
import java.util.Optional;

import com.siemens.nextwork.admin.model.NextWorkUser;

public interface UserService {

	String findUserIdByEmail(String userEmail);
		
	String findByEmail(String email);

	void checkAdminUserRole(String userEmail);

	void validateUserAndRole(String userEmail);

	List<NextWorkUser> findAll();

	Optional<NextWorkUser> findById(String id);

	Optional<NextWorkUser> findByUserEmail(String userEmail);

	NextWorkUser saveNextWorkUser(NextWorkUser nextworkUserData);

	void validateUserRole(String userEmail);

}
