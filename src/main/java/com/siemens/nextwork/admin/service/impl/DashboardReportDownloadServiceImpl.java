package com.siemens.nextwork.admin.service.impl;

import static com.siemens.nextwork.admin.util.NextworkConstants.ATTACHMENT_FILEKEY;
import static com.siemens.nextwork.admin.util.NextworkConstants.CONTENT_DISPOSITION;
import static com.siemens.nextwork.admin.util.NextworkConstants.CURRENT_HC_SUPPLY;
import static com.siemens.nextwork.admin.util.NextworkConstants.FILE_NAME_DASHBOARD;
import static com.siemens.nextwork.admin.util.NextworkConstants.FILE_NAME_DASHBOARD_ZIP;
import static com.siemens.nextwork.admin.util.NextworkConstants.FUTURE_HC_DEMAND;
import static com.siemens.nextwork.admin.util.NextworkConstants.FUTURE_HC_SUPPLY;
import static com.siemens.nextwork.admin.util.NextworkConstants.GRIP_CODE;
import static com.siemens.nextwork.admin.util.NextworkConstants.GRIP_NAME;
import static com.siemens.nextwork.admin.util.NextworkConstants.GRIP_POSITION;
import static com.siemens.nextwork.admin.util.NextworkConstants.HC_GAP;
import static com.siemens.nextwork.admin.util.NextworkConstants.JOB_PROFILES;
import static com.siemens.nextwork.admin.util.NextworkConstants.MEASURES_TAB;
import static com.siemens.nextwork.admin.util.NextworkConstants.NO_CURRENT_PROFICIENCY;
import static com.siemens.nextwork.admin.util.NextworkConstants.NO_FUTURE_PROFICIENCY;
import static com.siemens.nextwork.admin.util.NextworkConstants.PROJECTID_NOT_FOUND;
import static com.siemens.nextwork.admin.util.NextworkConstants.SKILLS;
import static com.siemens.nextwork.admin.util.NextworkConstants.SKILL_COUNT;
import static com.siemens.nextwork.admin.util.NextworkConstants.SKILL_GAP;
import static com.siemens.nextwork.admin.util.NextworkConstants.SKILL_TAB;
import static com.siemens.nextwork.admin.util.NextworkConstants.WORKSTREAMS;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;


import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import com.siemens.nextwork.admin.dto.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siemens.nextwork.admin.dto.matrix.LatestMatrixModel;
import com.siemens.nextwork.admin.dto.matrix.MeasureConfiguration;
import com.siemens.nextwork.admin.dto.matrix.MeasureDto;
import com.siemens.nextwork.admin.dto.matrix.Tier2Data;
import com.siemens.nextwork.admin.enums.AllTierEnum;
import com.siemens.nextwork.admin.enums.MatrixMeasureType;
import com.siemens.nextwork.admin.enums.StageAction;
import com.siemens.nextwork.admin.exception.ResourceNotFoundException;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.model.GidData;
import com.siemens.nextwork.admin.model.Workstream;
import com.siemens.nextwork.admin.model.WorkstreamGids;
import com.siemens.nextwork.admin.repo.RolesRepository;
import com.siemens.nextwork.admin.repo.WorkStreamRepository;
import com.siemens.nextwork.admin.repo.WorkstreamGidRepository;
import com.siemens.nextwork.admin.service.DashboardReportDownloadService;
import com.siemens.nextwork.admin.service.ProjectKpiDownloadService;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.util.NextworkConstants;
import com.siemens.nextwork.admin.util.NextworkDateUtils;

import io.jsonwebtoken.lang.Collections;



