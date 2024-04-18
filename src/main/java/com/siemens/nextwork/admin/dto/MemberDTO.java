package com.siemens.nextwork.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberDTO {
	private String uid;
	private String memberName;
	private String memberEmail;
	
	

}
