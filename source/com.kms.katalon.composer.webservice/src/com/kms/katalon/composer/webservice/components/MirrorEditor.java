package com.kms.katalon.composer.webservice.components;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.osgi.framework.FrameworkUtil;

import com.kms.katalon.composer.components.impl.dialogs.MultiStatusErrorDialog;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.webservice.constants.ComposerWebserviceMessageConstants;
import com.kms.katalon.composer.webservice.constants.TextContentType;
import com.kms.katalon.composer.webservice.editor.DocumentReadyHandler;
import com.kms.katalon.core.util.internal.ExceptionsUtil;
import com.kms.katalon.execution.classpath.ClassPathResolver;

public class MirrorEditor extends Composite {

    private static final String RESOURCES_TEMPLATE_EDITOR = "resources/template/editor";

    private Browser browser;

    private boolean documentReady = false;

    private DocumentReadyHandler documentReadyHandler;

    private File templateFile;

    // A collection of mirror modes for some text types
    private static final Map<String, String> TEXT_MODE_COLLECTION;

    static {
        TEXT_MODE_COLLECTION = new HashMap<>();
        TEXT_MODE_COLLECTION.put(TextContentType.TEXT.getText(), "text/plain");
        TEXT_MODE_COLLECTION.put(TextContentType.JSON.getText(), "application/ld+json");
        TEXT_MODE_COLLECTION.put(TextContentType.XML.getText(), "application/xml");
        TEXT_MODE_COLLECTION.put(TextContentType.HTML.getText(), "text/html");
        TEXT_MODE_COLLECTION.put(TextContentType.JAVASCRIPT.getText(), "application/javascript");

    }

    public MirrorEditor(Composite parent, int style) {
        super(parent, style);

        GridLayout gridLayout = new GridLayout();
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        this.setLayout(gridLayout);
        this.setLayoutData(new GridData(GridData.FILL_BOTH));

        browser = new Browser(this, style);
        browser.setLayoutData(new GridData(GridData.FILL_BOTH));
        browser.setJavascriptEnabled(true);

        templateFile = initHTMLTemplateFile();
        try {
            browser.setUrl(templateFile.toURI().toURL().toString());
        } catch (IOException e) {
            LoggerSingleton.logError(e);
        }

        browser.addProgressListener(new ProgressListener() {

            @Override
            public void completed(ProgressEvent event) {
                documentReady = true;
                onDocumentReady();
                
                browser.evaluate("document.addEventListener('contextmenu', function(e) { e.preventDefault();});");
            }

            @Override
            public void changed(ProgressEvent event) {
            }
        });

    }

    private File initHTMLTemplateFile() {
        File templateFile = null;
        try {
            File codeMirrorTempFolder = new File(ClassPathResolver.getConfigurationFolder(),
                    "resources/template/editor/codemirror");
            if (!codeMirrorTempFolder.exists() || ArrayUtils.isEmpty(codeMirrorTempFolder.listFiles())) {
                codeMirrorTempFolder.mkdirs();

                File bundleLocation = FileLocator.getBundleFile(FrameworkUtil.getBundle(MirrorEditor.class));

                if (bundleLocation.isDirectory()) {
                    FileUtils.copyDirectory(new File(bundleLocation, RESOURCES_TEMPLATE_EDITOR),
                            codeMirrorTempFolder.getParentFile());
                } else {
                    FileUtils.copyDirectory(
                            new File(ClassPathResolver.getConfigurationFolder(), RESOURCES_TEMPLATE_EDITOR),
                            codeMirrorTempFolder.getParentFile());
                }
            }
            templateFile = new File(codeMirrorTempFolder,
                    String.format("template_%d.html", System.currentTimeMillis()));
            FileUtils.copyFile(new File(codeMirrorTempFolder, "template.html"), templateFile);

        } catch (IOException e) {
            MultiStatusErrorDialog.showErrorDialog(ComposerWebserviceMessageConstants.PA_MSG_UNABLE_TO_OPEN_BODY_EDITOR,
                    e.getMessage(), ExceptionsUtil.getMessageForThrowable(e));
        }
        return templateFile;
    }

    public void setEditable(boolean editable) {
        String command = MessageFormat.format("editor.setOption(\"{0}\", {1});", "readOnly", !editable);
        if (documentReady) {
            browser.evaluate(command);
        } else {
            (new SettingOptionsThread(browser, command)).start();
        }
    }

    public void setText(String text) {

        String setTextCommand = String.format("editor.setValue(\"%s\");", StringEscapeUtils.escapeEcmaScript(text));
        if (documentReady) {
            browser.evaluate(setTextCommand);
        } else {
            (new SettingOptionsThread(browser, setTextCommand)).start();
        }
    }

    public void wrapLine(boolean wrapped) {
        browser.evaluate(MessageFormat.format("editor.setOption(\"{0}\", {1});", "lineWrapping", wrapped));
    }

    public Object evaluate(String script) {
        return browser.evaluate(script);
    }

    public void registerDocumentHandler(DocumentReadyHandler handler) {
        this.documentReadyHandler = handler;
    }

    public void changeMode(String text) {
        String textType = TEXT_MODE_COLLECTION.keySet()
                .stream()
                .filter(key -> text.toLowerCase().startsWith(key.toLowerCase()))
                .findFirst()
                .orElse(TextContentType.TEXT.getText());

        String mode = TEXT_MODE_COLLECTION.get(textType);

        String command = MessageFormat.format("changeMode(editor, \"{0}\");", mode);
        if (documentReady) {
            browser.evaluate(command);
        } else {
            (new SettingOptionsThread(browser, command)).start();
        }
    }

    public void beautify() {
        String command = String.format("format(editor); "
        								+ "editor.focus(); "
        								+ "editor.setCursor({line: 0, ch: 0});");
        if (documentReady) {
            browser.evaluate(command);
        } else {
            (new SettingOptionsThread(browser, command)).start();
        }
    }
    
    public String getText() {
        String command = "return editor.getValue();";
        return browser.evaluate(command).toString();
    }

    private void onDocumentReady() {
        if (documentReadyHandler != null) {
            documentReadyHandler.onDocumentReady();
        }

        new BrowserFunction(browser, "handleEditorChanged") {
            @Override
            public Object function(Object[] objects) {
                MirrorEditor.this.notifyListeners(SWT.Modify, new Event());
                return null;
            }
        };

        addDisposeListener(e -> {
            if (templateFile != null && templateFile.exists()) {
                templateFile.delete();
            }
        });
    }

}