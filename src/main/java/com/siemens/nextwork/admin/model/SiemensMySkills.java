package com.siemens.nextwork.admin.model;

import java.time.LocalDateTime;

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
@Document(collection="siemens_my_skills")
public class SiemensMySkills {

	@Id
	private String id;
	private String skillName;
	private String description;
	private String category;
	private String subCategory;
	private String skillCatalog;
	private String skillGroup;
	private LocalDateTime updatedOn;
	private String updatedBy;
	private Boolean isDeleted;
	private Boolean isModified;
}
