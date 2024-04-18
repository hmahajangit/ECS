package com.siemens.nextwork.admin.dto.hcbridge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class YearDataEntity {
    private Integer yearIndex;
    private Integer yearHC;
    private Double yearPercentage;
}
