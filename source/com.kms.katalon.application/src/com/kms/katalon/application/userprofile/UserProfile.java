package com.kms.katalon.application.userprofile;

import org.apache.commons.lang3.StringUtils;

import com.kms.katalon.core.webui.driver.WebUIDriverType;
import com.kms.katalon.entity.project.QuickStartProjectType;

public class UserProfile {
    private String email;

    private boolean doneFirstTimeUseSurvey;

    private UserExperienceLevel experienceLevel;

    private QuickStartProjectType preferredTestingType;

    private WebUIDriverType preferredBrowser;

    private String preferredSite;

    private boolean doneQuickStart;

    private boolean doneCreateFirstTestCase;

    private String quickCreateFirstTestCaseScenario;

    private boolean doneOpenRecorder;

    private boolean doneQuickRecordGuidingDialog;

    private boolean doneQuickRecordSurveyDialog;

    private boolean doneSaveFirstRecord;

    private boolean doneRunFirstTestCase;

    private boolean doneRunFirstTestCasePass;

    private boolean doneRunFirstTestCaseFail;

    private boolean isEnableKURecorderHint = true;

    public boolean isNewUser() {
        return experienceLevel == UserExperienceLevel.FRESHER;
    }

    public boolean isOldUser() {
        return experienceLevel != UserExperienceLevel.FRESHER;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserExperienceLevel getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(UserExperienceLevel experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public QuickStartProjectType getPreferredTestingType() {
        return preferredTestingType;
    }

    public void setPreferredTestingType(QuickStartProjectType preferredTestingType) {
        this.preferredTestingType = preferredTestingType;
    }

    public boolean isPreferWebUI() {
        return preferredTestingType == QuickStartProjectType.WEBUI;
    }

    public WebUIDriverType getPreferredBrowser() {
        return preferredBrowser;
    }

    public void setPreferredBrowser(WebUIDriverType preferredBrowser) {
        this.preferredBrowser = preferredBrowser;
    }

    public String getPreferredSite() {
        return preferredSite;
    }

    public void setPreferredSite(String preferredSite) {
        this.preferredSite = preferredSite;
    }

    public boolean isDoneFirstTimeUseSurvey() {
        return doneFirstTimeUseSurvey;
    }

    public void setDoneFirstTimeUseSurvey(boolean doneFirstTimeUseSurvey) {
        this.doneFirstTimeUseSurvey = doneFirstTimeUseSurvey;
    }

    public boolean isDoneQuickStart() {
        return doneQuickStart;
    }

    public void setDoneQuickStart(boolean doneQuickStart) {
        this.doneQuickStart = doneQuickStart;
    }

    public boolean isDoneCreateFirstTestCase() {
        return doneCreateFirstTestCase;
    }

    public void setDoneCreateFirstTestCase(boolean doneCreateFirstTestCase) {
        this.doneCreateFirstTestCase = doneCreateFirstTestCase;
    }

    public String getQuickCreateFirstTestCaseScenario() {
        return quickCreateFirstTestCaseScenario;
    }

    public void setQuickCreateFirstTestCaseScenario(String quickCreateFirstTestCaseScenario) {
        this.quickCreateFirstTestCaseScenario = quickCreateFirstTestCaseScenario;
    }

    public String getNextQuickCreateFirstTestCaseScenario() {
        String preConfigScenario = System.getProperty("quickCreateFirstTestCaseScenario");
        if (!StringUtils.isBlank(preConfigScenario)) {
            quickCreateFirstTestCaseScenario = preConfigScenario;
            return preConfigScenario;
        }

        if (StringUtils.isBlank(quickCreateFirstTestCaseScenario)) {
            quickCreateFirstTestCaseScenario = Math.random() < 0.5f
                    ? "A"
                    : "B";
        }
        quickCreateFirstTestCaseScenario = StringUtils.equals(quickCreateFirstTestCaseScenario, "A")
                ? "B"
                : "A";
        return quickCreateFirstTestCaseScenario;
    }

    public boolean isDoneOpenRecorder() {
        return doneOpenRecorder;
    }

    public void setDoneOpenRecorder(boolean doneOpenRecorder) {
        this.doneOpenRecorder = doneOpenRecorder;
    }

    public boolean isDoneQuickRecordGuidingDialog() {
        return doneQuickRecordGuidingDialog;
    }

    public void setDoneQuickRecordGuidingDialog(boolean doneQuickRecordGuidingDialog) {
        this.doneQuickRecordGuidingDialog = doneQuickRecordGuidingDialog;
    }

    public boolean isDoneQuickRecordSurveyDialog() {
        return doneQuickRecordSurveyDialog;
    }

    public void setDoneQuickRecordSurveyDialog(boolean doneQuickRecordSurveyDialog) {
        this.doneQuickRecordSurveyDialog = doneQuickRecordSurveyDialog;
    }

    public boolean isDoneSaveFirstRecord() {
        return doneSaveFirstRecord;
    }

    public void setDoneSaveFirstRecord(boolean doneSaveFirstRecord) {
        this.doneSaveFirstRecord = doneSaveFirstRecord;
    }

    public boolean isDoneRunFirstTestCase() {
        return doneRunFirstTestCase;
    }

    public void setDoneRunFirstTestCase(boolean doneRunFirstTestCase) {
        this.doneRunFirstTestCase = doneRunFirstTestCase;
    }

    public boolean isDoneRunFirstTestCasePass() {
        return doneRunFirstTestCasePass;
    }

    public void setDoneRunFirstTestCasePass(boolean doneRunFirstTestCasePass) {
        this.doneRunFirstTestCasePass = doneRunFirstTestCasePass;
    }

    public boolean isDoneRunFirstTestCaseFail() {
        return doneRunFirstTestCaseFail;
    }

    public void setDoneRunFirstTestCaseFail(boolean doneRunFirstTestCaseFail) {
        this.doneRunFirstTestCaseFail = doneRunFirstTestCaseFail;
    }

    public boolean isEnableKURecorderHint() {
        return isEnableKURecorderHint;
    }

    public void setEnableKURecorderHint(boolean isEnableKURecorderHint) {
        this.isEnableKURecorderHint = isEnableKURecorderHint;
    }
}