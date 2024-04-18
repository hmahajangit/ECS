package com.siemens.nextwork.admin.dto;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Schema(name = "FileDTO", description = "Data object for File Controller Request", oneOf = FileDTO.class)
public class FileDTO {
	
	private String uid;

	private String headline;

	private String subheadline1;
	
	private String subheadline2;

	private MultipartFile file;
	
}
