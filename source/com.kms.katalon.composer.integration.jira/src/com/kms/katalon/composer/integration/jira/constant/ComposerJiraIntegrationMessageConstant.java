package com.kms.katalon.composer.integration.jira.constant;

import org.eclipse.osgi.util.NLS;

public class ComposerJiraIntegrationMessageConstant extends NLS {
    private static final String MESSAGE_FILE_NAME = "com.kms.katalon.composer.integration.jira.constant.ComposerJiraIntegrationMessage";
    static {
        // initialize resource bundle
        NLS.initializeMessages(MESSAGE_FILE_NAME, ComposerJiraIntegrationMessageConstant.class);
    }

    private ComposerJiraIntegrationMessageConstant() {
    }

    public static String PREF_CHCK_ENABLE_INTEGRATION;

    public static String PREF_TITLE_AUTHENTICATION;

    public static String PREF_TITLE_SUBMIT_OPTIONS;

    public static String PREF_LBL_SERVER_URL;

    public static String PREF_LBL_USERNAME;

    public static String PREF_LBL_PASSWORD;

    public static String PREF_LBL_CONNECT;

    public static String PREF_LBL_DF_JIRA_PROJECT;

    public static String PREF_LBL_DF_JIRA_ISSUE_TYPE;

    public static String PREF_CHCK_USE_TEST_CASE_NAME_AS_SUMMARY;

    public static String PREF_CHCK_ATTACH_SCREENSHOT_TO_JIRA_TICKET;

    public static String PREF_CHCK_ATTACH_LOG_TO_JIRA_TICKET;

    public static String PREF_MSG_ACCOUNT_CONNECTED;

    public static String JOB_TASK_JIRA_CONNECTION;

    public static String JOB_SUB_TASK_VALIDATING_ACCOUNT;

    public static String JOB_SUB_TASK_FETCHING_PROJECTS;

    public static String JOB_SUB_TASK_FETCHING_ISSUE_TYPES;

    public static String TOOLTIP_CLICK_TO_MANAGE_JIRA_ISSUES;

    public static String DIA_TITLE_LINKED_JIRA_ISSUES;

    public static String DIA_ITEM_CREATE_NEW_JIRA_ISSUE;

    public static String DIA_ITEM_LINK_TO_JIRA_ISSUE;

    public static String DIA_ITEM_CREATE_AS_SUB_TASK;

    public static String DIA_LBL_SUMMARY;

    public static String DIA_ISSUE_BROWSE_NOTIFICATION;

    public static String DIA_TITLE_CREATE_NEW_AS_SUB_TASK;

    public static String DIA_MESSAGE_CREATE_NEW_AS_SUB_TASK;

    public static String DIA_LBL_CREATE_NEW_AS_SUB_TASK;

    public static String DIA_TITLE_LINK_TO_EXISTING_ISSUE;

    public static String DIA_MESSAGE_LINK_TO_EXISTING_ISSUE;

    public static String DIA_LBL_LINK_TO_EXISTING_ISSUE;

    public static String JOB_TASK_LINK_TO_JIRA_ISSUE;

    public static String JOB_TASK_UPDATE_JIRA_ISSUE;

    public static String JOB_TASK_VALIDATE_JIRA_ISSUE;

    public static String JOB_MSG_INVALID_JIRA_ISSUE_KEY;
}
