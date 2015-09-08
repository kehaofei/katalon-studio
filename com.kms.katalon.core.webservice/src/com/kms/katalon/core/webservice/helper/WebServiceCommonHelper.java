package com.kms.katalon.core.webservice.helper;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import com.kms.katalon.core.logging.KeywordLogger;
import com.kms.katalon.core.testobject.RequestObject;
import com.kms.katalon.core.testobject.ResponseObject;
import com.kms.katalon.core.webservice.constants.StringConstants;

public class WebServiceCommonHelper {
	public static final KeywordLogger logger = KeywordLogger.getInstance();

	public static void checkRequestObject(RequestObject requestObject) throws IllegalArgumentException {
		logger.logInfo(StringConstants.KW_LOG_INFO_CHECKING_REQUEST_OBJECT);
		if (requestObject == null) {
			throw new IllegalArgumentException(StringConstants.KW_LOG_FAILED_REQUEST_OBJECT_IS_NULL);
		}
	}
	
	public static void checkResponseObject(ResponseObject responseObject) throws IllegalArgumentException {
		logger.logInfo(StringConstants.KW_LOG_INFO_CHECKING_RESPONSE_OBJECT);
		if (responseObject == null) {
			throw new IllegalArgumentException(StringConstants.KW_LOG_FAILED_RESPONSE_OBJECT_IS_NULL);
		}
	}
	
	public static void checkResponseObjectContent(ResponseObject responseObject) throws Exception {
		logger.logInfo(StringConstants.KW_LOG_INFO_CHECKING_RESPONSE_OBJECT_CONTENT);
		if (responseObject.getResponseBodyContent() == null) {
			throw new IllegalArgumentException(StringConstants.KW_LOG_FAILED_RESPONSE_OBJECT_CONTENT_IS_NULL);
		}
	}
	
	public static Object parseAndExecuteExpressionForXml(String locator, String groovyFunction, String xmlText){
		String[] tokens = locator.split("\\.");
		String rootName = "";
		String locatorExp = "";
		for(int i=0; i<tokens.length; i++){
			String token = tokens[i];
			locatorExp += token;
			if(i < tokens.length -1){
				locatorExp += ".";
			}
			else if(i == tokens.length -1){
				locatorExp += "." + groovyFunction;
			}
			if(i==0){
				rootName = token;
			}
		}
		StringBuilder groovyScript = new StringBuilder();
		groovyScript.append("def "+ rootName +" = new XmlSlurper().parseText(xmlText);");
		groovyScript.append("return " + locatorExp);

		Binding binding = new Binding();
		binding.setVariable("xmlText", xmlText);
		GroovyShell shell = new GroovyShell(binding);
		return shell.evaluate(groovyScript.toString());
	}

	public static Object parseAndGetPropertyValueForXml(String locator, String xmlText){
		String[] tokens = locator.split("\\.");
		String rootName = "";
		String locatorExp = "";
		for(int i=0; i<tokens.length; i++){
			String token = tokens[i];
			//locatorExp += token;
			locatorExp += ((i == tokens.length -1) && !token.startsWith("@")? "@" : "") + token;
			if(i < tokens.length -1){
				locatorExp += ".";
			}
			if(i==0){
				rootName = token;
			}
		}
		StringBuilder groovyScript = new StringBuilder();
		groovyScript.append("def "+ rootName +" = new XmlSlurper().parseText(xmlText);");
		groovyScript.append("return " + locatorExp);

		Binding binding = new Binding();
		binding.setVariable("xmlText", xmlText);
		GroovyShell shell = new GroovyShell(binding);
		return shell.evaluate(groovyScript.toString());
	}

	public static Object parseAndExecuteExpressionForJson(String locator, String groovyFunction, String jsonText){
		String[] tokens = locator.split("\\.");
		String locatorExp = "";
		for(int i=0; i<tokens.length; i++){
			String token = tokens[i];
			locatorExp += token;
			if(i < tokens.length -1){
				locatorExp += ".";
			}
			else if(i == tokens.length -1){
				locatorExp += "." + groovyFunction;
			}
		}
		StringBuilder groovyScript = new StringBuilder();
		groovyScript.append("import groovy.json.JsonSlurper;");
		groovyScript.append("def root = new JsonSlurper().parseText(jsonText);");
		groovyScript.append("return root." + locatorExp +";");

		Binding binding = new Binding();
		binding.setVariable("jsonText", jsonText);
		GroovyShell shell = new GroovyShell(binding);
		return shell.evaluate(groovyScript.toString());
	}

	public static Object parseAndGetPropertyValueForJson(String locator, String jsonText){
		String[] tokens = locator.split("\\.");
		String locatorExp = "";
		for(int i=0; i<tokens.length; i++){
			String token = tokens[i];
			locatorExp += token;
			if(i < tokens.length -1){
				locatorExp += ".";
			}
		}
		StringBuilder groovyScript = new StringBuilder();
		groovyScript.append("import groovy.json.JsonSlurper;");
		groovyScript.append("def root = new JsonSlurper().parseText(jsonText);");
		groovyScript.append("return root." + locatorExp +";");

		Binding binding = new Binding();
		binding.setVariable("jsonText", jsonText);
		GroovyShell shell = new GroovyShell(binding);
		return shell.evaluate(groovyScript.toString());
	}

}