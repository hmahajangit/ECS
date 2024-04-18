package com.siemens.nextwork.admin.dto;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkStreamDTO {
	private String uid;
	private String name;
	private List<String> gidList;

    

}
