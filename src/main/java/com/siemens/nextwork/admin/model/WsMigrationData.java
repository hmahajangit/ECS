package com.siemens.nextwork.admin.model;

import java.util.List;

import com.siemens.nextwork.admin.dto.DevelopmentPath;
import com.siemens.nextwork.admin.dto.JobCluster;
import com.siemens.nextwork.admin.dto.JobProfile;
import com.siemens.nextwork.admin.dto.Matrix;
import com.siemens.nextwork.admin.dto.NeedForAction;
import com.siemens.nextwork.admin.dto.Skills;
import com.siemens.nextwork.admin.dto.Summary;
import com.siemens.nextwork.admin.dto.TrendsAndBizOutlook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WsMigrationData {

	private Summary summary;
	private List<JobProfile> jobProfiles;
	private List<JobCluster> jobClusters;
	private List<Skills> skills;
	private List<TrendsAndBizOutlook> trendsAndBizOutlook;
	private List<NeedForAction> needForAction;
	private List<Matrix> matrixes;
	private List<DevelopmentPath> developmentPath;
}
