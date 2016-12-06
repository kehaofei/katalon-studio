package com.kms.katalon.core.webui.keyword;

import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.exception.StepFailedException
import com.kms.katalon.core.keyword.AbstractKeyword;
import com.kms.katalon.core.keyword.SupportLevel;
import com.kms.katalon.core.logging.KeywordLogger;
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webui.common.ScreenUtil;
import com.kms.katalon.core.webui.exception.WebElementNotFoundException
import org.openqa.selenium.WebElement
import com.kms.katalon.core.webui.common.WebUiCommonHelper
import groovy.transform.CompileStatic

public abstract class WebUIAbstractKeyword extends AbstractKeyword {
	protected static ScreenUtil screenUtil = new ScreenUtil();
    
	@Override
	public SupportLevel getSupportLevel(Object ...params) {
		return SupportLevel.NOT_SUPPORT;
	}
    
    @CompileStatic
    public static WebElement findWebElement(TestObject to, int timeOut = RunConfiguration.getTimeOut()) throws IllegalArgumentException, WebElementNotFoundException, StepFailedException {
        return WebUiCommonHelper.findWebElement(to, timeOut);
    }

    @CompileStatic
    public static List<WebElement> findWebElements(TestObject to, int timeOut) throws WebElementNotFoundException {
        return WebUiCommonHelper.findWebElements(to, timeOut);
    }
}