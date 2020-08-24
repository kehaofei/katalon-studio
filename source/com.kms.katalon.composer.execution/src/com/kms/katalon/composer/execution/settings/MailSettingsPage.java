package com.kms.katalon.composer.execution.settings;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.kms.katalon.application.utils.LicenseUtil;
import com.kms.katalon.composer.components.dialogs.MessageDialogWithLink;
import com.kms.katalon.composer.components.dialogs.PreferencePageWithHelp;
import com.kms.katalon.composer.components.event.EventBrokerSingleton;
import com.kms.katalon.composer.components.impl.handler.KSEFeatureAccessHandler;
import com.kms.katalon.composer.components.impl.util.ControlUtils;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.execution.constants.ComposerExecutionMessageConstants;
import com.kms.katalon.composer.execution.constants.StringConstants;
import com.kms.katalon.constants.DocumentationMessageConstants;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.controller.GlobalVariableController;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.controller.exception.ControllerException;
import com.kms.katalon.core.setting.ReportFormatType;
import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.execution.entity.EmailConfig;
import com.kms.katalon.execution.setting.EmailSettingStore;
import com.kms.katalon.execution.util.MailUtil;
import com.kms.katalon.execution.util.MailUtil.MailSecurityProtocolType;
import com.kms.katalon.feature.KSEFeature;

public class MailSettingsPage extends PreferencePageWithHelp {
    public static final String MAIL_CONFIG_USERNAME_HINT = "E.g: testemailkms@gmail.com"; //$NON-NLS-1$

    public static final String MAIL_CONFIG_PORT_HINT = "E.g: 465"; //$NON-NLS-1$

    public static final String MAIL_CONFIG_HOST_HINT = "E.g: smtp.gmail.com"; //$NON-NLS-1$

    private static final char PASSWORD_CHAR_MASK = '\u2022';

    private EmailSettingStore store;

    private Text txtHost, txtPort, txtUsername, txtPassword;

    private Combo comboProtocol;

    private Button btnChkAttachment;

    private Text txtSender, txtRecipients, txtSubject, txtCc, txtBcc;

    private Link lnkEditTemplateForTestSuite, lnkEditTemplateForTestSuiteCollection;

    private Button btnSendTestEmail;

    private EmailConfigValidator validator;

    private Group grpReportFormatOptions;

    private Composite attachmentOptionsComposite;

    private Map<ReportFormatType, Button> formatOptionCheckboxes;

    private Button chckEncrypt;
    
    private Button cbSendTestSuiteReport;

    private Button rbSendReportFailedTestSuites;

    private Button rbSendReportAllTestSuites;
    
    private Button cbSendTestSuiteCollectionReport;
    
    private Button cbSkipIndividualTestSuiteReport;

    private Button chckUseUsernameAsSender;

    private Group testSuiteReportGroup;

    private Group testSuiteCollectionReportGroup;

    public MailSettingsPage() {
        super();
        noDefaultButton();
        store = new EmailSettingStore(ProjectController.getInstance().getCurrentProject());
        validator = new EmailConfigValidator();
        formatOptionCheckboxes = new HashMap<>();
    }

    public EmailSettingStore getSettingStore() {
        return store;
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = createComposite(parent, 1, 1);

        createServerGroup(container);

        createPostExecuteGroup(container);

        createReportFormatGroup(container);

        addTestSuiteReportGroup(container);
        
        addTestSuiteCollectionReportGroup(container);

        createSendTestEmailButton(container);

        registerControlListers();

        updateInput();

        return container;
    }

