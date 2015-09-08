package com.kms.katalon.composer.testdata.parts;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.kms.katalon.composer.components.dialogs.MultiStatusErrorDialog;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.testdata.constants.ImageConstants;
import com.kms.katalon.composer.testdata.constants.StringConstants;
import com.kms.katalon.composer.testdata.views.NewTestDataColumnDialog;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.controller.TestDataController;
import com.kms.katalon.entity.dal.exception.DuplicatedFileNameException;
import com.kms.katalon.entity.testdata.DataFileEntity;
import com.kms.katalon.entity.testdata.InternalDataColumnEntity;
import com.kms.katalon.entity.testdata.InternalDataFilePropertyEntity;
import com.kms.katalon.entity.testdata.DataFileEntity.DataFileDriverType;

public class InternalTestDataPart extends TestDataMainPart {

	private Image addImage;
	private Table table;

	@PostConstruct
	public void createControls(Composite parent, MPart mpart) {
		super.createControls(parent, mpart);
	}

	@Override
	public Composite createDataTablePart(Composite parent) {
		parent.setLayout(new GridLayout(1, true));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite tableViewerComposite = new Composite(parent, SWT.BORDER);
		tableViewerComposite.setLayout(new GridLayout(1, true));
		tableViewerComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createContents(tableViewerComposite);
		return tableViewerComposite;
	}

	private void createContents(final Composite parent) {
		parent.setLayout(new FillLayout());

		// Create the table
		table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		createTableEditor(table);

	}

	private void createColumns(DataFileEntity dataFile) {
		// Create five mock-up columns
		for (InternalDataColumnEntity dataCol : dataFile.getInternalDataColumns()) {
			CustomTableColumn column = new CustomTableColumn(table, SWT.LEFT);
			// column.setAlignment(SWT.RIGHT);
			column.setWidth(200);
			column.setText(dataCol.getName());
			column.setDataType(dataCol.getDataType());
		}
	}

	private void loadDataRows(DataFileEntity dataFile) {
		for (int i = 0; i < dataFile.getData().size(); i++) {
			List<Object> dataRow = dataFile.getData().get(i);
			TableItem tableRow = new TableItem(table, SWT.NONE);
			// Index column
			tableRow.setText(0, String.valueOf(i + 1));
			// Data columns
			for (int j = 0; j < dataRow.size(); j++) {
				tableRow.setText(j + 1, String.valueOf(dataRow.get(j)));
			}
		}
	}

	/**
	 * Last row is command row
	 */
	private void createLastRow(Table table) {
		if (addImage == null) {
			addImage = ImageConstants.IMG_16_ADD;
		}
		// The last row for adding new row
		TableItem lastRow = new TableItem(table, SWT.NONE);
		lastRow.setImage(0, addImage);
	}

