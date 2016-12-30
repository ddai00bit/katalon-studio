package com.kms.katalon.composer.webservice.parts;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.wsdl.WSDLException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.javalite.http.Request;

import com.kms.katalon.composer.components.impl.dialogs.ProgressMonitorDialogWithThread;
import com.kms.katalon.composer.components.impl.util.KeyEventUtil;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.resources.constants.IImageKeys;
import com.kms.katalon.composer.resources.image.ImageManager;
import com.kms.katalon.composer.webservice.constants.ComposerWebserviceMessageConstants;
import com.kms.katalon.composer.webservice.constants.StringConstants;
import com.kms.katalon.composer.webservice.util.WSDLHelper;
import com.kms.katalon.composer.webservice.view.xml.ColorManager;
import com.kms.katalon.composer.webservice.view.xml.XMLConfiguration;
import com.kms.katalon.composer.webservice.view.xml.XMLPartitionScanner;
import com.kms.katalon.constants.GlobalMessageConstants;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.entity.repository.WebElementPropertyEntity;
import com.kms.katalon.entity.repository.WebServiceRequestEntity;

public class SoapServicePart extends WebServicePart {

    private static final String[] FILTER_EXTS = new String[] { "*.xml; *.wsdl; *.txt" };

    private static final String[] FILTER_NAMES = new String[] { "XML content files (*.xml, *.wsdl, *.txt)" };

    private CCombo ccbOperation;

