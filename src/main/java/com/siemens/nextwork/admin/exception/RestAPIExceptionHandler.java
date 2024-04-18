package com.siemens.nextwork.admin.exception;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.nextwork.admin.util.NextworkConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * {@linkplain RestController} for mapping error url and respond with
 * {@linkplain ExceptionResponse}
 *
 * @author Z00427KA
 * @since Aug 7, 2019
 */
@ControllerAdvice
@Slf4j
public class RestAPIExceptionHandler extends ResponseEntityExceptionHandler {

    public RestAPIExceptionHandler() {
        log.info("RestAPIExceptionHandler: Initialized...[OK]");
    }

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ExceptionResponse> handleAllExceptions(Exception ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), NextworkConstants.METHOD_NOT_SUPPORTED,
        		NextworkConstants.METHOD_NOT_SUPPORTED);
        ex.printStackTrace();
        return new ResponseEntity<>(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ResponseBody
    @ExceptionHandler(RestClientResponseException.class)
    public final ResponseEntity<ExceptionResponse> handleRestClientResponseException(RestClientResponseException ex,
            WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Rest client response exception",
        		"Rest client response exception");
        return new ResponseEntity<>(exceptionResponse, HttpStatus.valueOf(ex.getRawStatusCode()));
    }

    @ResponseBody
    @ExceptionHandler(RestBadRequestException.class)
    public final ResponseEntity<ExceptionResponse> handleRestBadRequestException(RestBadRequestException ex,
            WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Bad Request", ex.getMessage());
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    
    @ResponseBody
    @ExceptionHandler(ResourceNotFoundException.class)
    public final ResponseEntity<ExceptionResponse> handleResourceNotFoundException(Exception ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Resource not found", ex.getMessage());
        return new ResponseEntity<>(exceptionResponse, HttpStatus.NOT_FOUND);
    }

    @ResponseBody
    @ExceptionHandler(ResourceUpdateException.class)
    public final ResponseEntity<ExceptionResponse> handleResourceUploadException(Exception ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Resource not Updated", "Resource not Updated");
        return new ResponseEntity<>(exceptionResponse, HttpStatus.CONFLICT);
    }
    
    @ResponseBody
    @ExceptionHandler(RestForbiddenException.class)
    public final ResponseEntity<ExceptionResponse> handleRestForbiddenException(Exception ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Resource Forbidden", ex.getMessage());
        return new ResponseEntity<>(exceptionResponse, HttpStatus.FORBIDDEN);
    }
    
    @ResponseBody
    @ExceptionHandler(RestTooManyRequestsException.class)
    public final ResponseEntity<ExceptionResponse> handleRestTooManyRequestsException(Exception ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Too Many Requests", "Too Many Requests");
        return new ResponseEntity<>(exceptionResponse, HttpStatus.TOO_MANY_REQUESTS);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        BindingResult result = ex.getBindingResult();
        Map<String, String> fieldErrorMap = result.getFieldErrors().stream().collect(
                Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (s, s2) -> s + "," + s2));
        ObjectMapper mapper = new ObjectMapper();
        String builder = "";
        try {
            builder = mapper.writeValueAsString(fieldErrorMap);
        } catch (JsonProcessingException e) {
            log.error("", e);
        }
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Validation Failed", builder);
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), NextworkConstants.METHOD_NOT_SUPPORTED,
        		NextworkConstants.METHOD_NOT_SUPPORTED);
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Media Type not supported",
        		"Media Type not supported");
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Media Type not accepted",
        		"Media Type not accepted");
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Missing path variable",
        		"Missing path variable");
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Missing request parameter",
                ex.getMessage());
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Message Not Readable",
                "Bad Request Input");
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Message Not Writable",
        		"Message Not Writable");
		return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
	}

    @ResponseBody
    @ExceptionHandler(RestOkException.class)
    public final ResponseEntity<ExceptionResponse> handleRestOkException(RestOkException ex,
            WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), "Ok", ex.getMessage());
        return new ResponseEntity<>(exceptionResponse, HttpStatus.OK);
    }
}
