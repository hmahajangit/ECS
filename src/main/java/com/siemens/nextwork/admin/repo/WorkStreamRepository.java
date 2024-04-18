package com.siemens.nextwork.admin.repo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.siemens.nextwork.admin.dto.IdDTO;
import com.siemens.nextwork.admin.enums.StageAction;
import com.siemens.nextwork.admin.model.Workstream;


@Repository
public interface WorkStreamRepository extends MongoRepository<Workstream, String>, WorkStreamRepositoryExt {

	@Query(value = "{'users.uid' : ?0}", fields = "{'_id' : 1}")
	List<IdDTO> findWSIdsByUserId(String id);

	@Query(value = "{'gidSummary' : { '$in' : ?0}, 'isDeleted' : false }", fields = "{'_id' : 1}")
	List<IdDTO> findAllWorkStreamIdByGids(List<String> allGidsList);

	@Query(value = "{'uid' : { '$in' : ?0}, 'isDeleted' : false }", fields = "{'_id' : 1}")
	List<IdDTO> findAllWorkStreamIdsByUids(List<String> wsIds);

	@Query(value = "{'skills.lexId' : { '$in' : ?0}, 'isDeleted' : false }", fields = "{'_id' : 1}")
    List<IdDTO> findAllWorkStreamIdsByUpdatedSkillIds(List<String> updatedIdsList);
	@Query(value = "{'uid' : { '$in' : ?0}, 'isDeleted' : false }")
    List<Workstream> findAllWorkStreamsById(List<String> ids);
	@Query(value = "{'stage': ?0, 'isDeleted': false}", fields = "{'_id': 1}")
	List<String> findIdsByStageAndIsDeleted(StageAction stage);

	@Query(value = "{ 'isDeleted' : false }", fields = "{ '_id': 1 }")
	List<String> findAllWorkStreamIdsAndNotDeleted();
}
