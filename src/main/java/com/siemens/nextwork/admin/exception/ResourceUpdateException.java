package com.siemens.nextwork.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceUpdateException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceUpdateException(String message) {
		super(message);
	}

}