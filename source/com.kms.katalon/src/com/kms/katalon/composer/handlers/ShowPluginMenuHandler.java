package com.kms.katalon.composer.handlers;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolItem;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolItem;

import com.kms.katalon.constants.IdConstants;
import com.kms.katalon.processors.ToolbarProcessor;

public class ShowPluginMenuHandler {

    @Inject
    private EModelService modelService;

    @Inject
    private MApplication application;

    @CanExecute
    public boolean canExecute() {
        return true;
    }

    @Execute
    public void execute() {
        MToolBar pluginToolbar = (MToolBar) modelService.find(ToolbarProcessor.KATALON_PLUGIN_TOOLBAR_ID, application);
        MToolItem mPluginToolItem = (MToolItem) modelService.find(IdConstants.MANAGE_PLUGIN_TOOL_ITEM_ID,
                pluginToolbar);
        ToolItem pluginToolItem = (ToolItem) mPluginToolItem.getWidget();
        Listener[] listeners = pluginToolItem.getListeners(SWT.Selection);
        if (listeners.length > 0) {
            Listener listener = listeners[0];
            Event e = new Event();
            e.type = SWT.Selection;
            e.widget = pluginToolItem;
            e.detail = SWT.DROP_DOWN;
            listener.handleEvent(e);
        }
    }
}