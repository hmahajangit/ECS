package com.siemens.nextwork.admin.repo;


import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.siemens.nextwork.admin.dto.IdDTO;
import com.siemens.nextwork.admin.model.Scoping;


@Repository
public interface ScopingRepository extends MongoRepository<Scoping, String> {

	void deleteByVid(String version);

	List<Scoping> findAllByVidAndIsCaptured(String versionId, Boolean false1);

	@Query(value = "{ 'vid' : ?0 }", fields ="{'are' : 1}")
	List<String> countByAreAndVid(String version);
	
	@Query(value = "{ 'vid' : ?0 }", fields ="{'_id' : -1}")
	List<IdDTO> findAllIdsByVersion(String version);
	
	@Query(value = "{ 'vid' : ?0 }", fields ="{'gid' : -1}")
	List<IdDTO> findAllGIdsByVersion(String version);

	List<Scoping> findAllByVidInAndGidIn(List<String> verList, List<String> subList, Sort sort);

	@Query(value = "{ 'vid' : ?0, 'gid' : { '$nin' : ?1} }", fields ="{'gid' : -1}")
	List<IdDTO> findAllGidsByVersionAndNotinGids(String versionId, List<String> gids);

	@Query(value = "{ 'vid' : ?0 , 'nullFields' : { $exists: true, $ne: null} }", fields ="{'gid' : -1, 'nullFields' : 2}")
	List<IdDTO> findAllGidsAndNullFiledsByVersion(String versionId);

	@Query("{'gid':  {'$regex' : ?0 ,  '$options': 'i'}})")
	List<Scoping> findByRegexGId(String gid);

	@Query(value = "{'gid' : { '$in' : ?0} }", fields ="{'gid' : -1}")
	List<IdDTO> findAllIdsByGIDS(List<String> ids);

	@Query(value = "{'id' : { '$in' : ?0} }", fields ="{'_id' : -1}")
    List<IdDTO> findAllVGIDS(List<String> dupGVIDs);
}
