package com.kms.katalon.composer.global.dialog;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.kms.katalon.composer.components.impl.dialogs.AbstractDialog;
import com.kms.katalon.composer.components.impl.util.ControlUtils;
import com.kms.katalon.composer.components.impl.util.TreeEntityUtil;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.global.constants.StringConstants;
import com.kms.katalon.composer.testcase.model.IInputValueType;
import com.kms.katalon.composer.testcase.model.InputValueType;
import com.kms.katalon.composer.testcase.support.AstInputBuilderValueTypeColumnSupport;
import com.kms.katalon.composer.testcase.util.AstTreeTableInputUtil;
import com.kms.katalon.composer.testcase.util.AstTreeTableTextValueUtil;
import com.kms.katalon.composer.testcase.util.AstTreeTableValueUtil;
import com.kms.katalon.core.ast.GroovyParser;
import com.kms.katalon.entity.global.GlobalVariableEntity;
import com.kms.katalon.groovy.constant.GroovyConstants;

public class GlobalVariableBuilderDialog extends AbstractDialog {
    private static final InputValueType[] defaultInputValueTypes = { InputValueType.String, InputValueType.Number,
            InputValueType.Boolean, InputValueType.Null, InputValueType.TestDataValue, InputValueType.TestObject,
            InputValueType.TestData, InputValueType.Property, InputValueType.List, InputValueType.Map };
    private static final String CUSTOM_TAG = "Global Variable";

    public enum DialogType {
        NEW, EDIT
    }

    private String dialogTitle;
    private GlobalVariableEntity fVariableEntity;
    private Point location;
    private TableViewer tableViewer;

    public GlobalVariableBuilderDialog(Shell parentShell, Point location) {
        this(parentShell, new GlobalVariableEntity("", "''"), DialogType.NEW, location);
    }

    public GlobalVariableBuilderDialog(Shell parentShell, GlobalVariableEntity variableEntity, Point location) {
        this(parentShell, variableEntity, DialogType.EDIT, location);
    }

    private GlobalVariableBuilderDialog(Shell parentShell, GlobalVariableEntity variableEntity, DialogType type,
            Point location) {
        super(parentShell);
        this.fVariableEntity = variableEntity.clone();
        this.location = location;
        switch (type) {
        case EDIT:
            dialogTitle = StringConstants.DIA_TITLE_EDIT_VAR;
            break;
        case NEW:
            dialogTitle = StringConstants.DIA_TITLE_NEW_VAR;
            break;
        }
    }

