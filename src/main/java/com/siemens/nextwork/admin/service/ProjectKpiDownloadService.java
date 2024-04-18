package com.siemens.nextwork.admin.service;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.siemens.nextwork.admin.dto.DevPathExcelDTO;
import com.siemens.nextwork.admin.dto.SkillAssignment;

public interface ProjectKpiDownloadService {

	byte[] getProjectKPIAsExcel(String[] ids, String userEmail, HttpServletResponse response);
	List<DevPathExcelDTO> listAll(String projectId);
	int claculateSkillGap(SkillAssignment jobProfileSkill);
	
	

}
