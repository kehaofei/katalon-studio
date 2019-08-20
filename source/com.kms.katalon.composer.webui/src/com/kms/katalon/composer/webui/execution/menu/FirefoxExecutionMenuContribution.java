package com.kms.katalon.composer.webui.execution.menu;

import com.kms.katalon.composer.execution.menu.AbstractExecutionMenuContribution;
import com.kms.katalon.composer.webui.constants.ComposerWebuiMessageConstants;
import com.kms.katalon.composer.webui.constants.ImageConstants;
import com.kms.katalon.core.webui.driver.WebUIDriverType;

public class FirefoxExecutionMenuContribution extends AbstractExecutionMenuContribution {
    private static final String FIREFOX_EXECUTION_COMMAND_ID = "com.kms.katalon.composer.webui.execution.command.firefox"; //$NON-NLS-1$

    @Override
    protected String getIconUri() {
        return ImageConstants.IMG_URL_16_FIREFOX;
    }

    @Override
    protected String getDriverTypeName() {
        return WebUIDriverType.FIREFOX_DRIVER.toString();
    }

    @Override
    protected String getCommandId() {
        return FIREFOX_EXECUTION_COMMAND_ID;
    }

    @Override
    protected String getMenuLabel() {
        return ComposerWebuiMessageConstants.LBL_FIREFOX_EXECUTION_MENU_ITEM0;
    }

}