package com.siemens.nextwork.admin.model;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@With
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection="grip_details")
public class GRIPCatalogue {

	@Id
	private String id;
	private String jobFamily;
	private String subJobFamily;
	private String positionType;
	private String position;
	private String positionHeadline;
	private String positionDescription;
	private String gripCode;
	private String changeByDate;
	private String techRole;
	private Integer numberOfEmployeesByDate;
	private Boolean isDeleted;
}