	private void createTableEditor(final Table table) {

		final TableEditor editor = new TableEditor(table);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		table.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event event) {

				Point cursorPt = table.getDisplay().map(null, table, new Point(event.x, event.y));
				Point plusedHorizontalPt = new Point(cursorPt.x + table.getHorizontalBar().getSelection(), cursorPt.y);
				Rectangle clientArea = table.getClientArea();
				boolean isHeader = cursorPt.y >= clientArea.y && cursorPt.y < (clientArea.y + table.getHeaderHeight());
				int colIndex = -1;
				int theWidth = 0;
				for (int i = 0; i < table.getColumns().length; i++) {
					TableColumn tc = table.getColumns()[i];
					if (theWidth < plusedHorizontalPt.x && plusedHorizontalPt.x < theWidth + tc.getWidth()) {
						colIndex = i;
						break;
					}
					theWidth += tc.getWidth();
				}
				createContextMenu(colIndex, isHeader);
			}
		});

		table.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent event) {
				if (event.button == 1) {
					handleLeftClick(event);
				}
			}

			private void handleLeftClick(MouseEvent event) {
				Control old = editor.getEditor();
				if (old != null) old.dispose();
				Point pt = new Point(event.x, event.y);
				final TableItem item = table.getItem(pt);
				if (item != null && item.getImage() == null) {
					int column = -1;
					for (int i = 0; i < table.getColumnCount(); i++) {
						Rectangle rect = item.getBounds(i);
						if (rect.contains(pt)) {
							column = i;
							break;
						}
					}
					if (column > 0) {
						final Text text = new Text(table, SWT.NONE);
						text.setForeground(item.getForeground());

						text.setText(item.getText(column));
						text.setForeground(item.getForeground());
						text.selectAll();
						text.setFocus();

						editor.minimumWidth = text.getBounds().width;

						editor.setEditor(text, item, column);

						final int col = column;
						text.addModifyListener(new ModifyListener() {
							public void modifyText(ModifyEvent event) {
								item.setText(col, text.getText());
								dirtyable.setDirty(true);
							}
						});
					}
				} else if (item != null && item.getImage() != null) {
					new TableItem(table, SWT.NONE, table.getItemCount() - 1).setText(0,
							String.valueOf(table.getItemCount() - 1));
					dirtyable.setDirty(true);
				}
			}
		});

	}

	private Menu createContextMenu(final int selectedColIndex, boolean isHeader) {

		Menu menu = table.getMenu();
		if (menu != null) menu.dispose();
		menu = new Menu(table);

		MenuItem menuItem = null;

		if (selectedColIndex > 0) {
			menuItem = new MenuItem(menu, SWT.PUSH);
			menuItem.setText(StringConstants.PA_MENU_CONTEXT_INSERT_COL_TO_THE_LEFT);
			menuItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					NewTestDataColumnDialog dialog = new NewTestDataColumnDialog(Display.getDefault().getActiveShell());
					dialog.open();
					if (dialog.getReturnCode() == Window.OK) {
						String columnName = dialog.getName();
						String dataType = dialog.getDataType();
						addColumn(selectedColIndex, columnName, dataType);
						dirtyable.setDirty(true);
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
		}

		menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText(StringConstants.PA_MENU_CONTEXT_INSERT_COL_TO_THE_RIGHT);
		menuItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				NewTestDataColumnDialog dialog = new NewTestDataColumnDialog(Display.getDefault().getActiveShell());
				dialog.open();
				if (dialog.getReturnCode() == Window.OK) {
					String columnName = dialog.getName();
					String dataType = dialog.getDataType();
					if (selectedColIndex >= 0) {
						addColumn(selectedColIndex + 1, columnName, dataType);
					} else {
						addColumn(table.getColumnCount(), columnName, dataType);
					}
					dirtyable.setDirty(true);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		if (selectedColIndex > 0) {
			menuItem = new MenuItem(menu, SWT.PUSH);
			menuItem.setText(StringConstants.PA_MENU_CONTEXT_EDIT_COL);
			menuItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					CustomTableColumn selectedCol = (CustomTableColumn) table.getColumn(selectedColIndex);
					NewTestDataColumnDialog dialog = new NewTestDataColumnDialog(Display.getDefault().getActiveShell(),
							selectedCol.getText(), selectedCol.getDataType());
					dialog.open();
					if (dialog.getReturnCode() == Window.OK) {
						String columnName = dialog.getName();
						String dataType = dialog.getDataType();
						if (!selectedCol.getText().equals(columnName) || !selectedCol.getDataType().equals(dataType)) {
							selectedCol.setText(columnName);
							selectedCol.setDataType(dataType);
							table.redraw();
							dirtyable.setDirty(true);
						}
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});

			menuItem = new MenuItem(menu, SWT.PUSH);
			menuItem.setText(StringConstants.PA_MENU_CONTEXT_DEL_COL);
			menuItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					deleteColumn(selectedColIndex);
					dirtyable.setDirty(true);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
		}

		menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText(StringConstants.PA_MENU_CONTEXT_INSERT_ROW);
		menuItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items = table.getSelection();
				if (items.length > 0) {
					new TableItem(table, SWT.NONE, table.indexOf(items[0]));
				} else {
					new TableItem(table, SWT.NONE, table.getItemCount() - 1);
				}
				reIndexRows();
				dirtyable.setDirty(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		if (table.getItemCount() > 1) {
			menuItem = new MenuItem(menu, SWT.PUSH);
			menuItem.setText(StringConstants.PA_MENU_CONTEXT_DEL_ROWS);
			menuItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					TableItem[] items = table.getSelection();
					for (TableItem tableItem : items) {
						if (tableItem.getImage() == null) {
							tableItem.dispose();
						}
					}
					table.redraw();
					reIndexRows();
					dirtyable.setDirty(true);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
		}

		table.setMenu(menu);

		return menu;
	}

	private TableColumn addColumn(int index, String title, String dataType) {
		CustomTableColumn tblColumn = new CustomTableColumn(table, SWT.LEFT, index);
		tblColumn.setText(title);
		tblColumn.setDataType(dataType);
		tblColumn.pack();
		tblColumn.getParent().showColumn(tblColumn);
		return tblColumn;
	}

	private void deleteColumn(int index) {
		TableColumn tblColumn = table.getColumn(index);
		if (tblColumn != null) {
			tblColumn.dispose();
			table.redraw();
		}
	}

	private void reIndexRows() {
		// Re-calculate indexes
		for (TableItem tableItem : table.getItems()) {
			if (tableItem.getImage() == null) {
				tableItem.setText(0, String.valueOf(table.indexOf(tableItem) + 1));
			}
		}
	}

	@Persist
	public void save() {
		List<Object> headers = new ArrayList<>();
		for (int i = 1; i < table.getColumnCount(); i++) {
			CustomTableColumn col = (CustomTableColumn) table.getColumn(i);
			InternalDataColumnEntity internalDataColumn = new InternalDataColumnEntity();
			internalDataColumn.setName(col.getText());
			internalDataColumn.setDataType(col.getDataType());
			internalDataColumn.setColumnIndex(i);

			headers.add(internalDataColumn);
		}
		List<List<Object>> datas = new ArrayList<>();
		for (TableItem tableItem : table.getItems()) {
			if (tableItem.getImage() != null) continue;
			List<Object> values = new ArrayList<>();
			for (int i = 1; i < table.getColumnCount(); i++) {
				String text = tableItem.getText(i);
				values.add(text == null ? "" : text);
			}
			datas.add(values);
		}

		try {
			String oldPk = dataFile.getId();
			String oldName = dataFile.getName();
			String oldIdForDisplay = TestDataController.getInstance().getIdForDisplay(dataFile);
			dataFile = updateInternalDataFileProperty(dataFile.getLocation(), txtName.getText(), txtDesc.getText(),
					dataFile.getDriver(), dataFile.getDataSourceUrl(), "", dataFile.getTableDataName(), headers, datas);
			updateDataFile(dataFile);
			dirtyable.setDirty(false);
			eventBroker.post(EventConstants.EXPLORER_REFRESH_TREE_ENTITY, null);
			if (!StringUtils.equalsIgnoreCase(oldName, dataFile.getName())) {
				eventBroker.post(EventConstants.EXPLORER_RENAMED_SELECTED_ITEM, new Object[] { oldIdForDisplay,
						TestDataController.getInstance().getIdForDisplay(dataFile) });
			}
			sendTestDataUpdatedEvent(oldPk);
		} catch (DuplicatedFileNameException e) {
			MultiStatusErrorDialog.showErrorDialog(e, StringConstants.PA_ERROR_MSG_UNABLE_TO_SAVE_TEST_DATA,
					MessageFormat.format(StringConstants.PA_ERROR_REASON_TEST_DATA_EXISTED, txtName.getText()));
		} catch (Exception e) {
			LoggerSingleton.logError(e);
			MultiStatusErrorDialog.showErrorDialog(e, StringConstants.PA_ERROR_MSG_UNABLE_TO_SAVE_TEST_DATA, e
					.getClass().getSimpleName());
		}
	}

	@Focus
	public void setFocus() {
		table.setFocus();
	}

	private DataFileEntity updateInternalDataFileProperty(String pk, String name, String description,
			DataFileDriverType dataFileDriver, String dataSourceURL, String sheetName, String tableName,
			List<Object> headerColumn, List<List<Object>> data) throws Exception {

		InternalDataFilePropertyEntity internalData = new InternalDataFilePropertyEntity();

		internalData.setPk(pk);
		internalData.setName(name);
		internalData.setDescription(description);
		internalData.setDataFileDriverName(dataFileDriver.name());
		internalData.setDataSourceURL(dataSourceURL);
		internalData.setSheetName(sheetName);
		internalData.setTableName(tableName);
		internalData.setHeaderColumn(headerColumn);
		internalData.setData(data);

		DataFileEntity dataFileEntity = TestDataController.getInstance().updateDataFile(internalData);
		return dataFileEntity;
	}

	private static class CustomTableColumn extends TableColumn {

		private String dataType;

		public CustomTableColumn(Table parent, int style) {
			super(parent, style);
		}

		public CustomTableColumn(Table parent, int style, int index) {
			super(parent, style, index);
		}

		public String getDataType() {
			return dataType;
		}

		public void setDataType(String dataType) {
			this.dataType = dataType;
		}

		@Override
		public void checkSubclass() {
		}
	}

	@Override
	protected Composite createFileInfoPart(Composite parent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void updateChildInfo(DataFileEntity dataFile) {

		clearTable();

		// Data columns
		createColumns(dataFile);

		loadDataRows(dataFile);

		// Last row with Add button
		createLastRow(table);
	}

	private void clearTable() {
		table.setRedraw(false);
		while (table.getColumnCount() > 0) {
			table.getColumns()[0].dispose();
		}
		table.removeAll();

		// Create No. column, read only
		TableColumn columnNumber = new CustomTableColumn(table, SWT.LEFT);
		columnNumber.setAlignment(SWT.LEFT);
		columnNumber.setWidth(50);
		columnNumber.setText(StringConstants.PA_COL_NO);
		table.setRedraw(true);
	}
}