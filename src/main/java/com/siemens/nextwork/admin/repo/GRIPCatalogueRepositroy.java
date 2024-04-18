package com.siemens.nextwork.admin.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.siemens.nextwork.admin.model.GRIPCatalogue;

public interface GRIPCatalogueRepositroy extends MongoRepository<GRIPCatalogue, String>{

	void deleteAllByIsDeleted(Boolean isDeleted);

}