@Service
public class DashboardReportDownloadServiceImpl implements DashboardReportDownloadService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DashboardReportDownloadServiceImpl.class);

	@Autowired
	WorkStreamRepository workStreamRepository;

	@Autowired
	private UserService userService;
	
	@Autowired
	RolesRepository rolesRepository; 
	
	@Autowired
	NextworkDateUtils nextworkDateUtils;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private ProjectKpiDownloadService projectKpiDownloadService;

	@Autowired
	private WorkstreamGidRepository workstreamGidRepository;
	

	
	@Transactional
	@Override
	public byte[] getDashboardReportAsExcel(String workstreamId, String userEmail, Boolean isBulkDownload, HttpServletResponse response) throws IOException {
		
		userService.checkAdminUserRole(userEmail);

		Optional<Workstream> workstream = workStreamRepository.findById(workstreamId);
		if (!workstream.isPresent())
			throw new ResourceNotFoundException(PROJECTID_NOT_FOUND);
		List<JobProfile> jobprofileList = (workstream.get().getJobProfiles() != null) ? workstream.get().getJobProfiles().stream().filter(e-> !e.getName().equalsIgnoreCase(NextworkConstants.INVALID_GRIP_JOB_PROFILE)).toList() : new ArrayList<>();
		List<Skills> skills = (workstream.get().getSkills() != null) ? workstream.get().getSkills() : new ArrayList<>();

		String headerValue;
		if(Boolean.TRUE.equals(isBulkDownload)) {
			 headerValue = ATTACHMENT_FILEKEY + FILE_NAME_DASHBOARD_ZIP;
		} else {
			 headerValue = ATTACHMENT_FILEKEY + FILE_NAME_DASHBOARD;
		}

		response.setHeader(CONTENT_DISPOSITION, headerValue);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("Workstream Info");
		
		try {

			formWorkstreamInfoLayout(workstream.get(), jobprofileList,skills, sheet, workbook);
			sheet = workbook.createSheet("Trend");
			formTrendInfoLayout(workstream.get(), jobprofileList, sheet, workbook);
			sheet = workbook.createSheet(SKILLS);
			formSkillsInfoLayout(workstream.get(), jobprofileList,skills, sheet, workbook);
			sheet = workbook.createSheet(MEASURES_TAB);
			formMeasuresInfoLayout(workstream.get(), jobprofileList, sheet, workbook);
			sheet = workbook.createSheet("Country");
			formCountryInfoLayout(workstream.get(), jobprofileList, sheet, workbook);
			
			workbook.write(bos);
			
		} catch (IOException e) {
			throw new RestBadRequestException(e.getMessage());
		}
		finally {
			workbook.close();
		}
		return bos.toByteArray();
	}
	
	public byte[] getDashboardReportAsExcelByListWsIds(List<String> workstreamIds, StageAction stage, String userEmail, Boolean isBulkDownload, HttpServletResponse response) throws IOException, InterruptedException, ExecutionException {
		userService.checkAdminUserRole(userEmail);

		if(Objects.nonNull(stage) && CollectionUtils.isEmpty(workstreamIds)) {
			List<WorkStreamIdDTO> workStreamIdsList= workStreamRepository.findWorkStreamIds(stage.toString());

			workstreamIds = workStreamIdsList.stream().map(WorkStreamIdDTO:: getId).collect(Collectors.toCollection(ArrayList:: new));

		} else if(Objects.isNull(stage) && CollectionUtils.isEmpty(workstreamIds)) {
			List<WorkStreamIdDTO> workStreamIdsList= workStreamRepository.findWorkStreamIdsWithoutStage();

			workstreamIds = workStreamIdsList.stream().map(WorkStreamIdDTO:: getId).collect(Collectors.toCollection(ArrayList:: new));
		}

		// Create a temporary directory to store the Excel files
		Path tempDir = Files.createTempDirectory("workstream_reports");

		// Create a FixedThreadPool executor
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		// List to hold Future objects
		List<Future<Path>> futures = new ArrayList<>();

		for (String workstreamId : workstreamIds) {
			// Submit a task to the executor for each workstream id
			futures.add(executor.submit(() -> {
				// Generate the Excel file for a workstream id
				return generateExcelReport(workstreamId, userEmail, tempDir, isBulkDownload, response);
			}));
		}

		// Shutdown the executor
		executor.shutdown();

		try {
			// Wait for all tasks to complete
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			// Zip the Excel files
			Path zipFile = Files.createTempFile("workstream_reports", ".zip");
			try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
				for (Future<Path> future : futures) {
					Path path = future.get();
					ZipEntry zipEntry = new ZipEntry(tempDir.relativize(path).toString());
					zos.putNextEntry(zipEntry);
					Files.copy(path, zos);
					zos.closeEntry();
				}
			}

			// Return the zip file as a byte array
			return Files.readAllBytes(zipFile);
		} finally {
			// Clean up temporary directory
			FileUtils.deleteDirectory(tempDir.toFile());
		}
	}

	private Path generateExcelReport(String workstreamId, String userEmail, Path tempDir, Boolean isBulkDownload, HttpServletResponse response) {
		try {
			byte[] excelData = getDashboardReportAsExcel(workstreamId, userEmail, isBulkDownload, response);

			String filename = workstreamId + ".xlsx";

			// Write the Excel data to a file in the temporary directory
			Path excelFile = tempDir.resolve(filename);
			Files.write(excelFile, excelData);

			return excelFile;
		} catch (IOException e) {
			// Log or handle the exception
			throw new RuntimeException("Error occurred while generating Excel report for workstream ID: " + workstreamId, e);
		}
	}

	private void formCountryInfoLayout(Workstream workstream, List<JobProfile> jobprofileList, XSSFSheet sheet,
			XSSFWorkbook workbook) {
		
		XSSFCellStyle styleBold = workbook.createCellStyle();
		XSSFFont fontBold = workbook.createFont();
		fontBold.setBold(true);
		styleBold.setFont(fontBold);

		int rowId = 0;
		XSSFRow row;
		row = sheet.createRow(rowId);

		createCell(row, 0, "Workstream Name", styleBold);
		createCell(row, 1, "OrgCode", styleBold);
		createCell(row, 2, "GID version", styleBold);
		createCell(row, 3, "Country (short)", styleBold);
		createCell(row, 4, "Countries", styleBold);
		createCell(row, 5, "Location / Office City", styleBold);
		createCell(row, 6, "HC in scope", styleBold);
		
		++rowId;
		row = sheet.createRow(rowId);
		
		WorkstreamGids workstreamGids = workstreamGidRepository.findByWorkstreamId(workstream.getUid());

		if(workstreamGids!=null) {
			List<GidData> gidDataList = CollectionUtils.isNotEmpty(workstreamGids.getGidDataList() )? workstreamGids.getGidDataList() : new ArrayList<>();
			
			Set<String> distinctCities = gidDataList.stream()  
	                .map(gidData -> StringUtils.isNotEmpty(gidData.getLocationOfficeCity())? gidData.getLocationOfficeCity():"")  
	                .distinct()  
	                .collect(Collectors.toSet());   
			for(String  city: distinctCities)
			{
				List<GidData> cityGidDataList = gidDataList.stream()
	                    .filter(data -> (StringUtils.isNotEmpty(data.getLocationOfficeCity())? data.getLocationOfficeCity():"").equals(city))
	                    .toList();

	            List<String> orgCodePACount = cityGidDataList.stream()
	                    .map(GidData::getOrgCodePA)
	                    .distinct()
	                    .toList();
	   
	            Set<String> uniqueVersions = cityGidDataList.stream()  
	                    .map(GidData::getVersion)  
	                    .collect(Collectors.toSet());  
	   
	            List<String> countryRegionARE = cityGidDataList.stream().map(GidData::getCountryRegionARE).distinct().toList();  
	  
	            long gidCount = cityGidDataList.stream().map(GidData::getGid).count();  
				if(!orgCodePACount.contains(null)) {
					createCell(row, 0, workstream.getName(), null);
					createCell(row, 1, orgCodePACount , null);
					createCell(row, 2, new ArrayList<>(uniqueVersions), null);
					createCell(row, 3, null, null);
					createCell(row, 4, countryRegionARE, null);
					createCell(row, 5, city, null);
					createCell(row, 6, gidCount, null);
					row = sheet.createRow(++rowId);
				}
			}
		}
		
	}

	private void formMeasuresInfoLayout(Workstream workstream, List<JobProfile> jobprofileList, XSSFSheet sheet,
			XSSFWorkbook workbook) {
		XSSFCellStyle styleBold = workbook.createCellStyle();
		XSSFFont fontBold = workbook.createFont();
		fontBold.setBold(true);
		styleBold.setFont(fontBold);

		int rowId = 0;
		XSSFRow row = sheet.createRow(rowId);

		createCell(row, 0, WORKSTREAMS, styleBold);
		createCell(row, 1, JOB_PROFILES, styleBold);
		createCell(row, 2, GRIP_NAME, styleBold);
		createCell(row, 3, GRIP_CODE, styleBold);
		createCell(row, 4, GRIP_POSITION, styleBold);
		createCell(row, 5, CURRENT_HC_SUPPLY, styleBold);
		createCell(row, 6, FUTURE_HC_SUPPLY, styleBold);
		createCell(row, 7, FUTURE_HC_DEMAND, styleBold);
		createCell(row, 8, "Development Paths", styleBold);
		createCell(row, 9, "Measure level 1", styleBold);
		createCell(row, 10, "Measure Level 2", styleBold);
		createCell(row, 11, "Measure Level 3", styleBold);
		createCell(row, 12, "Due date", styleBold);
		createCell(row, 13, "Cost", styleBold);
		createCell(row, 14, "Assigned HC", styleBold);
		createCell(row, 15, HC_GAP, styleBold);
		createCell(row, 16, "Year 1 HC", styleBold);
		createCell(row, 17, "Year 2 HC", styleBold);
		createCell(row, 18, "Year 3 HC", styleBold);

		List<JobProfilesMeasureDTO> jpMeasuresList= getMeasureDataForWorkstream(workstream.getLatestMatrixDetails(), jobprofileList);
		
		
		populateDataRows(workstream, jobprofileList, sheet, jpMeasuresList);  
		
	}

	private void populateDataRows(Workstream workstream, List<JobProfile> jobprofileList, XSSFSheet sheet,
			List<JobProfilesMeasureDTO> jpMeasuresList) {
		int rowId = 0;
	    XSSFRow row;
		for (JobProfile jobProfiles : jobprofileList) {
			row = sheet.createRow(++rowId);
		    createCell(row, 0, workstream.getName(), null);  
		    jobProfileExcelData(row, 1, jobProfiles, MEASURES_TAB);  
		    createCell(row, 8, null, null);  
		    createCell(row, 9, null, null);  
		    createCell(row, 10, null, null);  
		      
		    List<JobProfilesMeasureDTO> matrixListForJP = jpMeasuresList.stream()  
		            .filter(e -> e.getStausQuoJobProfileId().equalsIgnoreCase(jobProfiles.getUid()))  
		            .toList();  
		      
		    if (Collections.isEmpty(matrixListForJP)) {   
		        row = sheet.createRow(rowId++);  
		    } else {    
		        for (JobProfilesMeasureDTO matrix : matrixListForJP) {     
		        	row = handleMatrix(matrix, row, workstream, jobProfiles, sheet);
	                ++rowId;
		        }  
		    }  
		}
	}
	
	private XSSFRow handleMatrix(JobProfilesMeasureDTO matrix, XSSFRow row, Workstream workstream, JobProfile jobProfiles, XSSFSheet sheet) {
	    if (!matrix.getMeasure2NameAssignedHC().isEmpty()) {
	        for (Map.Entry<String, Integer> tier2Name : matrix.getMeasure2NameAssignedHC().entrySet()) {
	        	createCell(row, 0, workstream.getName(), null);  
	            jobProfileExcelData(row, 1, jobProfiles, MEASURES_TAB);  
                createCell(row, 8, matrix.getDevPath() + "::" + tier2Name.getKey(), null);  
                createCell(row, 9, matrix.getMeasureLevel1(), null);  
                createCell(row, 10, tier2Name.getKey(), null);  
                createCell(row, 14, tier2Name.getValue(), null);   
		        row = sheet.createRow(row.getRowNum()+1);
	        }
	    } else {
	    	createCell(row, 0, workstream.getName(), null);  
            jobProfileExcelData(row, 1, jobProfiles, MEASURES_TAB);
            createCell(row, 8, matrix.getDevPath(), null);
            createCell(row, 9, matrix.getMeasureLevel1(), null); 
            createCell(row, 10, null, null);  
            createCell(row, 14, null, null); 
	        row = sheet.createRow(row.getRowNum()+1);
	    }
	    return row;
	}

	private List<JobProfilesMeasureDTO> getMeasureDataForWorkstream(List<LatestMatrixModel> latestMatrixDetails, List<JobProfile> jobprofileList) {
		
		List<JobProfilesMeasureDTO> measureDataList=new ArrayList<>();
		if(Collections.isEmpty(latestMatrixDetails)){
			return measureDataList;
		}
		final HashSet<MatrixMeasureType> validMeasureTypes = new HashSet<>(Arrays.asList(MatrixMeasureType.WIN, MatrixMeasureType.GROW, MatrixMeasureType.BOND, MatrixMeasureType.MAINTAIN,MatrixMeasureType.RESHUFFLE)); 
		
		
		for(LatestMatrixModel matrix:latestMatrixDetails) {
			
			if(Collections.isEmpty(matrix.getMeasures()))
				continue;
			
			for(MeasureDto measure:matrix.getMeasures()) {
				getEachMatrixData(jobprofileList, measureDataList, validMeasureTypes, measure);	
			}
			
		}
		return measureDataList;
	}

	private void getEachMatrixData(List<JobProfile> jobprofileList, List<JobProfilesMeasureDTO> measureDataList,
			final HashSet<MatrixMeasureType> validMeasureTypes, MeasureDto measure) {
		Optional<String> futureStateJPName;
		JobProfilesMeasureDTO measureData;
		List<JobProfilesMeasureDTO> measureLevel2List;
		measureData=new JobProfilesMeasureDTO();

		measureData.setStausQuoJobProfileId(measure.getStatusQuoJPId());
		measureData.setFutureStateJobProfileId(measure.getFutureStateJPId());
		measureData.setDevPath("No Development Paths");
		futureStateJPName=jobprofileList.stream().filter(e-> e.getUid().equalsIgnoreCase(measure.getFutureStateJPId())).map(e->e.getName()).findFirst();
		if(futureStateJPName.isPresent()) {
			measureData.setFutureStateJobProfileName(futureStateJPName.get());
			if(validMeasureTypes.contains(measure.getMeasuresType())) {
				measureData.setMeasureLevel1(AllTierEnum.valueOf(measure.getMeasuresType().toString()).value);
				measureLevel2List = getMultipleMeasuresForSameProfiles(measure.getMeasureConfiguration(), measureData);

				if(!Collections.isEmpty(measureLevel2List))
					measureDataList.addAll(measureLevel2List);
			}
		}
		else
			measureDataList.add(measureData);
	}

	private List<JobProfilesMeasureDTO> getMultipleMeasuresForSameProfiles(List<MeasureConfiguration> measureConfiguration, JobProfilesMeasureDTO measureData) {
		
		List<JobProfilesMeasureDTO> allMeasure=new ArrayList<>();
		JobProfilesMeasureDTO measureDataNew;
		String measureTier2;
		String devPath;
		if(!Collections.isEmpty(measureConfiguration)){
			for(MeasureConfiguration measure:measureConfiguration) {
				measureDataNew=measureData;
				if(!Collections.isEmpty(measure.getTier2Data())){
					Map<String, Integer> measure2= new HashMap<>();
					measureTier2=AllTierEnum.valueOf(measure.getTier2Data().get(0).getTier2()).value;
					for (Tier2Data tier2Data : measure.getTier2Data()) {
						if(tier2Data!=null) {
							measure2.put(AllTierEnum.valueOf(tier2Data.getTier2()).value, tier2Data.getAssignedHeadCount());
						}
					}
					measureDataNew.setMeasure2NameAssignedHC(measure2);
					measureDataNew.setMeasureLevel2(measureTier2);
					measureDataNew.setMeasure2AssignedHC(measure.getTier2Data().get(0).getAssignedHeadCount());
					devPath=measureDataNew.getFutureStateJobProfileName();
					measureDataNew.setDevPath(devPath);
				}
				allMeasure.add(measureDataNew);
			}
		}
		return allMeasure;
	}



	private void formSkillsInfoLayout(Workstream workstream, List<JobProfile> jobprofileList, List<Skills> skills, XSSFSheet sheet,
			XSSFWorkbook workbook) {
		XSSFCellStyle styleBold = workbook.createCellStyle();
		XSSFFont fontBold = workbook.createFont();
		fontBold.setBold(true);
		styleBold.setFont(fontBold);

		int rowId = 0;
		XSSFRow row;
		row = sheet.createRow(rowId);

		createCell(row, 0, WORKSTREAMS, styleBold);
		createCell(row, 1, SKILLS, styleBold);
		createCell(row, 2, SKILL_COUNT, styleBold);
		createCell(row, 3, JOB_PROFILES, styleBold);
		createCell(row, 4, GRIP_NAME, styleBold);
		createCell(row, 5, GRIP_CODE, styleBold);
		createCell(row, 6, GRIP_POSITION, styleBold);
		createCell(row, 7, "Current Proficiency", styleBold);
		createCell(row, 8, "Future Proficiency", styleBold);
		createCell(row, 9, SKILL_GAP, styleBold);
		
		++rowId;
		row = sheet.createRow(rowId);
		int skillGap=0;
		List<SkillAssignment> jobProfileSkills;
		List<String> skillIdAssigned=new ArrayList<>();
		int skillCount = (skills != null) ? skills.size() : null;
		createCell(row, 0, workstream.getName(), null);
		String currentProfiency;
		String futureProfiency;
		for(JobProfile jobProfiles: jobprofileList)
		{
			
			jobProfileSkills = (!Collections.isEmpty(jobProfiles.getSkillAssignments()))? jobProfiles.getSkillAssignments() : new ArrayList<>();
			
			if (Collections.isEmpty(jobProfiles.getSkillAssignments())) {
				createCell(row, 0, workstream.getName(), null);
				createCell(row, 1, "No Skills", null);
				createCell(row, 2, skillCount, null);
				jobProfileExcelData(row, 3, jobProfiles, SKILL_TAB);
				createCell(row, 7, NO_CURRENT_PROFICIENCY, null);
				createCell(row, 8, NO_FUTURE_PROFICIENCY, null);
				createCell(row, 9, 0, null);
				++rowId;
				row = sheet.createRow(rowId);
				continue;
			}
			
			
			for (SkillAssignment skillData : jobProfileSkills) {
				skillGap= projectKpiDownloadService.claculateSkillGap(skillData);
				createCell(row, 0, workstream.getName(), null);
				createCell(row, 1, skillData.getSkillName(), null);
				createCell(row, 2, skillCount, null);
				jobProfileExcelData(row, 3, jobProfiles, SKILL_TAB);

				currentProfiency = (skillData.getCurrentSkillLevel() != null) ? skillData.getCurrentSkillLevel()
						: "No Current Proficiency";
				futureProfiency = (skillData.getFutureSkillLevel() != null) ? skillData.getFutureSkillLevel()
						: "No Future Proficiency";
				createCell(row, 7, currentProfiency, null);
				createCell(row, 8, futureProfiency, null);
				createCell(row, 9, skillGap, null);
				++rowId;
				row = sheet.createRow(rowId);
				skillIdAssigned.add(skillData.getSkillId());
			}
			
		}
		
		if(!Collections.isEmpty(skillIdAssigned)) {
			List<String> skillUnassigned=skills.stream().filter(e-> !skillIdAssigned.contains(e.getUid())).map(e->e.getName()).toList();
			
			getUnassignedSkillData(workstream, sheet, rowId, row, skillCount, skillUnassigned);
		}
		
	}

	private void getUnassignedSkillData(Workstream workstream, XSSFSheet sheet, int rowId, XSSFRow row,
			int skillCount, List<String> skillUnassigned) {
		if(!Collections.isEmpty(skillUnassigned)) {
			for(String skill:skillUnassigned) {
				createCell(row, 0, workstream.getName(), null);
				createCell(row, 1, skill, null);
				createCell(row, 2, skillCount, null);
				getDefaultJobProfileData(row, 3);
				createCell(row, 7, NO_CURRENT_PROFICIENCY, null);
				createCell(row, 8, NO_FUTURE_PROFICIENCY, null);
				createCell(row, 9, 0, null);
				++rowId;
				row = sheet.createRow(rowId);
			}
		}
	}

	private void formTrendInfoLayout(Workstream workstream, List<JobProfile> jobprofileList, XSSFSheet sheet, XSSFWorkbook workbook) {
		// set Style and font
		XSSFCellStyle styleBold = workbook.createCellStyle();
		XSSFFont fontBold = workbook.createFont();
		fontBold.setBold(true);
		styleBold.setFont(fontBold);

		int rowId = 0;
		XSSFRow row;
		row = sheet.createRow(rowId);

		createCell(row, 0, WORKSTREAMS, styleBold);
		createCell(row, 1, SKILL_COUNT, styleBold);
		createCell(row, 2, JOB_PROFILES, styleBold);
		createCell(row, 3, GRIP_NAME, styleBold);
		createCell(row, 4, GRIP_CODE, styleBold);
		createCell(row, 5, GRIP_POSITION, styleBold);
		createCell(row, 6, "Job Family", styleBold);
		createCell(row, 7, "Job clusters", styleBold);
		createCell(row, 8, CURRENT_HC_SUPPLY, styleBold);
		createCell(row, 9, FUTURE_HC_SUPPLY, styleBold);
		createCell(row, 10, FUTURE_HC_DEMAND, styleBold);
		createCell(row, 11, HC_GAP, styleBold);
		createCell(row, 12, "Impacts", styleBold);
		createCell(row, 13, "Impact Type", styleBold);
		createCell(row, 14, "Impact Category", styleBold);
		createCell(row, 15, "Impact On Skills", styleBold);
		createCell(row, 16, "Impact On HC", styleBold);
		createCell(row, 17, SKILL_GAP, styleBold);
		
		++rowId;
		row = sheet.createRow(rowId);
	
		List<ReportTrendImpactDTO> trendsList=(workstream.getTrends() != null) ? getImpactsReportData(workstream.getTrends()) : new ArrayList<>();
		
		
		createCell(row, 0, workstream.getName(), null);
		for(JobProfile jobProfiles: jobprofileList)
		{
			
			List<ReportTrendImpactDTO> trendsListForJP=trendsList.stream().filter(e-> (e.getJobProfileId() != null && e.getJobProfileId().equalsIgnoreCase(jobProfiles.getUid()))).toList();
			

			if(Collections.isEmpty(trendsListForJP)) 
			{
				ReportTrendImpactDTO trendImpact=new ReportTrendImpactDTO();
				trendImpact.setImpactName("No Impacts");
				trendImpact.setImpactType("No Impact Type");
				trendImpact.setImpactCategory("No Impact Category");
				trendImpact.setImpactOnHC("No Impact On HC");
				trendImpact.setImpactOnSkills("No Impact On Skills");
				setTrendsTabData(workstream, row, jobProfiles, trendImpact);
				++rowId;
				row = sheet.createRow(rowId);
				continue;
			}
				
			for(ReportTrendImpactDTO trendImpact : trendsListForJP) {
			
				setTrendsTabData(workstream, row, jobProfiles, trendImpact);
				++rowId;
				row = sheet.createRow(rowId);

			}
		}

		List<ReportTrendImpactDTO> trendsListForNoJP = trendsList.stream()
				.filter(e -> Objects.isNull(e.getJobProfileId())).toList();
		if (!Collections.isEmpty(trendsListForNoJP)) {
			for (ReportTrendImpactDTO trendImpact : trendsListForNoJP) {
				setTrendsTabData(workstream, row, null, trendImpact);
				++rowId;
				row = sheet.createRow(rowId);
			}
		}

	}

	private void formWorkstreamInfoLayout(Workstream workstream, List<JobProfile> jobprofileList, List<Skills> skills, XSSFSheet sheet, XSSFWorkbook workbook) {
		
		// set Style and font
		XSSFCellStyle styleBold = workbook.createCellStyle();
		XSSFFont fontBold = workbook.createFont();
		fontBold.setBold(true);
		styleBold.setFont(fontBold);

		int rowId = 0;
		XSSFRow row;
		row = sheet.createRow(rowId);
		LOGGER.info("MMXX: row={}",rowId);
		
		createCell(row, 0, WORKSTREAMS, styleBold);
		createCell(row, 1, "Status", styleBold);
		createCell(row, 2, "Org Code", styleBold);
		createCell(row, 3, "People Structure", styleBold);
		createCell(row, 4, "Creation Date", styleBold);
		createCell(row, 5, "Job Family Count", styleBold);
		createCell(row, 6, "Workstream Current HC In Scope", styleBold);
		createCell(row, 7, "Workstream Future HC In Scope", styleBold);
		createCell(row, 8, "Workstream Future HC In Demand", styleBold);
		createCell(row, 9, "Status Quo Job Profile Count", styleBold);
		createCell(row, 10, "Future State Job Profile Count", styleBold);
		createCell(row, 11, SKILL_COUNT, styleBold);
		createCell(row, 12, JOB_PROFILES, styleBold);
		createCell(row, 13, GRIP_NAME, styleBold);
		createCell(row, 14, GRIP_CODE, styleBold);
		createCell(row, 15, GRIP_POSITION, styleBold);
		createCell(row, 16, "Job Family", styleBold);
		createCell(row, 17, "Job clusters", styleBold);
		createCell(row, 18, CURRENT_HC_SUPPLY, styleBold);
		createCell(row, 19, FUTURE_HC_SUPPLY, styleBold);
		createCell(row, 20, FUTURE_HC_DEMAND, styleBold);
		createCell(row, 21, HC_GAP, styleBold);
		createCell(row, 22, SKILL_GAP, styleBold);
		
		++rowId;
		
		row = sheet.createRow(rowId);
		WorkstreamGids workstreamGids = workstreamGidRepository.findByWorkstreamId(workstream.getUid());

		if(workstreamGids!=null) {
			populateWorkstreamData(workstream, jobprofileList, skills, sheet, rowId, row, workstreamGids);
		}

	}

	private void populateWorkstreamData(Workstream workstream, List<JobProfile> jobprofileList, List<Skills> skills,
			XSSFSheet sheet, int rowId, XSSFRow row, WorkstreamGids workstreamGids) {
		List<GidData> gidList = CollectionUtils.isNotEmpty(workstreamGids.getGidDataList())? workstreamGids.getGidDataList() : new ArrayList<>();
		List<String> orgsList = gidList.stream().filter(e-> (e.getOrgCodePA()!=null)).map(GidData::getOrgCodePA).toList();
		List<String> gidStructure = gidList.stream().filter(e-> (e.getBlueCollarWhiteCollar()!=null)).map(GidData::getBlueCollarWhiteCollar).toList();
		List<String> gidValueStream = gidList.stream().map(GidData::getJobFamily).toList();

		String peopleStructure =(gidStructure!=null) ? gidStructure.stream().distinct().collect(Collectors.joining(", ")) : "No People Structure";		
		String csOrgs = (orgsList!=null) ? orgsList.stream().distinct().collect(Collectors.joining(", ")) : "No Org Code";
		
		
		int skillCount = (skills != null) ? skills.size() : null;
		int jobFamilyCount= (gidValueStream != null) ? gidValueStream.size() : null;

		int futureHCDemandSum = 0;
		int futureHCSupplySum = 0;
		int statusQuoHCSupplySum = 0;

		int col=0;
		int skillGap = 0;
		createCell(row, 0, workstream.getName(), null);
		createCell(row, 1, workstream.getStage().value, null);
		createCell(row, 2, csOrgs, null);
		createCell(row, 3, peopleStructure, null);
		createCell(row, 4, nextworkDateUtils.getFormattedDate(workstream.getCreatedOn()), null);
		createCell(row, 5, jobFamilyCount, null);
		
		LOGGER.info("MMXX: dis-5");
		for(JobProfile jobProfiles: jobprofileList)
		{
			
			if (jobProfiles != null && !StringUtils.isAllBlank(jobProfiles.getGripName())) {

				createCell(row, 0, workstream.getName(), null);
				createCell(row, 1, workstream.getStage().value, null);
				createCell(row, 2, csOrgs, null);
				createCell(row, 3, peopleStructure, null);
				createCell(row, 4, nextworkDateUtils.getFormattedDate(workstream.getCreatedOn()), null);
				createCell(row, 5, jobFamilyCount, null);

				col=6;
				getJobProfileCount(workstream, futureHCDemandSum, futureHCSupplySum, statusQuoHCSupplySum, 
						 row, col);

				createCell(row, 11, skillCount, null);
				
				col=12;
				jobProfileExcelData(row,col,jobProfiles,"workstream");
				
				List<SkillAssignment> jobProfileSkills = (jobProfiles.getSkillAssignments() != null) ? jobProfiles.getSkillAssignments() : new ArrayList<>();
				
				
				//HC gap
				col=21;
				calculateHcGap(jobProfiles,row,col);
				
				
				col=22;
				calculateSkillGap(row,col,jobProfileSkills,skillGap);
				
			

				++rowId;
				row = sheet.createRow(rowId);
			}
		}
		
	}

	private void setTrendsTabData(Workstream workstream, XSSFRow row, JobProfile jobProfiles,
			ReportTrendImpactDTO trendImpact) {
		List<Skills> skills = (workstream.getSkills() != null) ? workstream.getSkills() : new ArrayList<>();
		int skillCount = (skills != null) ? skills.size() : null;
		int col=0;
		int skillGap = 0;
		createCell(row, 0, workstream.getName(), null);
		createCell(row, 1, skillCount, null);
		
//		Default Values
		col=2;
		getDefaultJobProfileData(row, col);
		createCell(row, 8, null, null);
		createCell(row, 9, null, null);
		createCell(row, 10, null, null);
		createCell(row, 11, null, null);
		createCell(row, 17, null, null);
		
		if(Objects.nonNull(jobProfiles)) {
		col=2;
		jobProfileExcelData(row,col,jobProfiles,"trends");
		col=8;
		int currentHCSupply =jobProfiles.getCurrentHeadCountSupply()!=null?jobProfiles.getCurrentHeadCountSupply():0;
		int futureHCSupply = jobProfiles.getFutureHeadCountSupply()!=null?jobProfiles.getFutureHeadCountSupply():0;
		int futureHCDemand = jobProfiles.getFutureHeadCountDemand()!=null?jobProfiles.getFutureHeadCountDemand():0;
		getJobProfileCount(workstream, futureHCDemand, futureHCSupply, currentHCSupply, row, col);
		List<SkillAssignment> jobProfileSkills = (jobProfiles.getSkillAssignments() != null) ? jobProfiles.getSkillAssignments() : new ArrayList<>();
		
		//HC gap
		col=11;
		calculateHcGap(jobProfiles,row,col);
		col=17;
		calculateSkillGap(row,col,jobProfileSkills,skillGap);
		}
		
		createCell(row, 12, trendImpact.getImpactName(), null);
		createCell(row, 13, trendImpact.getImpactType(), null);
		createCell(row, 14, trendImpact.getImpactCategory(), null);
		createCell(row, 15, trendImpact.getImpactOnSkills(), null);
		createCell(row, 16, trendImpact.getImpactOnHC(), null);

		
	}

	private void getDefaultJobProfileData(XSSFRow row, int col) {
		createCell(row, col, "No Job Profiles", null);
		createCell(row, ++col, "No GRIP Name", null);
		createCell(row, ++col, "No GRIP Code", null);
		createCell(row, ++col, "No GRIP Position", null);
		createCell(row, ++col, "No Job Family", null);
		createCell(row, ++col, "No Job Clusters", null);
	}

	

	private void createCell(Row row, int columnCount, Object value, CellStyle style) {
		Cell cell = row.createCell(columnCount);
		if (value instanceof Integer val) {
			cell.setCellValue(val);
		} else if (value instanceof Boolean val) {
			cell.setCellValue(val);	
		} else if (value instanceof List) {
	        List<String> strings = (List<String>) value;
	        cell.setCellValue(String.join(", ", strings));  
	    } else if (value instanceof Long l) {  
	        cell.setCellValue(l);  
	    } else {
			cell.setCellValue((String) value);
		}
		cell.setCellStyle(style);
	}
	


	private void getJobProfileCount(Workstream project, int futureHCDemandTotal, int futureHCSupplyTotal,
			int statusQuoHCSupplyTotal, XSSFRow row, int col) {
		
		int statusQuoTotal = 0;
		int futureTotal = 0;
		List<JobProfile> jobProfileList = (project.getJobProfiles() != null) ? project.getJobProfiles(): new ArrayList<>();
		for (JobProfile jobProfile : jobProfileList) {
			if (Boolean.FALSE.equals(jobProfile.getIsOriginFuture())) {
				futureTotal++;
				statusQuoTotal++;
			} else
				futureTotal++;
		}
		createCell(row, col, statusQuoHCSupplyTotal, null);
		createCell(row, ++col, futureHCSupplyTotal, null);
		createCell(row, ++col, futureHCDemandTotal, null);
		createCell(row, ++col, statusQuoTotal, null);
		createCell(row, ++col, futureTotal, null);
	}
	
	private void jobProfileExcelData(XSSFRow row, int col, JobProfile jobProfiles, String tabName) {

		if(Objects.isNull(jobProfiles)) return;
		createCell(row, col, jobProfiles.getName(), null);
		createCell(row, ++col, jobProfiles.getGripName(), null);
		
		String gripPositionTypeList = jobProfiles.getGripPositionType() != null ? jobProfiles.getGripPositionType().stream().distinct().collect(Collectors.joining(", ")) : "No GRIP Position";
		String gripCodeList = jobProfiles.getGripPostion() != null ? jobProfiles.getGripPostion().stream().distinct().collect(Collectors.joining(", ")) : "No GRIP Code";
		String jobFamilyList = jobProfiles.getJobFamily() != null ? jobProfiles.getJobFamily().stream().distinct().collect(Collectors.joining(", ")) : "No Job Family";
		
		createCell(row, ++col, gripCodeList, null);
		createCell(row, ++col, gripPositionTypeList, null);
		
		if (!tabName.equalsIgnoreCase(SKILL_TAB) && !tabName.equalsIgnoreCase(MEASURES_TAB)) {
			createCell(row, ++col, jobFamilyList, null);
			createCell(row, ++col, "No Job Clusters", null);
		}
		
		if (!tabName.equalsIgnoreCase(SKILL_TAB)) {
		createCell(row, ++col, jobProfiles.getCurrentHeadCountSupply(), null);
		createCell(row, ++col, jobProfiles.getFutureHeadCountSupply(), null);
		createCell(row, ++col, jobProfiles.getFutureHeadCountDemand(), null);
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
	
	private int calculateSkillGap(XSSFRow row, int col, List<SkillAssignment> jobProfileSkills, int skillGap) {
		for(SkillAssignment jobProfileSkill:jobProfileSkills)
		{
				if(jobProfileSkill != null  && jobProfileSkill.getSkillId() !=null )
				{
					skillGap+=projectKpiDownloadService.claculateSkillGap(jobProfileSkill);
				}
		}
		createCell(row, col, skillGap, null);
		return skillGap;
		
	}
		
		private List<ReportTrendImpactDTO> getImpactsReportData(List<TrendsAndBizOutlook> trendList) {
			List<ReportTrendImpactDTO> impacts = new ArrayList<>();
			ReportTrendImpactDTO reportImpactsDTO;
			for (TrendsAndBizOutlook trend : trendList) {
				List<ImpactedJobProfiles> impactedJobProfilesList = trend.getImpactedJobProfiles();
				reportImpactsDTO = new ReportTrendImpactDTO();
				reportImpactsDTO.setImpactName(trend.getImpactSubCategory());
				reportImpactsDTO.setImpactType(trend.getImpactType());
				reportImpactsDTO.setImpactCategory(trend.getImpactCategory());
				reportImpactsDTO.setImpactOnHC("No Impact On HC");
				reportImpactsDTO.setImpactOnSkills("No Impact On Skills");
				if (null != impactedJobProfilesList && !impactedJobProfilesList.isEmpty()) {
					for (ImpactedJobProfiles impactedJP : impactedJobProfilesList) {
						reportImpactsDTO = new ReportTrendImpactDTO();
						reportImpactsDTO.setImpactName(trend.getImpactSubCategory());
						reportImpactsDTO.setImpactType(trend.getImpactType());
						reportImpactsDTO.setImpactCategory(trend.getImpactCategory());
						reportImpactsDTO.setJobProfileId(impactedJP.getJobProfileId());
						reportImpactsDTO.setImpactOnHC(impactedJP.getImpactOnHCDemand());
						reportImpactsDTO.setImpactOnSkills(impactedJP.getImpactOnSkill());
						impacts.add(reportImpactsDTO);
					}
				} else
					impacts.add(reportImpactsDTO);
			}
			return impacts;
		}

	@Override
	public String migrateWorkstream() {

		List<Workstream> workstreams = workStreamRepository.findAll();
		for (Workstream workstream : workstreams) {
			WorkstreamGids workstreamGids = new WorkstreamGids();
			workstreamGids.setWorkstreamId(workstream.getUid());
			workstreamGids.setGidList(workstream.getGidSummary());
			workstreamGids.setGidDataList(workstream.getGidList());
			WorkstreamGids savedWsGid = workstreamGidRepository.save(workstreamGids);
			workstream.setWorkstreamGidId(savedWsGid.getId());
			workStreamRepository.save(workstream);
		}
		return "success";
	}
	}
