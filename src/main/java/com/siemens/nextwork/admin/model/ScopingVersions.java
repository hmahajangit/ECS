package com.siemens.nextwork.admin.model;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.siemens.nextwork.admin.dto.NotesDTO;
import com.siemens.nextwork.admin.dto.VersionSummary;

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
@Document(collection="scoping_versions")
public class ScopingVersions {
	@Id
	private String vid;
	private String versionStatus;
	private Boolean latest;
	private Date uploadTime;
	private String previousVid;
	private VersionSummary summary;
	private List<NotesDTO> notes;
	private Integer recCount;

	
}
