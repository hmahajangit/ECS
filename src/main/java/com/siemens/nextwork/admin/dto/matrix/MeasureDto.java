package com.siemens.nextwork.admin.dto.matrix;

import java.time.LocalDate;
import java.util.List;

import com.siemens.nextwork.admin.enums.MatrixMeasureType;

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
public class MeasureDto {

	private String uid;
	private String statusQuoJPId;
	private String futureStateJPId;
	private MatrixMeasureType measuresType;
	private List<MeasureConfiguration> measureConfiguration;
	private LocalDate createdOn;
    private String createdBy;
    private LocalDate updatedOn;
    private String updatedBy;
	private List<MeasureTagDto> measureTags;

}
