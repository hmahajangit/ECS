package com.siemens.nextwork.admin.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.siemens.nextwork.admin.exception.RestBadRequestException;

@Component
public class CommonUtils {

	private CommonUtils() {
		super();
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonUtils.class);

	public static void parseErrors(BindingResult bindingResult) {
		StringBuilder errorMsg = new StringBuilder();
		String prefix = "";
		for (ObjectError object : bindingResult.getAllErrors()) {
			errorMsg.append(prefix);
			prefix = ",";
			errorMsg.append(object.getDefaultMessage());
		}
		throw new RestBadRequestException(errorMsg.toString());
	}

	public static String getEmailId(String authorizationHeader) {
		try {
			String authToken = authorizationHeader.substring(7);
			JWT jwt = JWTParser.parse(authToken);
			JWTClaimsSet jwtClaimSet = jwt.getJWTClaimsSet();
			return jwtClaimSet.getClaims().get(NextworkConstants.EMAIL).toString();
		} catch (Exception e) {
			throw new RestBadRequestException(NextworkConstants.EXCEPTION_EXTRACTING_TOKEN + e);
		}
	}
	
	public static String getAuthorName(String authorizationHeader) {
		try {
			String authToken = authorizationHeader.substring(7);
			JWT jwt = JWTParser.parse(authToken);
			JWTClaimsSet jwtClaimSet = jwt.getJWTClaimsSet();
			
			String firstName = jwtClaimSet.getClaims().get(NextworkConstants.GIVEN_NAME).toString();
			String lastName = jwtClaimSet.getClaims().get(NextworkConstants.FAMILY_NAME).toString();
			return firstName + " " + lastName;
		} catch (Exception e) {
			throw new RestBadRequestException(NextworkConstants.EXCEPTION_EXTRACTING_TOKEN + e);
		}
	}

	public static JWTClaimsSet getJwtClaimSet(String authorizationHeader) {
		try {
			String authToken = authorizationHeader.substring(7);
			JWT jwt = JWTParser.parse(authToken);
			return jwt.getJWTClaimsSet();
		} catch (Exception e) {
			throw new RestBadRequestException(NextworkConstants.EXCEPTION_EXTRACTING_TOKEN + e);
		}
	}

	public static <T> T toBean(Map<String, Object> beanPropMap, Class<T> type) {
		try {
			T beanInstance = type.getConstructor().newInstance();
			for (Object k : beanPropMap.keySet()) {
				String key = (String) k;
				Object value = beanPropMap.get(k);
				if (value != null) {
					try {
						Field field = type.getDeclaredField(key);
						field.setAccessible(true);
						field.set(beanInstance, value);
						field.setAccessible(false);
					} catch (Exception e) {
						e.printStackTrace();

					}
				}
			}
			return beanInstance;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean allNull(Object target) {
		return Arrays.stream(target.getClass().getDeclaredFields())
				.peek(f -> f.setAccessible(true))
				.map(f -> getFieldValue(f, target))
				.allMatch(ObjectUtils::isEmpty);
	}
	


	private static Object getFieldValue(Field field, Object target) {
		try {
			return field.get(target);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getNullFileds(Object target, List<String> elminates) {
		List<String> fields = new ArrayList<>();
		try {
		    for (Field f : target.getClass().getDeclaredFields()) {
		    	if(elminates.contains(f.getName())) continue;
		    	f.setAccessible(true);
				if (f.get(target) == null || f.get(target) == "")
				    fields.add(f.getName());
		    }
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	    return String.join(",", fields);            
	}
	
	public static void printMemoryDetails() {
		Runtime runTime = Runtime.getRuntime();
		LOGGER.info("#################*******MEMORY********#################");
		LOGGER.info("Max Memory : {}", runTime.maxMemory());
		LOGGER.info("Total Memory : {}", runTime.totalMemory());
		LOGGER.info("Free Memory : {}", runTime.freeMemory());
		LOGGER.info("Used Memory : {}", (runTime.totalMemory()- runTime.freeMemory()));
		LOGGER.info("################################################");
	}

	public static Pattern getRegexPattern(String regex) {
		return Pattern.compile(regex);
	}
	public static Boolean validateVersion(String version) {
		Pattern p = getRegexPattern( "^[0-9]{8}$");
		return p.matcher(version).matches();
	}

	public static Boolean validateGid(String version) {
		Pattern p1 = getRegexPattern("^(?=.*?[a-zA-Z])(?=.*?[0-9])(?!.*[<>'\"/;`%@#@#$^*()_+={}?\\[\\]]).{8}$");
		Pattern p2 = getRegexPattern( "^[a-zA-Z]{8}$");
		return p1.matcher(version).matches() || p2.matcher(version).matches();
	}

	public static List<String> getMySkillCategory(){
		List<String> categories = new ArrayList<>();
		categories.add("Interpersonal & Personal");
		categories.add("Leadership");
		categories.add("Technology & Market");
		categories.add("Function & Methods");
		return categories;
	}
}
