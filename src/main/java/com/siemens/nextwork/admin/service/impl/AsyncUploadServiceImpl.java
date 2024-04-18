package com.siemens.nextwork.admin.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.siemens.nextwork.admin.dto.*;
import com.siemens.nextwork.admin.model.Workstream;
import com.siemens.nextwork.admin.repo.WorkStreamRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.siemens.nextwork.admin.exception.ResourceNotFoundException;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.model.GRIPCatalogue;
import com.siemens.nextwork.admin.model.SiemensMySkills;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.repo.GRIPCatalogueRepositroy;
import com.siemens.nextwork.admin.repo.SiemensMySkillsRepository;
import com.siemens.nextwork.admin.service.AsyncUploadService;
import com.siemens.nextwork.admin.service.FileStore;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.util.CommonUtils;
import com.siemens.nextwork.admin.util.GRIPCatalogueExcelConstants;
import com.siemens.nextwork.admin.util.GRIPCatalogueExcelConstantsMappingDTO;
import com.siemens.nextwork.admin.util.NextworkConstants;
import com.siemens.nextwork.admin.util.SkillExcelConstants;
import com.siemens.nextwork.admin.util.SkillExcelConstantsMappingDTO;


import static com.siemens.nextwork.admin.util.NextworkConstants.WORDS_SEPARATOR;

