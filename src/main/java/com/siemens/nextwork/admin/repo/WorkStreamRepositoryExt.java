package com.siemens.nextwork.admin.repo;

import com.siemens.nextwork.admin.dto.WorkStreamIdDTO;

import java.util.List;

public interface WorkStreamRepositoryExt {
    List<WorkStreamIdDTO> findWorkStreamIds(String stage);

    public List<WorkStreamIdDTO> findWorkStreamIdsWithoutStage();
}
