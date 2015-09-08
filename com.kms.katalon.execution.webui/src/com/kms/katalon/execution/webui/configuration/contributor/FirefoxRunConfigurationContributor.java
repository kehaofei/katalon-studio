package com.kms.katalon.execution.webui.configuration.contributor;

import java.util.Map;

import com.kms.katalon.core.webui.driver.WebUIDriverType;
import com.kms.katalon.entity.testcase.TestCaseEntity;
import com.kms.katalon.entity.testsuite.TestSuiteEntity;
import com.kms.katalon.execution.configuration.contributor.IRunConfigurationContributor;
import com.kms.katalon.execution.entity.IRunConfiguration;
import com.kms.katalon.execution.webui.configuration.FirefoxRunConfiguration;

public class FirefoxRunConfigurationContributor implements IRunConfigurationContributor {

	@Override
	public String getId() {
		return WebUIDriverType.FIREFOX_DRIVER.toString();
	}

	@Override
	public IRunConfiguration getRunConfiguration(TestCaseEntity testCase, Map<String, String> runInput) {
		return new FirefoxRunConfiguration(testCase);
	}

	@Override
	public IRunConfiguration getRunConfiguration(TestSuiteEntity testSuite, Map<String, String> runInput) {
		return new FirefoxRunConfiguration(testSuite);
	}

}