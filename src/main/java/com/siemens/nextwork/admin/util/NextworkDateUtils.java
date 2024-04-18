package com.siemens.nextwork.admin.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.springframework.stereotype.Component;

@Component
public class NextworkDateUtils {

//	Method to convert date format to another String date format	
	public String getFormattedDate(Date createdOn) {
		LocalDateTime creationDate=LocalDateTime.ofInstant(createdOn.toInstant(), ZoneId.systemDefault());
		DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	    return creationDate.format(myFormatObj);
	}
}
