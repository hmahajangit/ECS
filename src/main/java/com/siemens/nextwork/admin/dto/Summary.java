package com.siemens.nextwork.admin.dto;

import java.util.Date;
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
public class Summary {
	private String uid;
	private String name;
	private String description;
	private List<String> orgCode;
	private String employeeStructure;
	private String createdBy;
	private Date createdOn;
	private int startYear;
	private int startMonth;

	private int endYear;
	private List<LocationCountry> locationCountry;
	private List<Users> users;
	private Boolean isTestProject;



}
