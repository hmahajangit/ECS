package com.siemens.nextwork.admin.dto.matrix;


import java.util.List;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LatestMatrixModel {
	@Id
	private String uid;
	private String matrixName;
    private String description;
    private AssociatedJobProfiles associatedJobProfiles;
    private List<MeasureDto> measures;
}
