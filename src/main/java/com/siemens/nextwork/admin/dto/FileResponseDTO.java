package com.siemens.nextwork.admin.dto;

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
@Schema(name = "FileResponseDTO", description = "Data object for File Controller Response", oneOf = FileResponseDTO.class)
public class FileResponseDTO {

    private String uid;
    
	private String headline;

	private String subheadline1;
	
	private String subheadline2;
	
    private String url;
    
    private String contentType;
    
    private String fileName;
    
    private long fileSize;
    
    private byte[] file; 
    
}