package com.kms.katalon.composer.integration.qtest.wizard.page;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.kms.katalon.composer.components.impl.control.GifCLabel;
import com.kms.katalon.composer.components.services.UISynchronizeService;
import com.kms.katalon.composer.components.util.ColorUtil;
import com.kms.katalon.composer.integration.qtest.constant.ImageConstants;
import com.kms.katalon.composer.integration.qtest.constant.StringConstants;
import com.kms.katalon.composer.integration.qtest.wizard.AbstractWizardPage;
import com.kms.katalon.integration.qtest.QTestIntegrationAuthenticationManager;
import com.kms.katalon.integration.qtest.exception.QTestException;
import com.kms.katalon.integration.qtest.setting.QTestSettingStore;

public class AuthenticationWizardPage extends AbstractWizardPage {

    // Field
    private String fToken;
    private String fServerUrl;
    private String fUsername;
    private String fPassword;

    private Text txtServerURL;
    private Text txtUsername;
    private Text txtPassword;
    private Button btnShowPassword;
    private Label lblConnectedStatus;
    private Button btnConnect;

    // Field
    private boolean isDirty;
    private boolean isPasswordShowed;
    private String lblStatusText;

    public AuthenticationWizardPage() {
        isDirty = false;
        isPasswordShowed = false;
        lblStatusText = "";
        fToken = "";
        fUsername = "";
        fPassword = "";
        fServerUrl = "https://";
    }