    private void updateInput() {
        try {
            EmailSettingStore settingStore = getSettingStore();
            boolean encrytionEnabled = settingStore.isEncryptionEnabled();
            chckEncrypt.setSelection(encrytionEnabled);
            txtHost.setText(settingStore.getHost(encrytionEnabled));
            txtPort.setText(settingStore.getPort(encrytionEnabled));
            txtUsername.setText(settingStore.getUsername(encrytionEnabled));
            txtPassword.setText(settingStore.getPassword(encrytionEnabled));
            comboProtocol.setText(settingStore.getProtocol(encrytionEnabled));
            btnChkAttachment.setSelection(settingStore.isAddAttachment());
            updateReportFormatOptionsStatus();

            chckUseUsernameAsSender.setSelection(settingStore.useUsernameAsSender());

            String sender = settingStore.getSender();
            if (settingStore.useUsernameAsSender()) {
                sender = txtUsername.getText();
            }
            txtSender.setText(sender);

            if (chckUseUsernameAsSender.getSelection()) {
                txtSender.setEnabled(false);
                ;
            } else {
                txtSender.setEnabled(true);
            }
            txtRecipients.setText(settingStore.getRecipients(encrytionEnabled));
            txtCc.setText(settingStore.getEmailCc());
            txtBcc.setText(settingStore.getEmailBcc());
            txtSubject.setText(settingStore.getEmailSubject());

            settingStore.getReportFormatOptions().forEach(format -> {
                formatOptionCheckboxes.get(format).setSelection(true);
            });
            
            cbSendTestSuiteReport.setSelection(settingStore.isSendTestSuiteReportEnabled());
            rbSendReportFailedTestSuites.setSelection(settingStore.isSendEmailTestFailedOnly());
            rbSendReportAllTestSuites.setSelection(!settingStore.isSendEmailTestFailedOnly());
            
            setTestSuiteReportGroupEnabled(settingStore.isSendTestSuiteReportEnabled());

            boolean isSendCollectionReportEnabled = LicenseUtil.isNotFreeLicense()
                    ? settingStore.isSendTestSuiteCollectionReportEnabled() : false;
            cbSendTestSuiteCollectionReport.setSelection(isSendCollectionReportEnabled);
            cbSkipIndividualTestSuiteReport.setSelection(settingStore.isSkipInvidualTestSuiteReport());

            setTestSuiteCollectionReportGroupEnabled(isSendCollectionReportEnabled);
        } catch (IOException | GeneralSecurityException e) {
            LoggerSingleton.logError(e);
        }
    }

