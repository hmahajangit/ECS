package com.siemens.nextwork.admin.util;

import java.util.Arrays;
import java.util.List;

import com.siemens.nextwork.admin.dto.AnnouncementDTO;
import com.siemens.nextwork.admin.dto.MemberDTO;
import com.siemens.nextwork.admin.dto.NextWorkUserPutRequestDTO;
import com.siemens.nextwork.admin.dto.RequestAccessDTO;
import com.siemens.nextwork.admin.dto.ResponseRoleDTO;

public class DTOUtil {

	private DTOUtil() {
	}

	public static RequestAccessDTO getRequestAccessDTOWithErrors() {
		return RequestAccessDTO.builder().email("")
				.purpose("")
				.build();
	}

	public static RequestAccessDTO getRequestAccessDTOWithEmailErrors() {
		return RequestAccessDTO.builder().email("bindushree.bs@gmail.com").build();
	}

	public static AnnouncementDTO getAnnouncementRequestDTO(String uid) {
		AnnouncementDTO announcement= AnnouncementDTO.builder()
				.uid(uid)
				.title("New Announcement")
				.description("New Announcement Description")
				.build();
		return announcement;
	}

	public static RequestAccessDTO getRequestAccessDTO() {
		return RequestAccessDTO.builder().email("1234@siemens.com")
				.purpose("Need Access for Nextwork Tool")
				.build();
	}
	
	
	public static ResponseRoleDTO getResponseRoleDTO() {
		MemberDTO member = new MemberDTO();
		member.setMemberEmail("test@test.com");
		member.setMemberName("PROJECT OWNER");
		List<MemberDTO> members = Arrays.asList(member);
		return ResponseRoleDTO.builder()
				.roleDisplayName("Dummy")
				.roleDescription("Power USER")
				.roleType("User")
				.memberList(members)
				
				.build();
	}
	
	
	public static NextWorkUserPutRequestDTO getRequestUserDTO() {
		return NextWorkUserPutRequestDTO.builder()
				.Id("12")
				.status("Active")				
				.build();
	}
}