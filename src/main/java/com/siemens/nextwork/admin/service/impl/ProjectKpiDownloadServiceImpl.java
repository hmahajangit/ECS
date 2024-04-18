package com.siemens.nextwork.admin.service.impl;

import static com.siemens.nextwork.admin.util.NextworkConstants.ATTACHMENT_FILEKEY;
import static com.siemens.nextwork.admin.util.NextworkConstants.CONTENT_DISPOSITION;
import static com.siemens.nextwork.admin.util.NextworkConstants.FILE_NAME;
import static com.siemens.nextwork.admin.util.NextworkConstants.FILE_SHEETNAME;
import static com.siemens.nextwork.admin.util.NextworkConstants.PROJECTID_NOT_FOUND;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import com.siemens.nextwork.admin.repo.WorkstreamGidRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siemens.nextwork.admin.dto.DevPathExcelDTO;
import com.siemens.nextwork.admin.dto.IdDTO;
import com.siemens.nextwork.admin.dto.ImpactedJobProfiles;
import com.siemens.nextwork.admin.dto.JobProfile;
import com.siemens.nextwork.admin.dto.SkillAssignment;
import com.siemens.nextwork.admin.dto.SkillResponseDTO;
import com.siemens.nextwork.admin.dto.Skills;
import com.siemens.nextwork.admin.dto.TrendsAndBOResponseDTO;
import com.siemens.nextwork.admin.dto.TrendsAndBizOutlook;
import com.siemens.nextwork.admin.dto.WorkStreamDTO;
import com.siemens.nextwork.admin.dto.matrix.LatestMatrixModel;
import com.siemens.nextwork.admin.dto.matrix.MeasureConfiguration;
import com.siemens.nextwork.admin.dto.matrix.MeasureDto;
import com.siemens.nextwork.admin.enums.MatrixMeasureType;
import com.siemens.nextwork.admin.excel.DevPathExcelExporter;
import com.siemens.nextwork.admin.exception.ResourceNotFoundException;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.exception.RestForbiddenException;
import com.siemens.nextwork.admin.model.GidData;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.model.Users;
import com.siemens.nextwork.admin.model.Workstream;
import com.siemens.nextwork.admin.model.WorkstreamGids;
import com.siemens.nextwork.admin.repo.RolesRepository;
import com.siemens.nextwork.admin.repo.WorkStreamRepository;
import com.siemens.nextwork.admin.service.ProjectKpiDownloadService;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.util.NextworkConstants;