    private ModifyListener modifyTextListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
            isDirty = true;
            if (!btnConnect.getEnabled()) {
                btnConnect.setEnabled(true);
            }
            lblConnectedStatus.setText("");
            firePageChanged();
        }
    };
    private GifCLabel connectingLabel;
    private Composite connectingComposite;
    private Label lblConnecting;
    private InputStream inputStream;
    private Composite headerComposite;
    private Label lblHeader;

    @Override
    public String getTitle() {
        return StringConstants.WZ_P_AUTHENTICATION_TITLE;
    }

    /**
     * @wbp.parser.entryPoint
     */
    @Override
    public void createStepArea(Composite parent) {
        Composite stepAreaComposite = new Composite(parent, SWT.NONE);
        stepAreaComposite.setLayout(new GridLayout(1, false));

        headerComposite = new Composite(stepAreaComposite, SWT.NONE);
        headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        headerComposite.setLayout(new GridLayout(1, false));

        lblHeader = new Label(headerComposite, SWT.WRAP);
        lblHeader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        lblHeader.setText(StringConstants.WZ_P_AUTHENTICATION_INFO);

        Composite authenticationComposite = new Composite(stepAreaComposite, SWT.NONE);
        GridLayout glAuthenticationComposite = new GridLayout(3, false);
        glAuthenticationComposite.horizontalSpacing = 15;
        authenticationComposite.setLayout(glAuthenticationComposite);
        authenticationComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        Label lblServerURL = new Label(authenticationComposite, SWT.NONE);
        lblServerURL.setText(StringConstants.CM_SERVER_URL);

        txtServerURL = new Text(authenticationComposite, SWT.BORDER);
        txtServerURL.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        new Label(authenticationComposite, SWT.NONE);

        Label lblUsername = new Label(authenticationComposite, SWT.NONE);
        lblUsername.setText(StringConstants.CM_USERNAME);

        txtUsername = new Text(authenticationComposite, SWT.BORDER);
        txtUsername.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        new Label(authenticationComposite, SWT.NONE);

        Label lblPassword = new Label(authenticationComposite, SWT.NONE);
        lblPassword.setText(StringConstants.CM_PASSWORD);

        txtPassword = new Text(authenticationComposite, SWT.BORDER);
        txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        btnShowPassword = new Button(authenticationComposite, SWT.CHECK);
        btnShowPassword.setText(StringConstants.WZ_P_AUTHENTICATION_SHOW_PASSWORD);
        new Label(authenticationComposite, SWT.NONE);

        Composite connectionComposite = new Composite(authenticationComposite, SWT.NONE);
        GridLayout glConectionComposite = new GridLayout(2, false);
        glConectionComposite.marginWidth = 0;
        connectionComposite.setLayout(glConectionComposite);
        connectionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        btnConnect = new Button(connectionComposite, SWT.FLAT);
        btnConnect.setText(StringConstants.WZ_P_AUTHENTICATION_CONNECT_ACCOUNT);

        connectingComposite = new Composite(connectionComposite, SWT.NONE);
        connectingComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        connectingComposite.setLayout(new GridLayout(2, false));

        connectingLabel = new GifCLabel(connectingComposite, SWT.DOUBLE_BUFFERED);
        connectingLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        lblConnecting = new Label(connectingComposite, SWT.NONE);
        lblConnecting.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblConnecting.setText(StringConstants.CM_CONNECTING);

        lblConnectedStatus = new Label(connectionComposite, SWT.WRAP);
        lblConnectedStatus.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        FontData[] fD = lblConnectedStatus.getFont().getFontData();
        fD[0].setHeight(10);
        lblConnectedStatus.setFont(new Font(Display.getCurrent(), fD));
    }

    @Override
    public void setInput(final Map<String, Object> sharedData) {
        txtServerURL.setText(fServerUrl);
        txtUsername.setText(fUsername);
        txtPassword.setText(fPassword);

        setConnectedStatus(lblStatusText, canFlipToNextPage());

        btnConnect.setEnabled(!canFlipToNextPage());
        btnShowPassword.setSelection(isPasswordShowed);
        updatePasswordField();
        setConnectingCompositeVisible(false);
    }

    @Override
    public void registerControlModifyListeners() {
        btnShowPassword.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updatePasswordField();
            }
        });

        txtServerURL.addModifyListener(modifyTextListener);
        txtUsername.addModifyListener(modifyTextListener);
        txtPassword.addModifyListener(modifyTextListener);

        btnConnect.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                isDirty = true;
                lblConnectedStatus.setText("");
            }
        });

        btnConnect.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                StringBuilder message = new StringBuilder();

                if (txtServerURL.getText().isEmpty()) {
                    message.append(StringConstants.DIA_MSG_ENTER_SERVER_URL);
                }

                if (txtUsername.getText().isEmpty()) {
                    if (message.length() > 0) {
                        message.append("\n");
                    }
                    message.append(StringConstants.DIA_MSG_ENTER_USERNAME);

                }

                if (txtPassword.getText().isEmpty()) {
                    if (message.length() > 0) {
                        message.append("\n");
                    }
                    message.append(StringConstants.DIA_MSG_ENTER_PASSWORD);
                }

                if (!message.toString().isEmpty()) {
                    MessageDialog.openInformation(null, StringConstants.INFO, message.toString());
                    return;
                }

                final String newServerUrl = txtServerURL.getText();
                final String newUsername = txtUsername.getText();
                final String newPassword = txtPassword.getText();

                setConnectingCompositeVisible(true);

                Job job = new Job("") {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            fToken = QTestIntegrationAuthenticationManager.getToken(newServerUrl, newUsername,
                                    newPassword);
                            setConnectedStatus(StringConstants.WZ_P_AUTHENTICATION_MGS_CONNECT_SUCCESSFUL, true);

                            fServerUrl = newServerUrl;
                            fUsername = newUsername;
                            fPassword = newPassword;
                            isDirty = false;

                            UISynchronizeService.getInstance().getSync().syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    firePageChanged();
                                }
                            });

                            return Status.OK_STATUS;
                        } catch (QTestException ex) {
                            setConnectedStatus(StringConstants.WZ_P_AUTHENTICATION_MGS_CONNECT_FAILED, false);
                            return Status.OK_STATUS;
                        } finally {
                            UISynchronizeService.getInstance().getSync().syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    setConnectingCompositeVisible(false);
                                }
                            });
                            monitor.done();
                        }
                    }
                };
                job.setUser(false);
                job.schedule();
            }
        });
    }

    private void setConnectedStatus(final String text, final boolean isSuccessful) {
        UISynchronizeService.getInstance().getSync().syncExec(new Runnable() {
            @Override
            public void run() {
                lblConnectedStatus.setText(text);
                lblConnectedStatus.setForeground(isSuccessful ? ColorUtil.getTextSuccessfulColor() : ColorUtil
                        .getTextErrorColor());
            }
        });
    }

    private void setConnectingCompositeVisible(boolean isConnectingCompositeVisible) {
        if (isConnectingCompositeVisible) {
            try {
                inputStream = ImageConstants.URL_16_LOADING.openStream();
                connectingLabel.setGifImage(inputStream);
                connectingComposite.layout(true, true);
            } catch (IOException ex) {
            } finally {
                if (inputStream != null) {
                    closeQuietly(inputStream);
                    inputStream = null;
                }
            }
        } else {
            if (inputStream != null) {
                closeQuietly(inputStream);
                inputStream = null;
            }
        }

        connectingComposite.setVisible(isConnectingCompositeVisible);
        btnConnect.setEnabled(!isConnectingCompositeVisible);
    }

    private void updatePasswordField() {
        if (btnShowPassword.getSelection()) {
            // show password
            txtPassword.setEchoChar('\0');
        } else {
            txtPassword.setEchoChar('*');
        }
    }

    @Override
    public boolean canFlipToNextPage() {
        return !StringUtils.isBlank(fToken) && !isDirty;
    }

    @Override
    public Map<String, Object> storeControlStates() {
        Map<String, Object> sharedData = new HashMap<String, Object>();
        sharedData.put(QTestSettingStore.SERVER_URL_PROPERTY, fServerUrl);
        sharedData.put(QTestSettingStore.USERNAME_PROPERTY, fUsername);
        sharedData.put(QTestSettingStore.PASSWORD_PROPERTY, fPassword);
        sharedData.put(QTestSettingStore.TOKEN_PROPERTY, fToken);

        lblStatusText = lblConnectedStatus.getText();
        isPasswordShowed = btnShowPassword.getSelection();
        return sharedData;
    }
}