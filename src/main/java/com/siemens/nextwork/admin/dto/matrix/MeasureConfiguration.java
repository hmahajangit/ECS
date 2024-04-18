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
public class MeasureConfiguration extends MeasureCommonFields{

	private String tier1;
	private List<Tier2Data> tier2Data;

}