    @Override
    protected Control createDialogContainer(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout glContainer = new GridLayout(1, false);
        glContainer.horizontalSpacing = ControlUtils.DF_HORIZONTAL_SPACING;
        glContainer.verticalSpacing = ControlUtils.DF_VERTICAL_SPACING;
        container.setLayout(glContainer);

        Composite compositeTable = new Composite(container, SWT.NONE);
        compositeTable.setLayout(new FillLayout(SWT.HORIZONTAL));
        compositeTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        tableViewer = new TableViewer(compositeTable, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        ControlDecoration controlDecoration = new ControlDecoration(tableViewer.getTable(), SWT.LEFT | SWT.TOP);
        controlDecoration.setDescriptionText(StringConstants.DIA_CTRL_VAR_INFO);
        controlDecoration.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());

        TableViewerColumn tableViewerColumnName = new TableViewerColumn(tableViewer, SWT.NONE);
        tableViewerColumnName.setEditingSupport(new EditingSupport(tableViewer) {
            @Override
            protected CellEditor getCellEditor(Object element) {
                return new TextCellEditor((Composite) this.getViewer().getControl());
            }

            @Override
            protected boolean canEdit(Object element) {
                return true;
            }

            @Override
            protected Object getValue(Object element) {
                if (element != null && element instanceof GlobalVariableEntity) {
                    return ((GlobalVariableEntity) element).getName();
                }
                return "";
            }

            @Override
            protected void setValue(Object element, Object value) {
                if (element != null && element instanceof GlobalVariableEntity && value != null
                        && value instanceof String) {
                    GlobalVariableEntity param = (GlobalVariableEntity) element;
                    String newParamName = (String) value;
                    if (!newParamName.equals(param.getName())) {
                        param.setName(newParamName);
                        getViewer().update(element, null);
                        refresh();
                    }
                }
            }
        });
        tableViewerColumnName.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element != null && element instanceof GlobalVariableEntity) {
                    return ((GlobalVariableEntity) element).getName();
                }
                return "";
            }
        });
        TableColumn tblclmnName = tableViewerColumnName.getColumn();
        tblclmnName.setWidth(100);
        tblclmnName.setText(StringConstants.PA_COL_NAME);

        TableViewerColumn tableViewerColumnDefaultValueType = new TableViewerColumn(tableViewer, SWT.NONE);
        tableViewerColumnDefaultValueType.setEditingSupport(new AstInputBuilderValueTypeColumnSupport(tableViewer,
                defaultInputValueTypes, CUSTOM_TAG, null, null) {
            @Override
            protected boolean canEdit(Object element) {
                if (element instanceof GlobalVariableEntity) {
                    return true;
                }
                return false;
            }

            @Override
            protected Object getValue(Object element) {
                if (element instanceof GlobalVariableEntity) {
                    try {
                        ASTNode astNode = GroovyParser
                                .parseGroovyScriptAndGetFirstItem(((GlobalVariableEntity) element).getInitValue());
                        IInputValueType valueType = AstTreeTableValueUtil.getTypeValue(astNode, null);
                        return inputValueTypeNames.indexOf(valueType.getName());
                    } catch (Exception e) {
                        LoggerSingleton.logError(e);
                    }
                }
                return 0;
            }

            @Override
            protected void setValue(Object element, Object value) {
                if (element instanceof GlobalVariableEntity && value instanceof Integer && (int) value > -1
                        && (int) value < inputValueTypeNames.size()) {
                    try {
                        ASTNode astNode = GroovyParser
                                .parseGroovyScriptAndGetFirstItem(((GlobalVariableEntity) element).getInitValue());
                        String newValueTypeString = inputValueTypeNames.get((int) value);
                        IInputValueType newValueType = AstTreeTableInputUtil
                                .getInputValueTypeFromString(newValueTypeString);
                        IInputValueType oldValueType = AstTreeTableValueUtil.getTypeValue(astNode, null);
                        if (newValueType != oldValueType) {
                            ASTNode newAstNode = (ASTNode) newValueType.getNewValue(astNode);
                            StringBuilder stringBuilder = new StringBuilder();
                            GroovyParser groovyParser = new GroovyParser(stringBuilder);
                            groovyParser.parse(newAstNode);
                            ((GlobalVariableEntity) element).setInitValue(stringBuilder.toString());
                            this.getViewer().update(element, null);
                            refresh();
                        }
                    } catch (Exception e) {
                        LoggerSingleton.logError(e);
                    }

                }
            }
        });
        TableColumn tblclmnDefaultValueType = tableViewerColumnDefaultValueType.getColumn();
        tblclmnDefaultValueType.setWidth(100);
        tblclmnDefaultValueType.setText(StringConstants.PA_COL_DEFAULT_VALUE_TYPE);
        tableViewerColumnDefaultValueType.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element != null && element instanceof GlobalVariableEntity) {
                    if (element instanceof GlobalVariableEntity) {
                        try {
                            IInputValueType valueType = AstTreeTableValueUtil.getTypeValue(GroovyParser
                                    .parseGroovyScriptAndGetFirstItem(((GlobalVariableEntity) element).getInitValue()),
                                    null);
                            if (valueType != null) {
                                return TreeEntityUtil.getReadableKeywordName(valueType.getName());
                            }
                        } catch (Exception e) {
                            LoggerSingleton.logError(e);
                        }
                    }
                }
                return "";
            }
        });

        TableViewerColumn tableViewerColumnDefaultValue = new TableViewerColumn(tableViewer, SWT.NONE);
        tableViewerColumnDefaultValue.setEditingSupport(new EditingSupport(tableViewer) {

            @Override
            protected CellEditor getCellEditor(Object element) {
                try {
                    ASTNode astNode = GroovyParser.parseGroovyScriptAndGetFirstItem(((GlobalVariableEntity) element)
                            .getInitValue());
                    IInputValueType inputValueType = AstTreeTableValueUtil.getTypeValue(astNode, null);
                    if (inputValueType != null) {
                        return inputValueType
                                .getCellEditorForValue((Composite) getViewer().getControl(), astNode, null);
                    }
                } catch (Exception e) {
                    LoggerSingleton.logError(e);
                }
                return null;
            }

            @Override
            protected boolean canEdit(Object element) {
                if (element instanceof GlobalVariableEntity) {
                    return true;
                }
                return false;
            }

            @Override
            protected Object getValue(Object element) {
                if (element instanceof GlobalVariableEntity) {
                    try {
                        ASTNode astNode = GroovyParser
                                .parseGroovyScriptAndGetFirstItem(((GlobalVariableEntity) element).getInitValue());
                        IInputValueType inputValueType = AstTreeTableValueUtil.getTypeValue(astNode, null);
                        if (inputValueType != null) {
                            return inputValueType.getValueToEdit(astNode, null);
                        }
                    } catch (Exception e) {
                        LoggerSingleton.logError(e);
                    }
                    return ((GlobalVariableEntity) element).getInitValue();
                }
                return "";
            }

            @Override
            protected void setValue(Object element, Object value) {
                if (element != null && element instanceof GlobalVariableEntity && value != null) {
                    try {
                        ASTNode astNode = GroovyParser
                                .parseGroovyScriptAndGetFirstItem(((GlobalVariableEntity) element).getInitValue());
                        IInputValueType inputValueType = AstTreeTableValueUtil.getTypeValue(astNode, null);
                        if (inputValueType != null) {
                            Object object = inputValueType.changeValue(
                                    astNode instanceof ExpressionStatement ? ((ExpressionStatement) astNode)
                                            .getExpression() : astNode, value, null);
                            if (object instanceof ASTNode) {
                                ASTNode newAstNode = (ASTNode) object;
                                StringBuilder stringBuilder = new StringBuilder();
                                GroovyParser groovyParser = new GroovyParser(stringBuilder);
                                groovyParser.parse(newAstNode);
                                ((GlobalVariableEntity) element).setInitValue(stringBuilder.toString());
                                this.getViewer().update(element, null);
                                refresh();
                            }
                        }
                    } catch (Exception e) {
                        LoggerSingleton.logError(e);
                    }
                }
            }
        });
        TableColumn tblclmnDefaultValue = tableViewerColumnDefaultValue.getColumn();
        tblclmnDefaultValue.setWidth(200);
        tblclmnDefaultValue.setText(StringConstants.PA_COL_DEFAULT_VALUE);
        tableViewerColumnDefaultValue.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element != null && element instanceof GlobalVariableEntity) {
                    try {
                        ASTNode astNode = GroovyParser
                                .parseGroovyScriptAndGetFirstItem(((GlobalVariableEntity) element).getInitValue());
                        return AstTreeTableTextValueUtil.getInstance().getTextValue(astNode);
                    } catch (Exception e) {
                        LoggerSingleton.logError(e);
                    }
                }
                return "";
            }
        });

        TableViewerColumn tableViewerColumnDescription = new TableViewerColumn(tableViewer, SWT.NONE);
        tableViewerColumnDescription.setEditingSupport(new EditingSupport(tableViewer) {
            @Override
            protected CellEditor getCellEditor(Object element) {
                return new TextCellEditor((Composite) this.getViewer().getControl());
            }

            @Override
            protected boolean canEdit(Object element) {
                if (element instanceof GlobalVariableEntity) {
                    return true;
                }
                return false;
            }

            @Override
            protected Object getValue(Object element) {
                if (element instanceof GlobalVariableEntity) {
                    return ((GlobalVariableEntity) element).getDescription();
                }
                return "";
            }

            @Override
            protected void setValue(Object element, Object value) {
                if (element instanceof GlobalVariableEntity && value instanceof String) {
                    GlobalVariableEntity param = (GlobalVariableEntity) element;
                    String newParamDesc = (String) value;
                    if (!newParamDesc.equals(param.getDescription())) {
                        param.setDescription(newParamDesc);
                        getViewer().update(element, null);
                        refresh();
                    }
                }
            }
        });
        tableViewerColumnDescription.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element != null && element instanceof GlobalVariableEntity) {
                    return ((GlobalVariableEntity) element).getDescription();
                }
                return "";
            }
        });
        TableColumn tblColumnDescription = tableViewerColumnDescription.getColumn();
        tblColumnDescription.setWidth(200);
        tblColumnDescription.setText(StringConstants.PA_COL_DESCRIPTION);

        tableViewer.setContentProvider(new ArrayContentProvider());

        return container;
    }

    @Override
    protected Point getInitialSize() {
        return new Point(650, 250);
    }

    private void refresh() {
        tableViewer.setInput(new Object[] { fVariableEntity });
        validate();
    }

    private void validate() {
        boolean enable = true;
        String newVariableName = fVariableEntity.getName();
        if (!GroovyConstants.VARIABLE_NAME_REGEX.matcher(newVariableName).find()) {
            enable &= false;
        } else {
            enable &= true;
        }
        getButton(OK).setEnabled(enable);
    }

    public String getDialogTitle() {
        return dialogTitle != null ? dialogTitle : StringConstants.EMPTY;
    }

    @Override
    protected final void setInput() {
        refresh();
    }

    public GlobalVariableEntity getVariableEntity() {
        return fVariableEntity;
    }

    @Override
    public Point getInitialLocation(Point initialSize) {
        return new Point(this.location.x - initialSize.x - 10, this.location.y);
    }

    @Override
    protected void registerControlModifyListeners() {
        // Do nothing

    }
}
