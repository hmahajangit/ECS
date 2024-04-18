package com.siemens.nextwork.admin.model;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.siemens.nextwork.admin.dto.WorkStreamDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "Users_data")
public class NextWorkUser {
	@Id
	private String id;
	private String name;
	private String email;
	private String info;
	@Field(name = "org_code")
	private String orgCode;
	@Field(name = "created_on")
	private String creationDate;
	private List<Roles> rolesDetails;
	private String status;
	private List<String> directGidList;
	private boolean haveGIDList;
	private List<String> gidList;
	private List<WorkStreamDTO> workStreamList;
	private List<WorkStreamDTO> directAssignmentWorkstreamList;
	private List<WorkStreamDTO> migratedWorkStreamList;

	private String purpose;
	private Map<String, Integer> userGidMap;


}