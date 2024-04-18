package com.siemens.nextwork.admin.repo;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.siemens.nextwork.admin.model.ScopingVersions;


@Repository
public interface ScopingVersionsRepository extends MongoRepository<ScopingVersions, String> {
	
	

}
