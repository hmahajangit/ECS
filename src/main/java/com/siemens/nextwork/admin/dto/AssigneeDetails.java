package com.siemens.nextwork.admin.dto;

import java.util.List;

import com.siemens.nextwork.admin.model.Users;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssigneeDetails {
	
	private List<Users> powerUsers;
	private List<Users> localAdmins;
	


}