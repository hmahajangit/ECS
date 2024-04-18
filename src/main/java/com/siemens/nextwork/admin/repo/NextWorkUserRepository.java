package com.siemens.nextwork.admin.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.siemens.nextwork.admin.dto.RoleDetailDTO;
import com.siemens.nextwork.admin.model.NextWorkUser;


@Repository
public interface NextWorkUserRepository extends MongoRepository<NextWorkUser, String> {
	
	@Query("{ 'email' : ?0 }")
	Optional<NextWorkUser> findByUserEmail(String email);

	NextWorkUser findByEmail(String email);

	@Query("{ 'id' : ?0 }")
	Optional<NextWorkUser> findById(String id);

	@Query("{ 'id' : ?0 }")
	List<RoleDetailDTO> findRolesDetailsByUserId(String userId);

	Optional<NextWorkUser> findUserEntityByEmail(String userEmailToSearch);
	
}
