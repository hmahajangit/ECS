package com.siemens.nextwork.admin.dto.matrix;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tier2Data extends MeasureCommonFields{

	private String tier2;
	private Boolean isSharedHC = false;
	private List<Tier3Data> tier3Data;
	
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Tier3Data extends MeasureCommonFields {
		private Boolean isSharedHC = false;
		private String tier3;
	}
}
