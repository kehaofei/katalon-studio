package com.kms.katalon.composer.testcase.parts;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.eclipse.e4.ui.model.application.ui.MGenericTile;
import org.eclipse.e4.ui.model.application.ui.basic.MCompositePart;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.kms.katalon.composer.testcase.integration.TestCaseIntegrationFactory;
import com.kms.katalon.composer.testcase.parts.integration.AbstractTestCaseIntegrationView;
import com.kms.katalon.composer.testcase.parts.integration.TestCaseIntegrationViewBuilder;

public class TestCaseIntegrationPart {
	private ToolBar toolBar;
	private Composite container;
	private MPart mpart;
	private TestCaseCompositePart parentTestCaseCompositePart;


	private Map<String, AbstractTestCaseIntegrationView> integrationCompositeMap;

	@PostConstruct
	public void init(Composite parent, MPart mpart) {
		this.mpart = mpart;

		if (mpart.getParent().getParent() instanceof MGenericTile
				&& ((MGenericTile<?>) mpart.getParent().getParent()) instanceof MCompositePart) {
			MCompositePart compositePart = (MCompositePart) (MGenericTile<?>) mpart.getParent().getParent();
			if (compositePart.getObject() instanceof TestCaseCompositePart) {
				parentTestCaseCompositePart = ((TestCaseCompositePart) compositePart.getObject());
			}
		}

		createControls(parent);
	}

	private void createControls(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		GridLayout gl_mainComposite = new GridLayout(2, false);
		gl_mainComposite.horizontalSpacing = 20;
		mainComposite.setLayout(gl_mainComposite);

		Composite toolBarComposite = new Composite(mainComposite, SWT.NONE);
		toolBarComposite.setLayout(new GridLayout(1, false));
		toolBarComposite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true, 1, 1));

		toolBar = new ToolBar(toolBarComposite, SWT.FLAT | SWT.RIGHT);
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		container = new Composite(mainComposite, SWT.NONE);
		container.setLayout(new FillLayout(SWT.HORIZONTAL));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
	}

	public MPart getMPart() {
		return mpart;
	}
	
	public void setDirty(boolean dirty) {
		mpart.setDirty(true);
		parentTestCaseCompositePart.checkDirty();
	}

	public void loadInput() {
		clearToolbar();
		integrationCompositeMap = new HashMap<String, AbstractTestCaseIntegrationView>();

		for (Entry<String, TestCaseIntegrationViewBuilder> builderEntry : TestCaseIntegrationFactory.getInstance()
				.getIntegrationViewMap().entrySet()) {
			ToolItem item = new ToolItem(toolBar, SWT.CHECK);
			item.setText(builderEntry.getKey());
			integrationCompositeMap.put(builderEntry.getKey(),
					builderEntry.getValue().getIntegrationView(parentTestCaseCompositePart.getTestCase(), mpart));
		}

		for (ToolItem item : toolBar.getItems()) {
			item.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					ToolItem toolItem = (ToolItem) e.getSource();
					if (toolItem.getSelection()) {
						changeContainer(toolItem.getText());
					} else {
						clearContainer();
					}
				}

			});
		}

		if (toolBar.getItems().length > 0) {
			toolBar.getItems()[0].setSelection(true);
			changeContainer(toolBar.getItems()[0].getText());
		}
	}

	private void clearContainer() {
		while (container.getChildren().length > 0) {
			container.getChildren()[0].dispose();
		}
	}
	
	private void clearToolbar() {
		while (toolBar.getItems().length > 0) {
			toolBar.getItems()[0].dispose();
		}			
	}
	

	private void changeContainer(String key) {
		clearContainer();
		
		integrationCompositeMap.get(key).createContainer(container);

		container.layout(true, true);
	}
	

	public boolean isParentDirty() {
		return parentTestCaseCompositePart.getDirty().isDirty();
	}

}