package com.siemens.nextwork.admin.model.dto;



import com.siemens.nextwork.admin.enums.Business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalFieldsUser {
	private String responsibleBusiness;
	private String responsiblePO;
	private String responsibleTransformation;
	private Business business;
	private Boolean isFutureFund;
	private String futureFundId;
	private String futureFundValue;
}
