package com.siemens.nextwork.admin.dto;

import java.util.List;

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
public class ScopingVersionResponseDTO {
	
	private String vid;

	private String status;
	
	private Integer recCount;
	
	private List<OlderVersionsDTO> olderVersions;

	
	
}
