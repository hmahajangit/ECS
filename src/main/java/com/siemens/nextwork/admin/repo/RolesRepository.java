package com.siemens.nextwork.admin.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.siemens.nextwork.admin.model.Roles;

@Repository
public interface RolesRepository extends MongoRepository<Roles, String> {
	
	Optional<Roles> findByRoleDisplayName(String roleDisplayName);
	
	List<Roles> findByEmail(String email);
	
	@Query("{ 'email' : ?0 }")
	Optional<Roles> findByUserEmail(String id);
	
	@Query(value = "{'roleDisplayName': {$regex : ?0, $options: 'i'}}")
	List<Roles> findByNameIsLike(String roleDisplayName);
	
	Optional<Roles> findByRoleType(String roleType);

	Optional<Roles> findByRoleTypeAndRoleDisplayNameAndIsDeleted(String roleType, String roleDisplayName,Boolean val);
	
	Optional<Roles> findByRoleTypeAndRoleDisplayName(String roleType, String roleDisplayName);


	@Query(value = "{'id' : { '$in' : ?0} }")
	List<Roles> findAllByRoleIds(List<String> roleIds);

	@Query(value = "{'id' : { '$in' : ?0}, 'isDeleted' : false }")
	List<Roles> findAllActiveRolesByIds(List<String> roleIds);

}
