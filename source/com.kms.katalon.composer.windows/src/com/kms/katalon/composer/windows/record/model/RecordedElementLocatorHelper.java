package com.kms.katalon.composer.windows.record.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.kms.katalon.composer.windows.element.CapturedWindowsElement;
import com.kms.katalon.entity.repository.WindowsElementEntity.LocatorStrategy;

public class RecordedElementLocatorHelper {

    private WindowsRecordedElement recordedElement;

    private WindowsRecordedPayload payload;

    private LocatorStrategy locatorStrategy;
    
    private String locator;

    public RecordedElementLocatorHelper(WindowsRecordedPayload payload) {
        this.payload = payload;
        this.recordedElement = payload.getElement();
    }
    
    public CapturedWindowsElement getCapturedElement() {
        CapturedWindowsElement element = new CapturedWindowsElement();
        element.setTagName(getTitleCaseName(recordedElement.getType()));
        element.setName(getTitleCaseName(recordedElement.getType()));
        element.setProperties(recordedElement.getAttributes());
        element.getProperties().put("XPath", buildXPath());
        
        buildXPathLocator();
        
        element.setLocator(locator);
        element.setLocatorStrategy(locatorStrategy);
        
        return element;
    }

    private void buildLocator() {
        Map<String, String> attributes = recordedElement.getAttributes();
        if (attributes == null || attributes.isEmpty()) {
            locatorStrategy = LocatorStrategy.XPATH;
            locator = "";
            return;
        }
        
        String name = attributes.get("Name");
        if (StringUtils.isNotEmpty(name)) {
            locatorStrategy = LocatorStrategy.NAME;
            locator = name;
            return;
        }

//        String automationId = attributes.get("AutomationId");
//        if (StringUtils.isNotEmpty(automationId)) {
//            locatorStrategy = LocatorStrategy.ACCESSIBILITY_ID;
//            locator = automationId;
//            return;
//        }
        
        locatorStrategy = LocatorStrategy.XPATH;
        locator = buildXPath();
    }
    
    private void buildXPathLocator() {
        Map<String, String> attributes = recordedElement.getAttributes();
        if (attributes == null || attributes.isEmpty()) {
            locatorStrategy = LocatorStrategy.XPATH;
            locator = "";
            return;
        }
        
        locatorStrategy = LocatorStrategy.XPATH;
        locator = buildXPath();
    }

    private String buildXPath() {
        StringBuilder sb = new StringBuilder("/");
        List<WindowsRecordedElement> elements = payload.getParent();
        for (int i = 0; i < elements.size(); i++) {
            WindowsRecordedElement p = elements.get(i);
            if (p.getAttributes() == null || p.getAttributes().isEmpty()) {
                continue;
            }
            sb.append(buildPartialIndexBasedXPath(p, i == 1));
            sb.append("/");
        }
        sb.append(buildPartialIndexBasedXPath(recordedElement, false));
        return sb.toString();
    }
    
    private String buildPartialXPath(WindowsRecordedElement e, boolean isMainWindow) {
        String type = getTitleCaseName(e.getType());
        
        if (isMainWindow) {
            return type;
        }
        Map<String, String> attributes = e.getAttributes();
        String automationId = attributes.get("AutomationId");
        String className = attributes.get("ClassName");
        String name = attributes.get("Name");
        
        if (StringUtils.isEmpty(automationId) && StringUtils.isEmpty(className) && StringUtils.isEmpty(name)) {
            String elementIndex = attributes.get("ElementIndex");
            if (attributes.containsKey("ElementIndex") && Integer.parseInt(attributes.get("ElementIndex")) > 0) {
                return type + "[" + elementIndex + "]";
            }
            return type;
        }
        
        if (StringUtils.isNotEmpty(automationId)) {
            return String.format("%s[@AutomationId = \"%s\"]", type, automationId);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(type + "[");
        String predicate = "";
        
        if (StringUtils.isNotEmpty(className)) {
            predicate = String.format("@ClassName = \"%s\"", className);
            sb.append(predicate);
        }
        
        if (StringUtils.isNotEmpty(name)) {
            if (StringUtils.isNotEmpty(predicate)) {
                sb.append(" and ");
            }
            sb.append(String.format("@Name = \"%s\"", name));
        }

        sb.append("]");
        return sb.toString();
    }

    private String buildPartialIndexBasedXPath(WindowsRecordedElement element, boolean isMainWindow) {
        String type = getTitleCaseName(element.getType());

        if (isMainWindow) {
            return type;
        }

        Map<String, String> attributes = element.getAttributes();

        String elementIndex = attributes.get("ElementIndex");
        if (attributes.containsKey("ElementIndex") && Integer.parseInt(attributes.get("ElementIndex")) > 0) {
            return type + "[" + elementIndex + "]";
        }
        return type;
    }

    private String getTitleCaseName(String name) {
        return toTitleCase(name).replace(" ", "");
    }

    private static String toTitleCase(String inputString) {
        if (StringUtils.isBlank(inputString)) {
            return "";
        }
 
        if (StringUtils.length(inputString) == 1) {
            return inputString.toUpperCase();
        }
 
        StringBuffer resultPlaceHolder = new StringBuffer(inputString.length());
 
        Stream.of(inputString.split(" ")).forEach(stringPart -> {
            char[] charArray = stringPart.toCharArray();
            charArray[0] = Character.toUpperCase(charArray[0]);
            resultPlaceHolder.append(new String(charArray)).append(" ");
        });
 
        return StringUtils.trim(resultPlaceHolder.toString());
    }
}