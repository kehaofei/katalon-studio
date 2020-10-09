package com.kms.katalon.composer.mobile.objectspy.components;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.kms.katalon.composer.components.impl.dialogs.ProgressMonitorDialogWithThread;
import com.kms.katalon.composer.components.services.UISynchronizeService;
import com.kms.katalon.composer.execution.util.MobileDeviceUIProvider;
import com.kms.katalon.composer.mobile.objectspy.actions.MobileAction;
import com.kms.katalon.composer.mobile.objectspy.actions.MobileActionMapping;
import com.kms.katalon.composer.mobile.objectspy.constant.StringConstants;
import com.kms.katalon.composer.mobile.objectspy.dialog.AppiumMonitorDialog;
import com.kms.katalon.composer.mobile.objectspy.dialog.MobileAppDialog;
import com.kms.katalon.composer.mobile.objectspy.dialog.MobileInspectorController;
import com.kms.katalon.composer.testcase.groovy.ast.expressions.ConstantExpressionWrapper;
import com.kms.katalon.core.mobile.driver.MobileDriverType;
import com.kms.katalon.execution.mobile.device.AndroidDeviceInfo;
import com.kms.katalon.execution.mobile.device.MobileDeviceInfo;

public class MobileLocalAppComposite implements MobileAppComposite {

    private static final int START_WITH_APPLICATION_FILE = 0;

    private static final int START_WITH_APPLICATION_ID = 1;

    private String ANDROID_FILTER_NAMES = "Android Application (*.apk)";

    private String ANDROID_FILTER_EXTS = "*.apk";

    private String IOS_FILTER_NAMES = "iOS Application (*.app, *.ipa)";

    private String IOS_FILTER_EXTS = "*.app;*.ipa";

    private MobileAppDialog parentDialog;

    private Combo cbbDevices;

    private Button btnBrowse, btnRefreshDevice;

    private Text txtAppFile;

    private List<MobileDeviceInfo> deviceInfos = new ArrayList<>();

    private MobileDeviceInfo selectedDevice = null;

    private Link linkLabel;

    private Composite composite;

    private MobileDriverType driverType;

    private Combo cbbStartType;

    private Text txtAppId;

    private Composite appFileChooserComposite;

    private Composite appComposite;

    private Composite appIdComposite;

    private StackLayout slAppComposite;

    private Label appFileLabel;

    public MobileLocalAppComposite(MobileDriverType driverType) {
        this.driverType = driverType;
    }

    private String[] getFilterNames() {
        switch (driverType) {
            case ANDROID_DRIVER:
                return new String[] { ANDROID_FILTER_NAMES };
            case IOS_DRIVER:
                return new String[] { IOS_FILTER_NAMES };
            default:
                return new String[0];
        }
    }

    private String[] getFilterExtensions() {
        switch (driverType) {
            case ANDROID_DRIVER:
                return new String[] { ANDROID_FILTER_EXTS };
            case IOS_DRIVER:
                return new String[] { IOS_FILTER_EXTS };
            default:
                return new String[0];
        }
    }

    public boolean isAbleToStart() {
        if (cbbDevices.getSelectionIndex() < 0) {
            return false;
        }
        switch (cbbStartType.getSelectionIndex()) {
            case START_WITH_APPLICATION_FILE: {
                return isNotBlank(getAppFile());
            }
            case START_WITH_APPLICATION_ID: {
                return isNotBlank(txtAppId.getText());
            }
        }
        return false;
    }