    private void registerControlListers() {
        lnkEditTemplateForTestSuite.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                EventBrokerSingleton.getInstance().getEventBroker().post(EventConstants.SETTINGS_PAGE_CHANGE,
                        StringConstants.TEST_SUITE_EMAIL_TEMPLATE_PAGE_ID);
            }
        });

        lnkEditTemplateForTestSuiteCollection.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                EventBrokerSingleton.getInstance().getEventBroker().post(EventConstants.SETTINGS_PAGE_CHANGE,
                        StringConstants.TEST_SUITE_COLLECTION_EMAIL_TEMPLATE_PAGE_ID);
            }
        });

        btnSendTestEmail.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                EmailConfig emailConfig = new EmailConfig();
                emailConfig.setFrom(txtSender.getText());
                emailConfig.setUsername(txtUsername.getText());
                emailConfig.setHost(txtHost.getText());
                emailConfig.setPassword(txtPassword.getText());
                emailConfig.setPort(txtPort.getText());
                emailConfig.setSecurityProtocol(MailSecurityProtocolType.valueOf(comboProtocol.getText()));
                emailConfig.addRecipients(txtRecipients.getText());
                emailConfig.setCc(txtCc.getText());
                emailConfig.setBcc(txtBcc.getText());
                emailConfig.setSubject(txtSubject.getText());
                emailConfig.setAttachmentOptions(getSelectedAttachmentOptions());
                try {
                    ProjectEntity project = ProjectController.getInstance().getCurrentProject();
                    MailUtil.overrideEmailSettings(emailConfig,
                            GlobalVariableController.getInstance().getDefaultExecutionProfile(project), null);
                    emailConfig.setHtmTemplateForTestSuite(getSettingStore().getEmailHTMLTemplateForTestSuite());
                } catch (ControllerException | IOException | URISyntaxException ex) {
                    LoggerSingleton.logError(ex);
                }
                sendTestEmail(emailConfig);
            }
        });

        txtPort.addVerifyListener(new VerifyListener() {

            @Override
            public void verifyText(VerifyEvent e) {
                e.doit = StringUtils.isNumeric(e.text);
                if (e.doit) {
                    setValidationAndEnableSendEmail("port", StringUtils.isNotEmpty(e.text)); //$NON-NLS-1$
                }
            }
        });

        txtHost.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                setValidationAndEnableSendEmail("host", StringUtils.isNotEmpty(txtHost.getText())); //$NON-NLS-1$
            }
        });

        txtPassword.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                setValidationAndEnableSendEmail("password", StringUtils.isNotEmpty(txtPassword.getText())); //$NON-NLS-1$
            }
        });

        txtUsername.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                setValidationAndEnableSendEmail("username", validator.isValidEmail(txtUsername.getText())); //$NON-NLS-1$
                if (chckUseUsernameAsSender.getSelection()) {
                    txtSender.setText(txtUsername.getText());
                }
            }
        });

        chckUseUsernameAsSender.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (chckUseUsernameAsSender.getSelection()) {
                    txtSender.setEnabled(false);
                    txtSender.setText(txtUsername.getText());
                } else {
                    txtSender.setEnabled(true);
                }
            }
        });

        btnChkAttachment.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateReportFormatOptionsStatus();
            }
        });
        
        cbSendTestSuiteReport.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setTestSuiteReportGroupEnabled(cbSendTestSuiteReport.getSelection());
            }
        });
        
        cbSendTestSuiteCollectionReport.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (LicenseUtil.isNotFreeLicense()) {
                    setTestSuiteCollectionReportGroupEnabled(cbSendTestSuiteCollectionReport.getSelection());
                } else {
                    KSEFeatureAccessHandler.handleUnauthorizedAccess(KSEFeature.TEST_SUITE_COLLECTION_EXECUTION_EMAIL);
                    cbSendTestSuiteCollectionReport.setSelection(false);
                }
            }
        });
    }

    private List<ReportFormatType> getSelectedAttachmentOptions() {
        return formatOptionCheckboxes.entrySet().stream().filter(e -> e.getValue().getSelection()).map(e -> e.getKey())
                .collect(Collectors.toList());
    }

    private void updateReportFormatOptionsStatus() {
        ControlUtils.recursiveSetEnabled(attachmentOptionsComposite, btnChkAttachment.getSelection());
    }

    private void setValidationAndEnableSendEmail(String property, boolean validated) {
        validator.setValidation(property, validated);
        btnSendTestEmail.setEnabled(validator.isValidated());
    }

    @Override
    protected void performApply() {
        super.performApply();
    }

    @Override
    public boolean performOk() {
        if (!isControlCreated()) {
            return super.performOk();
        }
        try {
            boolean encrytionEnabled = chckEncrypt.getSelection();

            EmailSettingStore settingStore = getSettingStore();
            settingStore.enableAuthenticationEncryption(encrytionEnabled);
            settingStore.setHost(txtHost.getText(), encrytionEnabled);
            settingStore.setPort(txtPort.getText(), encrytionEnabled);
            settingStore.setUsername(txtUsername.getText(), encrytionEnabled);
            settingStore.setPassword(txtPassword.getText(), encrytionEnabled);
            settingStore.setProtocol(comboProtocol.getText(), encrytionEnabled);
            settingStore.setIsAddAttachment(btnChkAttachment.getSelection());
            settingStore.setEmailSubject(txtSubject.getText());
            settingStore.setEmailCc(txtCc.getText());
            settingStore.setEmailBcc(txtBcc.getText());
            settingStore.setUseUsernameAsSender(chckUseUsernameAsSender.getSelection());
            settingStore.setSender(txtSender.getText());
            settingStore.setRecipients(txtRecipients.getText(), encrytionEnabled);
            settingStore.setReportFormatOptions(getSelectedAttachmentOptions());
            settingStore.setSendTestSuiteReportEnabled(cbSendTestSuiteReport.getSelection());
            settingStore.setSendEmailTestFailedOnly(rbSendReportFailedTestSuites.getSelection());
            settingStore.setSendTestSuiteCollectionReportEnabled(cbSendTestSuiteCollectionReport.getSelection());
            settingStore.setSkipIndividualTestSuiteReport(cbSkipIndividualTestSuiteReport.getSelection());
            return super.performOk();
        } catch (IOException | GeneralSecurityException e) {
            LoggerSingleton.logError(e);
            return false;
        }
    }

    private void createPostExecuteGroup(Composite container) {
        Group postExecuteGroup = createGroup(container, ComposerExecutionMessageConstants.PREF_GROUP_LBL_EXECUTION_MAIL,
                2, 1, GridData.FILL_HORIZONTAL);

        chckUseUsernameAsSender = new Button(postExecuteGroup, SWT.CHECK);
        chckUseUsernameAsSender.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        chckUseUsernameAsSender.setText(ComposerExecutionMessageConstants.PREF_CHECK_USE_USERNAME_AS_SENDER);

        txtSender = createTextFieldWithLabel(postExecuteGroup, ComposerExecutionMessageConstants.PREF_LBL_REPORT_SENDER,
                StringUtils.EMPTY, 1);

        txtRecipients = createTextFieldWithLabel(postExecuteGroup,
                ComposerExecutionMessageConstants.PREF_LBL_REPORT_RECIPIENTS,
                ComposerExecutionMessageConstants.PREF_TXT_PH_RECIPIENTS, 1);

        txtCc = createTextFieldWithLabel(postExecuteGroup, ComposerExecutionMessageConstants.PREF_LBL_CC,
                StringUtils.EMPTY, 1);

        txtBcc = createTextFieldWithLabel(postExecuteGroup, ComposerExecutionMessageConstants.PREF_LBL_BCC,
                StringUtils.EMPTY, 1);

        txtSubject = createTextFieldWithLabel(postExecuteGroup, ComposerExecutionMessageConstants.PREF_LBL_SUBJECT,
                StringUtils.EMPTY, 1);

        Label lblBody = new Label(postExecuteGroup, SWT.NONE);
        lblBody.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        lblBody.setText(ComposerExecutionMessageConstants.PREF_LBL_BODY);

        Composite linkComposite = new Composite(postExecuteGroup, SWT.NONE);
        GridLayout glLinkComposite = new GridLayout();
        glLinkComposite.marginWidth = 0;
        glLinkComposite.marginHeight = 0;
        linkComposite.setLayout(glLinkComposite);
        linkComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        lnkEditTemplateForTestSuite = new Link(linkComposite, SWT.NONE);
        lnkEditTemplateForTestSuite.setText(
                String.format("<a>%s</a>", ComposerExecutionMessageConstants.PREF_LNK_EDIT_TEMPLATE_TEST_SUITE)); //$NON-NLS-1$

        lnkEditTemplateForTestSuiteCollection = new Link(linkComposite, SWT.NONE);
        lnkEditTemplateForTestSuiteCollection.setText(String.format("<a>%s</a>", //$NON-NLS-1$
                ComposerExecutionMessageConstants.PREF_LNK_EDIT_TEMPLATE_TEST_SUITE_COLLECTION));
    }

    private void createReportFormatGroup(Composite container) {
        grpReportFormatOptions = new Group(container, SWT.NONE);
        grpReportFormatOptions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        grpReportFormatOptions.setText(ComposerExecutionMessageConstants.PREF_LBL_REPORT_FORMAT);

        GridLayout reportFormatLayout = new GridLayout(1, true);
        reportFormatLayout.marginLeft = 0;
        reportFormatLayout.marginRight = 0;
        reportFormatLayout.marginHeight = 5;
        grpReportFormatOptions.setLayout(reportFormatLayout);

        btnChkAttachment = new Button(grpReportFormatOptions, SWT.CHECK);
        btnChkAttachment.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        btnChkAttachment.setText(ComposerExecutionMessageConstants.PREF_LBL_INCLUDE_ATTACHMENT);

        attachmentOptionsComposite = new Composite(grpReportFormatOptions, SWT.NONE);
        attachmentOptionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        GridLayout attachmentsLayout = new GridLayout(1, true);
        attachmentsLayout.marginLeft = 15;
        attachmentsLayout.marginRight = 0;
        attachmentsLayout.marginHeight = 0;
        attachmentOptionsComposite.setLayout(attachmentsLayout);

        for (ReportFormatType formatType : ReportFormatType.values()) {
            Button btnFormmatingType = new Button(attachmentOptionsComposite, SWT.CHECK);
            btnFormmatingType.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
            btnFormmatingType.setText(formatType.toString());
            btnFormmatingType.setData(formatType);

            formatOptionCheckboxes.put(formatType, btnFormmatingType);
        }
    }

    private void addTestSuiteReportGroup(Composite container) {
        testSuiteReportGroup = new Group(container, SWT.NONE);
        testSuiteReportGroup.setLayout(new GridLayout());
        testSuiteReportGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        
        cbSendTestSuiteReport = new Button(testSuiteReportGroup, SWT.CHECK);
        cbSendTestSuiteReport.setText(ComposerExecutionMessageConstants.MailSettingsPage_MSG_ENABLE_TEST_SUITE_REPORT);

        GridData gdRadio = new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1);
        gdRadio.horizontalIndent = 15;
        
        rbSendReportAllTestSuites = new Button(testSuiteReportGroup, SWT.RADIO);
        rbSendReportAllTestSuites.setLayoutData(gdRadio);
        rbSendReportAllTestSuites.setText(StringConstants.DIA_MSG_SEND_EMAIL_REPORT_FOR_ALL_CASES);

        rbSendReportFailedTestSuites = new Button(testSuiteReportGroup, SWT.RADIO);
        rbSendReportFailedTestSuites.setLayoutData(gdRadio);
        rbSendReportFailedTestSuites.setText(StringConstants.DIA_MSG_SEND_EMAIL_REPORT_FOR_FAILED_TEST_ONLY);
    }
    
    private void setTestSuiteReportGroupEnabled(boolean enabled) {
        rbSendReportAllTestSuites.setEnabled(enabled);
        rbSendReportFailedTestSuites.setEnabled(enabled);
    }
    
    private void addTestSuiteCollectionReportGroup(Composite container) {
        testSuiteCollectionReportGroup = new Group(container, SWT.NONE);
        testSuiteCollectionReportGroup.setLayout(new GridLayout());
        testSuiteCollectionReportGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        cbSendTestSuiteCollectionReport = new Button(testSuiteCollectionReportGroup, SWT.CHECK);
        cbSendTestSuiteCollectionReport.setText(ComposerExecutionMessageConstants.MailSettingsPage_MSG_ENABLE_COLLECTION_REPORT);
        
        cbSkipIndividualTestSuiteReport = new Button(testSuiteCollectionReportGroup, SWT.CHECK);
        GridData gdRadio = new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1);
        gdRadio.horizontalIndent = 15;
        cbSkipIndividualTestSuiteReport.setLayoutData(gdRadio);
        cbSkipIndividualTestSuiteReport.setText(ComposerExecutionMessageConstants.MailSettingsPage_MSG_SKIP_SENDING_INDIVIDUAL_REPORT);
    }
    
    private void setTestSuiteCollectionReportGroupEnabled(boolean enabled) {
        cbSkipIndividualTestSuiteReport.setEnabled(enabled);
    }

    private void createSendTestEmailButton(Composite parent) {
        btnSendTestEmail = new Button(parent, SWT.PUSH);
        btnSendTestEmail.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        btnSendTestEmail.setText(ComposerExecutionMessageConstants.PREF_LBL_SEND_TEST_EMAIL);
    }

    private void createServerGroup(Composite container) {
        Group serverGroup = createGroup(container, StringConstants.PREF_GROUP_LBL_MAIL_SERVER, 4, 1,
                GridData.FILL_HORIZONTAL);

        txtHost = createTextFieldWithLabel(serverGroup, StringConstants.PREF_LBL_HOST, MAIL_CONFIG_HOST_HINT, 1);
        txtPort = createTextFieldWithLabel(serverGroup, StringConstants.PREF_LBL_PORT, MAIL_CONFIG_PORT_HINT, 1);

        txtUsername = createTextFieldWithLabel(serverGroup, StringConstants.PREF_LBL_USERNAME,
                MAIL_CONFIG_USERNAME_HINT, 1);
        txtPassword = createTextFieldWithLabel(serverGroup, StringConstants.PREF_LBL_PASSWORD, "", 1); //$NON-NLS-1$
        txtPassword.setEchoChar(PASSWORD_CHAR_MASK);

        createLabel(serverGroup, StringConstants.PREF_LBL_SECURITY_PROTOCOL);

        comboProtocol = new Combo(serverGroup, SWT.READ_ONLY);
        comboProtocol.setLayoutData(new GridData(SWT.LEFT, GridData.FILL, false, true, 1, 1));
        comboProtocol.setItems(MailSecurityProtocolType.getStringValues());

        chckEncrypt = new Button(serverGroup, SWT.CHECK);
        chckEncrypt.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 4, 1));
        chckEncrypt.setText(ComposerExecutionMessageConstants.PREF_CHECK_ENABLE_AUTHENTICATION_ENCRYPTION);
    }

    private void sendTestEmail(final EmailConfig conf) {
        String message = ComposerExecutionMessageConstants.PREF_MSG_TEST_EMAIL_IS_SENT_SUCCESSFULLY;
        String messageTitle = StringConstants.INFO;
        int messageType = MessageDialog.INFORMATION;
        Shell shell = getShell();
        try {

            new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask(StringConstants.PREF_SEND_TEST_EMAIL_JOB_NAME, IProgressMonitor.UNKNOWN);
                    try {
                        MailUtil.sendTestMail(conf);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }
            });
        } catch (InvocationTargetException ex) {
            Throwable rootException = ex.getTargetException();
            LoggerSingleton.logError(rootException);
            messageTitle = StringConstants.ERROR;
            messageType = MessageDialog.ERROR;
            message = rootException.getMessage();
            if (StringUtils.startsWith(message,
                    ComposerExecutionMessageConstants.PREF_FAILED_APACHE_MAIL_PREFIX_ERROR_MSG)) {
                message = StringUtils.removeStart(message,
                        ComposerExecutionMessageConstants.PREF_FAILED_APACHE_MAIL_PREFIX_ERROR_MSG);
                message = MessageFormat.format(ComposerExecutionMessageConstants.PREF_REPLACEMENT_APACHE_MAIL_ERROR_MSG,
                        message);
            }
        } catch (InterruptedException ex) {
            LoggerSingleton.logError(ex);
        } finally {
            MessageDialogWithLink.open(messageType, shell, messageTitle, message, SWT.NONE);
        }
    }

    private Text createTextFieldWithLabel(Composite parent, String labelText, String hintText, int hspan) {
        createLabel(parent, labelText);

        Text txtField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        GridData gridData = new GridData(GridData.FILL, GridData.CENTER, true, false, hspan, 1);
        txtField.setLayoutData(gridData);
        if (!ControlUtils.isDarkTheme(txtField.getDisplay())) {
            txtField.setMessage(hintText);
        }
        return txtField;
    }

    private void createLabel(Composite parent, String labelText) {
        Label label = new Label(parent, SWT.NONE);
        label.setLayoutData(new GridData(SWT.LEFT, GridData.CENTER, false, false, 1, 1));
        label.setText(labelText);
    }

    private Composite createComposite(Composite parent, int numColumns, int horizontalSpan) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, horizontalSpan, 1));
        container.setLayout(new GridLayout(numColumns, false));
        return container;
    }

    private static Group createGroup(Composite parent, String text, int columns, int hspan, int fill) {
        Group g = new Group(parent, SWT.NONE);
        g.setLayout(new GridLayout(columns, false));
        g.setText(text);
        g.setFont(parent.getFont());
        GridData gd = new GridData(fill);
        gd.horizontalSpan = hspan;
        g.setLayoutData(gd);
        return g;
    }

    @Override
    public boolean hasDocumentation() {
        return true;
    }

    @Override
    public String getDocumentationUrl() {
        return DocumentationMessageConstants.SETTINGS_EMAIL;
    }

    private class EmailConfigValidator {
        private Map<String, Boolean> validation;

        private static final String EMAIL_TEXT_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" //$NON-NLS-1$
                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"; //$NON-NLS-1$

        public EmailConfigValidator() {
            validation = new HashMap<>();
            validation.put("host", false); //$NON-NLS-1$
            validation.put("port", false); //$NON-NLS-1$
            validation.put("username", false); //$NON-NLS-1$
            validation.put("password", false); //$NON-NLS-1$
            validation.put("sender", true); //$NON-NLS-1$
            validation.put("recipients", true); //$NON-NLS-1$
            validation.put("cc", true); //$NON-NLS-1$
            validation.put("bcc", true); //$NON-NLS-1$
        }

        private boolean isValidated() {
            return !(validation.entrySet().parallelStream().filter(field -> !field.getValue()).count() > 0);
        }

        public void setValidation(String key, boolean value) {
            validation.put(key, value);
        }

        public boolean isValidEmail(String email) {
            if (StringUtils.isBlank(email)) {
                return false;
            }
            return Pattern.matches(EMAIL_TEXT_PATTERN, email.trim());
        }
    }
}