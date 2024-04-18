package com.siemens.nextwork.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
public class BucketName {

	@Value("${s3bucket.app-bucket-name}")
	public String s3bucketName;

	public String getBucketName() {
		return this.s3bucketName;
	}
}

