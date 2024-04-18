package com.siemens.nextwork.admin.repo;

import com.siemens.nextwork.admin.dto.WorkStreamIdDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.query.Criteria.where;

public class WorkStreamRepositoryExtImpl implements WorkStreamRepositoryExt{

    @Autowired
    private MongoTemplate mongoTemplate;
    @Override
    public List<WorkStreamIdDTO> findWorkStreamIds(String stage) {
        ProjectionOperation projection = Aggregation.project("id");
        MatchOperation match = match(where("stage").is(stage).and("isDeleted").is(false));
        Aggregation aggregation = Aggregation.newAggregation(match, projection);
        AggregationResults<WorkStreamIdDTO> results = mongoTemplate.aggregate(aggregation, "workstream", WorkStreamIdDTO.class);
        return results.getMappedResults();
    }

    @Override
    public List<WorkStreamIdDTO> findWorkStreamIdsWithoutStage() {
        ProjectionOperation projection = Aggregation.project("id");
        MatchOperation match = match(where("isDeleted").is(false));
        Aggregation aggregation = Aggregation.newAggregation(match, projection);
        AggregationResults<WorkStreamIdDTO> results = mongoTemplate.aggregate(aggregation, "workstream", WorkStreamIdDTO.class);
        return results.getMappedResults();
    }

}
