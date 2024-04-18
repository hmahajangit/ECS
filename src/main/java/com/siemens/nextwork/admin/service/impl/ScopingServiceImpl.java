package com.siemens.nextwork.admin.service.impl;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.nextwork.admin.config.BucketName;
import com.siemens.nextwork.admin.dto.*;
import com.siemens.nextwork.admin.enums.PublishType;
import com.siemens.nextwork.admin.exception.ResourceNotFoundException;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.model.Scoping;
import com.siemens.nextwork.admin.model.ScopingVersions;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.repo.ScopingRepository;
import com.siemens.nextwork.admin.repo.ScopingVersionsRepository;
import com.siemens.nextwork.admin.service.FileService;
import com.siemens.nextwork.admin.service.FileStore;
import com.siemens.nextwork.admin.service.ScopingService;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.util.*;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.poi.hpsf.IllegalPropertySetDataException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.binary.XSSFBSharedStringsTable;
import org.apache.poi.xssf.binary.XSSFBSheetHandler;
import org.apache.poi.xssf.binary.XSSFBStylesTable;
import org.apache.poi.xssf.eventusermodel.XSSFBReader;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScopingServiceImpl implements ScopingService {

	public static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
	@Autowired
	private FileDetailsRepository fileDetailsRepository;

	@Autowired
	private FileStore fileStore;

	@Autowired
	private UserService userService;

	@Autowired
	private ScopingRepository scopingRepository;

	@Autowired
	MongoOperations mongoOperations;

	@Autowired
	MongoTemplate mongoTemplate;

	@Autowired
	private FileService fileService;

	@Autowired
	private ScopingVersionsRepository scopingVersionsRepository;

	@Autowired
	BucketName bucketName;

	private static final Logger LOGGER = LoggerFactory.getLogger(ScopingServiceImpl.class);
	public static final String LATEST = "latest";
	public static final String DATEFORMAT = "yyyyMMdd";

	@Override
	public UploadFileStatusResponseDTO getAsyncJobProfileStatus(String userEmail, String id) {

		userService.checkAdminUserRole(userEmail);

		UploadFileStatusResponseDTO response = new UploadFileStatusResponseDTO();
		Optional<FileDetails> uploadJobId = fileDetailsRepository.findById(id);

		if (!uploadJobId.isPresent()) {
			throw new ResourceNotFoundException("FILE DETAILS NOT FOUND FOR GIVEN ASYNC JOB ID");
		}
		FileDetails fileEntity = uploadJobId.get();

		response.setTaskid(id);
		response.setStatus(fileEntity.getStatus());
		response.setErrors(fileEntity.getErrors());
		return response;
	}

	@Override
	@Transactional
	public IdResponseDTO appendScopingData(List<ScopingDataRequestListDTO> scopingDataRequestList, String versionId,
										   String userEmail, String action, String server) {

		String userId = userService.findUserIdByEmail(userEmail);
		userService.checkAdminUserRole(userEmail);

		IdResponseDTO res = new IdResponseDTO();

		if (!isVersionExists(versionId)) {
			throw new IllegalStateException("Version is not available.");
		}

		if (invalidateVersionId(versionId)) {
			throw new RestBadRequestException("Version is invalid for append");
		}

		
		if(scopingDataRequestList.size() > 25) {
			throw new RestBadRequestException("Exceed limit. Maximum 25 scoping data can be added at a time.");
		}


		List<String> errorList = new ArrayList<>();

		Set<String> gids = new HashSet<>();
		List<Scoping> scopeList = new ArrayList<>();
		for (ScopingDataRequestListDTO scopeReq : scopingDataRequestList) {
			String i = "{'" + scopeReq.getIndex();

			if (!scopeReq.getAction().equalsIgnoreCase("APPEND")) {
				errorList.add(i + "': 'Invalid action'}");
			}

			if (Objects.isNull(scopeReq.getGid())) {
				errorList.add(i + "': 'GID cannot be Blank'}");
			} else if (scopeReq.getGid().length() != 8) {
				LOGGER.info("dis-2");
				errorList.add(i + "': 'GID must be 8 characters'}");
			}

			else if (gids.contains(scopeReq.getGid().trim())) {
				errorList.add(i + "': 'duplicate GID in request list'}");
			}

			if (Boolean.FALSE.equals(CommonUtils.validateGid(scopeReq.getGid()))) {
				errorList.add("Row-" + i + " : gid must be 8 characters and contain at least 1 alphabet.");
			}

			gids.add(scopeReq.getGid());

			String id = versionId + "_" + scopeReq.getGid();
			Query qry = new Query();
			qry.addCriteria(Criteria.where("_id").is(id));

			if (mongoOperations.findOne(qry, Scoping.class) != null) {
				errorList.add(i + "': 'GID already exist'}");
			}

			Scoping scope = processScopingRequestFieldData(scopeReq.getGid(), id, scopeReq.getData(), versionId);
			scopeList.add(scope);

		}
		if (!errorList.isEmpty()) {
			throw new RestBadRequestException(errorList.toString());
		}

		scopingRepository.saveAll(scopeList);

		String uid = fileService.saveToolData(versionId, action, userId);

		res.setUid(uid);

		return res;
	}

	private boolean isVersionExists(String version) {
		Sort sort = Sort.by(Sort.Direction.DESC, NextworkConstants.UPDATE_ON);
		List<FileDetails> creatsList = fileDetailsRepository.findAllByVersionAndStatus(version,
				NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS, sort);

		LOGGER.info("Version files list size: {}", creatsList.size());

		return !creatsList.isEmpty() && Optional.ofNullable(creatsList.get(0)).isPresent();
	}

	@Override
	@Async("uploadScopes")
	@Transactional
	public void appendToolData(String uid, String versionId, String server) {
		try {
			FileDetails fileDetails = null;
			Optional<FileDetails> opFile = fileDetailsRepository.findById(uid);
			if (!opFile.isPresent()) {
				opFile = retryReplicaData(uid);
			}
			if (!opFile.isPresent()) {
				throw new ResourceNotFoundException("File process ID not Found");
			}

			fileDetails = opFile.get();
			String fileName = addVersionDataToExcel(uid, versionId, server, fileDetails);
			LOGGER.info("Final file name : {}", fileName);
			fileDetails.setFileName(fileName);
			LOGGER.info("Final record count : {}", fileDetails.getRecCount());
			fileDetails.setStatus(NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS);
			fileDetails.setUpdatedOn(LocalDateTime.now());
			String filePath = bucketName.getBucketName() + NextworkConstants.PATH_SLASH + NextworkConstants.FILE_AWS_PROCESSED + NextworkConstants.PATH_SLASH + versionId;
			fileDetails.setFilePath(filePath);
			fileDetailsRepository.save(fileDetails);
		} catch (Exception e) {
			FileDetails fileDetails = null;
			Optional<FileDetails> opFile = fileDetailsRepository.findById(uid);
			if (!opFile.isEmpty())
				fileDetails = opFile.get();
			if(fileDetails != null) {
				fileDetails.setStatus(NextworkConstants.FILE_UPLOAD_STATUS_FAILED);
				fileDetails.setErrors(e.getMessage());
				fileDetails.setUpdatedOn(LocalDateTime.now());
				fileDetailsRepository.save(fileDetails);
			}
		}

	}


	@Override
	public boolean invalidateVersionId(String versionId) {

		Query qry = new Query();
		qry.addCriteria(Criteria.where("versionStatus").is(PublishType.PUBLISH.value));
		List<ScopingVersions> scopingVersionList = mongoOperations.find(qry, ScopingVersions.class);

		List<String> scopingVersionLatestFour = scopingVersionList.stream()
				.sorted(Comparator.comparing(ScopingVersions::getUploadTime).reversed()).map(ScopingVersions::getVid)
				.toList();

		return (!scopingVersionLatestFour.contains(versionId));
	}

	private Scoping processScopingRequestFieldData(String gid, String id, List<ScopingDTO> reqDatalist,
												   String versionId) {

		Date date = new Date(System.currentTimeMillis());
		Scoping scope = new Scoping();
		scope.setGid(gid.toUpperCase());
		scope.setVid(versionId);
		scope.setUploadTime(date);
		scope.setIsVisible(true);
		scope.setId(id);
		scope.setIsCaptured(false);
		for (ScopingDTO scopeReqData : reqDatalist) {

			String columnName = scopeReqData.getFieldName().trim();
			String data = scopeReqData.getFieldData();
			if(columnName.equalsIgnoreCase(ScopingConstantsDTO.GID)){
				scope.setGid(data.toUpperCase());
			}else {
				Boolean isFound = getScopingPart1(columnName, scope, data);
				if(Boolean.FALSE.equals(isFound)) getScopingPart2(columnName, scope, data);
			}

		}

		return scope;

	}

	@Override
	public IdResponseDTO updateGID(GIDRequestDTO gidRequestDTO, String version, String userEmail, String gid,
								   String action) {
		String userId = userService.findUserIdByEmail(userEmail);
		userService.checkAdminUserRole(userEmail);

		if (!isVersionExists(version)) {
			throw new IllegalStateException("Version is not available.");
		}

		String id = version + "_" + gid;
		Query q = new Query();
		Criteria regex = Criteria.where("_id").regex("^" + id.toLowerCase(), "i");
		q.addCriteria(regex);
		Scoping scopingData = mongoOperations.findOne(q, Scoping.class);
		IdResponseDTO res = new IdResponseDTO();
		if (scopingData != null) {

			if (gidRequestDTO.getAction().equalsIgnoreCase("Deactivate")) {
				scopingData.setIsVisible(Boolean.FALSE);

			}
			if (gidRequestDTO.getAction().equalsIgnoreCase("Activate")) {
				scopingData.setIsVisible(Boolean.TRUE);

			}
			if (gidRequestDTO.getAction().equalsIgnoreCase("Update")) {
				processScopingRequestFieldDataForEditGID(gidRequestDTO.getData(), scopingData, gid);
			}
			scopingData.setIsCaptured(Boolean.FALSE);
			scopingRepository.save(scopingData);

			String uid = fileService.saveToolData(version, action, userId);
			res.setUid(uid);
		} else {
			throw new RestBadRequestException("No data found for given version Id/ gid");
		}
		return res;
	}

	@Override
	public List<ScopingDTO> searchGID(String id, String userEmail, List<ScopingDTO> searchGIDResponseDTOList) {

		userService.checkAdminUserRole(userEmail);

		Query q = new Query();
		Criteria regex = Criteria.where("_id").regex("^" + id.toLowerCase(), "i");
		q.addCriteria(regex);
		Scoping scoping = mongoOperations.findOne(q, Scoping.class);

		if (scoping != null) {

			validateAndGetScopingDetailsPart1(searchGIDResponseDTOList, scoping);
			validateAndGetScopingDetailsPart2(searchGIDResponseDTOList, scoping);
			validateAndGetScopingDetailsPart3(searchGIDResponseDTOList, scoping);
		} else {
			throw new RestBadRequestException("No data available for given version Id, gid");
		}
		return searchGIDResponseDTOList;

	}

	private void validateAndGetScopingDetailsPart3(List<ScopingDTO> searchGIDResponseDTOList, Scoping scoping) {
		if (null != scoping.getLocationOffice()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.LOCATION_OFFICE);
			searchGIDResponseDTO.setFieldData(scoping.getLocationOffice());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getLocationOfficeCity()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.LOCATION_OFFICE_CITY);
			searchGIDResponseDTO.setFieldData(scoping.getLocationOfficeCity());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getOrgCodePA()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.ORG_CODE_PA);
			searchGIDResponseDTO.setFieldData(scoping.getOrgCodePA());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getPositionType()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.POSITION_TYPE);
			searchGIDResponseDTO.setFieldData(scoping.getPositionType());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getRegionalOrganization()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.REGIONAL_ORGANIZATION);
			searchGIDResponseDTO.setFieldData(scoping.getRegionalOrganization());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getRegionalOrganizationDesc()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.REGIONAL_ORGANIZATION_TEXT);
			searchGIDResponseDTO.setFieldData(scoping.getRegionalOrganizationDesc());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getSubJobFamily()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.SUB_JOB_FAMILY);
			searchGIDResponseDTO.setFieldData(scoping.getSubJobFamily());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getUnit()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.UNIT);
			searchGIDResponseDTO.setFieldData(scoping.getUnit());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getVid()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.VID);
			searchGIDResponseDTO.setFieldData(scoping.getVid());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
	}

	private void validateAndGetScopingDetailsPart2(List<ScopingDTO> searchGIDResponseDTOList, Scoping scoping) {
		if (null != scoping.getDepthStructureKey()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.DEPTH_STRUCTURE_KEY);
			searchGIDResponseDTO.setFieldData(scoping.getDepthStructureKey());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getDepthStructureKeyDesc()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.DEPTH_STRUCTURE_KEY_TEXT);
			searchGIDResponseDTO.setFieldData(scoping.getDepthStructureKeyDesc());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getDivisionExternal()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.DIVISION_EXTERNAL);
			searchGIDResponseDTO.setFieldData(scoping.getDivisionExternal());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getDivisionExternalDesc()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.DIVISION_EXTERNAL_TEXT);
			searchGIDResponseDTO.setFieldData(scoping.getDivisionExternalDesc());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getDivisionInternal()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.DIVISION_INTERNAL);
			searchGIDResponseDTO.setFieldData(scoping.getDivisionInternal());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);

		}
		if (null != scoping.getDivisionInternalDesc()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.DIVISION_INTERNAL_TEXT);
			searchGIDResponseDTO.setFieldData(scoping.getDivisionInternalDesc());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getEmailId()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.EMAILADDRESS);
			searchGIDResponseDTO.setFieldData(scoping.getEmailId());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);

		}
		if (null != scoping.getGid()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.GID);
			searchGIDResponseDTO.setFieldData(scoping.getGid());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getGripPosition()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.GRIP_POSITION);
			searchGIDResponseDTO.setFieldData(scoping.getGripPosition());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getGripPositionDesc()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.GRIP_POSITION_TEXT);
			searchGIDResponseDTO.setFieldData(scoping.getGripPositionDesc());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getId()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.ID);
			searchGIDResponseDTO.setFieldData(scoping.getId());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getIsVisible()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.ISVISIBLE);
			searchGIDResponseDTO.setFieldData(scoping.getIsVisible().toString());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getJobFamily()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.JOB_FAMILY);
			searchGIDResponseDTO.setFieldData(scoping.getJobFamily());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getJobFamilyCategory()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.JOB_FAMILY_CATEGORY);
			searchGIDResponseDTO.setFieldData(scoping.getJobFamilyCategory());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}

	}

	private void validateAndGetScopingDetailsPart1(List<ScopingDTO> searchGIDResponseDTOList, Scoping scoping) {
		if (null != scoping.getAre()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.ARE);
			searchGIDResponseDTO.setFieldData(scoping.getAre());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getAreDesc()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.ARE_TEXT);
			searchGIDResponseDTO.setFieldData(scoping.getAreDesc());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getBlueCollarWhiteCollar()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.BLUECOLLARE_WHITECOLLAR);
			searchGIDResponseDTO.setFieldData(scoping.getBlueCollarWhiteCollar());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getBusinessUnit()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.BUSINESS_UNIT);
			searchGIDResponseDTO.setFieldData(scoping.getBusinessUnit());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getOrganizationClass()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.ORGANIZATION_CLASS);
			searchGIDResponseDTO.setFieldData(scoping.getOrganizationClass());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getCompany()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.COMPANY);
			searchGIDResponseDTO.setFieldData(scoping.getCompany());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getCompanyDesc()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.COMPANY_TEXT);
			searchGIDResponseDTO.setFieldData(scoping.getCompanyDesc());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getContractStatus()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.CONTRACT_STATUS);
			searchGIDResponseDTO.setFieldData(scoping.getContractStatus());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getCountryRegionARE()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.COUNTRY_REGION_ARE);
			searchGIDResponseDTO.setFieldData(scoping.getCountryRegionARE());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getCountryRegionPlaceOfAction()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.COUNTRY_REGION_PLACE_OF_ACTION);
			searchGIDResponseDTO.setFieldData(scoping.getCountryRegionPlaceOfAction());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getCountryRegionStateOffice()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.COUNTRY_REGION_STATE_OFFICE);
			searchGIDResponseDTO.setFieldData(scoping.getCountryRegionStateOffice());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
		if (null != scoping.getCountryRegionSubEntity()) {
			ScopingDTO searchGIDResponseDTO = new ScopingDTO();
			searchGIDResponseDTO.setFieldName(ScopingConstantsDTO.COUNTRY_REGION_SUBENTITY);
			searchGIDResponseDTO.setFieldData(scoping.getCountryRegionSubEntity());
			searchGIDResponseDTOList.add(searchGIDResponseDTO);
		}
	}

	@Override
	public ScopingVersionResponseDTO getAllVersions(String userEmail) {
		ScopingVersionResponseDTO scopingVersionResponseDTO = new ScopingVersionResponseDTO();
		userService.validateUserRole(userEmail);
		Query qryLatest = new Query();
		qryLatest.addCriteria(Criteria.where(LATEST).is(Boolean.TRUE));
		ScopingVersions scopingVersion = mongoOperations.findOne(qryLatest, ScopingVersions.class);
		if (null != scopingVersion) {
			scopingVersionResponseDTO.setVid(scopingVersion.getVid());
			scopingVersionResponseDTO.setStatus("Latest");
			scopingVersionResponseDTO.setRecCount(scopingVersion.getRecCount());
		}
		Query qryOld = new Query();
		qryOld.addCriteria(Criteria.where(LATEST).is(Boolean.FALSE));

		List<ScopingVersions> scopingVersionList = mongoOperations.find(qryOld, ScopingVersions.class);

		if (!scopingVersionList.isEmpty()) {
			List<OlderVersionsDTO> olderVersionsList = new ArrayList<>();
			for (ScopingVersions sv : scopingVersionList) {
				OlderVersionsDTO olderVersion = new OlderVersionsDTO();
				olderVersion.setVid(sv.getVid());
				olderVersion.setStatus(sv.getVersionStatus());
				olderVersion.setRecCount(sv.getRecCount());
				olderVersionsList.add(olderVersion);
			}
			scopingVersionResponseDTO.setOlderVersions(olderVersionsList);
		}
		return scopingVersionResponseDTO;

	}

	@Override
	public void publishUnpublishScopingMasterData(String versionId, String userEmail, String publishUnpublishFlag) {
		userService.checkAdminUserRole(userEmail);
		Query qry = new Query();
		qry.addCriteria(Criteria.where("vid").is(versionId));
		ScopingVersions scopingVersion = mongoOperations.findOne(qry, ScopingVersions.class);
		if (null != scopingVersion) {
			if (publishUnpublishFlag.equals(PublishType.UNPUBLISH.value) && scopingVersion.getLatest().equals(true)) {
				unpublishingLatestVersion(scopingVersion);
			}

			if (publishUnpublishFlag.equals(PublishType.PUBLISH.value)) {
				publishingVersions(versionId, scopingVersion);

			}

			scopingVersion.setVersionStatus(publishUnpublishFlag);
			scopingVersionsRepository.save(scopingVersion);
		} else {
			throw new RestBadRequestException("No data is present for given version Id");
		}
	}

	private void publishingVersions(String versionId, ScopingVersions scopingVersion) {
		LOGGER.info("Updating Publish Version");

		Query qryLatest = new Query();
		qryLatest.addCriteria(Criteria.where(LATEST).is(true));
		ScopingVersions scopingVersionLatest = mongoOperations.findOne(qryLatest, ScopingVersions.class);
		if (null != scopingVersionLatest) {
			String scopingVersionLatestVersion = scopingVersionLatest.getVid();

			DateFormat df = new SimpleDateFormat(DATEFORMAT);
			try {
				Date prevLatestDate = df.parse(scopingVersionLatestVersion);

				Date currentScopingDate = df.parse(versionId);


				if (currentScopingDate.after(prevLatestDate)) {
					LOGGER.info("currentScopingDate is bigger");

					scopingVersionLatest.setLatest(false);
					scopingVersionsRepository.save(scopingVersionLatest);
					scopingVersion.setLatest(true);

				}
			} catch (ParseException e) {
				LOGGER.error(e.getLocalizedMessage());
			}
		} else {
			throw new RestBadRequestException("There must be one latest version present");
		}
	}

	private void unpublishingLatestVersion(ScopingVersions scopingVersion) {
		LOGGER.info("Updating Latest Version");
		Query qrySv = new Query();
		qrySv.addCriteria(Criteria.where("versionStatus").is(PublishType.PUBLISH.value));
		qrySv.addCriteria(Criteria.where(LATEST).is(false));

		List<ScopingVersions> scopingVersionList = mongoOperations.find(qrySv, ScopingVersions.class);
		List<String> scopingVersionListVersions = scopingVersionList.stream().map(ScopingVersions::getVid)
				.toList();
		if (!scopingVersionListVersions.isEmpty()) {
			List<Date> versionDates = new ArrayList<>();
			for (String scopeVersion : scopingVersionListVersions) {

				DateFormat df = new SimpleDateFormat(DATEFORMAT);
				Date scopeVersionDate;
				try {

					scopeVersionDate = df.parse(scopeVersion);
					versionDates.add(scopeVersionDate);
				} catch (ParseException e) {
					LOGGER.error(e.getLocalizedMessage());
				}
			}
			LOGGER.info("versionDates : {}", versionDates);
			Collections.sort(versionDates);
			LOGGER.info("Sorted versionDates : {}", versionDates);
			Date lastDate = versionDates.get(versionDates.size() - 1);
			LOGGER.info("lastDate : {}", lastDate);

			DateFormat df = new SimpleDateFormat(DATEFORMAT);
			String version = df.format(lastDate);

			LOGGER.info("Final version :{} ", version);
			Query qryNew = new Query();
			qryNew.addCriteria(Criteria.where("vid").is(version));
			ScopingVersions scopingVersionLatest = mongoOperations.findOne(qryNew, ScopingVersions.class);
			if (null != scopingVersionLatest) {
				scopingVersionLatest.setLatest(true);
				scopingVersionsRepository.save(scopingVersionLatest);
			}
			scopingVersion.setLatest(false);
		} else {
			throw new RestBadRequestException(
					"This is the last published version. Therefore it cannot be unpublished.");
		}
	}


	private Scoping processScopingRequestFieldDataForEditGID(List<ScopingDTO> reqDatalist, Scoping scope, String gid) {

		Date date = new Date(System.currentTimeMillis());

		scope.setModifyTime(date);
		for (ScopingDTO scopeReqData : reqDatalist) {

			String columnName = scopeReqData.getFieldName().trim();
			String data = scopeReqData.getFieldData();
			if(columnName.equalsIgnoreCase(ScopingConstantsDTO.GID)){
				if (!data.equalsIgnoreCase(gid)) {
					throw new RestBadRequestException("GID in body and path variable doesn't match");
				} else {
					scope.setGid(data);
				}
			}else {
				Boolean isFound = getScopingPart1(columnName, scope, data);
				if(Boolean.FALSE.equals(isFound)) getScopingPart2(columnName, scope, data);
			}


		}

		return scope;

	}

	private Boolean getScopingPart1(String columnName, Scoping scope, String data) {
		Boolean isFound = Boolean.TRUE;
		switch(columnName) {
			case ScopingConstantsDTO.EMAILADDRESS:
				scope.setEmailId(data);
				break;
			case ScopingConstantsDTO.CONTRACT_STATUS:
				scope.setContractStatus(data);
				break;
			case ScopingConstantsDTO.ARE:
				scope.setAre(data);
				break;
			case ScopingConstantsDTO.ARE_TEXT:
				scope.setAreDesc(data);
				break;
			case ScopingConstantsDTO.COMPANY:
				scope.setCompany(data);
				break;
			case ScopingConstantsDTO.COMPANY_TEXT:
				scope.setCompanyDesc(data);
				break;
			case ScopingConstantsDTO.DIVISION_EXTERNAL:
				scope.setDivisionExternal(data);
				break;
			case ScopingConstantsDTO.DIVISION_EXTERNAL_TEXT:
				scope.setDivisionExternalDesc(data);
				break;
			case ScopingConstantsDTO.DIVISION_INTERNAL:
				scope.setDivisionInternal(data);
				break;
			case ScopingConstantsDTO.DIVISION_INTERNAL_TEXT:
				scope.setDivisionInternalDesc(data);
				break;
			case ScopingConstantsDTO.BUSINESS_UNIT:
				scope.setBusinessUnit(data);
				break;
			case ScopingConstantsDTO.DEPTH_STRUCTURE_KEY:
				scope.setDepthStructureKey(data);
				break;
			case ScopingConstantsDTO.DEPTH_STRUCTURE_KEY_TEXT:
				scope.setDepthStructureKeyDesc(data);
				break;
			case ScopingConstantsDTO.ORG_CODE_PA:
				scope.setOrgCodePA(data);
				break;
			default : isFound = Boolean.FALSE;
		}
		return isFound;
	}

	private void getScopingPart2(String columnName, Scoping scope, String data) {
		switch(columnName) {
			case ScopingConstantsDTO.ORGANIZATION_CLASS:
				scope.setOrganizationClass(data);
				break;
			case ScopingConstantsDTO.UNIT:
				scope.setUnit(data);
				break;
			case ScopingConstantsDTO.JOB_FAMILY_CATEGORY:
				scope.setJobFamilyCategory(data);
				break;
			case ScopingConstantsDTO.JOB_FAMILY:
				scope.setJobFamily(data);
				break;
			case ScopingConstantsDTO.SUB_JOB_FAMILY:
				scope.setSubJobFamily(data);
				break;
			case ScopingConstantsDTO.POSITION_TYPE:
				scope.setPositionType(data);
				break;
			case ScopingConstantsDTO.GRIP_POSITION:
				scope.setGripPosition(data);
				break;
			case ScopingConstantsDTO.GRIP_POSITION_TEXT:
				scope.setGripPositionDesc(data);
				break;
			case ScopingConstantsDTO.REGIONAL_ORGANIZATION:
				scope.setRegionalOrganization(data);
				break;
			case ScopingConstantsDTO.REGIONAL_ORGANIZATION_TEXT:
				scope.setRegionalOrganizationDesc(data);
				break;
			case ScopingConstantsDTO.COUNTRY_REGION_ARE:
				scope.setCountryRegionARE(data);
				break;
			case ScopingConstantsDTO.COUNTRY_REGION_PLACE_OF_ACTION:
				scope.setCountryRegionPlaceOfAction(data);
				break;
			case ScopingConstantsDTO.COUNTRY_REGION_STATE_OFFICE:
				scope.setCountryRegionStateOffice(data);
				break;
			case ScopingConstantsDTO.LOCATION_OFFICE_CITY:
				scope.setLocationOfficeCity(data);
				break;
			case ScopingConstantsDTO.LOCATION_OFFICE:
				scope.setLocationOffice(data);
				break;
			case ScopingConstantsDTO.COUNTRY_REGION_SUBENTITY:
				scope.setCountryRegionSubEntity(data);
				break;
			case ScopingConstantsDTO.BLUECOLLARE_WHITECOLLAR:
				scope.setBlueCollarWhiteCollar(data);
				break;

			default:
				throw new RestBadRequestException("Bad data received, columns not found: " + columnName);
		}
	}

	public List<Scoping> retryReplicaAppendData(String versionId) {
		List<Scoping> appendedData;
		threadWait();
		appendedData = scopingRepository.findAllByVidAndIsCaptured(versionId, Boolean.FALSE);
		
		if (appendedData.isEmpty()) {
			threadWait();
			appendedData = scopingRepository.findAllByVidAndIsCaptured(versionId, Boolean.FALSE);
			
			if (appendedData.isEmpty()) {
				threadWait();
				appendedData = scopingRepository.findAllByVidAndIsCaptured(versionId, Boolean.FALSE);
			}
		}
		return appendedData;
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

	@Override
	@Async("uploadScopes")
	@Transactional
	public void processMasterScopingDataWithVersion(String jobId, String userEmail, String server, String versionId,
													String action) {
		userService.checkAdminUserRole(userEmail);

		Optional<FileDetails> fileDetails = fileDetailsRepository.findById(jobId);
		if (!fileDetails.isPresent()) {
			fileDetails = retryReplicaData(jobId);
		}
		if (!fileDetails.isPresent()) {
			throw new ResourceNotFoundException("File process ID not Found");
		}
		FileDetails s3FileDetails = fileDetails.get();
		LOGGER.info("MSD FILE: Processed file name : {} and Action : {}", s3FileDetails.getFileName(),
				s3FileDetails.getAction());

		String filelocation = System.getProperty(NextworkConstants.JAVA_IO_TEMPDIR);
		LOGGER.info("Scoping data filelocation= {}", filelocation);
		String tempId = null;
		try {
			LOGGER.info("MSD FILE:File downloading from s3");
			File localFile = null;
			tempId = validateSheetWithTempCollection(server, s3FileDetails, filelocation);
			localFile = processScopingAndPersistActual(server, action, s3FileDetails, filelocation, localFile);
			LOGGER.info("Deleting temp file...");
			if (server.equalsIgnoreCase(NextworkConstants.LOCAL_HOST) && localFile != null) {
				Files.deleteIfExists(Paths.get(localFile.getPath()));
			} else {
				String extension = s3FileDetails.getFileName().substring(s3FileDetails.getFileName().lastIndexOf("."));
				String newFileName = String.valueOf(
						versionId + "_" + NextworkConstants.PROCESSED_FILE_VERSION_DOWNLOADED + "" + extension);
				fileService.copyProcessedFile(bucketName.getBucketName(), s3FileDetails,
						NextworkConstants.FILE_AWS_INPUT, NextworkConstants.FILE_AWS_PROCESSED, newFileName);
				LOGGER.info("Processed File Name : {}", newFileName);
				s3FileDetails.setFileName(newFileName);
			}

			String path = String.format("%s/%s/%s", bucketName.getBucketName(), NextworkConstants.FILE_AWS_PROCESSED,
					s3FileDetails.getVersion());
			s3FileDetails.setFilePath(path);
			s3FileDetails.setStatus(NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS);
			s3FileDetails.setErrors(NextworkConstants.NO_ERROR);
			if (action.equals(NextworkConstants.FILE_UPLOAD_PUT_APPEND)) {

				String fileName = addVersionDataToExcel(jobId, versionId, server, s3FileDetails);
				LOGGER.info("Final file name : {}", fileName);
				s3FileDetails.setFileName(fileName);
			}
			LOGGER.info("Final record count : {}", s3FileDetails.getRecCount());
			s3FileDetails.setUpdatedOn(LocalDateTime.now());
			fileDetailsRepository.save(s3FileDetails);

			if (action.equals(NextworkConstants.FILE_UPLOAD_POST_CREATE)) {
				processScopingVersion(versionId, s3FileDetails);
			}

		} catch (Exception e) {
			LOGGER.info("Exception in processing...{}",e.getLocalizedMessage());
			s3FileDetails.setStatus(NextworkConstants.FILE_UPLOAD_STATUS_FAILED);
			s3FileDetails.setReportStatus(NextworkConstants.FILE_UPLOAD_STATUS_FAILED);
			s3FileDetails.setErrors(e.getMessage());
			s3FileDetails.setUpdatedOn(LocalDateTime.now());
			fileDetailsRepository.save(s3FileDetails);
			String collectionName = "scoping_" + tempId;
			mongoOperations.dropCollection(collectionName);
		}
	}

	private File processScopingAndPersistActual(String server, String action, FileDetails s3FileDetails,
												String filelocation, File localFile)
			throws IOException, SAXException, OpenXML4JException {
		InputStream fileStream;
		if (!server.equalsIgnoreCase(NextworkConstants.LOCAL_HOST)) {
			fileStream = fileStore.download(s3FileDetails.getFilePath(), s3FileDetails.getFileName());
		} else {
			localFile = new File(filelocation + NextworkConstants.PATH_SLASH  + s3FileDetails.getFileName());
			fileStream = new FileInputStream(localFile);
		}
		saveExcelPOIToMongo(fileStream, s3FileDetails.getVersion(), "master_scoping_data", Boolean.FALSE, action);
		fileStream.close();
		return localFile;
	}

	private String validateSheetWithTempCollection(String server, FileDetails s3FileDetails, String filelocation)
			throws  IOException, OpenXML4JException, SAXException {
		InputStream fileStream = null;
		File localFile = null;
		String tempId = null;
		if (!server.equalsIgnoreCase(NextworkConstants.LOCAL_HOST)) {
			fileStream = fileStore.download(s3FileDetails.getFilePath(), s3FileDetails.getFileName());
		} else {
			localFile = new File(filelocation + NextworkConstants.PATH_SLASH  + s3FileDetails.getFileName());
			LOGGER.info("Full File path  {}", localFile.getAbsolutePath());
			fileStream = new FileInputStream(localFile);
		}
		LOGGER.info("MSD FILE:File downloaded from s3 success");
		tempId = UUID.randomUUID().toString();
		processScopingData(fileStream, s3FileDetails.getVersion(), tempId, s3FileDetails.getAction());
		fileStream.close();
		return tempId;
	}

	private void processScopingVersion(String versionId, FileDetails s3FileDetails) {
		Date now = new Date(System.currentTimeMillis());
		try {
			addScopingVersion(versionId, now);
			s3FileDetails.setReportStatus(NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS);
			s3FileDetails.setReportUpdatedOn(LocalDateTime.now());
			fileDetailsRepository.save(s3FileDetails);
		} catch (Exception e) {
			s3FileDetails.setReportStatus(NextworkConstants.FILE_UPLOAD_STATUS_FAILED);
			s3FileDetails.setErrors(e.getMessage());
			s3FileDetails.setReportUpdatedOn(LocalDateTime.now());
			fileDetailsRepository.save(s3FileDetails);
		}
	}

	public String processScopingData(InputStream inputStream, String version, String tempId, String action)
			throws IOException, OpenXML4JException, SAXException {
		CommonUtils.printMemoryDetails();
		String collectionName = "scoping_" + tempId;
		Integer records = saveExcelPOIToMongo(inputStream, version, collectionName, Boolean.TRUE, action);

		LOGGER.info("********************************* {}",records);

		List<String> duplicateList = retryGetDuplicateGIDS(collectionName);
		LOGGER.info("duplicate records size : {}", duplicateList);
		if (!duplicateList.isEmpty()) {
			LOGGER.info("Deleting the temp collection...");
			mongoOperations.dropCollection(collectionName);
			throw new IllegalPropertySetDataException("The file contains duplicate gid's");
		}

		if (action.equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_POST_CREATE)) {
			scopingRepository.deleteByVid(version);
			Sort sort = Sort.by(Sort.Direction.DESC,NextworkConstants.UPDATE_ON);
			List<FileDetails> files = fileDetailsRepository.findAllByVersion(version, sort);
			LOGGER.info("APPend files size : {}", files.size());
			var mFiles = new ArrayList<FileDetails>();
			for (FileDetails fl : files) {
				if (fl.getAction().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_POST_CREATE)) {
					break;
				}
				fl.setFileName("");
				fl.setRecCount(0);
				mFiles.add(fl);
			}
			if (!mFiles.isEmpty()) {
				fileDetailsRepository.saveAll(mFiles);
			}

		}
		LOGGER.info("Saved all data into actual");
		mongoOperations.dropCollection(collectionName);
		LOGGER.info("Deleting the temp collection...");
		CommonUtils.printMemoryDetails();
		return collectionName;
	}


	private ObjectMapper getObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}

	public Integer saveExcelPOIToMongo(InputStream fileStream, String version, String collectionName, Boolean isTemp,
									   String action) throws IOException, SAXException, OpenXML4JException{

		LOGGER.info("Calling saveExcelPOIToMongo for : {}", collectionName);
		LOGGER.info("InputStream check : {}", (fileStream != null));
		LOGGER.info("InputStream check : {}", fileStream.available());
		StringBuilder errorMsg = new StringBuilder();
		TestSheetHandler testSheetHandler = getTestSheetHandler(fileStream);

		LOGGER.info("All Rows : {}", testSheetHandler.getRows().size() );
		if(action.equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_PUT_APPEND) && testSheetHandler.getRows().size() > 5000) {
			throw new RestBadRequestException("Invalid file. File contains more than 5000 records");
		}
		if (Boolean.TRUE.equals(isTemp)) createCollection(collectionName);
		Map<String, String> headersMap = validateAndGetHeadersMap(errorMsg, testSheetHandler);

		int cnt = 1;
		if (Boolean.TRUE.equals(isTemp)) {
			cnt = getRowsByProcessTempCollection(version, collectionName, action, errorMsg, testSheetHandler,  headersMap);
		}else {
			cnt = getRowsByProcessedCollection(version, collectionName, action, testSheetHandler, headersMap);
		}

		if (cnt == 2) {
			errorMsg.append("EMPTY DATA FILE.");
		}

		CommonUtils.printMemoryDetails();
		if (( errorMsg.length() > 0)) {
			List<String> duplicateList = retryGetDuplicateGIDS(collectionName);
			LOGGER.info("duplicate records size : {}", duplicateList.size());
			if (!duplicateList.isEmpty()) {
				errorMsg = new StringBuilder("The file contains duplicate gid's. "+ errorMsg.toString() ) ;
			}
			LOGGER.info("GID validation is failed and deleting the collection : {}", errorMsg);
			mongoOperations.dropCollection(collectionName);
			throw new IllegalPropertySetDataException(errorMsg.toString());
		}

		CommonUtils.printMemoryDetails();
		return cnt;
	}

	private int getRowsByProcessedCollection(String version, String collectionName, String action,
			 TestSheetHandler testSheetHandler, Map<String, String> headersMap)
			throws JsonProcessingException {

		int cnt = 1;
		List<String> elminates = new ArrayList<>();
		elminates.add("vGid");
		elminates.add("modifyTime");
		elminates.add("nullFields");
		List<Scoping> dtoActualSet = new ArrayList<>();
		Map<String, String> parsedMap = getTemplateExcelHeadersMap();
		List<String> dupGVIDs = new ArrayList<>();
		StringBuilder mappingError=new StringBuilder();
		
		for (String row : testSheetHandler.getRows()) {
			
			if (row.equalsIgnoreCase("BLANK") || (cnt == 1)) {
				cnt = cnt + 1;
				continue;
			}
			
			Map<String, Object> dataMap = getDataMap(parsedMap, headersMap, cnt, row, mappingError);

			if (mappingError.length() == 0) {
				Scoping dto = CommonUtils.toBean(dataMap, Scoping.class);
				if (dto.getContractStatus().equalsIgnoreCase(NextworkConstants.CONTRACT_STATUS_ACTIVE)
						&& !StringUtils.isBlank(dto.getGid())) {
					dtoActualSet.add(getScopedWithData(version, action, elminates, dto));
					dupGVIDs.add(dto.getVid() + "_" + dto.getGid());
				}
				if (dtoActualSet.size() > NextworkConstants.BATCH_THRESHOLD) {
					processScopingNewAndExisingList(collectionName, dupGVIDs, dtoActualSet);
					dtoActualSet = new ArrayList<>();
					dupGVIDs = new ArrayList<>();
				}
			}
			cnt++;
		}

		if(mappingError.length()>0) {
			throw new RestBadRequestException("Invalid or unrecognized character found at row : " + mappingError.toString());
		}
		if (!dtoActualSet.isEmpty()) {
			processScopingNewAndExisingList(collectionName, dupGVIDs, dtoActualSet);
		}
		return cnt;
	}

	private void processScopingNewAndExisingList(String collectionName, List<String> dupGVIDs, List<Scoping> dtoActualSet) {
		List<IdDTO> existingIds = scopingRepository.findAllVGIDS(dupGVIDs);
		List<String> existingIdList = (existingIds != null) ? existingIds.stream().map(IdDTO::get_id).toList() : new ArrayList<>();
		LOGGER.info("Exising Ids : {}", existingIdList);
		if(existingIds == null || existingIds.isEmpty()){
			mongoOperations.insert(dtoActualSet, collectionName);
		}else{
			List<Scoping> unList = dtoActualSet.stream().filter(s -> !existingIdList.contains(s.getId())).toList();
			List<Scoping> exList = dtoActualSet.stream().filter(s -> existingIdList.contains(s.getId())).toList();
			mongoOperations.insert(unList, collectionName);
			LOGGER.info("");
			scopingRepository.saveAll(exList);
		}
	}

	private Scoping getScopedWithData(String version, String action, List<String> elminates, Scoping dto) {
		Date date = new Date(System.currentTimeMillis());
		dto.setVid(version);
		dto.setIsVisible(true);
		dto.setUploadTime(date);
		dto.setModifyTime(null);
		dto.setId(version + "_" + dto.getGid());
		dto.setActionType(action);
		dto.setGid(dto.getGid().toUpperCase());
		dto.setIsCaptured(Boolean.FALSE);
		dto.setNullFields(CommonUtils.getNullFileds(dto, elminates));
		if (action.equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_POST_CREATE)) {
			dto.setIsCaptured(Boolean.TRUE);
		}
		return dto;
	}

	private int getRowsByProcessTempCollection(String version, String collectionName, String action, StringBuilder errorMsg,
			TestSheetHandler testSheetHandler, Map<String, String> headersMap ) throws JsonProcessingException {

		Map<String, String> parsedMap = getTemplateExcelHeadersMap();
		List<NextWorkExcelMasterScopes> dtoSet = new ArrayList<>();
		int cnt = 1;
		StringBuilder mappingError=new StringBuilder();
		
		for (String row : testSheetHandler.getRows()) {
			if (!row.equalsIgnoreCase("BLANK")) {
				if (cnt == 1) {
					cnt = cnt + 1;
					continue;
				}
				Map<String, Object> dataMap = getDataMap(parsedMap, headersMap, cnt, row, mappingError);
				
				if (mappingError.length() == 0) {
					dtoSet = insertRowDataIntoCollection(version, collectionName, action, errorMsg, dtoSet, cnt,
							dataMap);
				}	
			}
			cnt++;
		}
		
		if(mappingError.length()>0) {
			throw new RestBadRequestException("Invalid or unrecognized character found at row : " + mappingError.toString());
		}
		if (!dtoSet.isEmpty()) {
			mongoOperations.insert(dtoSet, collectionName);
		}
		return cnt;
	}

	private List<NextWorkExcelMasterScopes> insertRowDataIntoCollection(String version, String collectionName,
			String action, StringBuilder errorMsg, List<NextWorkExcelMasterScopes> dtoSet, int cnt,
			Map<String, Object> dataMap) {
		NextWorkExcelMasterScopes dto = CommonUtils.toBean(dataMap, NextWorkExcelMasterScopes.class);
		if (dto.getContractStatus().equalsIgnoreCase(NextworkConstants.CONTRACT_STATUS_ACTIVE)
				&& !StringUtils.isBlank(dto.getGid())) {
			errorMsg.append(validateScopeFields(dto, version, cnt, action));
			dtoSet.add(dto);
			if (dtoSet.size() > NextworkConstants.BATCH_THRESHOLD) {
				mongoOperations.insert(dtoSet, collectionName);
				dtoSet = new ArrayList<>();
			}
		}
		return dtoSet;
	}
	@SuppressWarnings("deprecation")
	private Map<String, Object> getDataMap(Map<String, String> parsedMap, Map<String, String> headersMap, int cnt,
			String row, StringBuilder mappingError) throws JsonProcessingException {
		Map<String, Object> dataMap = new HashMap<>();
		try {
			Map<String, Object> result = new ObjectMapper().configure(Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,true)
					.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
					.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
					.readValue(row, HashMap.class);
			for (String ky : headersMap.keySet()) {
				String aky = ky;
				aky = aky.replaceAll(NextworkConstants.PATTERN_MATCHES, "");
				String val = String.valueOf(result.getOrDefault(ky + cnt, ""));
				if (val.equalsIgnoreCase("æææ"))
					val = "";
				String key = parsedMap.get(headersMap.get(aky));
				dataMap.put(key, val);
			}

		} catch (JsonProcessingException e) {
			if(mappingError.length()> 0) {
				mappingError.append(",");
			}
			mappingError.append(cnt);
		}
		return dataMap;
	}

	@SuppressWarnings("deprecation")
	private Map<String, String> validateAndGetHeadersMap(StringBuilder errorMsg, TestSheetHandler testSheetHandler) throws JsonProcessingException {
		Map<String, String> headersMap = new HashMap<>();
		Map<String, String> unKnwnHeadersMap = new HashMap<>();
		Map<String, String> parsedMap = getTemplateExcelHeadersMap();
		if (testSheetHandler.getRows() != null && !testSheetHandler.getRows().isEmpty()) {

			try {
				Map<String, Object> result = new ObjectMapper()
						.configure(Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
						.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
						.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
						.readValue(testSheetHandler.getRows().get(0), HashMap.class);
				for (String ky : result.keySet()) {
					ky = ky.replaceAll(NextworkConstants.PATTERN_MATCHES, "");
					String hdr = String.valueOf(result.get(ky + "1"));
					LOGGER.info("Header Key Pos : {} and Value: {} ", ky, hdr);
					if (parsedMap.containsKey(hdr) && !headersMap.containsValue(hdr)) {
						headersMap.put(ky, hdr);

					} else {
						unKnwnHeadersMap.put(ky, hdr);
					}
				}
			} catch (JsonProcessingException e) {
				throw new RestBadRequestException("Invalid or unrecognized character found in the header");
			}
		}
		validateHeadersAndDuplicates(errorMsg, headersMap, parsedMap);
		return headersMap;
	}

	private static void validateHeadersAndDuplicates(StringBuilder errorMsg, Map<String, String> headersMap, Map<String, String> parsedMap) {
		if (headersMap.size() != 32) {
			List<String> headers = new ArrayList<>(headersMap.values());
			List<String> unClaimedHeaders = parsedMap.keySet().stream().filter(key ->
					!headers.contains(key)
			).toList();
			if (!unClaimedHeaders.isEmpty()) {
				errorMsg.append("Incorrect file and Missing Headers are : " + unClaimedHeaders.toString());
			} else {
				Map<String, Long> duplicates = headers.stream()
						.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
				List<String> dupHeaders = duplicates.entrySet().stream().filter(h -> (h.getValue() > 1)).map(Entry::getKey).toList();
				if (!dupHeaders.isEmpty()) {
					errorMsg.append("Incorrect file and Headers are Duplicated : " + dupHeaders.toString());
				}
			}
			throw new ResourceNotFoundException(errorMsg.toString());
		}
	}

	private TestSheetHandler getTestSheetHandler(InputStream fileStream)
			throws IOException, OpenXML4JException, SAXException {
		OPCPackage pkg = OPCPackage.open(fileStream);
		LOGGER.info("OPCPackage check : {}", (pkg != null));
		XSSFBReader r = new XSSFBReader(pkg);
		XSSFBSharedStringsTable sst = new XSSFBSharedStringsTable(pkg);
		LOGGER.info("XSSFBSharedStringsTable check : {}", (sst != null));
		XSSFBStylesTable xssfbStylesTable = r.getXSSFBStylesTable();
		LOGGER.info("XSSFBStylesTable check : {}", (xssfbStylesTable != null));
		XSSFBReader.SheetIterator it = (XSSFBReader.SheetIterator) r.getSheetsData();
		CommonUtils.printMemoryDetails();
		TestSheetHandler testSheetHandler = new TestSheetHandler();
		if (it.hasNext()) {
			InputStream is = it.next();
			String name = it.getSheetName();
			testSheetHandler.startSheet(name);

			XSSFBSheetHandler sheetHandler = new XSSFBSheetHandler(is, xssfbStylesTable, null, sst, testSheetHandler,
					new DataFormatter(), false);
			sheetHandler.parse();
			testSheetHandler.endSheet();
		}
		pkg.close();

		CommonUtils.printMemoryDetails();
		LOGGER.info("Count: {}", testSheetHandler.getRows().size());
		LOGGER.info("Count in sst : {}", sst.getCount());
		return testSheetHandler;
	}

	private StringBuilder validateScopeFields(NextWorkExcelMasterScopes dto, String version, int row, String action) {
		StringBuilder errorMsg = new StringBuilder();
		if (Boolean.FALSE.equals(CommonUtils.validateGid(dto.getGid()))) {
			errorMsg.append("Row-" + row + " : gid must be 8 characters and contain at least 1 alphabet.");
		}
		if (action.equals("APPEND")) {
			Optional<Scoping> scping = scopingRepository.findById(version + "_" + dto.getGid());
			if (scping.isPresent()) {
				errorMsg.append("Row-" + row + ": gid is already existed. ");
			}
		}
		return errorMsg;
	}

	private List<String> retryGetDuplicateGIDS(String collection) {
		List<String> dupRecs;
		threadWait();
		dupRecs = getDuplicateRecsByAggregator(collection);
		LOGGER.info("First upload check : {}", dupRecs.isEmpty());
		if(dupRecs.isEmpty()){
			threadWait();
			dupRecs = getDuplicateRecsByAggregator(collection);
			LOGGER.info("Second upload check : {}", dupRecs.isEmpty());
			if(dupRecs.isEmpty()){
				threadWait();
				dupRecs = getDuplicateRecsByAggregator(collection);
				LOGGER.info("Third upload check : {}", dupRecs.isEmpty());
			}
		}
		return dupRecs;
	}


	@NotNull
	private List<String> getDuplicateRecsByAggregator(String collection) {
		GroupOperation group = Aggregation.group("gid").count().as(NextworkConstants.COUNT);
		MatchOperation match = new MatchOperation(Criteria.where(NextworkConstants.COUNT).gt(1));
		Fields fields = Fields.from(Fields.field("gid", "$_id"), Fields.field(NextworkConstants.COUNT, "$count"));
		ProjectionOperation project = Aggregation.project(fields);
		Aggregation aggregate = Aggregation.newAggregation(group, match, project);
		AggregationResults<GIDAggregratorDTO> gidAggregate = mongoOperations.aggregate(aggregate, collection,
				GIDAggregratorDTO.class);
		return gidAggregate.getMappedResults().stream()
				.map(d -> d.getGid() + " has " + d.getCount() + " duplicate records").toList();
	}
 
	public void createCollection(String collectionName) {
		mongoOperations.createCollection(collectionName);
	}

	public Map<String, String> getTemplateExcelHeadersMap() {
		var xlHeaders = new LinkedHashMap<String, String>();
		xlHeaders.put(ScopingConstants.GID, ScopingConstantsDTO.GID);
		xlHeaders.put(ScopingConstants.EMAILADDRESS, ScopingConstantsDTO.EMAILADDRESS);
		xlHeaders.put(ScopingConstants.CONTRACT_STATUS, ScopingConstantsDTO.CONTRACT_STATUS);
		xlHeaders.put(ScopingConstants.ARE, ScopingConstantsDTO.ARE);
		xlHeaders.put(ScopingConstants.ARE_TEXT, ScopingConstantsDTO.ARE_TEXT);
		xlHeaders.put(ScopingConstants.COMPANY, ScopingConstantsDTO.COMPANY);
		xlHeaders.put(ScopingConstants.COMPANY_TEXT, ScopingConstantsDTO.COMPANY_TEXT);
		xlHeaders.put(ScopingConstants.DIVISION_EXTERNAL, ScopingConstantsDTO.DIVISION_EXTERNAL);
		xlHeaders.put(ScopingConstants.DIVISION_EXTERNAL_TEXT, ScopingConstantsDTO.DIVISION_EXTERNAL_TEXT);
		xlHeaders.put(ScopingConstants.DIVISION_INTERNAL, ScopingConstantsDTO.DIVISION_INTERNAL);
		xlHeaders.put(ScopingConstants.DIVISION_INTERNAL_TEXT, ScopingConstantsDTO.DIVISION_INTERNAL_TEXT);
		xlHeaders.put(ScopingConstants.BUSINESS_UNIT, ScopingConstantsDTO.BUSINESS_UNIT);
		xlHeaders.put(ScopingConstants.DEPTH_STRUCTURE_KEY, ScopingConstantsDTO.DEPTH_STRUCTURE_KEY);
		xlHeaders.put(ScopingConstants.DEPTH_STRUCTURE_KEY_TEXT, ScopingConstantsDTO.DEPTH_STRUCTURE_KEY_TEXT);
		xlHeaders.put(ScopingConstants.ORG_CODE_PA, ScopingConstantsDTO.ORG_CODE_PA);
		xlHeaders.put(ScopingConstants.ORGANIZATION_CLASS, ScopingConstantsDTO.ORGANIZATION_CLASS);
		xlHeaders.put(ScopingConstants.UNIT, ScopingConstantsDTO.UNIT);
		xlHeaders.put(ScopingConstants.JOB_FAMILY_CATEGORY, ScopingConstantsDTO.JOB_FAMILY_CATEGORY);
		xlHeaders.put(ScopingConstants.JOB_FAMILY, ScopingConstantsDTO.JOB_FAMILY);
		xlHeaders.put(ScopingConstants.SUB_JOB_FAMILY, ScopingConstantsDTO.SUB_JOB_FAMILY);
		xlHeaders.put(ScopingConstants.POSITION_TYPE, ScopingConstantsDTO.POSITION_TYPE);
		xlHeaders.put(ScopingConstants.GRIP_POSITION, ScopingConstantsDTO.GRIP_POSITION);
		xlHeaders.put(ScopingConstants.GRIP_POSITION_TEXT, ScopingConstantsDTO.GRIP_POSITION_TEXT);
		xlHeaders.put(ScopingConstants.REGIONAL_ORGANIZATION, ScopingConstantsDTO.REGIONAL_ORGANIZATION);
		xlHeaders.put(ScopingConstants.REGIONAL_ORGANIZATION_TEXT, ScopingConstantsDTO.REGIONAL_ORGANIZATION_TEXT);
		xlHeaders.put(ScopingConstants.COUNTRY_REGION_ARE, ScopingConstantsDTO.COUNTRY_REGION_ARE);
		xlHeaders.put(ScopingConstants.COUNTRY_REGION_PLACE_OF_ACTION,
				ScopingConstantsDTO.COUNTRY_REGION_PLACE_OF_ACTION);
		xlHeaders.put(ScopingConstants.COUNTRY_REGION_STATE_OFFICE, ScopingConstantsDTO.COUNTRY_REGION_STATE_OFFICE);
		xlHeaders.put(ScopingConstants.LOCATION_OFFICE_CITY, ScopingConstantsDTO.LOCATION_OFFICE_CITY);
		xlHeaders.put(ScopingConstants.LOCATION_OFFICE, ScopingConstantsDTO.LOCATION_OFFICE);
		xlHeaders.put(ScopingConstants.COUNTRY_REGION_SUBENTITY, ScopingConstantsDTO.COUNTRY_REGION_SUBENTITY);
		xlHeaders.put(ScopingConstants.BLUECOLLARE_WHITECOLLAR, ScopingConstantsDTO.BLUECOLLARE_WHITECOLLAR);
		return xlHeaders;
	}

	public void addScopingVersion(String versionId, Date now) {
		LOGGER.info("Add Scoping Version");
		List<ScopingVersions> scopingVersionList = mongoOperations.findAll(ScopingVersions.class);
		if (scopingVersionList.isEmpty()) {
			setFirstScopingVersion(versionId, now);

		} else {
			LOGGER.info("Scoping Version is not empty");

			Query qrySv = new Query();
			qrySv.addCriteria(Criteria.where("vid").is(versionId));
			ScopingVersions scopingVersion = mongoOperations.findOne(qrySv, ScopingVersions.class);

			Query prevLatestVersionQuery = new Query();
			prevLatestVersionQuery.addCriteria(Criteria.where(LATEST).is(Boolean.TRUE));
			ScopingVersions scopingVersionPrevLatestVersion = mongoOperations.findOne(prevLatestVersionQuery,
					ScopingVersions.class);

			if (null == scopingVersion) {
				scopingVersion = new ScopingVersions();
				scopingVersion.setVid(versionId);

			}

			scopingVersionList = setScopingVersion(versionId, scopingVersionList, scopingVersion,
					scopingVersionPrevLatestVersion);

			LOGGER.info("MMXX:Going for summary calculation");
			List<Integer> versionList = scopingVersionList.stream().map(e -> Integer.parseInt(e.getVid())).sorted()
					.collect(Collectors.toList());

			if (!versionList.contains(Integer.parseInt(versionId))) {
				versionList.add(Integer.parseInt(versionId));
			}
			Collections.sort(versionList);

			String prevVersionId = "";
			String nextVersionId = "";

			int currentVersionId = 0;
			if (versionList.contains(Integer.parseInt(versionId))) {
				currentVersionId = versionList.indexOf(Integer.parseInt(versionId));
			}
			if (currentVersionId > 0) {
				prevVersionId = Integer.toString(versionList.get(currentVersionId - 1));
			}

			if ((versionList.size() - 1) > currentVersionId) {
				nextVersionId = Integer.toString(versionList.get(currentVersionId + 1));
			}

			scopingVersion.setVersionStatus(PublishType.PUBLISH.value);
			scopingVersion.setUploadTime(now);
			scopingVersion.setPreviousVid(prevVersionId);
			scopingVersion.setSummary(null);
			scopingVersionsRepository.save(scopingVersion);

			LOGGER.info("MMXX:Current versionId={}", versionId);
			LOGGER.info("MMXX:prevVersion={}", prevVersionId);
			LOGGER.info("MMXX:nextVersion={}", nextVersionId);

			try {
				validateAndProcessSummary(scopingVersion, versionId, prevVersionId, nextVersionId);
			} catch (Exception e) {
				throw new RestBadRequestException(e.getMessage());
			}

		}

	}

	private List<ScopingVersions> setScopingVersion(String versionId, List<ScopingVersions> scopingVersionList,
													ScopingVersions scopingVersion, ScopingVersions scopingVersionPrevLatestVersion) {
		if (scopingVersionPrevLatestVersion != null) {
			addingNewVersion(versionId, scopingVersion, scopingVersionPrevLatestVersion);
		} else {
			scopingVersionList = setScopingVersionIfNoLatestVersionAvailable(versionId, scopingVersionList,
					scopingVersion);
		}
		return scopingVersionList;
	}

	private List<ScopingVersions> setScopingVersionIfNoLatestVersionAvailable(String versionId,
																			  List<ScopingVersions> scopingVersionList, ScopingVersions scopingVersion) {
		LOGGER.info("No latest flag in db");
		if (null != scopingVersionList && !scopingVersionList.isEmpty()) {
			scopingVersionList = scopingVersionList.stream()
					.filter(i -> (i.getVersionStatus().equalsIgnoreCase(PublishType.PUBLISH.value)))
					.sorted(Comparator.comparing(ScopingVersions::getVid).reversed()).toList();
			String version = scopingVersionList.get(0).getVid();
			Integer lastPublishedVersion = Integer.parseInt(version);
			Integer currentVersion = Integer.parseInt(versionId);
			if (lastPublishedVersion > currentVersion) {
				Query qryLatest = new Query();
				qryLatest.addCriteria(Criteria.where("vid").is(version));
				ScopingVersions scopingVersionLatest = mongoOperations.findOne(qryLatest,
						ScopingVersions.class);
				if (null != scopingVersionLatest) {
					scopingVersionLatest.setLatest(true);
					scopingVersionsRepository.save(scopingVersionLatest);
				}
			} else {
				scopingVersion.setLatest(true);
			}

		} else {
			scopingVersion.setLatest(true);
		}
		return scopingVersionList;
	}

	private void setFirstScopingVersion(String versionId, Date now) {
		LOGGER.info("Scoping Version is empty");
		ScopingVersions scopingVersion = new ScopingVersions();
		scopingVersion.setVid(versionId);
		scopingVersion.setLatest(true);
		scopingVersion.setVersionStatus(PublishType.PUBLISH.value);
		scopingVersion.setUploadTime(now);
		scopingVersion.setPreviousVid("");
		scopingVersion.setSummary(null);
		scopingVersionsRepository.save(scopingVersion);
	}

	private void validateAndProcessSummary(ScopingVersions scopingVersion, String versionId, String prevVersionId,
										   String nextVersionId)  {
		VersionSummary summary;

		if (!prevVersionId.isBlank()) {

			summary = calculateScopingDifference1(versionId, prevVersionId);
		} else {

			summary = calculateScopingDifference1(versionId, "");
		}
		scopingVersion.setSummary(summary);
		scopingVersionsRepository.save(scopingVersion);

		if (!nextVersionId.isBlank()) {

			Optional<ScopingVersions> scopingVersionNext = scopingVersionsRepository.findById(nextVersionId);

			if (!scopingVersionNext.isPresent())
				throw new RestBadRequestException("Next version no found");

			VersionSummary summaryNext = calculateScopingDifference1(nextVersionId, versionId);

			scopingVersionNext.get().setPreviousVid(versionId);
			scopingVersionNext.get().setSummary(summaryNext);
			scopingVersionsRepository.save(scopingVersionNext.get());
		}

	}

	private void addingNewVersion(String versionId, ScopingVersions scopingVersion,
								  ScopingVersions scopingVersionPrevLatestVersion) {
		LOGGER.info("FOUND prev latest and new entry now");
		DateFormat df = new SimpleDateFormat(DATEFORMAT);
		try {
			Date prevLatestDate = df.parse(scopingVersionPrevLatestVersion.getVid());
			LOGGER.info("prevLatestDate : {}", prevLatestDate);
			Date currentScopingDate = df.parse(versionId);
			LOGGER.info("currentScopingDate : {}", currentScopingDate);

			if (currentScopingDate.after(prevLatestDate)) {
				LOGGER.info("currentScopingDate is bigger");

				scopingVersionPrevLatestVersion.setLatest(false);
				scopingVersionsRepository.save(scopingVersionPrevLatestVersion);
				scopingVersion.setLatest(true);

			} else if(currentScopingDate.before(prevLatestDate)){
				LOGGER.info("prevLatestDate is bigger");
				scopingVersion.setLatest(false);

			}
			else
			{
				LOGGER.info("Both version are same");

				scopingVersion.setLatest(true);
			}

		} catch (Exception e) {
			throw new RestBadRequestException("Some issue with the date format " + e.getMessage());
		}
	}

	@Override
	public List<MasterScopingDataSummary> getVersionSummaryByVersionId(String versionId, String userEmail) {

		LOGGER.info("Inside serviceImpl - getVersionSummaryByVersionId");

		userService.checkAdminUserRole(userEmail);

		Query q = new Query();
		q.addCriteria(Criteria.where("vid").is(versionId));
		ScopingVersions scopingVersionData = mongoOperations.findOne(q, ScopingVersions.class);

		if (Objects.isNull(scopingVersionData)) {
			throw new RestBadRequestException("Version not found");
		}

		if (Objects.isNull(scopingVersionData.getSummary())) {
			throw new RestBadRequestException("No summary present for this version");
		}


		List<SummaryData> summaryList = new ArrayList<>();
		getAddedScopeList(scopingVersionData, summaryList);
		getModifiedScopedList(scopingVersionData, summaryList);
		getRemovedScopedList(scopingVersionData, summaryList);

		List<MasterScopingDataSummary> maasterSummaryList = new ArrayList<>();
		MasterScopingDataSummary masterScopingDataSummary = new MasterScopingDataSummary();
		masterScopingDataSummary.setVid(versionId);
		masterScopingDataSummary.setSummaryData(summaryList);

		Sort sort = Sort.by(Sort.Direction.DESC, "updatedOn");
		Integer recCount = 0;
		List<FileDetails> files = fileDetailsRepository.findAllByVersion(versionId, sort);
		for (FileDetails fl : files) {
			if (fl.getStatus().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS) &&  (fl.getAction().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_PUT_APPEND)
						|| fl.getAction().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_PUT_TOOL) &&  (!StringUtils.isAllBlank(fl.getFileName())))) {
						recCount = fl.getRecCount();
						break;
			}
		}
		LOGGER.info("Added Rec Count : {}", recCount);
		masterScopingDataSummary.setRecCount(recCount);

		maasterSummaryList.add(masterScopingDataSummary);

		return maasterSummaryList;
	}

	private void getRemovedScopedList(ScopingVersions scopingVersionData, List<SummaryData> summaryList) {
		SummaryData summaryData;
		if (Objects.nonNull(scopingVersionData.getSummary().getRemovedFields())) {

			for (Map.Entry<String, Integer> entry : scopingVersionData.getSummary().getRemovedFields().entrySet()) {
				summaryData = new SummaryData();
				summaryData.setFiledAction("Removed");
				summaryData.setFieldName(entry.getKey());
				summaryData.setFieldCount(entry.getValue());
				summaryList.add(summaryData);
			}
		}
	}

	private void getModifiedScopedList(ScopingVersions scopingVersionData, List<SummaryData> summaryList) {
		SummaryData summaryData;
		if (Objects.nonNull(scopingVersionData.getSummary().getModifiedFields())) {

			for (Map.Entry<String, Integer> entry : scopingVersionData.getSummary().getModifiedFields().entrySet()) {
				summaryData = new SummaryData();
				summaryData.setFiledAction("Modified");
				summaryData.setFieldName(entry.getKey());
				summaryData.setFieldCount(entry.getValue());
				summaryList.add(summaryData);
			}
		}
	}

	private void getAddedScopeList(ScopingVersions scopingVersionData, List<SummaryData> summaryList) {
		SummaryData summaryData;
		if (Objects.nonNull(scopingVersionData.getSummary().getAddedFields())) {

			for (Map.Entry<String, Integer> entry : scopingVersionData.getSummary().getAddedFields().entrySet()) {
				summaryData = new SummaryData();
				summaryData.setFiledAction("Added");
				summaryData.setFieldName(entry.getKey());
				summaryData.setFieldCount(entry.getValue());
				summaryList.add(summaryData);
			}
		}
	}

	private VersionSummary calculateScopingDifference1(String versionId, String prevVersionId) {
		LOGGER.info("calculateScopingDifference1 {} --> {} ", versionId, prevVersionId);

		List<String> verList = new ArrayList<>();
		verList.add(versionId);
		verList.add(prevVersionId);
		List<String> modList = findModifiedGIDSInVersions("master_scoping_data", verList);
		LOGGER.info("Mod List : {}", modList);
		Map<String, Integer> addedFieldMapping = new HashMap<>();
		Map<String, Integer> removedFieldMapping = new HashMap<>();
		Map<String, Integer> modifiedFieldMapping = new HashMap<>();

	 	List<String> addList = scopingRepository.findAllGIdsByVersion(versionId).stream().map(IdDTO::getGid).filter(a -> !modList.contains(a)).toList();
	 	LOGGER.info("Loop Added : {}, Modified : {} ", addList.size(), modList.size());
	 	Map<String, String> headersMap = getTemplateExcelHeadersMap();
	 	List<String> remList = scopingRepository.findAllGIdsByVersion(prevVersionId).stream().map(IdDTO::getGid).filter(a -> !modList.contains(a)).toList();
	 	LOGGER.info("Loop Added : {}, Modified : {}, Removed : {}", addList.size(), modList.size(), remList.size());
	 	addedFieldMapping.put(ScopingConstantsDTO.GID, addList.size());


		removedFieldMapping.put(ScopingConstantsDTO.GID, remList.size());
		modifiedFieldMapping.put(ScopingConstantsDTO.GID, modList.size());


		getAddedFieldMapping(versionId, modList, addedFieldMapping);
		getRemovedFieldMapping(prevVersionId, modList, removedFieldMapping);

		LOGGER.info("Calculating the modified fields...");
		List<String> mList = modList.stream().sorted(Comparator.reverseOrder()).toList();
	 	List<List<String>> modSubList = ListUtils.partition(mList, 5000);

	 	LOGGER.info("modSubList size : {}", modSubList.size());
	 	getModifiedFieldMapping(verList, modifiedFieldMapping, headersMap, modSubList);

	 	int addGid = addedFieldMapping.get(ScopingConstantsDTO.GID);
	 	Map<String, Integer> addMap = new HashMap<>();
	 	Map<String, Integer> remMap = new HashMap<>();
	 	for(Entry<String, Integer> entry : addedFieldMapping.entrySet()){
	 		int addGidVal = addGid;
	 		if(!entry.getKey().equalsIgnoreCase(ScopingConstantsDTO.GID)){
	 			addGidVal = addGid - entry.getValue();
 			}
	 		addMap.put(entry.getKey(), addGidVal);

 		}
	 	headersMap.values().stream().filter(h -> !addMap.containsKey(h)).forEach(v -> addMap.put(v, addGid));


	 	int remGid = removedFieldMapping.get(ScopingConstantsDTO.GID);
	 	for(Entry<String, Integer> entry : removedFieldMapping.entrySet()){
	 		int remGidVal = remGid;
	 		if(!entry.getKey().equalsIgnoreCase(ScopingConstantsDTO.GID)){
	 			remGidVal = remGid - entry.getValue();
 			}
	 		remMap.put(entry.getKey(), remGidVal);
 		}
	 	headersMap.values().stream().filter(h -> !remMap.containsKey(h)).forEach(v -> remMap.put(v, remGid));


	 	VersionSummary summary = new VersionSummary();
	 	summary.setAddedFields(addMap);
	 	summary.setModifiedFields(modifiedFieldMapping);
	 	summary.setRemovedFields(remMap);

	 	return summary;
	}

	private ArrayList<String> getExcludeFields() {
		var excFileds = new ArrayList<String>();
		excFileds.add(ScopingConstantsDTO.ID);
		excFileds.add(ScopingConstantsDTO.VID);
		excFileds.add("uploadTime");
		excFileds.add("modifyTime");
		excFileds.add("actionType");
		excFileds.add("isCaptured");
		excFileds.add("isVisible");
		excFileds.add("nullFields");
		excFileds.add("vGid");
		return excFileds;
	}

	private void getRemovedFieldMapping(String prevVersionId, List<String> modList,
										Map<String, Integer> removedFieldMapping) {
		List<IdDTO> remFields = scopingRepository.findAllGidsAndNullFiledsByVersion(prevVersionId);
		LOGGER.info("remFields Null objects count : {} ", remFields.size());
		for (IdDTO f : remFields) {
			if (!modList.contains(f.getGid())) {
				String[] args = f.getNullFields().split(",");
				for (String arg : args) {
					if (StringUtils.isAllBlank(arg)) continue;
					int cnt = 1;
					cnt += removedFieldMapping.computeIfAbsent(arg, k -> 0);
					removedFieldMapping.put(arg, cnt);
				}
			}
		}
	}

	private void getAddedFieldMapping(String versionId, List<String> modList, Map<String, Integer> addedFieldMapping) {
		List<IdDTO> addFields = scopingRepository.findAllGidsAndNullFiledsByVersion(versionId);
		LOGGER.info("addFields Null objects count : {}", addFields.size());
		for (IdDTO f : addFields) {
			if (!modList.contains(f.getGid())) {
				String[] args = f.getNullFields().split(",");
				for (String arg : args) {
					if (StringUtils.isAllBlank(arg)) continue;
					int cnt = 1;
					cnt += addedFieldMapping.computeIfAbsent(arg, k -> 0);
					addedFieldMapping.put(arg, cnt);
				}
			}
		}
	}

	private void getModifiedFieldMapping(List<String> verList, Map<String, Integer> modifiedFieldMapping,
			Map<String, String> headersMap, List<List<String>> modSubList) {

		int itr = 1;
		var excFileds = getExcludeFields();
		Sort sort = Sort.by(Sort.Direction.DESC, ScopingConstantsDTO.GID);
		ObjectMapper mapper = new ObjectMapper();
		for (List<String> subList : modSubList) {
			List<Scoping> subScopesList = scopingRepository.findAllByVidInAndGidIn(verList, subList, sort);
			int cnt = 0;
			for(int i =0 ;i < subScopesList.size(); i = cnt) {
				Scoping curScp = subScopesList.get(i); cnt = cnt+1;
				Scoping preScp = subScopesList.get(i); cnt = cnt+1;

				boolean isSame = EqualsBuilder.reflectionEquals(curScp, preScp, excFileds);

				if(!isSame) {
					processModifiedFieldMapping(modifiedFieldMapping, headersMap, mapper, curScp, preScp);
				}
			}
			LOGGER.info("Map data for iteration: {} and data : {}", itr, modifiedFieldMapping);
			itr = itr+1;
		}
	}

	private void processModifiedFieldMapping(Map<String, Integer> modifiedFieldMapping, Map<String, String> headersMap,
											 ObjectMapper mapper, Scoping curScp, Scoping preScp) {
		Map<String, Object> curMap = mapper.convertValue(curScp, Map.class);

		Map<String, Object> prevMap = mapper.convertValue(preScp, Map.class);
		for(Entry<String, String> entry : headersMap.entrySet()) {
			if(entry.getKey().equalsIgnoreCase(ScopingConstants.GID)) continue;

			String curVal = (String) curMap.get(entry.getValue());
			String preVal = (String) prevMap.get(entry.getValue());
			LOGGER.info("GID current Previous: {} -> {} , Field : {}, Current : {} and Prev : {} and check : {}",  curMap.get(ScopingConstantsDTO.GID), prevMap.get(ScopingConstantsDTO.GID), entry.getValue(), curVal, preVal, curVal.equalsIgnoreCase(preVal));
			if(!curVal.equalsIgnoreCase(preVal)) {
				Integer val = 1;
				if(modifiedFieldMapping.containsKey(entry.getValue())) {
					val = modifiedFieldMapping.get(entry.getValue());
					val = val + 1;
				}
				modifiedFieldMapping.put(entry.getValue(), val);
			}

		}
	}

	private List<String> findModifiedGIDSInVersions(String collection, List<String> versionsList) {
		GroupOperation group = Aggregation.group("gid").count().as(NextworkConstants.COUNT);
		MatchOperation match = new MatchOperation(Criteria.where("count").gt(1));
		MatchOperation matchS = new MatchOperation(Criteria.where("vid").in(versionsList));
		Fields fields = Fields.from(Fields.field("gid", "$_id"), Fields.field("count", "$count"));
		ProjectionOperation project = Aggregation.project(fields);
		Aggregation aggregate = Aggregation.newAggregation(matchS, group, match, project);
		AggregationResults<GIDAggregratorDTO> gidAggregate = mongoOperations.aggregate(aggregate, collection,
				GIDAggregratorDTO.class);
		return gidAggregate.getMappedResults().stream().map(GIDAggregratorDTO::getGid).toList();
	}

	@Override
	public IdResponseDTO addNote(NotesDTO notesDTO, String versionId, String userEmail, String authorName) {
		userService.checkAdminUserRole(userEmail);
		IdResponseDTO res = new IdResponseDTO();

		Query qrySv = new Query();
		qrySv.addCriteria(Criteria.where("vid").is(versionId));
		ScopingVersions scopingVersion = mongoOperations.findOne(qrySv, ScopingVersions.class);
		if (scopingVersion != null) {
			if (null != notesDTO.getIndex()) {
				LOGGER.info("This is an existing note");

				NotesDTO noteDTO = scopingVersion.getNotes().get(notesDTO.getIndex());
				if(!notesDTO.getEmailId().equalsIgnoreCase(userEmail))
				{
					throw new RestBadRequestException("User email doesn't matches");
				}
				if (notesDTO.getEmailId().equals(noteDTO.getEmailId())) {
					noteDTO.setNote(notesDTO.getNote());
					scopingVersion.getNotes().set(notesDTO.getIndex(), noteDTO);
				} else {
					throw new RestBadRequestException("User can edit only their note");
				}

			} else {
				addNewNote(notesDTO, scopingVersion,authorName, userEmail);

			}
			scopingVersionsRepository.save(scopingVersion);

		} else {
			throw new ResourceNotFoundException("No data found for given version Id");
		}

		res.setUid(versionId);
		return res;
	}

	private void addNewNote(NotesDTO notesDTO, ScopingVersions scopingVersion, String authorName, String userEmail) {
		LOGGER.info("This is a new note");
		NotesDTO noteDTO = new NotesDTO();
		noteDTO.setNote(notesDTO.getNote());
		if(!notesDTO.getEmailId().equalsIgnoreCase(userEmail))
		{
			throw new RestBadRequestException("User email doesn't matches");
		}
		noteDTO.setEmailId(notesDTO.getEmailId());
		if(!notesDTO.getAuthor().equalsIgnoreCase(authorName))
		{
			throw new RestBadRequestException("Author name doesn't matches");
		}
		noteDTO.setAuthor(notesDTO.getAuthor());
		noteDTO.setDate(new Date(System.currentTimeMillis()));
		int size = 0;
		if (null != scopingVersion.getNotes() && !scopingVersion.getNotes().isEmpty()) {
			size = scopingVersion.getNotes().size();

		}

		LOGGER.info("Previous size : {}", size);
		if (size == 0) {
			scopingVersion.setNotes(new ArrayList<>());
			noteDTO.setIndex(size);

		} else {
			noteDTO.setIndex(size);
		}
		scopingVersion.getNotes().add(noteDTO);

	}

	@Override
	public List<NotesDTO> getNotes(String versionId, String userEmail) {
		userService.checkAdminUserRole(userEmail);
		List<NotesDTO> notesList;
		Query qrySv = new Query();
		qrySv.addCriteria(Criteria.where("vid").is(versionId));
		ScopingVersions scopingVersion = mongoOperations.findOne(qrySv, ScopingVersions.class);
		if (scopingVersion != null) {
			if (null != scopingVersion.getNotes() && !scopingVersion.getNotes().isEmpty()) {
				notesList = scopingVersion.getNotes();

			} else {
				notesList = new ArrayList<>();

			}
		} else {
			throw new ResourceNotFoundException("Scoping Version not found");
		}
		return notesList;
	}

	public String addVersionDataToExcel(String uid, String versionId, String serverName, FileDetails s3FileDetails)
			throws  IOException {
		String fileName = null;
		String orgFileName = null;
		LOGGER.info("UID : {}", uid);
		String filePath = s3FileDetails.getFilePath();
		Integer recCount = (s3FileDetails.getRecCount() == null) ? 0 : s3FileDetails.getRecCount();
		Sort sort = Sort.by(Sort.Direction.DESC, "updatedOn");
		List<FileDetails> files = fileDetailsRepository.findAllByVersion(versionId, sort);
		LOGGER.info("APPend files size : {} and Record : {}", files.size(), recCount);
		FileDetails fileDetail = getProcessingFile(files);
		Boolean isAppendExists = isScopingDataAvailable( files);
		recCount = getRecCount(recCount, files);
		LOGGER.info("isAppendExists : {} and Rec Count : {}", isAppendExists, recCount);

		if (Boolean.TRUE.equals(isAppendExists)) {
			if(fileDetail != null) {
				fileName = fileDetail.getFileName();
				orgFileName = fileDetail.getFileName();
				filePath = fileDetail.getFilePath();
			}
		} else {
			fileName = versionId;
			String filelocation = System.getProperty(JAVA_IO_TMPDIR);
			orgFileName = versionId + "_" + NextworkConstants.PROCESSED_FILE_CHANGE_DOWNLOADED + ".xlsx";
			fileName = fileName + "_" + NextworkConstants.PROCESSED_FILE_CHANGE_DOWNLOADED + ".xlsx";
			if (StringUtils.isBlank(filePath)) {
				filePath = bucketName.getBucketName() + NextworkConstants.PATH_SLASH + NextworkConstants.FILE_AWS_PROCESSED + NextworkConstants.PATH_SLASH  + versionId;
			}
			if (serverName.equalsIgnoreCase(NextworkConstants.LOCAL_HOST)) {
				fileName = filelocation + NextworkConstants.PATH_SLASH  + fileName;
			}
		}

		LOGGER.info("APPENDED File Name : {} and isAppendedFileExists : {}", fileName, isAppendExists);
		List<Scoping> appendedData = scopingRepository.findAllByVidAndIsCaptured(versionId, Boolean.FALSE);
		LOGGER.info("File Path : {}, Total appended Records: {} ", filePath, appendedData.size());
		if(appendedData.isEmpty()){
			appendedData = retryReplicaAppendData(versionId);
		}
		if (appendedData.isEmpty()){
			LOGGER.info("Append data size : {}  and recCount : {}", appendedData.size(), recCount  );
			s3FileDetails.setFileName("");
			s3FileDetails.setRecCount(recCount);
			return fileName;
		}
		createOrAppendDataToExcel(appendedData, serverName, filePath, fileName, isAppendExists, recCount);
		recCount = ((recCount != null) ? recCount : 0) + appendedData.size();
		s3FileDetails.setRecCount(recCount);

		if (recCount >= NextworkConstants.SCOPING_FILE_REC_COUNT_THRESHOLD) {
			s3FileDetails.setRecCount(NextworkConstants.SCOPING_FILE_REC_COUNT_THRESHOLD);
			Optional<ScopingVersions> opsv= scopingVersionsRepository.findById(versionId);
			if(!opsv.isEmpty()) {
				ScopingVersions sv = opsv.get();
				sv.setRecCount(NextworkConstants.SCOPING_FILE_REC_COUNT_THRESHOLD);
				scopingVersionsRepository.save(sv);
			}
		}
		return orgFileName;
	}

	private Integer getRecCount(Integer recCount, List<FileDetails> files) {
		for (FileDetails fl : files) {
			if (fl.getStatus().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS) &&  (fl.getAction().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_PUT_APPEND)
					|| fl.getAction().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_PUT_TOOL) )&&  (!StringUtils.isAllBlank(fl.getFileName()))) {
				recCount = fl.getRecCount();
				break;
			}
		}
		return recCount;
	}

	private Boolean isScopingDataAvailable(List<FileDetails> files) {
		for (FileDetails fl : files) {
			LOGGER.info("Append File sequence : {}", fl.getUid());
			if (isCreateSuccess(fl)) {
				return Boolean.FALSE;
			}
			if(isApppendSucess(fl) || isToolExists(fl)){
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}

	private static boolean isCreateSuccess(FileDetails fl) {
		return fl.getAction().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_POST_CREATE)
				&& fl.getStatus().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS);
	}

	private static boolean isToolExists(FileDetails fl) {
		return fl.getAction().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_PUT_TOOL) && (!StringUtils.isAllBlank(fl.getFileName()));
	}

	private static boolean isApppendSucess(FileDetails fl) {
		return fl.getAction().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_PUT_APPEND)
				&& fl.getStatus().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS)
				&& !StringUtils.isAllBlank(fl.getFileName());
	}
	private FileDetails getProcessingFile(List<FileDetails> files) {
		FileDetails fileDetails = null;
		for (FileDetails fl : files) {
			fileDetails = fl;
			LOGGER.info("Append File sequence : {}", fl.getUid());
			if (isCreateSuccess(fl)) {
				return fileDetails;
			}
			if(isApppendSucess(fl) || isToolExists(fl)){
				return fileDetails;
			}
		}
		return fileDetails;
	}

	private void createOrAppendDataToExcel(List<Scoping> appendedData, String server, String filePath, String fileName,
										   Boolean isAppendedFileExists, Integer recCount) throws IOException{
		File tempFile = new File(fileName);
		if (Boolean.TRUE.equals(isAppendedFileExists)) {
			fileName = updateConsolidatedFileWithAppendData(appendedData, server, filePath, fileName, recCount, tempFile);
		} else {
			createConsolidatedFileWithAppendData(appendedData, recCount, tempFile);
		}
		LOGGER.info("Writing is completed and before saving and the list size : {}", appendedData.size());
		scopingRepository.saveAll(appendedData);
		addFileDataForRemoteServer(server, filePath, fileName, tempFile, System.getProperty(JAVA_IO_TMPDIR));

		LOGGER.info("Append is done.");
		Files.deleteIfExists(Paths.get(tempFile.getPath()));
	}

	private File createConsolidatedFileWithAppendData(List<Scoping> appendedData, Integer recCount, File tempFile) throws IOException {
		int row = 1;
		int	col = 0;
		Map<String, String> headersMap = getTemplateExcelHeadersMap();
		Map<Integer, String> dataPatternMap = new HashMap<>();
		LOGGER.info("New File path {}", tempFile.getAbsolutePath());
		String filelocation = System.getProperty(JAVA_IO_TMPDIR);
		LOGGER.info("New scoping file and location : {}", filelocation);
		try(XSSFWorkbook workbook = new XSSFWorkbook()){
			XSSFSheet sheet = workbook.createSheet("Appended Scoping Details.");
			XSSFRow header = sheet.createRow(0);
			Set<String> headerSet = headersMap.keySet();
			for (String key : headerSet) {
				header.createCell(col).setCellValue(key);
				dataPatternMap.put(col, headersMap.get(key));
				col++;
			}
			writeDataToSheet(appendedData, recCount, sheet, row, dataPatternMap);
			FileOutputStream out = new FileOutputStream(tempFile);
			workbook.write(out);
			out.close();
		}catch (Exception e){
			throw new IOException(e.getMessage());
		}

		return tempFile;
	}

	@NotNull
	private String updateConsolidatedFileWithAppendData(List<Scoping> appendedData, String server, String filePath, String fileName, Integer recCount, File tempFile) throws IOException {
		Map<String, String> headersMap = getTemplateExcelHeadersMap();
		Map<Integer, String> dataPatternMap = new HashMap<>();
		LOGGER.info("Full File path {}", tempFile.getAbsolutePath());
		String filelocation = System.getProperty(JAVA_IO_TMPDIR);
		LOGGER.info("filelocation= {}", filelocation);
		if (server.equalsIgnoreCase(NextworkConstants.LOCAL_HOST)) {
			fileName = filelocation + NextworkConstants.PATH_SLASH + fileName;
		}
		tempFile = new File(fileName);
		InputStream fileStream = null;
		LOGGER.info("S3 Server Name : {} and file path : {} ", server, filePath);
		if (!server.equalsIgnoreCase("localhost")) {
			fileStream = fileStore.download(filePath, fileName);
			copyInputStreamToFile(fileStream, tempFile);
		}
		LOGGER.info("Full File path {}", tempFile.getAbsolutePath());
		try(XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(tempFile))){
			XSSFSheet sheet = workbook.getSheetAt(0);
			int row = sheet.getLastRowNum() + 1;

			XSSFRow rw = sheet.getRow(0);
			rw.cellIterator().forEachRemaining(c ->
					dataPatternMap.put(dataPatternMap.size(), headersMap.get(c.getStringCellValue())));
			writeDataToSheet(appendedData, recCount, sheet, row, dataPatternMap);
			FileOutputStream out = new FileOutputStream(tempFile);
			workbook.write(out);
			out.close();
		}catch (IOException e){
			throw  new IOException(e.getMessage());
		}

		return fileName;
	}

	private void writeDataToSheet(List<Scoping> appendedData, Integer recCount, XSSFSheet sheet, int row, Map<Integer, String> dataPatternMap) {
		int col;
		LOGGER.info("Create or append data to excel additional fields...");
		ObjectMapper mapper = getObjectMapper();
		for (Scoping scops : appendedData) {
			XSSFRow data = sheet.createRow(row);
			recCount = (Objects.nonNull(recCount)) ? recCount + 1 : 1;

			Map<String, Object> props = mapper.convertValue(scops, Map.class);
			for (col = 0; col < dataPatternMap.size(); col++) {
				data.createCell(col).setCellValue(String.valueOf(props.getOrDefault(dataPatternMap.get(col), "")));
			}
			scops.setIsCaptured(Boolean.TRUE);
			if (recCount >= NextworkConstants.SCOPING_FILE_REC_COUNT_THRESHOLD) {
				break;
			}
			row++;
		}
	}

	private void addFileDataForRemoteServer(String server, String filePath, String fileName, File tempFile,
											String filelocation) {
		if (!server.equalsIgnoreCase("localhost")) {
			filePath = filePath.replaceAll(bucketName.getBucketName() + NextworkConstants.PATH_SLASH, "");
			fileName = fileName.replace(filelocation + NextworkConstants.PATH_SLASH, "");
			filePath = filePath + NextworkConstants.PATH_SLASH + fileName;
			LOGGER.info("Uploading temp to s3 and the bucketName:{} filePath : {}, fileName : {}, tempFileName :{}",
					bucketName.getBucketName(), filePath, fileName, tempFile);
			fileStore.upload(bucketName.getBucketName(), filePath, tempFile);
		}
	}

	private void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {


		LOGGER.info("Inside copyInputStreamToFile copying bytes bytes ");
		try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
			int read;
			byte[] bytes = new byte[10000];
			while ((read = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
		}

	}

}
