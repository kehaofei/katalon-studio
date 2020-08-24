package com.kms.katalon.composer.mobile.objectspy.composites;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.kms.katalon.composer.components.impl.util.ControlUtils;
import com.kms.katalon.composer.mobile.objectspy.components.MobileAppComposite;
import com.kms.katalon.composer.mobile.objectspy.constant.StringConstants;
import com.kms.katalon.composer.mobile.objectspy.dialog.MobileAppDialog;

public class MobileConfigurationsComposite extends Composite {

    private Dialog parentDialog;

    private MobileAppComposite mobileComposite;

    private Composite appsComposite;

    public Composite getAppsComposite() {
        return appsComposite;
    }

    public MobileConfigurationsComposite(Dialog parentDialog, Composite parent, MobileAppComposite mobileComposite, int style) {
        super(parent, style);
        this.parentDialog = parentDialog;
        this.mobileComposite = mobileComposite;
        this.createComposite(parent);
    }

    public MobileConfigurationsComposite(Dialog parentDialog, Composite parent, MobileAppComposite mobileComposite) {
        this(parentDialog, parent, mobileComposite, SWT.NONE);
    }

    private void createComposite(Composite parent) {
        setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        GridLayout glSettingComposite = new GridLayout(2, false);
        setLayout(glSettingComposite);

        createCompositeLabel(this);
        createConfigurationsComposite(this);
    }

    private void createCompositeLabel(Composite parent) {
        Label lblConfiguration = new Label(parent, SWT.NONE);
        lblConfiguration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        ControlUtils.setFontToBeBold(lblConfiguration);
        lblConfiguration.setText(StringConstants.DIA_LBL_CONFIGURATIONS);
    }

    private void createConfigurationsComposite(Composite parent) {
        appsComposite = new Composite(parent, SWT.NONE);
        appsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        appsComposite.setLayout(new FillLayout());

        mobileComposite.createComposite(appsComposite, SWT.NONE, (MobileAppDialog) this.parentDialog);
    }
}