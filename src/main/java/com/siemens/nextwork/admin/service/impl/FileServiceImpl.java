package com.siemens.nextwork.admin.service.impl;

import static com.siemens.nextwork.admin.util.NextworkConstants.USER;
import static com.siemens.nextwork.admin.util.NextworkConstants.USER_NOT_EXIST;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime; 
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.mongodb.MongoException;
import com.siemens.nextwork.admin.config.BucketName;
import com.siemens.nextwork.admin.dto.IdDTO;
import com.siemens.nextwork.admin.dto.IdResponseDTO;
import com.siemens.nextwork.admin.dto.UploadFileStatusResponseDTO;
import com.siemens.nextwork.admin.enums.RoleType;
import com.siemens.nextwork.admin.exception.ResourceNotFoundException;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.exception.RestForbiddenException;
import com.siemens.nextwork.admin.model.FileDetails;
import com.siemens.nextwork.admin.model.NextWorkUser;
import com.siemens.nextwork.admin.model.Roles;
import com.siemens.nextwork.admin.repo.FileDetailsRepository;
import com.siemens.nextwork.admin.repo.NextWorkUserRepository;
import com.siemens.nextwork.admin.repo.RolesRepository;
import com.siemens.nextwork.admin.repo.ScopingRepository;
import com.siemens.nextwork.admin.service.FileService;
import com.siemens.nextwork.admin.service.FileStore;
import com.siemens.nextwork.admin.service.UserService;
import com.siemens.nextwork.admin.util.NextworkConstants;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Service
@AllArgsConstructor
@NoArgsConstructor
public class FileServiceImpl implements FileService {

	@Autowired
	private FileStore fileStore;
	
	@Autowired
	private FileDetailsRepository fileRepository;

	@Autowired
	private UserService userService;
	
	@Autowired
	private NextWorkUserRepository nextWorkUserRepository;
	
	@Autowired
	private RolesRepository newRoleRepository;
	
	@Autowired
	MongoOperations mongoOperations;
	
	@Autowired
	ScopingRepository scopingRepository;