    @Override
    protected void createAPIControls(Composite parent) {
        super.createAPIControls(parent);

        wsApiControl.addRequestMethodSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ccbOperation.removeAll();
            }
        });

        wsApiControl.addSendSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (dirtyable.isDirty()) {
                    boolean isOK = MessageDialog.openConfirm(null, StringConstants.WARN,
                            ComposerWebserviceMessageConstants.PART_MSG_DO_YOU_WANT_TO_SAVE_THE_CHANGES);
                    if (!isOK) {
                        return;
                    }
                    save();
                }

                // clear previous response
                responseHeader.setDocument(new Document());
                responseBody.setDocument(new Document());

                String requestURL = wsApiControl.getRequestURL().trim();
                if (isInvalidURL(requestURL) || ccbOperation.getText().isEmpty()) {
                    return;
                }

                try {
                    Shell activeShell = Display.getCurrent().getActiveShell();
                    new ProgressMonitorDialogWithThread(activeShell).run(true, true, new IRunnableWithProgress() {

                        @Override
                        public void run(IProgressMonitor monitor)
                                throws InvocationTargetException, InterruptedException {
                            monitor.beginTask(ComposerWebserviceMessageConstants.PART_MSG_SENDING_TEST_REQUEST,
                                    IProgressMonitor.UNKNOWN);
                            Display.getDefault().asyncExec(new Runnable() {

                                @Override
                                public void run() {
                                    try {
                                        tabResponse.getParent().setSelection(tabResponse);

                                        Request<?> response = WSDLHelper
                                                .newInstance(requestURL, getAuthorizationHeaderValue()).sendSOAPRequest(
                                                        wsApiControl.getRequestMethod(), ccbOperation.getText(),
                                                        httpHeaders, requestBody.getTextWidget().getText());
                                        if (response == null) {
                                            return;
                                        }

                                        responseHeader.setDocument(new Document(getPrettyHeaders(response)));

                                        String bodyContent = null;
                                        try {
                                            bodyContent = response.text();
                                        } catch (Exception e) {
                                            // Bad request. Ignore this.
                                        }

                                        if (bodyContent == null) {
                                            return;
                                        }

                                        try {
                                            bodyContent = formatXMLContent(bodyContent);
                                        } catch (DocumentException | IOException e) {
                                            // The responded message has issue with syntax, then reuse raw message.
                                        }
                                        responseBody.setDocument(createXMLDocument(bodyContent));
                                    } catch (WSDLException e) {
                                        LoggerSingleton.logError(e);
                                        ErrorDialog.openError(activeShell, StringConstants.ERROR_TITLE,
                                                ComposerWebserviceMessageConstants.PART_MSG_CANNOT_SEND_THE_TEST_REQUEST,
                                                new Status(Status.ERROR, WS_BUNDLE_NAME, e.getMessage(), e));
                                    } finally {
                                        monitor.done();
                                    }
                                }
                            });
                        }
                    });
                } catch (InvocationTargetException | InterruptedException ex) {
                    LoggerSingleton.logError(ex);
                }
            }
        });

        Composite operationComposite = new Composite(parent, SWT.NONE);
        GridLayout glOperation = new GridLayout(3, false);
        glOperation.marginWidth = 0;
        glOperation.marginHeight = 0;
        operationComposite.setLayout(glOperation);
        operationComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label lblOperation = new Label(operationComposite, SWT.NONE);
        lblOperation.setText(StringConstants.PA_LBL_SERVICE_FUNCTION);
        GridData gdLblOperation = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gdLblOperation.widthHint = 102;
        lblOperation.setLayoutData(gdLblOperation);

        ccbOperation = new CCombo(operationComposite, SWT.BORDER | SWT.FLAT | SWT.READ_ONLY);
        ccbOperation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        ccbOperation.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                setDirty();
            }
        });

        Button btnLoadFromWSDL = new Button(operationComposite, SWT.FLAT);
        btnLoadFromWSDL.setText(StringConstants.LBL_LOAD_FROM_WSDL);
        GridData gdBtnLoadFromWSDL = new GridData(SWT.CENTER, SWT.FILL, false, false);
        gdBtnLoadFromWSDL.widthHint = 100; // same width with send button
        btnLoadFromWSDL.setLayoutData(gdBtnLoadFromWSDL);
        btnLoadFromWSDL.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                // Load operations from WS
                String requestURL = wsApiControl.getRequestURL().trim();
                if (isInvalidURL(requestURL)) {
                    return;
                }

                try {
                    Shell activeShell = Display.getCurrent().getActiveShell();
                    new ProgressMonitorDialogWithThread(activeShell).run(true, true, new IRunnableWithProgress() {

                        @Override
                        public void run(IProgressMonitor monitor)
                                throws InvocationTargetException, InterruptedException {
                            monitor.beginTask(StringConstants.MSG_FETCHING_FROM_WSDL, IProgressMonitor.UNKNOWN);
                            Display.getDefault().asyncExec(new Runnable() {

                                @Override
                                public void run() {
                                    try {
                                        List<String> servFuncs = WSDLHelper
                                                .newInstance(requestURL, getAuthorizationHeaderValue())
                                                .getOperationNamesByRequestMethod(wsApiControl.getRequestMethod());
                                        ccbOperation.setItems(servFuncs.toArray(new String[0]));
                                        if (servFuncs.size() > 0) {
                                            ccbOperation.select(0);
                                        }
                                    } catch (WSDLException e) {
                                        LoggerSingleton.logError(e);
                                        MessageDialog.openError(activeShell, StringConstants.ERROR_TITLE,
                                                StringConstants.MSG_CANNOT_LOAD_WS);
                                    } finally {
                                        monitor.done();
                                    }
                                }
                            });
                        }
                    });
                } catch (InvocationTargetException | InterruptedException ex) {
                    LoggerSingleton.logError(ex);
                }
            }
        });
    }

    @Override
    protected void createParamsComposite(Composite parent) {
        // SOAP does not need params
    }

    @Override
    protected void addTabBody(TabFolder parent) {
        super.addTabBody(parent);
        tabBody.setText(StringConstants.PA_LBL_XML_REQ_MSG);
        Composite tabComposite = (Composite) tabBody.getControl();

        ToolBar toolbar = new ToolBar(tabComposite, SWT.FLAT | SWT.RIGHT);

        // TODO This feature will be added later
        // Start - Load From Operation
        // ToolItem tiLoadFromOperation = new ToolItem(toolbar, SWT.PUSH);
        // tiLoadFromOperation.setText(ComposerWebserviceMessageConstants.BTN_LOAD_FROM_OPERATION);
        // tiLoadFromOperation.setImage(ImageManager.getImage(IImageKeys.REFRESH_16));
        // tiLoadFromOperation.addSelectionListener(new SelectionAdapter() {
        //
        // @Override
        // public void widgetSelected(SelectionEvent e) {
        // if (!warningIfBodyNotEmpty()) {
        // return;
        // }
        // // Generate SOAP input message from selected Operation
        // try {
        // String soapMessageText = WSDLHelper.generateInputSOAPMessageText(wsApiControl.getRequestURL(),
        // getAuthorizationHeaderValue(), wsApiControl.getRequestMethod(), ccbOperation.getText());
        // requestBody.getTextWidget().setText(formatXMLContent(soapMessageText));
        // setDirty();
        // } catch (Exception ex) {
        // ErrorDialog.openError(null, StringConstants.ERROR_TITLE,
        // ComposerWebserviceMessageConstants.PART_MSG_CANNOT_FORMAT_THE_XML_CONTENT,
        // new Status(IStatus.ERROR, WS_BUNDLE_NAME, ex.getMessage(), ex));
        // }
        // }
        // });
        // End - Load From Operation

        ToolItem tiLoadFromFile = new ToolItem(toolbar, SWT.PUSH);
        tiLoadFromFile.setText(ComposerWebserviceMessageConstants.BTN_LOAD_FROM_FILE);
        tiLoadFromFile.setImage(ImageManager.getImage(IImageKeys.ATTACHMENT_16));
        tiLoadFromFile.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!warningIfBodyNotEmpty()) {
                    return;
                }
                // Load body template from file
                FileDialog dialog = new FileDialog(toolbar.getShell());
                dialog.setFilterNames(FILTER_NAMES);
                dialog.setFilterExtensions(FILTER_EXTS);
                dialog.setFilterPath(ProjectController.getInstance().getCurrentProject().getFolderLocation());
                String filePath = dialog.open();
                if (StringUtils.isEmpty(filePath)) {
                    return;
                }
                try {
                    String xmlContent = FileUtils.readFileToString(new File(filePath));
                    requestBody.setDocument(createXMLDocument(xmlContent));
                    setDirty();
                } catch (IOException ex) {
                    LoggerSingleton.logError(ex);
                }
            }
        });

        requestBody = createXMLSourceViewer(tabComposite);
        StyledText requestBodyWidget = requestBody.getTextWidget();

        Menu requestBodyContextMenu = requestBodyWidget.getMenu();
        new MenuItem(requestBodyContextMenu, SWT.SEPARATOR);
        MenuItem miFormat = new MenuItem(requestBodyContextMenu, SWT.PUSH);
        miFormat.setText(getLabelWithHotKeys(GlobalMessageConstants.FORMAT,
                new String[] { IKeyLookup.M1_NAME, IKeyLookup.SHIFT_NAME, "F" }));
        miFormat.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                formatRequestBody();
            }
        });

        requestBodyWidget.addMenuDetectListener(new MenuDetectListener() {

            @Override
            public void menuDetected(MenuDetectEvent e) {
                miFormat.setEnabled(requestBodyWidget.getEditable() && !requestBodyWidget.getText().isEmpty());
            }
        });

        requestBodyWidget.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                if (!requestBodyWidget.getEditable() || !requestBodyWidget.isFocusControl()) {
                    return;
                }

                if (KeyEventUtil.isKeysPressed(e, new String[] { IKeyLookup.M1_NAME, IKeyLookup.SHIFT_NAME, "F" })) {
                    formatRequestBody();
                }
            }
        });
    }

    @Override
    protected void addTabResponse(TabFolder parent) {
        super.addTabResponse(parent);
        Composite tabComposite = (Composite) tabResponse.getControl();
        responseBody = createXMLSourceViewer(tabComposite);
        responseBody.setEditable(false);
    }

    @Override
    protected void preSaving() {
        originalWsObject.setWsdlAddress(wsApiControl.getRequestURL());
        originalWsObject.setSoapRequestMethod(wsApiControl.getRequestMethod());
        originalWsObject.setSoapServiceFunction(ccbOperation.getText());

        tblHeaders.removeEmptyProperty();
        originalWsObject.setHttpHeaderProperties(httpHeaders);

        originalWsObject.setSoapBody(requestBody.getTextWidget().getText());
    }

    @Override
    protected void populateDataToUI() {
        wsApiControl.getRequestURLControl().setText(originalWsObject.getWsdlAddress());
        String soapRequestMethod = originalWsObject.getSoapRequestMethod();
        int index = Arrays.asList(WebServiceRequestEntity.SOAP_REQUEST_METHODS).indexOf(soapRequestMethod);
        wsApiControl.getRequestMethodControl().select(index < 0 ? 0 : index);
        ccbOperation.setText(originalWsObject.getSoapServiceFunction());

        tempPropList = new ArrayList<WebElementPropertyEntity>(originalWsObject.getHttpHeaderProperties());
        httpHeaders.clear();
        httpHeaders.addAll(tempPropList);
        tblHeaders.refresh();

        populateBasicAuthFromHeader();

        requestBody.setDocument(createXMLDocument(originalWsObject.getSoapBody()));
        dirtyable.setDirty(false);
    }

    private SourceViewer createXMLSourceViewer(Composite parent) {
        SourceViewer sv = createSourceViewer(parent, new GridData(SWT.FILL, SWT.FILL, true, true));
        sv.configure(new XMLConfiguration(new ColorManager()));
        return sv;
    }

    private IDocument createXMLDocument(String documentContent) {
        IDocument document = new Document(documentContent);
        IDocumentPartitioner partitioner = new FastPartitioner(new XMLPartitionScanner(),
                new String[] { XMLPartitionScanner.XML_START_TAG, XMLPartitionScanner.XML_PI,
                        XMLPartitionScanner.XML_END_TAG, XMLPartitionScanner.XML_TEXT, XMLPartitionScanner.XML_CDATA,
                        XMLPartitionScanner.XML_COMMENT });
        partitioner.connect(document);
        document.setDocumentPartitioner(partitioner);
        document.addDocumentListener(new IDocumentListener() {

            @Override
            public void documentChanged(DocumentEvent event) {
                setDirty();
            }

            @Override
            public void documentAboutToBeChanged(DocumentEvent event) {
                // do nothing
            }
        });
        return document;
    }

    private void formatRequestBody() {
        try {
            StyledText requestBodyWidget = requestBody.getTextWidget();
            String sw = formatXMLContent(requestBodyWidget.getText());
            requestBodyWidget.setText(sw);
            setDirty();
        } catch (Exception ex) {
            ErrorDialog.openError(null, StringConstants.ERROR_TITLE,
                    ComposerWebserviceMessageConstants.PART_MSG_CANNOT_FORMAT_THE_XML_CONTENT,
                    new Status(IStatus.ERROR, WS_BUNDLE_NAME, ex.getMessage(), ex));
        }
    }

    private String formatXMLContent(String content) throws DocumentException, IOException {
        org.dom4j.Document doc = DocumentHelper.parseText(content);
        StringWriter sw = new StringWriter();
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setIndent(TAB_SPACE);
        format.setNewLineAfterDeclaration(false);
        XMLWriter xw = new XMLWriter(sw, format);
        xw.write(doc);
        return sw.toString();
    }

    private String getAuthorizationHeaderValue() {
        for (WebElementPropertyEntity header : httpHeaders) {
            if (HttpHeaders.AUTHORIZATION.equals(header.getName())) {
                return header.getValue();
            }
        }
        return null;
    }

}