    public void updateLocalDevices() throws InvocationTargetException, InterruptedException {
        final IRunnableWithProgress runnable = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask(StringConstants.DIA_JOB_TASK_LOADING_DEVICES, IProgressMonitor.UNKNOWN);

                deviceInfos = getDeviceList();
                final List<String> devices = getAllDevicesName();

                checkMonitorCanceled(monitor);

                UISynchronizeService.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        cbbDevices.setItems(devices.toArray(new String[] {}));
                        if (!devices.isEmpty()) {
                            cbbDevices.select(Math.max(0, devices.indexOf(cbbDevices.getText())));
                        }
                        setLinkLabelVisible(devices.isEmpty());
                    }
                });

                monitor.done();
            }
        };
        new ProgressMonitorDialogWithThread(Display.getDefault().getActiveShell()).run(true, true, runnable);
    }

    private List<MobileDeviceInfo> getDeviceList() {
        switch (driverType) {
            case ANDROID_DRIVER:
                return MobileDeviceUIProvider.getAndroidDevices();
            case IOS_DRIVER:
                return MobileDeviceUIProvider.getIOSDevices();
            default:
                return Collections.emptyList();
        }
    }

    private void checkMonitorCanceled(IProgressMonitor monitor) throws InterruptedException {
        if (monitor.isCanceled()) {
            throw new InterruptedException(StringConstants.DIA_ERROR_MSG_OPERATION_CANCELED);
        }
    }

    private List<String> getAllDevicesName() {
        List<String> devicesNameList = new ArrayList<String>();
        for (MobileDeviceInfo deviceInfo : deviceInfos) {
            devicesNameList.add(deviceInfo.getDisplayName());
        }
        return devicesNameList;
    }

    public String getAppName() {
        return FilenameUtils.getName(getAppFile());
    }

    public String getAppFile() {
        return txtAppFile.getText();
    }

    public MobileDeviceInfo getSelectedMobileDeviceInfo() {
        if (cbbDevices == null || cbbDevices.isDisposed()) {
            return selectedDevice;
        }
        int selectedMobileDeviceIndex = cbbDevices.getSelectionIndex();
        if (selectedMobileDeviceIndex < 0 || selectedMobileDeviceIndex >= deviceInfos.size()) {
            selectedDevice = null;
            return selectedDevice;
        }
        selectedDevice = deviceInfos.get(selectedMobileDeviceIndex);
        return selectedDevice;
    }

    public boolean validateLocalAppSetting() {
        if (cbbDevices.getSelectionIndex() < 0) {
            MessageDialog.openError(getShell(), StringConstants.ERROR_TITLE,
                    StringConstants.DIA_ERROR_MSG_PLS_CONNECT_AND_SELECT_DEVICE);
            return false;
        }

        switch (cbbStartType.getSelectionIndex()) {
            case START_WITH_APPLICATION_FILE: {
                String appFilePath = getAppFile().trim();

                if (appFilePath.equals("")) {
                    MessageDialog.openError(getShell(), StringConstants.ERROR_TITLE,
                            StringConstants.DIA_ERROR_MSG_PLS_SELECT_APP_FILE);
                    return false;
                }
                File appFile = new File(appFilePath);

                if (!appFile.exists()) {
                    MessageDialog.openWarning(getShell(), StringConstants.ERROR_TITLE,
                            StringConstants.DIA_ERROR_MSG_APP_FILE_NOT_EXIST);
                    return false;
                }
                return true;
            }
            case START_WITH_APPLICATION_ID: {
                return true;
            }
        }
        return false;
    }

    private Shell getShell() {
        return composite.getShell();
    }

    private void setLinkLabelVisible(boolean visible) {
        linkLabel.setVisible(visible);
        ((GridData) linkLabel.getLayoutData()).exclude = !visible;
        linkLabel.pack();
        linkLabel.getParent().layout();
    }

    @Override
    public Composite createComposite(Composite parent, int type, MobileAppDialog parentDialog) {
        this.parentDialog = parentDialog;
        composite = new Composite(parent, SWT.NONE);
        final GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        Composite deviceNameCompposite = new Composite(composite, SWT.NONE);
        final GridLayout deviceNameComppositeLayout = new GridLayout(3, false);
        deviceNameComppositeLayout.marginHeight = 0;
        deviceNameComppositeLayout.marginWidth = 0;
        deviceNameCompposite.setLayout(deviceNameComppositeLayout);
        deviceNameCompposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        // Device Name
        Label lblDeviceName = new Label(deviceNameCompposite, SWT.NONE);
        lblDeviceName.setText(StringConstants.DIA_LBL_DEVICE_NAME);
        GridData gdDeviceNameLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        lblDeviceName.setLayoutData(gdDeviceNameLabel);

        cbbDevices = new Combo(deviceNameCompposite, SWT.READ_ONLY);
        cbbDevices.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        cbbDevices.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                parentDialog.refreshButtonsState();
            }
        });

        btnRefreshDevice = new Button(deviceNameCompposite, SWT.PUSH);
        btnRefreshDevice.setText(StringConstants.REFRESH);
        btnRefreshDevice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                parentDialog.updateDeviceNames();
            }
        });

        linkLabel = new Link(composite, SWT.NONE);
        linkLabel.setText(
                StringConstants.MSG_NO_DEVICES + " <a href=\"" + StringConstants.NO_DEVICES_TROUBLESHOOTING_GUIDE_LINK
                        + "\">" + StringConstants.MSG_WRAPPED_NO_DEVICES_TROUBLESHOOTING_GUIDE + "</a>");

        linkLabel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Program.launch(e.text);
            }
        });
        linkLabel.setVisible(false);
        
        Composite startTypeComposite = new Composite(composite, SWT.NONE);
        GridLayout glStartTypeComposite = new GridLayout(2, false);
        glStartTypeComposite.marginHeight = 0;
        glStartTypeComposite.marginWidth = 0;
        startTypeComposite.setLayout(glStartTypeComposite);
        Label lblStartType = new Label(startTypeComposite, SWT.NONE);
        GridData gdStartWithLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        lblStartType.setLayoutData(gdStartWithLabel);
        lblStartType.setText("Start with");

        cbbStartType = new Combo(startTypeComposite, SWT.READ_ONLY);
        cbbStartType.setItems(new String[] { "Application File", "Appication ID"});
        cbbStartType.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                switch (cbbStartType.getSelectionIndex()) {
                    case START_WITH_APPLICATION_FILE: {
                        slAppComposite.topControl = appFileChooserComposite;
                        appComposite.layout();
                        parentDialog.refreshButtonsState();
                        break;
                    }
                    case START_WITH_APPLICATION_ID: {
                        slAppComposite.topControl = appIdComposite;
                        appComposite.layout();
                        parentDialog.refreshButtonsState();
                        break;
                    }
                }
            }
        });
        
        appComposite = new Composite(composite, SWT.NONE);
        appComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
         slAppComposite = new StackLayout();
        appComposite.setLayout(slAppComposite);

        appFileChooserComposite = new Composite(appComposite, SWT.NONE);
        appFileChooserComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        GridLayout glAppFileChooserComposite = new GridLayout(3, false);
        glAppFileChooserComposite.marginHeight = 0;
        glAppFileChooserComposite.marginWidth = 0;
        appFileChooserComposite.setLayout(glAppFileChooserComposite);

        // Application File location
        appFileLabel = new Label(appFileChooserComposite, SWT.NONE);
        appFileLabel.setText(StringConstants.DIA_LBL_APP_FILE);
        GridData gdAppFileLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        appFileLabel.setLayoutData(gdAppFileLabel);

        txtAppFile = new Text(appFileChooserComposite, SWT.BORDER);
        GridData gdText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        txtAppFile.setLayoutData(gdText);
        txtAppFile.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                parentDialog.refreshButtonsState();
            }
        });
        
        txtAppFile.addListener(SWT.Resize, new Listener() {
            
            @Override
            public void handleEvent(Event event) {
                gdText.widthHint = 100;
            }
        });

        btnBrowse = new Button(appFileChooserComposite, SWT.PUSH);
        final GridData btnBrowserGridData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        btnBrowse.setLayoutData(btnBrowserGridData);
        btnBrowse.setText(StringConstants.DIA_BTN_BROWSE);
        btnBrowse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(btnBrowse.getShell());
                dialog.setFilterNames(getFilterNames());
                dialog.setFilterExtensions(getFilterExtensions());
                String absolutePath = dialog.open();
                if (StringUtils.isEmpty(absolutePath)) {
                    return;
                }
                parentDialog.getPreferencesHelper().setLastAppFile(absolutePath);
                txtAppFile.setText(absolutePath);
            }
        });

        appIdComposite = new Composite(appComposite, SWT.NONE);
        appIdComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        GridLayout glAppIdComposite = new GridLayout(2, false);
        glAppIdComposite.marginHeight = 0;
        glAppIdComposite.marginWidth = 0;
        appIdComposite.setLayout(glAppIdComposite);

        Label appIdLabel = new Label(appIdComposite, SWT.NONE);
        appIdLabel.setText("Application ID");
        GridData gdAppIdLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        appIdLabel.setLayoutData(gdAppIdLabel);

        txtAppId = new Text(appIdComposite, SWT.BORDER);
        GridData gdAppId = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gdAppId.widthHint = 100;
        txtAppId.setLayoutData(gdText);
        txtAppId.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                parentDialog.refreshButtonsState();
            }
        });
        
        txtAppId.addListener(SWT.Resize, new Listener() {
            
            @Override
            public void handleEvent(Event event) {
                gdAppId.widthHint = 100;
            }
        });
        
        appFileLabel.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event event) {
                int appFileLabelWidth = appFileLabel.getBounds().width;
                if (appFileLabelWidth == 0) {
                    return;
                }
                gdDeviceNameLabel.widthHint = appFileLabelWidth;
                gdStartWithLabel.widthHint = appFileLabelWidth;
                gdAppIdLabel.widthHint = appFileLabelWidth;
                composite.layout(true, true);
            }
        });

        return composite;
    }

    @Override
    public boolean validateSetting() {
        return validateLocalAppSetting();
    }

    @Override
    public MobileDriverType getSelectedDriverType() {
        return driverType;
    }

    @Override
    public boolean startApp(MobileInspectorController inspectorController, AppiumMonitorDialog progressDlg)
            throws InvocationTargetException, InterruptedException {
        final MobileDeviceInfo selectDeviceInfo = getSelectedMobileDeviceInfo();
        if (selectDeviceInfo == null) {
            return false;
        }

        if (selectDeviceInfo instanceof AndroidDeviceInfo && !MobileDeviceUIProvider.checkAndroidSDKExist(getShell())) {
            return false;
        }
        final String appFile = getAppFile();
        final String appId = txtAppId.getText();
        final int startType = cbbStartType.getSelectionIndex();

        IRunnableWithProgress processToRun = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask(StringConstants.DIA_LBL_STATUS_APP_STARTING, IProgressMonitor.UNKNOWN);

                progressDlg.runAndWait(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        switch (startType) {
                            case START_WITH_APPLICATION_FILE: 
                                inspectorController.startMobileApp(selectDeviceInfo, appFile, false);
                                break;
                            case START_WITH_APPLICATION_ID: 
                                inspectorController.startExistingApp(selectDeviceInfo, appId);
                                break;
                        }
                        
                        return null;
                    }
                });
                checkMonitorCanceled(monitor);

                monitor.done();
            }
        };

        progressDlg.run(true, true, processToRun);
        return true;
    }

    @Override
    public void setInput() throws InvocationTargetException, InterruptedException {
        updateLocalDevices();
        cbbStartType.select(0);
        cbbStartType.notifyListeners(SWT.Selection, new Event());
        appFileLabel.notifyListeners(SWT.Resize, new Event());
    }

    @Override
    public void loadDevices() throws InvocationTargetException, InterruptedException {
        updateLocalDevices();
    }

    @Override
    public MobileActionMapping buildStartAppActionMapping() {
        int startType = cbbStartType.getSelectionIndex();

        switch (startType) {
            case START_WITH_APPLICATION_FILE: {
                MobileActionMapping startAppAction = new MobileActionMapping(MobileAction.StartApplication, null);
                String appValue = getAppFile();
                startAppAction.getData()[0].setValue(new ConstantExpressionWrapper(appValue));
                return startAppAction;
            }
            case START_WITH_APPLICATION_ID: {
                MobileActionMapping startAppAction = new MobileActionMapping(MobileAction.StartExistingApplication, null);
                String appValue = txtAppId.getText();
                startAppAction.getData()[0].setValue(new ConstantExpressionWrapper(appValue));
                return startAppAction;
            }
        }
        return null;
    }
}