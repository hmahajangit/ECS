package com.siemens.nextwork.admin.repo;

import com.siemens.nextwork.admin.model.WorkstreamGids;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkstreamGidRepository extends MongoRepository<WorkstreamGids, String> {

    WorkstreamGids findByWorkstreamId(String workstreamId);
}