@Service
public class AsyncUploadServiceImpl implements AsyncUploadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncUploadServiceImpl.class);

	@Autowired
	private UserService userService;

	@Autowired
	private SiemensMySkillsRepository siemensMySkillsRepository;

	@Autowired
	private GRIPCatalogueRepositroy gripCatalogueRepositroy;

	@Autowired
	private FileDetailsRepository fileDetailsRepository;

	@Autowired
	private WorkStreamRepository workStreamRepository;

	@Autowired
	private FileStore fileStore;

	@Autowired
	private MongoOperations mongoOperations;

	@Override
	@Async("uploadScopes")
	public void processExcelDataByType(String jobId, String server, String type) {
		Optional<FileDetails> fileDetails = fileDetailsRepository.findById(jobId);
		LOGGER.info("First upload check : {}", fileDetails.isPresent());
		if (!fileDetails.isPresent()) {
			fileDetails = retryReplicaData(jobId);
		}
		if (!fileDetails.isPresent()) {
			throw new ResourceNotFoundException("File process ID not Found");
		}

		FileDetails s3FileDetails = fileDetails.get();
		LOGGER.info("Processed file name : {} and Action : {}", s3FileDetails.getFileName(), s3FileDetails.getAction());
		String filelocation = System.getProperty("java.io.tmpdir");
		LOGGER.info("filelocation= {}", filelocation);
		try(InputStream fileStream =  getFileStream(server, s3FileDetails, filelocation)) {
			LOGGER.info("File downloading from s3");
			LOGGER.info("File downloaded from s3 success");
			String errorMsg = readAndSaveExcelData(fileStream, type);
			if (StringUtils.isNotBlank(errorMsg))
				throw new RestBadRequestException(errorMsg);

			s3FileDetails.setStatus(NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS);
			s3FileDetails.setErrors(NextworkConstants.NO_ERROR);
			s3FileDetails.setUpdatedOn(LocalDateTime.now());
			fileDetailsRepository.save(s3FileDetails);

		} catch (Exception e) {
			s3FileDetails.setStatus(NextworkConstants.FILE_UPLOAD_STATUS_FAILED);
			s3FileDetails.setErrors(e.getMessage());
			s3FileDetails.setUpdatedOn(LocalDateTime.now());
			fileDetailsRepository.save(s3FileDetails);
		}
	}

	private InputStream getFileStream(String server, FileDetails s3FileDetails, String filelocation)
			throws FileNotFoundException {
		InputStream fileStream;
		File localFile;
		if (!server.equalsIgnoreCase("localhost")) {
			fileStream = fileStore.download(s3FileDetails.getFilePath(), s3FileDetails.getFileName());
		} else {
			localFile = new File(filelocation + NextworkConstants.PATH_SLASH + s3FileDetails.getFileName());
			LOGGER.info("Full File path {}", localFile.getAbsolutePath());
			fileStream = new FileInputStream(localFile);
		}
		return fileStream;
	}

	public Optional<FileDetails> retryReplicaData(String jobId) {
		Optional<FileDetails> fileDetails;
		threadWait();
		fileDetails = fileDetailsRepository.findById(jobId);
		LOGGER.info("Second upload check : {}", fileDetails.isPresent());
		if (!fileDetails.isPresent()) {
			threadWait();
			fileDetails = fileDetailsRepository.findById(jobId);
			LOGGER.info("Third upload check : {}", fileDetails.isPresent());
			if (!fileDetails.isPresent()) {
				threadWait();
				fileDetails = fileDetailsRepository.findById(jobId);
			}
		}
		return fileDetails;
	}

	private void threadWait() {
		try {
			Thread.sleep(5000l);
		} catch (InterruptedException e) {
			LOGGER.error("Interrupted ", e);
			Thread.currentThread().interrupt();
		}
	}

	public String readAndSaveExcelData(InputStream in, String type) {
		LOGGER.info("Inside readExcelData method....");
		var errorMsg = new StringBuilder();
		Map<Integer, Map<String, Object>> dataRows = processHeaderAndGetSkillData(in, type);
		validateSkill(type, errorMsg, dataRows);

		processSkillDataAndUpdateWSData(type, dataRows);

		processGripDataRecs(type, dataRows);
		LOGGER.info("***********#########*****************");

		return errorMsg.toString();

	}

	private void processSkillDataAndUpdateWSData(String type, Map<Integer, Map<String, Object>> dataRows) {
		List<String> updatedIdsList = getLatestUpdatedSkillIdsList(type, dataRows);
		LOGGER.info("Modified skills ids :{}", updatedIdsList);
		List<String> rmSkills = getRemovedSkillIdsList(dataRows);
		LOGGER.info("Removing skills : {}", rmSkills);
		if(Objects.nonNull(rmSkills) && !rmSkills.isEmpty())
			siemensMySkillsRepository.deleteAllById(rmSkills);
		saveSkillDataRecs(type, dataRows, updatedIdsList);
		updateWorkStreamSkillByUpdatedSkills(updatedIdsList, rmSkills);
		updateSiemensSkillsWithModified();
	}

	private void updateWorkStreamSkillByUpdatedSkills(List<String> updatedIdsList, List<String> remList) {
		List<String> allList = new ArrayList<>();
		allList.addAll(updatedIdsList);
		allList.addAll(remList);
		LOGGER.info("All workstream data change id's : {}", allList);
		List<IdDTO> wsIdsList = workStreamRepository.findAllWorkStreamIdsByUpdatedSkillIds(allList);
		LOGGER.info("All WorkStream ids list size : {}", wsIdsList);
		List<SiemensMySkills> modSkills = siemensMySkillsRepository.findAllByIsModified(Boolean.TRUE);
		LOGGER.info("Modified skills data : {}", modSkills);
		Map<String, SiemensMySkills> idSkillsMap = modSkills.stream().collect(Collectors.toMap(s -> s.getId(), s -> s));

		List<String> wsIds = wsIdsList.stream().map(IdDTO::get_id).toList();
		LOGGER.info("WorkStream Ids list : {}", wsIds);
		List<String> ids = new ArrayList<>();
		for(String wId : wsIds){
			ids.add(wId);
			if(ids.size() >= 10){
				updateWorkStreamBySkill(ids, idSkillsMap, remList);
			}
		}
		LOGGER.info("Remaing data list : {}", ids);
		if(!ids.isEmpty()){
			updateWorkStreamBySkill(ids, idSkillsMap, remList);
		}
	}

	private void updateSiemensSkillsWithModified() {
		Query qry = new Query();
		qry.addCriteria(Criteria.where("isModified").is(Boolean.TRUE));
		Update ud = new Update().set("isModified", Boolean.FALSE);
		mongoOperations.updateMulti(qry, ud, SiemensMySkills.class);
	}

	private void updateWorkStreamBySkill(List<String> ids, Map<String, SiemensMySkills> idSkillsMap, List<String> remList) {
		List<Workstream> uWsList = new ArrayList<>();
		List<Workstream>  wsList = workStreamRepository.findAllWorkStreamsById(ids);
		LOGGER.info("ID Skill Map : {}", idSkillsMap);
		for(Workstream ws : wsList){
			List<Skills> wsSkils = ws.getSkills().stream().map(s -> updateLatestSkill(s, idSkillsMap, remList)).toList();
			ws.setSkills(wsSkils);
			uWsList.add(ws);
		}
		LOGGER.info("Saved list size : {}", uWsList.size());
		workStreamRepository.saveAll(uWsList);
	}

	private Skills updateLatestSkill(Skills s, Map<String, SiemensMySkills> idSkillsMap, List<String> remList) {
		LOGGER.info("Skill check : {}", idSkillsMap.containsKey(s.getLexId()));
		if(idSkillsMap.containsKey(s.getLexId())){
			SiemensMySkills uSkill = idSkillsMap.get(s.getLexId());
			s.setName(uSkill.getSkillName());
			s.setLexId(uSkill.getId());
			s.setDescription(uSkill.getDescription());
			SkillCategory sc = new SkillCategory();
			sc.setName(uSkill.getCategory());
			s.setSkillCategory(sc);
		}
		if(remList.contains(s.getLexId())){
			s.setIsDeleted(Boolean.TRUE);
		}
		return s;
	}

	private List<String> getRemovedSkillIdsList(Map<Integer, Map<String, Object>> dataRows) {
		List<IdDTO> existingIds = siemensMySkillsRepository.findAllIdsByIsDeleted(Boolean.FALSE);
		List<SiemensMySkills> alkills = generateNewSkillsByMap(dataRows);
		List<String> cSkills = alkills.stream().map(s -> s.getId()).toList();
		return existingIds.stream().map(IdDTO::get_id).filter(i -> !(cSkills.contains(i)))
				.toList();
	}

	private List<String> getLatestUpdatedSkillIdsList(String type, Map<Integer, Map<String, Object>> dataRows) {
		List<String> latestSkillIds = new ArrayList<>();
		if (type.equalsIgnoreCase(NextworkConstants.UPLOAD_TYPE_SKILL)) {
			List<SiemensMySkills> alSkills = siemensMySkillsRepository.findAll();
			Map<String, String> alSkilIdMap = alSkills.stream().collect(Collectors.toMap(SiemensMySkills::getId, AsyncUploadServiceImpl::getConcatinatedSkill));
			List<SiemensMySkills> newSkils = generateNewSkillsByMap(dataRows);
			Map<String, String> newSkilIdMap = newSkils.stream().collect(Collectors.toMap(SiemensMySkills::getId, AsyncUploadServiceImpl::getConcatinatedSkill));
			LOGGER.info("Old Skills Map size : {} ", alSkilIdMap.size());
			LOGGER.info("New Skills Map size : {} ", newSkilIdMap.size());

			latestSkillIds =  newSkilIdMap.entrySet().stream().filter(e -> alSkilIdMap.containsKey(e.getKey()))
					.filter(e -> !alSkilIdMap.get(e.getKey()).equalsIgnoreCase(e.getValue()) ).map(Entry::getKey).toList();

		}
		return latestSkillIds;
	}

	private List<SiemensMySkills>  generateNewSkillsByMap(Map<Integer, Map<String, Object>> dataRows) {
		List<SiemensMySkills> newSkils = new ArrayList<>();
		for (Entry<Integer, Map<String, Object>> rw : dataRows.entrySet()) {
			SiemensMySkills recOb = CommonUtils.toBean(rw.getValue(), SiemensMySkills.class);
			recOb.setIsDeleted(Boolean.TRUE);
			if (StringUtils.isNotBlank(recOb.getSkillGroup())
					&& recOb.getSkillGroup().equalsIgnoreCase(NextworkConstants.SKILL_GROUP_GLOBAL)) {
				newSkils.add(recOb);
			}
		}
		return newSkils;
	}

	private static String getConcatinatedSkill(SiemensMySkills e) {
		return String.join(e.getCategory() , WORDS_SEPARATOR , e.getSkillName() , WORDS_SEPARATOR , e.getSkillCatalog() , WORDS_SEPARATOR , e.getDescription() , WORDS_SEPARATOR , e.getSubCategory());
	}

	private List<GRIPCatalogue> processGripDataRecs(String type,  Map<Integer, Map<String, Object>>  dataRows) {
		var grpRecords = new ArrayList<GRIPCatalogue>();
		if (type.equalsIgnoreCase(NextworkConstants.UPLOAD_TYPE_GRIP)) {
			for (Entry<Integer, Map<String, Object>> rw : dataRows.entrySet()) {
				GRIPCatalogue recOb = CommonUtils.toBean(rw.getValue(), GRIPCatalogue.class);
				grpRecords.add(recOb);
	
				if ((grpRecords.size() % 1000) == 0) {
					gripCatalogueRepositroy.saveAll(grpRecords);
					grpRecords = new ArrayList<>();
				}
			}

			if (!grpRecords.isEmpty()) {
				gripCatalogueRepositroy.saveAll(grpRecords);
			}
		}
		return grpRecords;
	}

	private List<String> saveSkillDataRecs(String type, Map<Integer, Map<String, Object>> dataRows, List<String> updatedSkillIdsList) {
		var cSkills = new  ArrayList<String>();
		var seRecords = new  ArrayList<SiemensMySkills>();
		if (type.equalsIgnoreCase(NextworkConstants.UPLOAD_TYPE_SKILL)) {
			for (Entry<Integer, Map<String, Object>> rw : dataRows.entrySet()) {
				SiemensMySkills recOb = CommonUtils.toBean(rw.getValue(), SiemensMySkills.class);
				recOb.setIsDeleted(Boolean.FALSE);
				if(updatedSkillIdsList.contains(recOb.getId())){
					recOb.setIsModified(Boolean.TRUE);
					LOGGER.info("Row : {}, value : {} ", rw.getKey(), recOb);
				}
				cSkills.add(recOb.getId());
				if (StringUtils.isNotBlank(recOb.getSkillGroup())
						&& recOb.getSkillGroup().equalsIgnoreCase(NextworkConstants.SKILL_GROUP_GLOBAL)) {
					seRecords.add(recOb);
				}
				if ((seRecords.size() % 1000) == 0) {
					siemensMySkillsRepository.saveAll(seRecords);
					seRecords = new ArrayList<SiemensMySkills>();
				}
			}
			
			if (!seRecords.isEmpty()) {
				siemensMySkillsRepository.saveAll(seRecords);
			}
		}
		
		return cSkills;
	}

	private void validateSkill(String type, StringBuilder errorMsg, Map<Integer, Map<String, Object>> dataRows) {
		List<String> skillsList = new ArrayList<>();
		for(Entry<Integer, Map<String, Object>> rw : dataRows.entrySet()) {
			if (type.equalsIgnoreCase(NextworkConstants.UPLOAD_TYPE_SKILL)) {
				errorMsg.append(validateSkillData(skillsList, rw)); 
			}
		}
		var errorVal = String.valueOf(errorMsg);
		if (StringUtils.isNotBlank(errorVal))
			throw new RestBadRequestException(errorVal);
	}

	private StringBuilder validateSkillData( List<String> skillsList, Entry<Integer, Map<String, Object>> rw) {
		StringBuilder errorMsg = new StringBuilder();
		SiemensMySkills recOb = CommonUtils.toBean(rw.getValue(), SiemensMySkills.class);
		recOb.setIsDeleted(Boolean.TRUE);
		var skillCatList = CommonUtils.getMySkillCategory();
		String unSkill = recOb.getId() + "_" + recOb.getSkillName();
		if (skillsList.contains(unSkill)) {
			errorMsg.append(NextworkConstants.ROW + ( rw.getKey() + 1) + " : Skill Name is Duplicated."
					+ System.lineSeparator());
		} else {
			skillsList.add(unSkill);
		}
		if (StringUtils.isBlank(recOb.getSkillName())) {
			errorMsg.append(NextworkConstants.ROW + (rw.getKey() + 1) + " : Skill Name is Blank."
					+ System.lineSeparator());
		}
		if (StringUtils.isBlank(recOb.getId())) {
			errorMsg.append(NextworkConstants.ROW + (rw.getKey() + 1) + " : Skill Id is Blank."
					+ System.lineSeparator());
		}
		
		if (StringUtils.isBlank(recOb.getCategory())) {
			errorMsg.append(NextworkConstants.ROW + (rw.getKey() + 1) + " : Skill Category is Blank."
					+ System.lineSeparator());
		} else if (!skillCatList.contains(recOb.getCategory().trim())) {
			errorMsg.append(NextworkConstants.ROW + (rw.getKey() + 1) + " : Skill Category is invalid."
					+ System.lineSeparator());
		}
		return errorMsg;
	}

	private Map<Integer, Map<String, Object>> processHeaderAndGetSkillData(InputStream in, String type){
		try(OPCPackage pkg = OPCPackage.open(in)){
			
			Map<String, String> headersMap = getHeadersMap(type);
			var xwb = new XSSFWorkbook(pkg);
			XSSFSheet sheet = xwb.getSheetAt(0);
			Iterator<Row> rows = sheet.iterator();
			var headers = new ArrayList<String>();
			var uniqueHeaders = new ArrayList<String>();
			
			LOGGER.info("Reading headers.....");
			Boolean isHeaderRow = Boolean.FALSE;
			int lastCol = 0;
			Row row = null;
			while (rows.hasNext()) {
				row = rows.next();
				lastCol = row.getLastCellNum();
				LOGGER.info("isHeaderRow {} and First col Value : {} ", isHeaderRow, row.getCell(0));

				isHeaderRow = isHeaderAvailable(headersMap, isHeaderRow, lastCol, row);
				LOGGER.info("Row Num {} and first cell : {} ", isHeaderRow, row.getCell(0));
				if (Boolean.TRUE.equals(isHeaderRow))
					break;
			}
			if (rows.hasNext() && !Objects.isNull(row)) {
				getUniqueHeaders(headersMap, headers, uniqueHeaders, lastCol, row);
			}
			
			xwb.close();
			in.close();
			
			if (uniqueHeaders.size() < headersMap.size()) {
				var unClaimedHeaders = new ArrayList<String>();
				for (String hdr : headersMap.keySet()) {
					if (!uniqueHeaders.contains(hdr)) {
						unClaimedHeaders.add(hdr);
					}
				}
				throw new IllegalStateException("Incorrect file and Missing Headers are : " + unClaimedHeaders.toString());
			}
			LOGGER.info("**********************************");
			LOGGER.info("***********#########*****************");
			
			return getDataRows(headersMap, rows, headers, lastCol);
		}catch(IllegalStateException ie) {
			throw new IllegalStateException(ie.getMessage());
		}catch(Exception e) {
			LOGGER.error("Error in reading skill data : {}",e.getLocalizedMessage());
		}
		return new HashMap<>();
	}

	private Map<String, String> getHeadersMap(String type) {
		Map<String, String> headersMap = new HashMap<>();
		if (StringUtils.isNoneBlank(type)) {
			if (type.equalsIgnoreCase(NextworkConstants.UPLOAD_TYPE_SKILL)) {
				headersMap = getSkillExcelHeadersMap();
			}
			if (type.equalsIgnoreCase(NextworkConstants.UPLOAD_TYPE_GRIP)) {
				headersMap = getGRIPExcelHeadersMap();
			}
		}
		return headersMap;
	}

	private Map<Integer, Map<String, Object>> getDataRows(Map<String, String> headersMap, Iterator<Row> rows,
			ArrayList<String> headers, int lastCol) {
		Map<Integer, Map<String, Object>> dataRows = new HashMap<>();
		while (rows.hasNext()) {
			Row row = rows.next();
			Map<String, Object> rec = new HashMap<>();
			for (int i = 0; i < lastCol; i++) {
				String cell = Objects.isNull(row.getCell(i)) ? "" : String.valueOf(row.getCell(i));
				String key = headers.get(i);
				if (headersMap.containsKey(key)) {
					rec.put(headersMap.get(key), cell);
				}
			}
			dataRows.put(row.getRowNum(), rec);
		}
		return dataRows;
	}

	private void getUniqueHeaders(Map<String, String> headersMap, ArrayList<String> headers, ArrayList<String> uniqueHeaders, int lastCol, Row row) {
		LOGGER.info("Row Value : {}", row.getRowNum());
		for (int i = 0; i < lastCol; i++) {
			String cell = Objects.isNull(row.getCell(i)) ? "" : String.valueOf(row.getCell(i));
			headers.add(cell);
			LOGGER.info("Cell Value : {}", cell);
			if (headersMap.containsKey(cell) && !uniqueHeaders.contains(cell)) {
				uniqueHeaders.add(cell);
			}
		}
	}

	private Boolean isHeaderAvailable(Map<String, String> headersMap, Boolean isHeaderRow, int lastCol, Row row) {
		for (int i = 0; i < lastCol; i++) {
			String cell = Objects.isNull(row.getCell(i)) ? "" : String.valueOf(row.getCell(i));
			if (headersMap.containsKey(cell)) {
				isHeaderRow = Boolean.TRUE;
			}
		}
		return isHeaderRow;
	}

	private Map<String, String> getSkillExcelHeadersMap() {
		Map<String, String> proMap = new HashMap<>();
		proMap.put(SkillExcelConstants.SKILL_ID, SkillExcelConstantsMappingDTO.SKILL_ID);
		proMap.put(SkillExcelConstants.SKILL_NAME, SkillExcelConstantsMappingDTO.SKILL_NAME);
		proMap.put(SkillExcelConstants.SKILL_DESCRIPTION, SkillExcelConstantsMappingDTO.SKILL_DESCRIPTION);
		proMap.put(SkillExcelConstants.SKILL_CATEGORY, SkillExcelConstantsMappingDTO.SKILL_CATEGORY);
		proMap.put(SkillExcelConstants.SKILL_SUB_CATEGORY, SkillExcelConstantsMappingDTO.SKILL_SUB_CATEGORY);
		proMap.put(SkillExcelConstants.SKILL_CATALOG, SkillExcelConstantsMappingDTO.SKILL_CATALOG);
		proMap.put(SkillExcelConstants.SKILL_GROUP, SkillExcelConstantsMappingDTO.SKILL_GROUP);
		LOGGER.info("Skills header map size : {} ", proMap.size());
		return proMap;
	}

	private Map<String, String> getGRIPExcelHeadersMap() {
		Map<String, String> proMap = new HashMap<>();
		proMap.put(GRIPCatalogueExcelConstants.GRIP_JOB_FAMILY, GRIPCatalogueExcelConstantsMappingDTO.GRIP_JOB_FAMILY);
		proMap.put(GRIPCatalogueExcelConstants.GRIP_SUB_JOB_FAMILY,
				GRIPCatalogueExcelConstantsMappingDTO.GRIP_SUB_JOB_FAMILY);
		proMap.put(GRIPCatalogueExcelConstants.GRIP_POSITION_TYPE,
				GRIPCatalogueExcelConstantsMappingDTO.GRIP_POSITION_TYPE);
		proMap.put(GRIPCatalogueExcelConstants.GRIP_POSITION, GRIPCatalogueExcelConstantsMappingDTO.GRIP_POSITION);
		proMap.put(GRIPCatalogueExcelConstants.GRIP_POSIION_HEAD_LINE,
				GRIPCatalogueExcelConstantsMappingDTO.GRIP_POSIION_HEAD_LINE);
		proMap.put(GRIPCatalogueExcelConstants.GRIP_POSITION_DESCRIPTION,
				GRIPCatalogueExcelConstantsMappingDTO.GRIP_POSITION_DESCRIPTION);
		proMap.put(GRIPCatalogueExcelConstants.GRIP_CODE, GRIPCatalogueExcelConstantsMappingDTO.GRIP_CODE);
		proMap.put(GRIPCatalogueExcelConstants.GRIP_CHANGE, GRIPCatalogueExcelConstantsMappingDTO.GRIP_CHANGE);
		proMap.put(GRIPCatalogueExcelConstants.GRIP_TECH_ROLE, GRIPCatalogueExcelConstantsMappingDTO.GRIP_TECH_ROLE);
		proMap.put(GRIPCatalogueExcelConstants.GRIP_NO_OF_EMPLOYEES,
				GRIPCatalogueExcelConstantsMappingDTO.GRIP_NO_OF_EMPLOYEES);
		LOGGER.info("Grip header map size : {} ", proMap.size());
		return proMap;
	}

	@Override
	public AsyncJobStatusDTO getAsyncJobProfileStatus(String userEmail, String asyncJobId) {

		userService.checkAdminUserRole(userEmail);

		Optional<FileDetails> uploadJobId = fileDetailsRepository.findById(asyncJobId);
		if (!uploadJobId.isPresent()) {
			throw new ResourceNotFoundException("FILE DETAILS NOT FOUND FOR GIVEN ASYNC JOB ID");
		}
		FileDetails response = uploadJobId.get();
		AsyncDetailsDTO details = AsyncDetailsDTO.builder().keyName(response.getType())
				.errorMessage(response.getErrors()).build();
		return AsyncJobStatusDTO.builder().message(response.getStatus()).details(details).build();
	}

}
