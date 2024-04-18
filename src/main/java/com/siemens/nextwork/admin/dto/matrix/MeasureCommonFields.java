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
public class MeasureCommonFields {
	private Integer assignedHeadCount;
    private Integer availableHeadCount;
    private Integer measureDuration;
    private String measureStartDate;
    private String measureEndDate;
    private Double measureCost;
    private List<YearHC> yearHC;
    private List<Note> notes;
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note {

    	private String noteName;
        private String author;
        private String emailId;
        private Integer index;
        private String date;
    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearHC {

    	private Integer year;
        private Integer headCount;
    }
}
