package com.siemens.nextwork.admin.dto;

import java.util.Date;

import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Trainings {

	@Id
	private String trainingId;
	private String name;
	private Double cost;
	private Integer minutes;
	private Integer hours;
	private String skillId;
	private String skillName;
	private String devPathId;
	protected String createdBy;
	protected Date createdOn;
	private String updatedBy;
	protected Date updatedOn;
	
}
