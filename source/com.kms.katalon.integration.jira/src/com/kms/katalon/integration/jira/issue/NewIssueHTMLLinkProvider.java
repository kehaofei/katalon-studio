package com.kms.katalon.integration.jira.issue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.kms.katalon.core.logging.model.TestCaseLogRecord;
import com.kms.katalon.integration.jira.constant.StringConstants;
import com.kms.katalon.integration.jira.entity.JiraIssue;
import com.kms.katalon.integration.jira.setting.JiraIntegrationSettingStore;

public class NewIssueHTMLLinkProvider extends DefaultIssueHTMLLinkProvider {


    public NewIssueHTMLLinkProvider(TestCaseLogRecord logRecord, JiraIntegrationSettingStore settingStore) {
        super(logRecord, settingStore);
    }

    @Override
    public String getIssueUrl() throws IOException {
        return settingStore.getServerUrl() + StringConstants.HREF_CREATE_ISSUE;
    }

    @Override
    public String getIssueUrlPrefix() throws IOException, URISyntaxException {
        return settingStore.getServerUrl() + StringConstants.HREF_CREATE_ISSUE_PREFIX;
    }

    @Override
    public List<NameValuePair> getIssueParameters() throws IOException {
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair(JiraIssue.FIELD_PROJECT_ID,
                Long.toString(settingStore.getStoredJiraProject().getDefaultJiraObject().getId())));
        pairs.add(new BasicNameValuePair(JiraIssue.FIELD_ISSUE_TYPE,
                Long.toString(settingStore.getStoredJiraIssueType().getDefaultJiraObject().getId())));
        if (settingStore.isUseTestCaseNameAsSummaryEnabled()) {
            pairs.add(new BasicNameValuePair(JiraIssue.FIELD_SUMMARY, issueMetaData.getSummary()));
        }
        pairs.add(new BasicNameValuePair(JiraIssue.FIELD_REPORTER, settingStore.getJiraUser().getName()));
        pairs.add(new BasicNameValuePair(JiraIssue.FIELD_DESCRIPTION, issueMetaData.getDescription()));
        pairs.add(new BasicNameValuePair(JiraIssue.FIELD_ENVIRONMENT, issueMetaData.getEnvironment()));
        return pairs;
    }
}
