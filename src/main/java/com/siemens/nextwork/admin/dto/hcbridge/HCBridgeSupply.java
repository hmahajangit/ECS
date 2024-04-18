package com.siemens.nextwork.admin.dto.hcbridge;

import java.util.List;

import javax.persistence.Id;

import com.siemens.nextwork.admin.enums.HCAssignedBy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HCBridgeSupply {
    @Id
    private String hcAssignmentId;
    private HCAssignedBy lastSavedAssignedBy;
    private Boolean isRescoped=false;
    private Boolean isImpactLeverModified = false;
    private List<HCBridgeAssignedData> assignedData;

}

