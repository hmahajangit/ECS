package com.siemens.nextwork.admin.repo;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.siemens.nextwork.admin.model.FileDetails;
@Repository
public interface FileDetailsRepository extends MongoRepository<FileDetails, String> {

	List<FileDetails> findAllByVersionAndStatus(String versionId, String status, Sort sort);

	List<FileDetails> findAllByVersionAndAction(String versionId, String action);

	List<FileDetails> findAllByVersionAndStatusAndActionIn(String versionId, String status, List<String> actionList, Sort sort);

	List<FileDetails> findAllByActionIn(List<String> actionList, Sort sort);

	List<FileDetails> findAllByVersion(String versionId, Sort sort);

	List<FileDetails> findAllByActionInAndType(List<String> actionList, String type, Sort sort);

}