@Service
@Transactional
public class ProjectKpiDownloadServiceImpl implements ProjectKpiDownloadService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectKpiDownloadServiceImpl.class);

	@Autowired
	WorkStreamRepository workStreamRepository;

	@Autowired
	private UserService userService;
	
	@Autowired
	RolesRepository rolesRepository;

	@Autowired
	private WorkstreamGidRepository workstreamGidRepository;
	
	@Override
	public  byte[] getProjectKPIAsExcel(String[] projectIds, String userEmail, HttpServletResponse response) {
		
		Optional<NextWorkUser> userPlatformRole = userService.findByUserEmail(userEmail);
		validateKPIUserRole(userPlatformRole, projectIds);
		validateProjectForLocalAdmin(projectIds, userPlatformRole);
		List<Workstream> projectEntities = getAllProjectsByIds(projectIds);
		
		String headerValue = ATTACHMENT_FILEKEY + FILE_NAME;
		response.setHeader(CONTENT_DISPOSITION, headerValue);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet(FILE_SHEETNAME);
		
		try {
			formTheInitiativeLayout(projectEntities, sheet, workbook);
			int count = 1;
			for(Workstream proj:projectEntities) {
				XSSFSheet sheetForInitiativeDetails = workbook.createSheet(count + " Details - "+proj.getName().replaceAll("[^a-züäößA-ZÜÄÖ0-9()!@#&|+-,_<>.©α∞™µ®]", " ") );
				formTheDetailsInitiativeLayout(proj, sheetForInitiativeDetails, workbook);
				count = count + 1;
			}
			workbook.write(bos);
			workbook.close();
		} catch (Exception e) {
			throw new RestBadRequestException(e.getLocalizedMessage());
		}	
		return bos.toByteArray();
	}

	private void validateProjectForLocalAdmin(String[] projectIds, Optional<NextWorkUser> userPlatformRole) {
		if(projectIds.length == 0) throw new ResourceNotFoundException("Workstream Ids are requied.");
		if(userPlatformRole.isPresent()) {
			NextWorkUser ur = userPlatformRole.get();
			
			List<Roles> roles= ur.getRolesDetails();
			Optional<Roles> role = roles.stream().filter(r -> r.getRoleType().equalsIgnoreCase(NextworkConstants.ROLE_TYPE_LOCAL_ADMIN)).findFirst();
			if (role.isPresent()) {
				List<String> wsList = getLocalAdminAllWorkStreamIds(ur);
				List<String> wsIds = Arrays.asList(projectIds);
				LOGGER.info("WorkStream IDS : {}", wsIds);
				LOGGER.info("Actual WS IDs : {}", wsList);
				
				List<String> wsListMigrated = getLocalAdminMigratedWorkStreamIds(ur);
				
				List<String> unMapedList = wsIds.stream().filter(w -> !wsList.contains(w) && !wsListMigrated.contains(w)).toList();
				if(unMapedList != null && !unMapedList.isEmpty()) {
					throw new ResourceNotFoundException("Workstream are not valid "+ unMapedList);
				}
			}
		}
	}

	private List<String> getLocalAdminMigratedWorkStreamIds(NextWorkUser nextUser) {
		if(nextUser.getMigratedWorkStreamList() != null) {
			return (nextUser.getMigratedWorkStreamList().stream().map(WorkStreamDTO::getUid).toList());		
							
		}
		return (new ArrayList<>());
	}

	private List<String> getLocalAdminAllWorkStreamIds(NextWorkUser nextUser) {
		List<String> wsIds = getLocalAdminUserWorkStreams(nextUser);
		List<String> laRoleIds = (nextUser.getRolesDetails() != null) ?nextUser.getRolesDetails().stream().map(Roles::getId).toList() : new ArrayList<>();
		List<String> allGidsList = new ArrayList<>();
		allGidsList.addAll((nextUser.getGidList() != null) ? nextUser.getGidList() : new ArrayList<>());
		LOGGER.info("User GIDS list : {}", allGidsList);
		
		if((laRoleIds != null) && !laRoleIds.isEmpty()) {
			List<Roles> laRoles = rolesRepository.findAllActiveRolesByIds(laRoleIds);
			LOGGER.info("User associated Roles size : {}", laRoles.size());
			wsIds.addAll(getLocalAdminRolesWorkStreams(laRoles));
			allGidsList.addAll(laRoles.stream().map(Roles::getGidList).filter(Objects::nonNull).flatMap(Collection::stream).toList());
		}
		LOGGER.info("User WorkStream Ids : {}", wsIds);
		Map<String, Long> dupGidsMap = allGidsList.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));		
		allGidsList = dupGidsMap.entrySet().stream().map(Entry::getKey).collect(Collectors.toList());
		LOGGER.info("All Unqiue Gids list size : {} ", allGidsList.size());
		List<IdDTO> wsIdDTOs = (!allGidsList.isEmpty()) ? workStreamRepository.findAllWorkStreamIdByGids(allGidsList) : new ArrayList<>();
		LOGGER.info("GID  Based WS size : {}", wsIdDTOs);
		wsIds.addAll((wsIdDTOs != null) ? wsIdDTOs.stream().map(IdDTO::get_id).toList() : new ArrayList<>());
		
		wsIdDTOs = (!wsIds.isEmpty()) ?  workStreamRepository.findAllWorkStreamIdsByUids(wsIds) : new ArrayList<>();
		wsIds = (wsIdDTOs != null && !wsIdDTOs.isEmpty()) ? wsIdDTOs.stream().map(IdDTO::get_id).toList() : new ArrayList<>();
	
		return wsIds;
	}

	private List<String> getLocalAdminRolesWorkStreams(List<Roles> laRoles) {
		List<String> wsIds = new ArrayList<>();
		if(!laRoles.isEmpty()) {
			wsIds.addAll(laRoles.stream().map(Roles::getWorkstreamList).filter(Objects::nonNull).flatMap(Collection::stream).map(WorkStreamDTO::getUid).toList());
		}
		LOGGER.info("User associated Roles Workstream ids : {}", wsIds.size());
		return wsIds;
	}

	private List<String> getLocalAdminUserWorkStreams(NextWorkUser nextUser) {
	
		List<String> wsList = new ArrayList<>();
		if(nextUser.getWorkStreamList() != null)
			wsList.addAll(nextUser.getWorkStreamList().stream().map(WorkStreamDTO::getUid).toList());
		wsList.addAll((nextUser.getDirectAssignmentWorkstreamList() != null) ? nextUser.getDirectAssignmentWorkstreamList().stream().map(WorkStreamDTO::getUid).toList() : new ArrayList<>());
		LOGGER.info("User Direct or member WorkStream size : {}", wsList.size());
		return wsList;
	}
	

	private void validateKPIUserRole(Optional<NextWorkUser> userPlatformRole, String[] projectIds) {
		if (userPlatformRole.isPresent() && userPlatformRole.get().getStatus().equalsIgnoreCase(NextworkConstants.ACTIVE)) {
			List<Roles> roles= userPlatformRole.get().getRolesDetails();
			if(roles == null || roles.isEmpty()) {
				throw new ResourceNotFoundException("User roles doesn't exist.");
			}
			
			
			List<String> nextWorkRoles = new ArrayList<>();
			nextWorkRoles.add(NextworkConstants.ROLE_TYPE_LOCAL_ADMIN);
			nextWorkRoles.add(NextworkConstants.ADMIN);
			Optional<Roles> role = roles.stream().filter(r -> nextWorkRoles.contains(r.getRoleType())).findFirst();
			if (!role.isPresent() && ifNotMigratedWorkstream(projectIds, userPlatformRole.get().getMigratedWorkStreamList())) {
				throw new RestForbiddenException("Only Admin Or Local Admin can access this functionality.");
			}
		}else {
			throw new RestForbiddenException("User doesn't exists/Deactive User");
		}
	}

	private boolean ifNotMigratedWorkstream(String[] projectIds, List<WorkStreamDTO> migratedWorkStreamList) {
		if (!CollectionUtils.isEmpty(migratedWorkStreamList)) {	
			List<String> migratedWsId=migratedWorkStreamList.stream().map(WorkStreamDTO::getUid).toList();
			List<String> wsIds = Arrays.asList(projectIds);
			List<String> unMapedList = wsIds.stream().filter(ws -> !migratedWsId.contains(ws)).toList();
			if (unMapedList.isEmpty())
				return false;
		}
		return true;
	}

	private void formTheDetailsInitiativeLayout(Workstream project, XSSFSheet sheet,
			XSSFWorkbook workbook) {
		// set Style and font
		XSSFCellStyle styleBold = workbook.createCellStyle();
		XSSFFont fontBold = workbook.createFont();
		fontBold.setBold(true);
		int rowId = 2;
		XSSFRow row;
		row = sheet.createRow(rowId);
		createCell(row, 1, "#NextWork Tool: KPI extraction - Details Workstream - [ "+project.getName() +" ]", styleWithLargeFontSize(workbook));
		
		rowId += 2;
		row = sheet.createRow(rowId);
		createCell(row, 1, "Job Profile details", styleBold);
		sheet.addMergedRegion(new CellRangeAddress(rowId, rowId, 1, 13));
		
		rowId+=2;
		row = sheet.createRow(rowId);
//  NEX-1025++ Add four  new columns in KPI excel sheet++ 		

		createCell(row, 3, "Key job profile data", style(workbook)); 
		sheet.addMergedRegion(new CellRangeAddress(rowId, rowId, 3, 8));
		
		createCell(row, 9, "Trends & Biz. Outlook", styleColourLigh(workbook));
		sheet.addMergedRegion(new CellRangeAddress(rowId, rowId, 9, 16));
	
		createCell(row, 17, "Skills", style(workbook));
		sheet.addMergedRegion(new CellRangeAddress(rowId, rowId, 17, 66));
		
		createCell(row, 67, "Need for action matrix", styleColourLigh(workbook));
		sheet.addMergedRegion(new CellRangeAddress(rowId, rowId, 67, 68));
		
		++rowId;
		row = sheet.createRow(rowId);
		
		createCell(row, 1, "Workstream Name", styleBold);
		createCell(row, 2, "ORG code", styleBold);
		createCell(row, 3, "Job Profiles", styleBold);
		createCell(row, 4, "GRIP CODE", styleBold);
		createCell(row, 5, "GRIP NAME", styleBold);
		createCell(row, 6, "Current HC supply", styleBold);
		createCell(row, 7, "Future HC supply", styleBold);
		createCell(row, 8, "Future HC Demand", styleBold);
		
		createCell(row, 9, "Trend / Biz. Outlook 1", styleBold);
		createCell(row, 10, "Trend / Biz. Outlook 2", styleBold);
		createCell(row, 11, "Trend / Biz. Outlook 3", styleBold);
		createCell(row, 12, "Trend / Biz. Outlook 4", styleBold);
		createCell(row, 13, "Trend / Biz. Outlook 5", styleBold);
		createCell(row, 14,"Trend / Biz. Outlook 6", styleBold);
		createCell(row, 15,"Trend / Biz. Outlook 7", styleBold);
		createCell(row, 16,"Trend / Biz. Outlook 8", styleBold);
		
		int skilRowCount=17;
		int skillcount=1;
		while(skillcount<=50) {
			createCell(row, skilRowCount++,"Skill "+skillcount++, styleBold);
		}

		createCell(row, 67,"HC Gap", styleBold);
		createCell(row, 68,"Skill Gap", styleBold);
		createCell(row, 69,"Required Skills for Status Quo", styleBold);
		createCell(row, 70,"Required Skills for Future State", styleBold);
		
		
		++rowId;
		row = sheet.createRow(rowId);
		
		rowId = getJobProfilesData(project, sheet, rowId, row);
		
		List<DevPathExcelDTO> listAll = listAll(project.getUid());
		DevPathExcelExporter excelExporter = new DevPathExcelExporter(listAll,rowId,sheet,workbook);
		excelExporter.writeHeaderLine();
		
	}

	private int getJobProfilesData(Workstream project, XSSFSheet sheet, int rowId, XSSFRow row) {
		
		Map<String, List<ImpactedJobProfiles>> jpTrendMap = (project.getTrends() != null) ? project.getTrends().stream().filter(t -> (t.getImpactedJobProfiles() != null)).map(TrendsAndBizOutlook::getImpactedJobProfiles)
				.flatMap(Collection::stream).collect(Collectors.groupingBy(ImpactedJobProfiles::getJobProfileId)) : new HashMap<>();
		List<JobProfile> profiles = (project.getJobProfiles() != null) ? project.getJobProfiles() : new ArrayList<>();
		List<Skills> skills = (project.getSkills() != null) ? project.getSkills() : new ArrayList<>();
		Map<String, String> skillNameMap = skills.stream().collect(Collectors.toMap(Skills::getUid, Skills::getName));
		WorkstreamGids workstreamGids = workstreamGidRepository.findByWorkstreamId(project.getUid());
		Map<String, List<GidData>> gripsMap = (workstreamGids.getGidDataList() != null) ? workstreamGids.getGidDataList().stream().filter(g -> Objects.nonNull(g.getGripPositionDesc())).collect(Collectors.groupingBy(GidData::getGripPositionDesc)) : new HashMap<>();
		Map<String, String> gripOrgCodesMap = (gripsMap != null) ? gripsMap.entrySet().stream().collect(Collectors.toMap(Entry::getKey,  s -> getDelimSepartedGripOrgCode(s.getValue()))) : new HashMap<>();

		for(JobProfile jobProfiles: profiles)
		{
			int skillGap=0;
			
			if(jobProfiles!=null && !StringUtils.isAllBlank(jobProfiles.getGripName()))
			{	
				
				createCell(row, 1, project.getName(), null);
				
				createCell(row, 2, getCommaSeperatedOrgs(gripsMap, gripOrgCodesMap, jobProfiles), null);

				jobProfileExcelData(row,jobProfiles);
				
				int col=9;
				
				List<ImpactedJobProfiles> trendProfiles = jpTrendMap.get(jobProfiles.getUid());
				trendsAndBoExcelData(trendProfiles,col,row, project);
				
				List<SkillAssignment> jobProfileSkills = (jobProfiles.getSkillAssignments() != null) ? jobProfiles.getSkillAssignments() : new ArrayList<>();
				
				col=17;
				
				skillGap=jobProfileSkillExcelData(row,col,jobProfileSkills,skillGap, skillNameMap);
				
				//HC gap
				col=67;
				calculateHcGap(jobProfiles,row,col);
				
				//Skill Gap
				col=68;
				createCell(row, col, skillGap, null);

				
				calculateRequiredSkills(row, jobProfileSkills);

				
				++rowId;
				row = sheet.createRow(rowId);
					
			}	
		}
		return rowId;
	}

	private String getCommaSeperatedOrgs(Map<String, List<GidData>> gripsMap, Map<String, String> gripOrgCodesMap,
			JobProfile jobProfiles) {
		String csOrgs = "";
		if(Boolean.TRUE.equals(jobProfiles.getIsTwin())) {
			csOrgs = getCommaSeparatedOrgs(gripsMap, gripOrgCodesMap, jobProfiles);
		}
		return csOrgs;
	}

	private String getCommaSeparatedOrgs(Map<String, List<GidData>> gripsMap, Map<String, String> gripOrgCodesMap,
			JobProfile jobProfiles) {
		String csOrgs = null;
		if(gripOrgCodesMap.containsKey(jobProfiles.getGripName())) {
			if(jobProfiles.getGidList().isEmpty()) {
				csOrgs = gripOrgCodesMap.get(jobProfiles.getGripName());
			}else {
				List<GidData> jpGrips = gripsMap.get(jobProfiles.getGripName());
				List<GidData> orgs = jpGrips.stream().filter(g -> jobProfiles.getGidList().contains(g.getGid())).toList();
				csOrgs = getDelimSepartedGripOrgCode(orgs);
			}
		}
		return csOrgs;
	}
	
	private String getDelimSepartedGripOrgCode(List<GidData> gids) {
		Map<String, Long> orgCnts = (gids != null) ? gids.stream().map(GidData::getOrgCodePA).filter(Objects::nonNull).collect(Collectors.groupingBy(Function.identity(), Collectors.counting())) : new HashMap<>();
		return (orgCnts.isEmpty()) ? "" : orgCnts.entrySet().stream().map(e -> e.getKey()+" ("+e.getValue()+")").collect(Collectors.joining("; "));
	}
	
