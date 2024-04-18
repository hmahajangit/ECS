package com.siemens.nextwork.admin.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalFieldsAdmin {
	private String orgCode;
	private Integer inScopeHC;
	private MeasuresHC measuresHC;
	private List<CountriesHC> countries;
	private Boolean haveJobFamily;
	private JobFamilyHC jobFamilyHC;
}
