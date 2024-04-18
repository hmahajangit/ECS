package com.siemens.nextwork.admin.repo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.siemens.nextwork.admin.dto.IdDTO;
import com.siemens.nextwork.admin.model.SiemensMySkills;

@Repository
public interface SiemensMySkillsRepository extends MongoRepository<SiemensMySkills, String> {

	void deleteAllByIsDeleted(Boolean isDeleted);

	@Query(value = "{'isDeleted' : ?0}", fields = "{'_id' : 1}")
	List<IdDTO> findAllIdsByIsDeleted(Boolean true1);

    List<SiemensMySkills> findAllByIsModified(Boolean isModfied);
}