//NEX-1025 ++
	private void calculateRequiredSkills(XSSFRow row, List<SkillAssignment> jobProfileSkills) 
	{
		int statusQuoRequiredSkills=0;
		int futureStateRequiredSkills=0;
		String futureSkillLevel="";
		String currentSkillLevel="";
		
		for(SkillAssignment jobProfileSkill:jobProfileSkills)
		{
			if(jobProfileSkill!=null)
			{
				futureSkillLevel=jobProfileSkill.getFutureSkillLevel();
				currentSkillLevel=jobProfileSkill.getCurrentSkillLevel();
				
				if(currentSkillLevel != null){
					statusQuoRequiredSkills=statusQuoRequiredSkills + 1;
				}
				if(futureSkillLevel != null){
					futureStateRequiredSkills=futureStateRequiredSkills + 1;
				}
			}
		}
		createCell(row, 69, statusQuoRequiredSkills, null);
		createCell(row, 70, futureStateRequiredSkills, null);
		
	}
//NEX-1025 --

	private int  jobProfileSkillExcelData(XSSFRow row, int col, List<SkillAssignment> jobProfileSkills, int skillGap, Map<String, String> skillMap) {
		for(SkillAssignment jobProfileSkill:jobProfileSkills)
		{
				if(jobProfileSkill != null && col<65 && jobProfileSkill.getSkillId() !=null )
				{
					createCell(row, col++, skillMap.get(jobProfileSkill.getSkillId()), null);
					
					skillGap+=claculateSkillGap(jobProfileSkill);
				}
		}
		return skillGap;
	}

	private void jobProfileExcelData(XSSFRow row, JobProfile jobProfiles) {

		if(Objects.isNull(jobProfiles)) return;
//NEX-1025++
		createCell(row, 3, jobProfiles.getName(), null);
		
		String gpPosList = (jobProfiles.getGripPostion() != null) ? getDelimSeparatedString(getCountryCountsMap(jobProfiles.getGripPostion())) : "";

		createCell(row, 4, gpPosList, null);
		createCell(row, 5, jobProfiles.getGripName(), null);
		createCell(row, 6, jobProfiles.getCurrentHeadCountSupply(), null);
		createCell(row, 7, jobProfiles.getFutureHeadCountSupply(), null);
		createCell(row, 8, jobProfiles.getFutureHeadCountDemand(), null);
//NEX-1025--
		
	}

	private void trendsAndBoExcelData(List<ImpactedJobProfiles> trendProfiles, int col, XSSFRow row, Workstream ws) {
		Map<String, TrendsAndBizOutlook> trendsIdMap = (ws.getTrends() != null) ? ws.getTrends().stream().collect(Collectors.toMap(TrendsAndBizOutlook::getUid, t -> t)) : new HashMap<>();
		if(trendProfiles != null) {
			for(ImpactedJobProfiles trends:trendProfiles)
			{
				if(trends.getTrendAndBOId() !=null && trendsIdMap.containsKey(trends.getTrendAndBOId()) && col<13)
				{
					createCell(row, col, trendsIdMap.get(trends.getTrendAndBOId()).getImpactSubCategory() , null);
				}
				col++;
			}
		}
	}

	private void calculateHcGap(JobProfile jobProfiles, XSSFRow row, int col) {
		if(jobProfiles != null) {
			int currentHCSupply=jobProfiles.getCurrentHeadCountSupply() ==null ? 0 : jobProfiles.getCurrentHeadCountSupply();
			int futureHCDemand=jobProfiles.getFutureHeadCountDemand() ==null ? 0 : jobProfiles.getFutureHeadCountDemand();
			
			int hcGap= futureHCDemand - currentHCSupply;
			createCell(row, col, hcGap, null);
		}else {
			createCell(row, col, 0, null);
		}
		
	}

	private List<Workstream> getAllProjectsByIds(String[] projectIds) {
		List<Workstream> projects = new ArrayList<>();
		for (String projectId : projectIds) {
			Optional<Workstream> projectEntityOptional = workStreamRepository.findById(projectId);
			if (!projectEntityOptional.isPresent())
				throw new ResourceNotFoundException(PROJECTID_NOT_FOUND);
			projects.add(projectEntityOptional.get());
		}
		return projects;
	}
	
	

	private void formTheInitiativeLayout(List<Workstream> projects, XSSFSheet sheet, XSSFWorkbook workbook) {

		// set Style and font
		XSSFCellStyle styleBold = workbook.createCellStyle();
		XSSFFont fontBold = workbook.createFont();
		fontBold.setBold(true);
		styleBold.setFont(fontBold);

		int rowId = 1;
		XSSFRow row;
		row = sheet.createRow(rowId);
		createCell(row, 1, "#NextWork Tool: KPI extraction - General Overview", styleWithLargeFontSize(workbook));

		rowId += 3;
		row = sheet.createRow(rowId);
		createCell(row, 1, "Data from pop-up \"Create new workstream\"", style(workbook));
		sheet.addMergedRegion(new CellRangeAddress(rowId, rowId, 1, 13));
		createCell(row, 14, "Data from Overview page", styleColourLigh(workbook));
		sheet.addMergedRegion(new CellRangeAddress(rowId, rowId, 14, 26));
		++rowId;
		row = sheet.createRow(rowId);
		createCell(row, 1, "Workstream Name", styleBold);
		createCell(row, 2, "Description", styleBold);
		createCell(row, 3, "Workstream Type", styleBold);
		createCell(row, 4, "Start date", styleBold);
		createCell(row, 5, "End date", styleBold);
		createCell(row, 6, "Workstream Status", styleBold);
		createCell(row, 7, "People structure", styleBold);
		createCell(row, 8, "ORG code", styleBold);
		createCell(row, 9, "Countries", styleBold);
		createCell(row, 10, "Location", styleBold);
		createCell(row, 11, "HC in scope", styleBold);
		createCell(row, 12, "Job families in scope", styleBold);
		createCell(row, 13, "Member (E-Mail)", styleBold);
		createCell(row, 14, "Current HC supply", styleBold);
		createCell(row, 15, "Future HC supply", styleBold);
		createCell(row, 16, "Future HC demand", styleBold);
		createCell(row, 17, "Upskilling", styleBold);
		createCell(row, 18, "Reskilling", styleBold);
		createCell(row, 19, "Win", styleBold);
		createCell(row, 20, "Reshuffle", styleBold);
		createCell(row, 21, "Status Quo Job Profiles", styleBold);
		createCell(row, 22, "Future Job Profiles", styleBold);
		createCell(row, 23, "Identified Trends", styleBold);
		createCell(row, 24, "Identified Biz. Outlook", styleBold);
		createCell(row, 25, "Top Skills", styleBold);
		createCell(row, 26, "Top Trends", styleBold);

		for (Workstream project : projects) {
			++rowId;
			row = sheet.createRow(rowId);
			WorkstreamGids workstreamGids = workstreamGidRepository.findByWorkstreamId(project.getUid());
			List<GidData> gidList = workstreamGids.getGidDataList();
			List<String> gidCountry = gidList.stream().map(GidData::getCountryRegionARE).toList();
			List<String> gidCities = gidList.stream().map(GidData::getCountryRegionARE).toList();
			List<String> gidStructure = gidList.stream().map(GidData::getBlueCollarWhiteCollar).toList();
			List<String> gidValueStream = gidList.stream().map(GidData::getJobFamily).toList();
			List<String> orgsList = gidList.stream().map(GidData::getOrgCodePA).toList();
			List<String> members = (project.getUsers() != null) ? project.getUsers().stream().map(Users::getEmail).toList() : new ArrayList<>();
			
			String csCountries = getDelimSeparatedString(getCountryCountsMap(gidCountry));
			String csLocations = getDelimSeparatedString(getCountryCountsMap(gidCities));
			String csMembers = String.join(";", members);
			String peopleStructure =  getDelimSeparatedString(getCountryCountsMap(gidStructure));
			String csValueStreams = getDelimSeparatedString(getCountryCountsMap(gidValueStream));
			String csOrgs = getDelimSeparatedString(getCountryCountsMap(orgsList));
			
			// get top skills and trendandBOs count
			List<SkillAssignment> skills = (project.getJobProfiles() != null) ?  project.getJobProfiles().stream().filter(Objects::nonNull).filter(jp -> (jp.getSkillAssignments()!= null)).map(JobProfile::getSkillAssignments)
					.flatMap(Collection::stream).toList() : new ArrayList<>();
			List<TrendsAndBizOutlook> trendsAndBOs = (project.getTrends() != null) ? project.getTrends() : new ArrayList<>();
			Map<String, Skills> skillMap = (project.getSkills() != null) ? project.getSkills().stream().collect(Collectors.toMap(Skills::getUid, s ->s)) : new HashMap<>();
			
			List<SkillResponseDTO> skillResponse = skillResponseMethod(skills, skillMap);
			List<TrendsAndBOResponseDTO> trendsAndBOResponse = trendsAndBOResponseMethod(trendsAndBOs);
			List<String> skillList = skillResponse.stream().map(SkillResponseDTO::getName).toList();
			List<String> trendBOList = trendsAndBOResponse.stream().map(TrendsAndBOResponseDTO::getName)
					.toList();
			String csSkills = String.join("; ", skillList);
			String csTrendBOs = String.join("; ", trendBOList);

			createCell(row, 1, project.getName(), styleBold);
			createCell(row, 2, project.getDescription(), null);
			createCell(row, 3, project.getStage().toString(), null);
			createCell(row, 4, project.getStartDate().toString(), null);
			createCell(row, 5, project.getEndDate().toString(), null);
			createCell(row, 6, project.getPublishedStatus().toString(), null);
			createCell(row, 7, peopleStructure, null);
			createCell(row, 8, csOrgs, null);
			createCell(row, 9, csCountries, null);
			createCell(row, 10, csLocations, null);
			if (Objects.nonNull(project.getProjectHC()))
				createCell(row, 11, String.valueOf(project.getProjectHC()), null);
			createCell(row, 12, csValueStreams, null);
			createCell(row, 13, csMembers, null);

			// get job profile HCs
			int futureHCDemandSum = 0;
			int futureHCSupplySum = 0;
			int statusQuoHCSupplySum = 0;
			int statusQuoSum = 0;
			int futureSum = 0;
			getJobProfileCount(project, futureHCDemandSum, futureHCSupplySum, statusQuoHCSupplySum, statusQuoSum,
					futureSum, row);

			// get matrix counts
			int upskillingCount = 0;
			int reskillingCount = 0;
			int hireCount = 0;
			int otherMeasuresCount = 0;
			getMatrixCount(project, upskillingCount, reskillingCount, hireCount, otherMeasuresCount, row);

			// get trends and BOs count
			int trendsCount = 0;
			int businessOutlookCount = 0;
			getTrendAndBOCount(project, trendsCount, businessOutlookCount, row);

			createCell(row, 25, csSkills, null);
			createCell(row, 26, csTrendBOs, null);
		}
	}

	private String getDelimSeparatedString(Map<String, Long> coutryCnts) {
		return (coutryCnts.isEmpty()) ? "" : coutryCnts.entrySet().stream().map(e -> e.getKey()+" ("+e.getValue()+")").collect(Collectors.joining("; "));
	}

	private Map<String, Long> getCountryCountsMap(List<String> gidCountry) {
		return (gidCountry != null) ? gidCountry.stream().filter(Objects::nonNull).collect(Collectors.groupingBy(Function.identity(), Collectors.counting())) : new HashMap<>();
	}

	private void createCell(Row row, int columnCount, Object value, CellStyle style) {
		Cell cell = row.createCell(columnCount);
		if (value instanceof Integer val) {
			cell.setCellValue(val);
		} else if (value instanceof Boolean val) {
			cell.setCellValue(val);	
		} else {
			cell.setCellValue((String) value);
		}
		cell.setCellStyle(style);
	}


	private void getJobProfileCount(Workstream project, int futureHCDemandSum, int futureHCSupplySum,
			int statusQuoHCSupplySum, int statusQuoSum, int futureSum, XSSFRow row) {
		List<JobProfile> jobProfiles = (project.getJobProfiles() != null) ? project.getJobProfiles() : new ArrayList<>();
		for (JobProfile jobProfile : jobProfiles) {
			if (Objects.nonNull(jobProfile.getCurrentHeadCountSupply()))
				statusQuoHCSupplySum += jobProfile.getCurrentHeadCountSupply();
			if (Objects.nonNull(jobProfile.getFutureHeadCountDemand()))
				futureHCDemandSum += jobProfile.getFutureHeadCountDemand();
			if (Objects.nonNull(jobProfile.getFutureHeadCountSupply()))
				futureHCSupplySum += jobProfile.getFutureHeadCountSupply();
			if (Boolean.FALSE.equals(jobProfile.getIsOriginFuture())) {
				futureSum++;
				statusQuoSum++;
			} else
				futureSum++;
		}
		createCell(row, 14, statusQuoHCSupplySum, null);
		createCell(row, 15, futureHCSupplySum, null);
		createCell(row, 16, futureHCDemandSum, null);
		createCell(row, 21, statusQuoSum, null);
		createCell(row, 22, futureSum, null);
	}

	private void getMatrixCount(Workstream project, int upskillingCount, int reskillingCount, int hireCount,
			int otherMeasuresCount, XSSFRow row) {

		List<LatestMatrixModel> matrixList = project.getLatestMatrixDetails();

		if (matrixList != null && !matrixList.isEmpty()) {
			for (LatestMatrixModel matrix : matrixList) {
				if (!CollectionUtils.isEmpty(matrix.getMeasures())) {
					hireCount = matrix.getMeasures().stream()
							.filter(measure -> measure.getMeasuresType().equals(MatrixMeasureType.WIN) || measure
									.getMeasuresType().toString().substring(0, 3).equals(MatrixMeasureType.WIN.name()))
							.mapToInt(measure -> measure.getMeasureConfiguration().get(0).getAssignedHeadCount()).sum();

					List<MeasureDto> measureList = matrix.getMeasures().stream()
							.filter(measure -> measure.getMeasuresType().equals(MatrixMeasureType.GROW)
									|| measure.getMeasuresType().equals(MatrixMeasureType.GROW_BOND))
							.toList();
					upskillingCount = CollectionUtils.emptyIfNull(measureList).stream()
							.filter(measure -> measure.getStatusQuoJPId().equals(measure.getFutureStateJPId()))
							.mapToInt(measure -> measure.getMeasureConfiguration().get(0).getAssignedHeadCount()).sum();
					reskillingCount = CollectionUtils.emptyIfNull(measureList).stream()
							.filter(measure -> !measure.getStatusQuoJPId().equals(measure.getFutureStateJPId()))
							.mapToInt(measure -> measure.getMeasureConfiguration().get(0).getAssignedHeadCount()).sum();

					otherMeasuresCount = matrix.getMeasures().stream()
							.filter(measure -> measure.getMeasuresType().equals(MatrixMeasureType.RESHUFFLE))
							.mapToInt(measure -> measure.getMeasureConfiguration().get(0).getAssignedHeadCount()).sum();

				}
			}
		}
		createCell(row, 17, upskillingCount, null);
		createCell(row, 18, reskillingCount, null);
		createCell(row, 19, hireCount, null);
		createCell(row, 20, otherMeasuresCount, null);
	}

	private void getTrendAndBOCount(Workstream project, int trendsCount, int businessOutlookCount, XSSFRow row) {
		List<TrendsAndBizOutlook> trendsAndBOs = (project.getTrends() != null) ? project.getTrends() : new ArrayList<>();
		for (TrendsAndBizOutlook trendsAndBO : trendsAndBOs) {
			if (trendsAndBO.getImpactType().equalsIgnoreCase("trends"))
				trendsCount += 1;
			else if (trendsAndBO.getImpactType().equalsIgnoreCase("Business Outlook"))
				businessOutlookCount += 1;
		}
		createCell(row, 23, trendsCount, null);
		createCell(row, 24, businessOutlookCount, null);
	}

	private List<SkillResponseDTO> skillResponseMethod(List<SkillAssignment> skills, Map<String, Skills> skillMap) {
		List<SkillResponseDTO> skillResponseDTO = new ArrayList<>();
		int count = 0;
		Map<String, Integer> hm = new HashMap<>();
		for (SkillAssignment skill : skills) {
			Integer j = hm.get(skill.getSkillId());
			hm.put(skill.getSkillId(), (j == null) ? 1 : j + 1);
		}
		List<Entry<String, Integer>> list = new LinkedList<>(hm.entrySet());

		// Sort the list
		list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

		// put data from sorted list to hashmap
		HashMap<String, Integer> temp = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> aa : list) {
			temp.put(aa.getKey(), aa.getValue());
		}

		for (Entry<String, Integer> entry : temp.entrySet()) {
			if (count < 5) {
				SkillResponseDTO responseDTO = new SkillResponseDTO();
				responseDTO.setId(entry.getKey());
				if(skillMap.containsKey(entry.getKey())) {
					responseDTO.setName(skillMap.get(entry.getKey()).getName());
				}
				responseDTO.setCount(entry.getValue());
				skillResponseDTO.add(responseDTO);
				count++;
			} else if (count >= 5
					&& (skillResponseDTO.get(skillResponseDTO.size() - 1).getCount() == entry.getValue().intValue())) {
				SkillResponseDTO response = new SkillResponseDTO();
				response.setId(entry.getKey());
				if(skillMap.containsKey(entry.getKey())) {
					response.setName(skillMap.get(entry.getKey()).getName());
				}
				response.setCount(entry.getValue());
				skillResponseDTO.add(response);
				count++;
			}
		}
		return skillResponseDTO;
	}

	private List<TrendsAndBOResponseDTO> trendsAndBOResponseMethod(List<TrendsAndBizOutlook> trendsAndBO) {
		if(trendsAndBO == null || trendsAndBO.isEmpty()) return new ArrayList<>();
		
		Map<String, TrendsAndBizOutlook> trendMap = trendsAndBO.stream().collect(Collectors.toMap(TrendsAndBizOutlook::getUid, s -> s));
		int count = 0;
		Map<String, Integer> hm = new HashMap<>();
		for (TrendsAndBizOutlook trendsBO : trendsAndBO) {
			Integer j = 1;
			if(hm.containsKey(trendsBO.getUid())) {
				j = hm.get(trendsBO.getUid());
				j = j +1;
			}
			hm.put(trendsBO.getUid(), j);
		}
		List<Entry<String, Integer>> list = new LinkedList<>(hm.entrySet());

		// Sort the list
		list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

		// put data from sorted list to hashmap
		HashMap<String, Integer> temp = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> aa : list) {
			temp.put(aa.getKey(), aa.getValue());
		}

		return getTrendsAndBOResponse(trendMap, count, temp);
	}

	private List<TrendsAndBOResponseDTO> getTrendsAndBOResponse(Map<String, TrendsAndBizOutlook> trendMap, int count, HashMap<String, Integer> temp) {
		List<TrendsAndBOResponseDTO> trendsAndBOResponse = new ArrayList<>();
		for (Entry<String, Integer> entry : temp.entrySet()) {
			if (count < 5) {
				TrendsAndBOResponseDTO responseDTO = new TrendsAndBOResponseDTO();
				responseDTO.setId(entry.getKey());
				if(trendMap.containsKey(entry.getKey())) {
					responseDTO.setName(trendMap.get(entry.getKey()).getImpactSubCategory());
				}
				responseDTO.setCount(entry.getValue());
				trendsAndBOResponse.add(responseDTO);
				count++;
			} else if (count >= 5 && (trendsAndBOResponse.get(trendsAndBOResponse.size() - 1).getCount() == entry
					.getValue().intValue())) {
				TrendsAndBOResponseDTO response = new TrendsAndBOResponseDTO();
				response.setId(entry.getKey());
				if(trendMap.containsKey(entry.getKey())) {
					response.setName(trendMap.get(entry.getKey()).getImpactSubCategory());
				}
				response.setCount(entry.getValue());
				trendsAndBOResponse.add(response);
				count++;
			}
		}
		return trendsAndBOResponse;
	}

	@Override
	public List<DevPathExcelDTO> listAll(String projectId) {
		Optional<Workstream> projectEntity = workStreamRepository.findById(projectId);
		List<DevPathExcelDTO> list=new ArrayList<>();
		if (!projectEntity.isPresent()) {
			throw new ResourceNotFoundException(NextworkConstants.PROJECTID_NOT_FOUND);
		}
		
		Workstream ws = projectEntity.get();
		List<LatestMatrixModel> matrixs = (ws.getLatestMatrixDetails() != null) ?  ws.getLatestMatrixDetails() : new ArrayList<>();
		Map<String, JobProfile> jpMap = (ws.getJobProfiles() != null) ? ws.getJobProfiles().stream().collect(Collectors.toMap(JobProfile::getUid , j -> j)) : new HashMap<>();

		for (LatestMatrixModel matrix : matrixs) {
			if (Objects.isNull(matrix.getMeasures())) {
				continue;
			}
			getMeasureData(list,  jpMap, matrix);
		}
		return list;
	}

	private List<MeasureDto> getMeasureData(List<DevPathExcelDTO> list,  Map<String, JobProfile> jpMap,
			LatestMatrixModel matrix) {
		List<MeasureDto> findMatrixLinksByMatrixId = matrix.getMeasures();
		for(MeasureDto measureData:findMatrixLinksByMatrixId) {
			DevPathExcelDTO devPathExcelDTO = new DevPathExcelDTO();		
			devPathExcelDTO.setMeasure((measureData.getMeasuresType().name()));
			devPathExcelDTO.setAssignedHC(getTotalAssignedHeadCount(measureData.getMeasureConfiguration()));
			devPathExcelDTO.setTrainings(new ArrayList<>());
			if (measureData.getStatusQuoJPId() != null) {
				JobProfile jp = jpMap.get( measureData.getStatusQuoJPId());
				if(!Objects.isNull(jp)){
					devPathExcelDTO.setStatusQuoJobProfile(jp.getName());
					devPathExcelDTO.setGripCodeStatusQuoJobProfile(jp.getGripName());
				}	
			}
			
			if (measureData.getFutureStateJPId() != null) {
				JobProfile jp = jpMap.get( measureData.getFutureStateJPId());
				if(!Objects.isNull(jp)){
					devPathExcelDTO.setFutureStateJobProfile(jp.getName());
					devPathExcelDTO.setGripCodeFutureStateJobProfile(jp.getGripName());	
				}
			}
			list.add(devPathExcelDTO);
		}
		return findMatrixLinksByMatrixId;
	}

	private int getTotalAssignedHeadCount(List<MeasureConfiguration> measureConfigurations) {
		if (measureConfigurations == null || measureConfigurations.isEmpty()) {
			return 0;
		}
		MeasureConfiguration firstMeasureConfig = measureConfigurations.get(0);
		if (firstMeasureConfig == null || firstMeasureConfig.getAssignedHeadCount() == null) {
			return 0;
		}
		return firstMeasureConfig.getAssignedHeadCount();
	}
	
	public String convertTocamelCase(String measure) {
		StringJoiner joiner = new StringJoiner(" ");
		String[] words = measure.split("[\\W_]+");
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();    
			joiner.add(word);
		}
		return joiner.toString();
	}
	
	CellStyle style(XSSFWorkbook workbook)
	{
		XSSFCellStyle styleBold = workbook.createCellStyle();
		XSSFFont fontBold = workbook.createFont();
		fontBold.setBold(true);
		styleBold.setFont(fontBold);
		  styleBold.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		  styleBold.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return styleBold;
		
	}
	CellStyle styleColourLigh(XSSFWorkbook workbook)
	{
		XSSFCellStyle styleBold = workbook.createCellStyle();
		XSSFFont fontBold = workbook.createFont();
		fontBold.setBold(true);
		styleBold.setFont(fontBold);
		
		  styleBold.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE1.getIndex());
		  styleBold.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return styleBold;
		
	}
	
	CellStyle styleWithLargeFontSize(XSSFWorkbook workbook)
	{
		XSSFCellStyle styleBold = workbook.createCellStyle();
		XSSFFont fontBold = workbook.createFont();
		fontBold.setBold(true);
		fontBold.setFontHeight(16);
		styleBold.setFont(fontBold);
		return styleBold;
		
	}
	
	@Override
	public int claculateSkillGap(SkillAssignment jobProfileSkill)
	{
		String futureSkillLevel="";
		String currentSkillLevel="";
		if(jobProfileSkill!=null)
		{
			futureSkillLevel=jobProfileSkill.getFutureSkillLevel();
			currentSkillLevel=jobProfileSkill.getCurrentSkillLevel();
		}
		int difference=0;
		difference=skillLevelValue(futureSkillLevel)-skillLevelValue(currentSkillLevel);
		if(difference>0)
		{
			return difference;
		}
		else
		{
			return 0;
		}
	}
	
	public int  skillLevelValue(String skill) {
		
		if(skill==null)
		{
			return 0;
		}
		if(skill.equalsIgnoreCase("BEGINNER"))
		{
			return 1;
		}
		else if(skill.equalsIgnoreCase("BASIC"))
		{
			return 2;
		}
		else if(skill.equalsIgnoreCase("ADVANCED"))
		{
			return 3;
		}
		else if(skill.equalsIgnoreCase("EXPERT"))
		{
			return 4;
		}
		else
		{
			return 0;
		} 
	  }
			
}
