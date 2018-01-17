package com.kms.katalon.selenium.ide.format;

import org.apache.commons.lang3.StringUtils;

import com.kms.katalon.selenium.ide.model.Command;
import com.kms.katalon.selenium.ide.util.ClazzUtils;

public interface Formatter {

	public String format(Command command);
	
	public default String stringValue(String value) {
		if (StringUtils.isNotBlank(value)) {
			value = value.replace("\"", "'");
		}
		return "\"" + value + "\"";
	}
	
	public default String paramOf(String param) {
		if (StringUtils.isNotBlank(param)) {
			param = param.trim();
			
			if (param.contains("${")) {
				param = param.replace("${", "+");
				param = param.replace("}", "+");
				
				if (param.startsWith("+")) {
					param = param.replaceFirst("\\+", "");
				} else {
					param =  "\"" + param;
					param = param.replaceFirst("\\+", "\" + ");
				}
				
				if (param.endsWith("+")) {
					param = param.substring(0, param.lastIndexOf("+"));
				} else {
					String tail = param.substring(param.lastIndexOf("+"), param.length());
					param = param.substring(0, param.indexOf("+")) + " + \""+ tail + "\"";
				}
			} 
			
			if (param.startsWith("KEY_")) {
				param = param.replace("KEY_", "");
				param = "Keys." + param;
			}
		}
		
		return param;
	}
	
	public default String valueOf(String param) {
		if (StringUtils.isNotBlank(param) && param.contains("${")) {
			return paramOf(param);
		}
		return stringValue(param);
	}
	
	public default String getPattern(String suffixMethodName, String target, String value) {
		boolean hasParam = ClazzUtils.hasParam("get" + suffixMethodName);
		return hasParam ? value : target;
	}
	
	public default boolean isMatching(String pattern) {
		return pattern.contains("*") || pattern.startsWith("glob:") || pattern.startsWith("regrex:");
	}
	
	public default String toPattern(String userPattern) {
		if (userPattern.startsWith("glob:")) {
			userPattern = userPattern.replace("glob:", "");
			return com.kms.katalon.selenium.ide.util.StringUtils.convertGlobToRegex(userPattern);
		}
		if (userPattern.startsWith("regex:")) {
			userPattern = userPattern.replace("regex:", "");
		}
		return userPattern.replaceAll("\\*", ".*");
	}
	
	public default String conditionWithMatchingOrNot(String suffixMethodName, String target, String value) throws Exception {
		String cleanMethodName = getCleanCommandTail(suffixMethodName);
		String method = getNormalMethod(cleanMethodName, target);
		String pattern = getPattern(cleanMethodName, target, value);
		String ret = "";
		if (isMatching(pattern)) {
			ret = method + ".matches(\"" + toPattern(pattern) + "\")";
		} else {
			ret = "\"" + pattern + "\", " + method + "";
		}
		return ret;
	}
	
	public default String conditionWithMatchingOrNotForWaitFor(String suffixMethodName, String target, String value) throws Exception {
		String cleanMethodName = getCleanCommandTail(suffixMethodName);
		String method = getNormalMethod(cleanMethodName, target);
		String pattern = getPattern(cleanMethodName, target, value);
		String ret = "";
		if (isMatching(pattern)) {
			ret = method + ".matches(\"" + toPattern(pattern) + "\")";
		} else {
			ret = "\"" + pattern + "\".equals(" + method + ")";
		}
		return ret;
	}
	
	public default String getCleanCommandTail(String commandTail) {
		String method = commandTail.replace("Not", "");
		return method.replace("AndWait", "");
	}
	
	public default String getWaitIfHas(String commandTail) {
		if (commandTail.lastIndexOf("AndWait") != -1) {
			return "\nselenium.andWait()";
		}
		return null;
	}
	
	public default String getParamName(String method, String target, String value) {
		boolean hasParam = ClazzUtils.hasParam(getCleanCommandTail(method));
		if (hasParam) {
			return paramOf(value);
		} 
		return paramOf(target);
	}
	
	public default String getParamMethod(String method, String target) throws Exception {
		boolean hasMethod = ClazzUtils.hasMethod(method);
		if (!hasMethod) {
			throw new Exception("Method is not found");
		}
		boolean hasParam = ClazzUtils.hasParam(method);
		if (hasParam) {
			return "(" + valueOf(target) + ")";
		} 
		return "()";
	}
	
	public default String getNormalMethod(String commandTail, String target) throws Exception {
		String methodName = getCleanCommandTail(commandTail);
		String param = getParamMethod("get" + methodName, target);
		boolean isArray = ClazzUtils.isArrayReturned(methodName);
		String method = methodName + param;
		return isArray ? "join(selenium.get" + method + ", ',')" : "selenium.get" + method;
	}
	
	public default String getBoolMethod(String commandTail, String target) throws Exception {
		String methodName = getCleanCommandTail(commandTail);
		String param = getParamMethod("is" + methodName + "Present", target);
		return "selenium.is" + methodName + "Present" + param;
	}
	
	public default String getBoolCheckedMethod(String commandTail, String target) throws Exception {
		String methodName = getCleanCommandTail(commandTail);
		String param = getParamMethod("is" + methodName, target);
		return "selenium.is" + methodName + param;
	}
	
	public default String getBoolWhetherMethod(String commandTail, String target, String value) throws Exception {
		String methodName = getCleanCommandTail(commandTail);
		int paramCount = ClazzUtils.getParamCount("get" + methodName);
		StringBuffer params = new StringBuffer();
		params.append("(");
		if (paramCount == 2) {
			params.append("\"" + target + "\"");
			params.append(", \"" + value + "\"");
		} else if (paramCount == 1){
			params.append("\"" + target + "\"");
		}
		params.append(")");
		return "selenium.get" + methodName + params.toString();
	}
}
