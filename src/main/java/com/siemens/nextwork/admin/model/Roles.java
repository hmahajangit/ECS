package com.siemens.nextwork.admin.model;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.siemens.nextwork.admin.dto.WorkStreamDTO;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
@NoArgsConstructor
@AllArgsConstructor
@With
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "Roles_Data")
public class Roles {

	@Id
	private String id;
	private String roleDisplayName;
	private String roleDescription;
	private String roleType;
	private List<String> gidList;
	private List<WorkStreamDTO> workstreamList;
	private List<String> directGidList;
	private List<Member> memberList;
	private String createdBy;
	private String createdByEmail;
	private String linkedGIDupdatedBy;
	private String linkedGIDupdatedByEmail;
	private Date date;
	private Date time;		
	private String email;
	private boolean haveGIDList;
	private Boolean isDeleted = false;
	private Map<String, Integer> gidMap;
	
}
	