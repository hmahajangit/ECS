package com.siemens.nextwork.admin.service;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class FileStore {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FileStore.class);
	
	@Autowired
    private final AmazonS3 amazonS3;

	
	public Boolean upload(String bucketName, String pathKey, File file) {
		try {
		  PutObjectResult resp = amazonS3.putObject(bucketName, pathKey, file );
		  return resp.isRequesterCharged();
		} catch (AmazonServiceException e ) {
			throw new IllegalStateException("FileStore:Failed to upload the file : "+ e.getMessage());
		}
	}

	
	public InputStream download(String path, String key) {
      try {
          S3Object object = amazonS3.getObject(path, key);
        
          return object.getObjectContent();
      } catch (AmazonServiceException e) {
          throw new IllegalStateException("Failed to download the file "+e.getLocalizedMessage(), e);
      }
  }
    
    public String delete(String path, String key) {
    	
        try {
            amazonS3.deleteObject(path, key);
            return key+" Deleted Successfully from S3 Bucket";
        } catch (AmazonServiceException e) {
        	return "File not found in S3 Bucket";
        }
    }
    
    public void deleteDirectory(String bucketName, String prefix) {
    	LOGGER.info("Bucket Name : {} and key : {} ", bucketName, prefix);
    	boolean isExists = amazonS3.doesObjectExist(bucketName, prefix);
    	LOGGER.info("IS File Exists : {}", isExists);
		ObjectListing objectList = this.amazonS3.listObjects( bucketName, prefix );
        List<S3ObjectSummary> objectSummeryList =  objectList.getObjectSummaries();
        for( S3ObjectSummary summery : objectSummeryList ) {
        	LOGGER.info("Key : {} ", summery.getKey());
            delete(bucketName, summery.getKey());
        }
    }

    public String copyBucketObject (String fromBucket, String srcKey, String toBucket, String toKey) {
        CopyObjectRequest copyReq = new CopyObjectRequest(fromBucket, srcKey, toBucket, toKey);
        CopyObjectResult copyRes = amazonS3.copyObject(copyReq);
        return copyRes.toString();
    }

}
