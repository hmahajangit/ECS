package com.siemens.nextwork.admin.dto;

import org.springframework.data.annotation.Id;

import com.siemens.nextwork.admin.enums.LeverCategory;
import com.siemens.nextwork.admin.enums.LeverDirection;
import com.siemens.nextwork.admin.enums.SourceType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Levers {

    @Id
    private String uid;

    private String leverName;

    private String description;

    private Integer index;

    private LeverDirection leverDirection;

    private LeverCategory leverCategory;

    private Boolean isGlobal = false;

    private Boolean isAssigned = false;

    private SourceType source;

}
