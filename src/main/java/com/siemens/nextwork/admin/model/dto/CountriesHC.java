package com.siemens.nextwork.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CountriesHC {

	private String countryId;
	private String countryName;
	private Integer countryHC;
}
