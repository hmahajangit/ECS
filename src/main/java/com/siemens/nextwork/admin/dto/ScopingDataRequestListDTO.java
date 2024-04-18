package com.siemens.nextwork.admin.dto;

import java.util.List;
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
@Schema(
		name = "ScopingDataRequestListDTO", 
		description = "Data object for Scoping Data Request", 
		oneOf = ScopingDataRequestListDTO.class
		)
public class ScopingDataRequestListDTO {
	
	private String gid;
	private Integer index;
	private String action;
	private List<ScopingDTO> data;

}
