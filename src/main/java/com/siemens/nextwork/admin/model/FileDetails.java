package com.siemens.nextwork.admin.model;


import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "upload_file_details")
public class FileDetails {
	
	@Id
	private String uid;
	private String fileName;
	private String displayName;
	private String filePath;
	private String username;
	private String version;
	private String status; 
	private LocalDateTime createdOn;
	private String createdBy;
	private LocalDateTime updatedOn;
	private String updatedBy;
	private String errors;
	private String action;
	private String type;
	private String reportStatus;
	private Integer recCount;
	private LocalDateTime reportUpdatedOn;
}
