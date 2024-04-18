package com.siemens.nextwork.admin.dto.hcbridge;

import com.siemens.nextwork.admin.enums.HCAssignedBy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HCBridgeAssignedData {

    private HCAssignedBy assignedBy;
    private List<DataEntity> data;
}

