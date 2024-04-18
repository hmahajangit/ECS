package com.siemens.nextwork.admin.validator;

import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import com.siemens.nextwork.admin.dto.RequestAccessDTO;
import com.siemens.nextwork.admin.exception.RestBadRequestException;
import com.siemens.nextwork.admin.service.RequestAccessService;

@Component
public class RequestAccessValidator {

	@Autowired
	RequestAccessService requestAccessService;
	
	public static final String EMAIL = "email";

	public void validate(RequestAccessDTO request, Errors errors) {
		validatePurpose(errors, request);

		if (Objects.isNull(request.getEmail()) || request.getEmail().trim().isEmpty()) {
			errors.rejectValue(EMAIL,HttpStatus.BAD_REQUEST.toString(), "email cannot be blank");
		} else {
			isValidEmail(request.getEmail(), errors);
		}
	}

	private void isValidEmail(String email, Errors errors) {
		String emailFirstPart = StringUtils.substringBefore(email, "@").trim();
		String emailSecondPart = "@" + StringUtils.substringAfter(email, "@").trim();
		String emailFirstPartRegex = "^[a-zA-Z0-9-_.]+";
		Pattern emailPattern = Pattern.compile(emailFirstPartRegex);
		if (!StringUtils.endsWithIgnoreCase(emailSecondPart, "@siemens.com"))
			errors.rejectValue(EMAIL,HttpStatus.BAD_REQUEST.toString(),
					"Email domain not supported. Only @siemens.com is supported");
		else if (!emailPattern.matcher(emailFirstPart).matches())
			errors.rejectValue(EMAIL,HttpStatus.BAD_REQUEST.toString(), "This email id is not supported.");
	}

	private void validatePurpose(Errors errors, RequestAccessDTO request) {
		if (Objects.isNull(request.getPurpose()) || request.getPurpose().trim().isEmpty()) {
			errors.rejectValue("purpose",HttpStatus.BAD_REQUEST.toString(), "purpose cannot be blank");
		} else if (request.getPurpose().length() > 256) {
			errors.rejectValue("purpose",HttpStatus.BAD_REQUEST.toString(), "purpose can have only 256 characters");
		}
	}

	public void parseErrors(BindingResult bindingResult) {
		StringBuilder errorMsg = new StringBuilder();
		String prefix = "";
		for (ObjectError object : bindingResult.getAllErrors()) {
			if (object instanceof FieldError fieldError) {
				errorMsg.append(prefix);
				prefix = ",";
				fieldError = (FieldError) object;
				errorMsg.append(fieldError.getDefaultMessage());
			}
		}
		throw new RestBadRequestException(errorMsg.toString());
	}
}
