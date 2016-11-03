package com.kms.katalon.composer.integration.jira.report;

import java.io.IOException;

import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.report.parts.integration.ReportTestCaseIntegrationViewBuilder;
import com.kms.katalon.composer.report.parts.integration.TestCaseLogColumnIntegrationView;
import com.kms.katalon.composer.report.parts.integration.TestCaseLogDetailsIntegrationView;
import com.kms.katalon.core.logging.model.TestSuiteLogRecord;
import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.entity.report.ReportEntity;
import com.kms.katalon.integration.jira.setting.JiraIntegrationSettingStore;

public class JiraReportIntegrationBuilder implements ReportTestCaseIntegrationViewBuilder {

    @Override
    public TestCaseLogDetailsIntegrationView getIntegrationDetails(ReportEntity report,
            TestSuiteLogRecord testSuiteLogRecord) {
        return null;
    }

    @Override
    public TestCaseLogColumnIntegrationView getIntegrationColumn(ReportEntity report) {
        return new JiraReportTestCaseColumnView(report);
    }

    @Override
    public int getPreferredOrder() {
        return 1;
    }

    @Override
    public boolean isIntegrationEnabled(ProjectEntity project) {
        try {
            return new JiraIntegrationSettingStore(project.getFolderLocation()).isIntegrationEnabled();
        } catch (IOException e) {
            LoggerSingleton.logError(e);
            return false;
        }
    }

}
