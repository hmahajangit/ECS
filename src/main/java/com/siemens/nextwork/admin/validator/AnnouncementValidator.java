package com.siemens.nextwork.admin.validator;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import com.siemens.nextwork.admin.dto.AnnouncementDTO;

@Component
public class AnnouncementValidator {

	public void validate(Object target, Errors errors) {
		AnnouncementDTO request = (AnnouncementDTO) target;

		if ((Objects.isNull(request.getTitle()) || request.getTitle().trim().isEmpty())) {
			errors.rejectValue("title",HttpStatus.BAD_REQUEST.toString(), "title cannot be blank");
		}
		if ((Objects.isNull(request.getDescription()) || request.getDescription().trim().isEmpty())) {
			errors.rejectValue("description",HttpStatus.BAD_REQUEST.toString(), "description cannot be blank");
		}
	}
}