	@Autowired
	private BucketName s3bucketName;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FileServiceImpl.class);
	public static final String ROLE = "ROLE";
	public static final String INCLUSION = "gidInclusion";
	public static final String EXCLUSION = "gidExclusion";

	private static final String LOCALHOST = "Localhost";

	@Override
	public IdResponseDTO saveFile(MultipartFile file,String version, String userEmail, String action, String serverName, Boolean isCheckSize) {
		LOGGER.info("Inside save file");
		
		
		userService.checkAdminUserRole(userEmail);
		Optional<NextWorkUser> nxtUser = userService.findByUserEmail(userEmail);
		if (!nxtUser.isPresent())
			throw new RestBadRequestException(NextworkConstants.USER_NOT_EXIST);

		if (file.isEmpty()) {
			throw new IllegalStateException(NextworkConstants.CANNOT_UPLOAD_EMPTY_FILE);
		}
		
		if(action.equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_PUT_APPEND) && (!isVersionExists(version))) {
				throw new IllegalStateException("Version is not available.");
			
		}

		validateFileTypeAndSize(file, isCheckSize);
		trashFileIfVersionExists(version, action, serverName);
		
		FileDetails fileDetails = saveFileAndUploadDetails(file, version, nxtUser.get(), action, serverName, NextworkConstants.UPLOAD_TYPE_GID, NextworkConstants.FILE_TYPE_XLSB);
		return IdResponseDTO.builder().uid(fileDetails.getUid()).build();
	}


	private void validateFileTypeAndSize(MultipartFile file, Boolean isCheckSize) {
		if (!file.isEmpty()) {
			String fileFormate;
			if( Boolean.TRUE.equals(isCheckSize)) {
				long tenMb = (long) 40 * 1024 * 1024;
				if(file.getSize() > tenMb) {
					throw new IllegalStateException("File Size is exceeded and the file size not more than 35MB.");
				}
			}
			String fileName = file.getOriginalFilename();
			if (fileName != null) {
				String[] fileFormateArray = fileName.split("\\.");
				fileFormate = fileFormateArray[fileFormateArray.length - 1];
				if( Boolean.TRUE.equals(isCheckSize) &&  (!(fileFormate.equals(NextworkConstants.FILE_TYPE_XLSB)))) {
						throw new IllegalStateException(NextworkConstants.FILE_FORMAT_NOT_SUPPORTED);
					
				}
			}
		}
	}


	private FileDetails saveFileAndUploadDetails(MultipartFile file, String version, NextWorkUser user, String action,
			String serverName, String type, String fileType) {
		
		var bucketName= "Local";
		var pathKey= file.getOriginalFilename();
		var path = "Local/"+version;
		var fileName = file.getOriginalFilename();
		FileDetails fileDetails = null;
		String uploadKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		
		if(!serverName.equalsIgnoreCase(NextworkConstants.LOCAL_HOST)) {
			bucketName=s3bucketName.getBucketName();
			if(type.equalsIgnoreCase(NextworkConstants.UPLOAD_TYPE_GID)) {
				
				pathKey=String.format(NextworkConstants.TRIPLE_STRING_FORMAT,NextworkConstants.FILE_AWS_INPUT, version,file.getOriginalFilename());
				path = String.format(NextworkConstants.TRIPLE_STRING_FORMAT, s3bucketName.getBucketName(), NextworkConstants.FILE_AWS_INPUT, version);
				fileName = String.format("%s", file.getOriginalFilename());
			}
			if(type.equalsIgnoreCase(NextworkConstants.UPLOAD_TYPE_SKILL)
					|| type.equalsIgnoreCase(NextworkConstants.UPLOAD_TYPE_GRIP)) {
				
				pathKey=String.format("%s/%s/%s/%s", type.toUpperCase(), NextworkConstants.FILE_AWS_INPUT, uploadKey,file.getOriginalFilename());
				path = String.format("%s/%s/%s/%s", s3bucketName.getBucketName(), type.toUpperCase(),  NextworkConstants.FILE_AWS_INPUT, uploadKey);
				fileName = String.format("%s", file.getOriginalFilename());
			}
		}
		
		
		String filelocation = System.getProperty(NextworkConstants.TMP_DIR);
		LOGGER.info("filelocation = {}",filelocation);
		File tempFile = new File(filelocation+"/mfile"+System.currentTimeMillis()+"."+fileType);
		if(serverName.equalsIgnoreCase(NextworkConstants.LOCAL_HOST)) {
			fileName = tempFile.getName();
		}
		try {
			file.transferTo(tempFile);
			LOGGER.info("Local file location : {}", file.getOriginalFilename());
			LOGGER.info("Inside save file - uploading file to s3");
			if(!serverName.equalsIgnoreCase(NextworkConstants.LOCAL_HOST)) {
				fileStore.upload(bucketName, pathKey, tempFile);
			}
			
			
			LOGGER.info("Inside save file - uploaded file to s3 successfully");
			if(!serverName.equalsIgnoreCase(NextworkConstants.LOCAL_HOST)) {
				Files.deleteIfExists(Paths.get(tempFile.getPath()));
			}
			
			LocalDateTime  date = LocalDateTime.now();
			fileDetails = new FileDetails();
			fileDetails.setFileName(fileName);
			fileDetails.setDisplayName(fileName);
			fileDetails.setVersion(version);
			fileDetails.setFilePath(path);
			fileDetails.setCreatedOn(date);
			fileDetails.setCreatedBy(user.getEmail());
			fileDetails.setUpdatedOn(null);
			fileDetails.setUpdatedBy(null);
			fileDetails.setStatus(NextworkConstants.IN_PROGRESS);
			fileDetails.setErrors(null);
			fileDetails.setAction(action);
			fileDetails.setUsername(user.getName());
			fileDetails.setType(type);
			LOGGER.info("Inside save file - uploaded file to s3 successfully");
			fileDetails = fileRepository.save(fileDetails);
	
			LOGGER.info("After file save...");
		} catch (AmazonServiceException | MongoException | IllegalStateException | IOException e) {
			throw new IllegalStateException("Failed to upload file", e);
		}
		return fileDetails;
	}


	private boolean isVersionExists(String version) {
	    List<FileDetails> createsList = fileRepository.findAllByVersionAndStatus(version, NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS, Sort.by(Sort.Direction.DESC, NextworkConstants.UPDATE_ON));
	    LOGGER.info("Version files list size: {}", createsList.size());
	    return !createsList.isEmpty() && createsList.get(0) != null;
	}

	private void trashFileIfVersionExists(String version, String action, String serverName) {
		if(action.equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_POST_CREATE) 
				&& !serverName.equalsIgnoreCase(NextworkConstants.LOCAL_HOST)) {
			List<String> actionList = new ArrayList<>();
			actionList.add(action);
			
			FileDetails createFile = getLatestSucessfullFileByActions(version, actionList);
			if(createFile != null) {
				String path = NextworkConstants.FILE_AWS_PROCESSED+ NextworkConstants.PATH_SLASH +version;
				fileStore.deleteDirectory(s3bucketName.getBucketName(), path);
			}
		}
	}


	private FileDetails getLatestSucessfullFileByActions(String version, List<String> actionList) {
		Sort sort = Sort.by(Sort.Direction.DESC, NextworkConstants.UPDATE_ON); 
		List<FileDetails> creatsList = fileRepository.findAllByVersionAndStatusAndActionIn(version, NextworkConstants.FILE_UPLOAD_STATUS_SUCCESS, actionList, sort);
		if(creatsList.isEmpty()) return null;
		return creatsList.get(0);
	}

	@Override
	public List<UploadFileStatusResponseDTO> getScopingUploadFileStatus(String userEmail, String type) {
		
		userService.checkAdminUserRole(userEmail);
		
		List<String> actionList = new ArrayList<>();
		actionList.add(NextworkConstants.FILE_UPLOAD_POST_CREATE); 
		actionList.add(NextworkConstants.FILE_UPLOAD_PUT_APPEND);
		actionList.add(NextworkConstants.UPLOAD_TYPE_SKILL);
		actionList.add(NextworkConstants.UPLOAD_TYPE_GRIP);
		actionList.add(NextworkConstants.WORKSTREAM);
		
		Sort sort = Sort.by(Sort.Direction.DESC, NextworkConstants.UPDATE_ON);
		List<FileDetails> fileDetailsList=fileRepository.findAllByActionInAndType(actionList, type, sort);
		
		List<UploadFileStatusResponseDTO> responseList=new ArrayList<>();
		
		for(FileDetails fileData: fileDetailsList) {
			UploadFileStatusResponseDTO response=new UploadFileStatusResponseDTO();
			response.setTaskid(fileData.getUid());
			response.setErrors(fileData.getErrors());
			response.setOriginalfileName(fileData.getFileName());
			if(type.equalsIgnoreCase(NextworkConstants.UPLOAD_TYPE_GID)) {
				response.setOriginalfileName(fileData.getDisplayName());
			}
			response.setStatus(fileData.getStatus());
			response.setDate(fileData.getCreatedOn().toString());
			response.setType(fileData.getType());
			response.setUsername(fileData.getUsername());
			response.setIsSummary(Boolean.FALSE);
			response.setAction(fileData.getAction());
			if(!Objects.isNull(fileData.getReportStatus())) {
				response.setIsSummary(Boolean.TRUE);
			}
			
			responseList.add(response);
			
		}	
		return responseList;
	}


	@Override
	public void copyProcessedFile(String bucketName, FileDetails s3FileDetails, String awsInput, String awsProcessed, String newFileName) {
		if(s3FileDetails != null) {
			LOGGER.info("Input Path : {}",s3FileDetails.getFilePath());
			if(s3FileDetails.getAction().equalsIgnoreCase(NextworkConstants.FILE_UPLOAD_POST_CREATE)) {
				String processPath = String.format(NextworkConstants.DOUBLE_STRING_FORMAT,NextworkConstants.FILE_AWS_PROCESSED, s3FileDetails.getVersion());
				LOGGER.info("Deleted Directory path and bucket : {},{}", bucketName, processPath);
				fileStore.deleteDirectory(bucketName, processPath);

				LOGGER.info("Processed File Name : {}", newFileName);
				processPath = String.format("%s/%s/%s",NextworkConstants.FILE_AWS_PROCESSED, s3FileDetails.getVersion(),newFileName);
				String inputPath = String.format("%s/%s/%s",NextworkConstants.FILE_AWS_INPUT, s3FileDetails.getVersion(),s3FileDetails.getFileName());
				fileStore.copyBucketObject(bucketName, inputPath, bucketName, processPath);
			}
		}
	}


	@Override
	public String createAsyncJobForFileUpload(String userEmail, String targetId, String target, String action,
			MultipartFile file, String serverName) {
		LOGGER.info("Inside createAsyncJob file");
		
		String type=target.toUpperCase()+"_GID";
		String filelocation = String.format(NextworkConstants.FORMATTER,System.getProperty(NextworkConstants.TMP_DIR),type);
		String userName= validateUserAndRole(userEmail);
		
		if (file.isEmpty()) {
			throw new IllegalStateException("Cannot upload empty file");
		}

		if (file.getSize() > (1024*1024)) {
			throw new IllegalStateException("File Size is exceded and the file size not more than 1MB.");
		}
			
		String fileFormate = null;
		String fileName = file.getOriginalFilename();
	
		if (!file.isEmpty() && fileName != null) {
			String[] fileFormateArray = fileName.split("\\.");
			fileFormate = fileFormateArray[fileFormateArray.length - 1];
			if (!(fileFormate.equals("xls") || fileFormate.equals("xlsx"))) {
				throw new IllegalStateException(NextworkConstants.FILE_FORMAT_NOT_SUPPORTED);
			}
		}


		String bucketName = s3bucketName.getBucketName();
		String path = String.format(NextworkConstants.FORMATTER,bucketName,type);
		fileName = String.format("%s", file.getOriginalFilename());
		
		
		File tempFile = new File(filelocation+fileName);
		
		FileDetails fileDetails = new FileDetails();

		try {
			LOGGER.info("Inside createAsyncJob file - check tempfile exists or not ");
			file.transferTo(tempFile);
			if (tempFile.exists()) {
				LOGGER.info("uploading file to s3");
				if(!serverName.equalsIgnoreCase(NextworkConstants.LOCAL_HOST)) {
				fileStore.upload(path, fileName, tempFile);
				LOGGER.info("File uploded to s3");
				Files.deleteIfExists(Paths.get(tempFile.getPath()));
				}
				
			} else {
				
				LOGGER.info("Temp file not exists");
				throw new RestBadRequestException("tempfile not exists");
			}
			LocalDateTime  date = LocalDateTime.now();
			fileDetails.setFileName(fileName);
			fileDetails.setVersion(targetId);
			fileDetails.setFilePath(path);
			fileDetails.setCreatedOn(date);
			fileDetails.setCreatedBy(userEmail);
			fileDetails.setUpdatedOn(null);
			fileDetails.setUpdatedBy(null);
			fileDetails.setStatus("In Progress");
			fileDetails.setErrors(null);
			fileDetails.setAction(action);
			fileDetails.setUsername(userName);
			fileDetails.setType(type);
			
			fileRepository.save(fileDetails);
	
		} catch (AmazonServiceException | MongoException | IllegalStateException | IOException e) {
			throw new IllegalStateException("Failed to upload file"+ e.getMessage());
		}

		return fileDetails.getUid();
	}



	


	@Override
	public UploadFileStatusResponseDTO getAsyncUploadFileStatusById(String userEmail, String asyncJobId) {
		return null;
	}


	@Override
	@Async("asyncTaskExecutor")
	@Transactional
	public void processFileById(String userEmail, String targetIdReq, String actionGidList, String asyncJobId,
			String server)  {
		LOGGER.info("Inside Async function : processFileById service" );
		Optional<FileDetails> fileDetailsOps = fileRepository.findById(asyncJobId);

		if (!fileDetailsOps.isPresent()) {
			fileDetailsOps = tryAgainToFindFile(asyncJobId);
		}
		
		LOGGER.info("MMXX: Check-1");
		FileDetails fileDetails = fileDetailsOps.get();
		
		String type=fileDetails.getType();
		String fileName=fileDetails.getFileName();
		String filePath=fileDetails.getFilePath();
		String actionToPerform=fileDetails.getAction();
		String targetId=fileDetails.getVersion();
		
		
		String[] target = type.split("_");
		
		InputStream fileStream = null;
		LOGGER.info("MMXX: Check-2");
		try {			
			
			fileStream = fileStore.download(filePath, fileName);	
			
			StringBuilder errorList=new StringBuilder();
			Map<String, Integer> gidMap = readExelSheetData(fileStream, errorList);
			fileStream.close();
			
			if(target[0].equalsIgnoreCase(ROLE)) {
				LOGGER.info("GIDXX: Inside Role");
				Optional<Roles> roleOpt = newRoleRepository.findById(targetId);
				if(!roleOpt.isPresent())
					throw new RestBadRequestException("No Role found with the given id");
				
				validateAndUpdateGidListForRole(gidMap, actionToPerform, roleOpt.get(), errorList);
			}
			
			
			else if(target[0].equalsIgnoreCase(USER)) {
				
				LOGGER.info("GIDXX: Inside USER userId={}",targetId);
				Optional<NextWorkUser> userOpt = nextWorkUserRepository.findById(targetId);
				if(!userOpt.isPresent())
					throw new RestBadRequestException("No User found with the given id");
				validateAndUpdateGidListForUser(gidMap, actionToPerform, userOpt.get(), errorList);
			}
			
			
			LOGGER.info("GIDXX: processing done");
		
			fileDetails.setStatus("SUCCESS");
			LOGGER.info("Processing completed successfully");
			
		}
		catch(Exception | OutOfMemoryError e) {
			
			fileDetails.setStatus("FAILED");
			fileDetails.setErrors(e.getMessage());
			LOGGER.info("Processing completed **Failed**");
		}
		finally {
			
			if (!server.equalsIgnoreCase(LOCALHOST)) 
				fileStore.delete(fileDetails.getFilePath(), fileDetails.getFileName());
			
			fileDetails.setUpdatedOn(LocalDateTime.now());
			fileDetails.setUpdatedBy(userEmail);
			fileRepository.save(fileDetails);
		}

		
	}
	
	private Optional<FileDetails> tryAgainToFindFile(String asyncJobId) {
		Optional<FileDetails> fileDetails;
		try {
			Thread.sleep(5000l);
		} catch (InterruptedException e) {
			LOGGER.error("Interrupted1 ", e);
			Thread.currentThread().interrupt();
		}

		fileDetails = fileRepository.findById(asyncJobId);
		LOGGER.info("Second upload check : {}", fileDetails.isPresent());
		if (!fileDetails.isPresent()) {
			try {
				Thread.sleep(5000l);
			} catch (InterruptedException e) {
				LOGGER.error("Interrupted2 ", e);
				Thread.currentThread().interrupt();
			}
			fileDetails = fileRepository.findById(asyncJobId);
			LOGGER.info("Third upload check : {}", fileDetails.isPresent());
			if (!fileDetails.isPresent()) {
				try {
					Thread.sleep(5000l);
				} catch (InterruptedException e) {
					LOGGER.error("Interrupted3 ", e);
					Thread.currentThread().interrupt();
				}
				throw new ResourceNotFoundException("File process ID not Found");
			}
		}
		return fileDetails;
	}

	public Map<String, Integer> readExelSheetData(InputStream fileStream,StringBuilder errorList) throws IOException {
		LOGGER.info("Inside read excel processing");
		Map<String, Integer> gidMap=new LinkedHashMap<>();

		StringBuilder invalidGID=new StringBuilder();
		StringBuilder duplicateGID=new StringBuilder();
		
		try(OPCPackage pkg= OPCPackage.open(fileStream)) {
			XSSFWorkbook workbook = new XSSFWorkbook(pkg);
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rows = sheet.iterator();

			validateFirstSecondRow(workbook, rows); 
			
			LOGGER.info("Read Excel dis-2 skipping header");
			while (rows.hasNext()) 
			{
				Row currentRow = rows.next();
				
				Cell currentCell=currentRow.getCell(0);
				int rowNum=currentRow.getRowNum() + 1;

				if(rowNum>5000) {
					throw new RestBadRequestException("Invalid file. File contains more than 5000 records");
				}
				var cellValue="";

				if (currentCell.getCellType().equals(CellType.STRING)) {
					cellValue = currentCell.getStringCellValue().trim();
			
					if(cellValue.trim().isBlank()){
						workbook.close();
						break;
					}
					validateCellValue(gidMap, invalidGID, duplicateGID, rowNum, cellValue);
				}
				else {

					if (invalidGID.length() > 0) {
						invalidGID.append(",");
					}
					invalidGID.append(rowNum);
				}
				
			}

			workbook.close();
			LOGGER.info("GIDXX: Read excel completed");
			checkErrors(errorList, invalidGID, duplicateGID);
		}
		catch(InvalidFormatException | IOException | OutOfMemoryError e) {
			fileStream.close();
			throw new RestBadRequestException("Read excel exception"+ e.getMessage());
		}
		
		fileStream.close();
		return gidMap;
	}


	private void checkErrors(StringBuilder errorList, StringBuilder invalidGID, StringBuilder duplicateGID) {
		if(invalidGID.length()>0) {
			errorList.append("GID must be 8 characters and contain at least 1 alphabet: "+ invalidGID.toString());
			
		}
		
		if(duplicateGID.length()>0) {
			if(errorList.length()>0) {
				errorList.append("; ");
			}
			errorList.append("Duplicate GIDs found: "+ duplicateGID.toString());
		}
	}


	private void validateCellValue(Map<String, Integer> gidMap, StringBuilder invalidGID, StringBuilder duplicateGID,
			int rowNum, String cellValue) {
		if (cellValue.length() != 8) {
			if (invalidGID.length() > 0) {
				invalidGID.append(",");
			}
			invalidGID.append(rowNum);
		}
		else if(gidMap.containsKey(cellValue)) {

			if (duplicateGID.length() > 0) {
				duplicateGID.append(",");
			}
			duplicateGID.append(rowNum);
		}
		else {
			gidMap.put(cellValue.toUpperCase(), rowNum);
		}
	}

	private void validateFirstSecondRow(XSSFWorkbook workbook, Iterator<Row> rows) throws IOException {
		
		if(!rows.hasNext()) {
			workbook.close();
			throw new RestBadRequestException("Excel sheet is blank");
		}
		
		Row firstRow=rows.next(); 
		
		if (firstRow.getCell(0).getCellType().equals(CellType.STRING)) {
			if (firstRow.getCell(0).getStringCellValue().isBlank()) {
				workbook.close();
				throw new RestBadRequestException("Invalid column header. Please use column header as 'GID'.");
			}
			
			if(!rows.hasNext()) {
				workbook.close();
				throw new RestBadRequestException("No records present in excel");
			}
		}
		else {
			throw new RestBadRequestException("Invalid column header.Please use column header as text value like 'GID'");
		}
		
		
		
	}

	private void validateAndUpdateGidListForRole(Map<String, Integer> gidMap, String action, Roles role, StringBuilder errorList) {
		
		if(Objects.nonNull(gidMap)) {
			List<String> gidList=role.getGidList();
			Map<String, Integer> roleGidMap = role.getGidMap();
			if(Objects.isNull(gidList)) {
				gidList = new ArrayList<>();
			}
			List<String> directGidList = role.getDirectGidList();
			if(directGidList == null)
			{
				directGidList = new ArrayList<>();
			}
			if(roleGidMap==null)
				roleGidMap=new HashMap<>();
			
			LOGGER.info("non empty gid list");
			if(action.equalsIgnoreCase("Inclusion")) {
				LOGGER.info("GIDXX: calling addingGID");
				addingNewGID(gidMap, gidList,directGidList, errorList);
				addGidToRoleGidMap(gidMap,roleGidMap);
			}
			
			else if(action.equalsIgnoreCase("Exclusion")) {
				LOGGER.info("GIDEXCLUSION");
				removeGID(gidMap,gidList,directGidList, errorList);
				removeGidFromRoleGidMap(gidMap,roleGidMap);
			}
			else {
				throw new RestBadRequestException("action should be either Inclusion or Exclusion");
			}
			LOGGER.info("GIDXX: setting role object");
			role.setDirectGidList(directGidList);
			role.setGidList(gidList);
			
			role.setGidMap(roleGidMap);
			LOGGER.info("GIDXX: saving to db ROLE");
			newRoleRepository.save(role);
			
		}
		
	}
	
	private void removeGidFromRoleGidMap(Map<String, Integer> gidMap, Map<String, Integer> roleGidMap) {
		int count=0;
		for (Map.Entry<String, Integer> gidReq : gidMap.entrySet()) {
			if (roleGidMap.containsKey(gidReq.getKey())) {
				
				if(roleGidMap.get(gidReq.getKey()) == 1)
					roleGidMap.remove(gidReq.getKey());
				else {
					count=roleGidMap.get(gidReq.getKey());
					count=count-1;
					roleGidMap.put(gidReq.getKey(), count);
				}
			}
		}
	}


	private void addGidToRoleGidMap(Map<String, Integer> gidMap, Map<String, Integer> roleGidMap) {
		
		if(roleGidMap==null)
			roleGidMap=new HashMap<>();
		
		for (Map.Entry<String, Integer> gidReq : gidMap.entrySet()) {
			if (!roleGidMap.containsKey(gidReq.getKey())) {
				roleGidMap.put(gidReq.getKey(), 1);
			} else {
				roleGidMap.put(gidReq.getKey(), gidMap.get(gidReq.getKey()) + 1);
			}
		}
		
	}


	private void validateAndUpdateGidListForUser(Map<String, Integer> gidMap, String action,
			NextWorkUser nextWorkUser, StringBuilder errorList) {
		LOGGER.info("Inside validateAndUpdateGidListForUser");
		if(Objects.nonNull(gidMap)) {
			
			
			LOGGER.info("dis-1");
			List<String> gidList=nextWorkUser.getGidList();
			Map<String, Integer> userGidMap = nextWorkUser.getUserGidMap();
			List<String> directGidList = nextWorkUser.getDirectGidList();
			if(directGidList == null)
			{
				directGidList = new ArrayList<>();
			}
			if(userGidMap==null)
				userGidMap=new HashMap<>();
			
			if(Objects.isNull(gidList)) {
				LOGGER.info("dis-2 creating new arraylist");
				gidList = new ArrayList<>();
			}
			LOGGER.info("non empty gid list");
			if(action.equalsIgnoreCase("Inclusion")) {
				LOGGER.info("GIDXX: calling addingGID");
				addingNewGID(gidMap, gidList, directGidList, errorList);
				addGidToRoleGidMap(gidMap,userGidMap);
				
			}
			
			else if(action.equalsIgnoreCase("Exclusion")) {
				LOGGER.info("GIDEXCLUSION");
				removeGID(gidMap,gidList,directGidList, errorList);
				removeGidFromRoleGidMap(gidMap,userGidMap);
			}
			else {
				throw new RestBadRequestException("action should be either Inclusion or Exclusion");
			}
			
			LOGGER.info("adding to nextWorkUser object ");
			nextWorkUser.setDirectGidList(directGidList);
			nextWorkUser.setGidList(gidList);
			nextWorkUser.setUserGidMap(userGidMap);
			LOGGER.info("saving to db");
			nextWorkUserRepository.save(nextWorkUser);
			
		}
		
	}


	private void removeGID(Map<String, Integer> gidMap, List<String> gidList, List<String> directGidList, StringBuilder error) {
		
		List<String> updatedGidDataList=new ArrayList<>();

		StringBuilder rowNotExist=new StringBuilder();
		StringBuilder rowInvalidGID=new StringBuilder();
		
		for (Map.Entry<String, Integer> gid : gidMap.entrySet()) {
			
			if(gid.getKey().length() != 8) {
				if (rowInvalidGID.length() > 0) {
					rowInvalidGID.append(",");
				}
				rowInvalidGID.append(gid.getValue());
			}
			
			else if(gidList.contains(gid.getKey())) {
				updatedGidDataList.add(gid.getKey());
			}
			else {

				if (rowNotExist.length() > 0) {
					rowNotExist.append(",");
				}
				rowNotExist.append(gid.getValue());
			}
		}
		checkErrorsforGID(error, rowNotExist, rowInvalidGID);
		gidList.removeAll(updatedGidDataList);
		directGidList.removeAll(updatedGidDataList);
		
	}


	private void checkErrorsforGID(StringBuilder error, StringBuilder rowNotExist, StringBuilder rowInvalidGID) {
		LOGGER.info("Inside checkErrorsforGID");
		if(rowNotExist.length()>0) {
			if(error.length()>0) {
				error.append("; ");
			}
			error.append("GID not found in scoping versions: "+ rowNotExist.toString());
			
		}
		LOGGER.info("Inside checkErrorsforGID- next error 2");
		if(rowInvalidGID.length()>0) {
			if(error.length()>0) {
				error.append("; ");
			}
			error.append("GID must be 8 alphanumeric characters: "+ rowInvalidGID.toString());
		}
		LOGGER.info("Inside checkErrorsforGID- final check for errors");
		if(error.length()>0) {
			LOGGER.info("Inside checkErrorsforGID- BAD REquest");
			throw new RestBadRequestException(error.toString());
		}
	}


	private void addingNewGID(Map<String, Integer> gidMap, List<String> gidList, List<String> directGidList, StringBuilder errorList) {
		LOGGER.info("Inside addingNewGID gidList size={}",gidList.size());
		StringBuilder rowNotExist=new StringBuilder();
		StringBuilder rowInvalidGID=new StringBuilder();
		List<String> ids = gidMap.entrySet().stream().map(e -> e.getKey().toUpperCase()).filter(k -> !gidList.contains(k)).toList();
		LOGGER.info("Ids found with size={}",ids.size());
		
		if(ids.isEmpty()) {
			LOGGER.info("found ids as empty so return");
			return;
		}
		LOGGER.info("fecthing scoping data for gids");
		List<IdDTO> scopingList = scopingRepository.findAllIdsByGIDS(ids);
		LOGGER.info("Scoping list size : {}", scopingList.size());
		Map<String, List<IdDTO>> gidScpIdMap = scopingList.stream().collect(Collectors.groupingBy(IdDTO::getGid));
		
		for (Map.Entry<String, Integer> gid : gidMap.entrySet()) {
			
			if(gidList.contains(gid.getKey())) {
				continue;
			}
			if (!gidScpIdMap.containsKey(gid.getKey())) {
				if (rowNotExist.length() > 0) {
					rowNotExist.append(",");
				}
				rowNotExist.append(gid.getValue());
			}
			else {
				LOGGER.info("Add Gid to List");
				gidList.add(gid.getKey());
				directGidList.add(gid.getKey());
			}
			
		}
		LOGGER.info("MMXX:check for errors");
		checkErrorsforGID(errorList, rowNotExist, rowInvalidGID);
		LOGGER.info("MMXX:No errors");
	}


	@Override
	public String saveToolData(String versionId, String action, String userId) {
		FileDetails fileDetails = null;
		Optional<NextWorkUser> user = nextWorkUserRepository.findById(userId);
		if (!user.isPresent())
			throw new RestBadRequestException("USER_NOT_EXIST");
		
		List<FileDetails> toolFile = fileRepository.findAllByVersionAndAction(versionId, action);
		LocalDateTime  date = LocalDateTime.now();
		if(toolFile.isEmpty()) {
			fileDetails = new FileDetails();
			fileDetails.setVersion(versionId);
			fileDetails.setCreatedOn(date);
			fileDetails.setCreatedBy(user.get().getEmail());
			fileDetails.setUpdatedOn(date);
			fileDetails.setUpdatedBy(user.get().getEmail());
			fileDetails.setStatus(NextworkConstants.FILE_UPLOAD_STATUS_IN_PROGRESS);
			fileDetails.setErrors(null);
			fileDetails.setAction(action);
			fileDetails.setUsername(user.get().getName());
			fileDetails.setType("GID");
		}else {
			fileDetails = toolFile.get(0);
			fileDetails.setUpdatedOn(date);
			fileDetails.setUpdatedBy(user.get().getEmail());
			fileDetails.setStatus(NextworkConstants.FILE_UPLOAD_STATUS_IN_PROGRESS);
		}
		fileDetails = fileRepository.save(fileDetails);
		
		return fileDetails.getUid();
	}
	
	
	public String validateUserAndRole(String userEmail) {

		String userName = "";
		Optional<NextWorkUser> findUserByEmail = nextWorkUserRepository.findByUserEmail(userEmail);
		LOGGER.info("user Email:{}", userEmail);
		if (findUserByEmail.isPresent() && findUserByEmail.get().getStatus().equalsIgnoreCase("Active")) {
			NextWorkUser user = findUserByEmail.get();
			userName = user.getName();
			if (null != user.getRolesDetails() && !user.getRolesDetails().isEmpty()) {
				String userRole = user.getRolesDetails().get(0).getRoleType();
				LOGGER.info("NextWORK userRole :{}", userRole);
				if (!userRole.equalsIgnoreCase(RoleType.ADMIN.toString())) {
					throw new RestForbiddenException(
							"User does't have privilege to access this request. Please contact #Nextwork Support Team or ADMIN");
				}
			}
		} else
			throw new ResourceNotFoundException(USER_NOT_EXIST);

		return userName;

	}


	@Override
	public IdResponseDTO saveAsyncFileByType(MultipartFile mFile, String userEmail, String serverName, String type, String action) {
		userService.checkAdminUserRole(userEmail);
		Optional<NextWorkUser> nxtUser = userService.findByUserEmail(userEmail);
		if (!nxtUser.isPresent())
			throw new RestBadRequestException(NextworkConstants.USER_NOT_EXIST);

		if (mFile.isEmpty()) {
			throw new IllegalStateException(NextworkConstants.CANNOT_UPLOAD_EMPTY_FILE);
		}
		
		String fileName = mFile.getOriginalFilename();
		LOGGER.info("file Name : {}", fileName);
		if (!StringUtils.isAllBlank(fileName) && fileName != null) {
			String fileFormate = fileName.substring(fileName.lastIndexOf(".")+1);
			LOGGER.info("file format : {}", fileFormate);
			if (!(fileFormate.equals(NextworkConstants.FILE_TYPE_XLSX))) {
				throw new IllegalStateException("File format Not Supported");
			}
			long threeMb =(long)  3 * 1024 * 1024;
			if(mFile.getSize() > threeMb) {
				throw new IllegalStateException("File Size is exceded and the file size not more than 3MB.");
			}
		}
		
	    FileDetails	fileDetails = saveFileAndUploadDetails(mFile, null, nxtUser.get(), action, serverName, type, NextworkConstants.FILE_TYPE_XLSX);
	    return IdResponseDTO.builder().uid(fileDetails.getUid()).build();
	}

}
