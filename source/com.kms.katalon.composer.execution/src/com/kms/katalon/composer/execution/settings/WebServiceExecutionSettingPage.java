package com.kms.katalon.composer.execution.settings;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.kms.katalon.application.utils.LicenseUtil;
import com.kms.katalon.composer.components.impl.handler.KSEFeatureAccessHandler;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.execution.constants.ComposerExecutionMessageConstants;
import com.kms.katalon.execution.webservice.setting.WebServiceExecutionSettingStore;
import com.kms.katalon.feature.KSEFeature;

public class WebServiceExecutionSettingPage extends AbstractExecutionSettingPage {

    private WebServiceExecutionSettingStore webServiceSettingStore;

    private Text txtConnectionTimeout, txtSocketTimeout;

    public WebServiceExecutionSettingPage() {
        webServiceSettingStore = WebServiceExecutionSettingStore.getStore();
    }

    @Override
    protected Composite createSettingsArea(Composite container) {
        createHTTPSettings(container);
        return container;
    }

    private void createHTTPSettings(Composite parent) {
        Group grpHTTPSettings = new Group(parent, SWT.NONE);
        grpHTTPSettings.setText(ComposerExecutionMessageConstants.PREF_WEBSERVICE_GROUP_LBL_HTTP);
        GridLayout glGrpHTTPSettings = new GridLayout();
        grpHTTPSettings.setLayout(glGrpHTTPSettings);
        grpHTTPSettings.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));

        Composite httpComposite = new Composite(grpHTTPSettings, SWT.NONE);
        GridLayout glHTTPComposite = new GridLayout(2, false);
        glHTTPComposite.verticalSpacing = 10;
        glHTTPComposite.marginHeight = 0;
        glHTTPComposite.marginWidth = 0;
        httpComposite.setLayout(glHTTPComposite);

        Label lblConnectionTimeout = new Label(httpComposite, SWT.NONE);
        lblConnectionTimeout.setText(ComposerExecutionMessageConstants.PREF_WEBSERVICE_LBL_CONNECTION_TIMEOUT);
        GridData gdLblConnectionTimeout = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        lblConnectionTimeout.setLayoutData(gdLblConnectionTimeout);

        txtConnectionTimeout = new Text(httpComposite, SWT.BORDER);
        GridData gdTxtConnectionTimeout = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gdTxtConnectionTimeout.widthHint = INPUT_WIDTH;
        txtConnectionTimeout.setLayoutData(gdTxtConnectionTimeout);
        txtConnectionTimeout.setEnabled(canCustomizeRequestTimeout());

        Label lblSocketTimeout = new Label(httpComposite, SWT.NONE);
        lblSocketTimeout.setText(ComposerExecutionMessageConstants.PREF_WEBSERVICE_LBL_SOCKET_TIMEOUT);
        GridData gdLblSocketTimeout = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        lblSocketTimeout.setLayoutData(gdLblSocketTimeout);

        txtSocketTimeout = new Text(httpComposite, SWT.BORDER);
        GridData gdTxtSocketTimeout = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gdTxtSocketTimeout.widthHint = INPUT_WIDTH;
        txtSocketTimeout.setLayoutData(gdTxtSocketTimeout);
        txtSocketTimeout.setEnabled(canCustomizeRequestTimeout());
    }

    @Override
    protected void registerListeners() {
        addNumberVerification(txtConnectionTimeout, TIMEOUT_MIN_VALUE_IN_MILISEC, TIMEOUT_MAX_VALUE_IN_MILISEC, true,
                StringUtils.EMPTY);
        addNumberVerification(txtSocketTimeout, TIMEOUT_MIN_VALUE_IN_MILISEC, TIMEOUT_MAX_VALUE_IN_MILISEC, true,
                StringUtils.EMPTY);

        if (!canCustomizeRequestTimeout()) {
            KSEFeatureAccessHandler.handleUnauthorizedAccess(KSEFeature.CUSTOM_WEB_SERVICE_REQUEST_TIMEOUT);
        }
    }

    @Override
    protected void initialize() throws IOException {
        int connectionTimeout = webServiceSettingStore.getConnectionTimeout();
        boolean isDefaultConnectionTimeout = connectionTimeout == WebServiceExecutionSettingStore.EXECUTION_DEFAULT_CONNECTION_TIMEOUT_MS;
        if (!isDefaultConnectionTimeout) {
            txtConnectionTimeout.setText(String.valueOf(connectionTimeout));
        }
        int socketTimeout = webServiceSettingStore.getSocketTimeout();
        boolean isDefaultSocketTimeout = socketTimeout == WebServiceExecutionSettingStore.EXECUTION_DEFAULT_SOCKET_TIMEOUT_MS;
        if (!isDefaultSocketTimeout) {
            txtSocketTimeout.setText(String.valueOf(socketTimeout));
        }
    }

    @Override
    protected void performDefaults() {
        if (!canCustomizeRequestTimeout()) {
            return;
        }

        txtConnectionTimeout.setText(StringUtils.EMPTY);
        txtSocketTimeout.setText(StringUtils.EMPTY);

        super.performDefaults();
    }

    @Override
    protected boolean saveSettings() {
        if (!isValid()) {
            return false;
        }

        try {
            int connectionTimeout = StringUtils.isNotBlank(txtConnectionTimeout.getText())
                    ? Integer.parseInt(txtConnectionTimeout.getText())
                    : WebServiceExecutionSettingStore.EXECUTION_DEFAULT_CONNECTION_TIMEOUT_MS;
            webServiceSettingStore.setConnectionTimeout(connectionTimeout);

            int socketTimeout = StringUtils.isNotBlank(txtSocketTimeout.getText())
                    ? Integer.parseInt(txtSocketTimeout.getText())
                    : WebServiceExecutionSettingStore.EXECUTION_DEFAULT_SOCKET_TIMEOUT_MS;
            webServiceSettingStore.setSocketTimeout(socketTimeout);
        } catch (IOException error) {
            LoggerSingleton.logError(error);
            return false;
        }

        return true;
    }

    @Override
    protected boolean hasChanged() {
        if (!isValid()) {
            return false;
        }

        try {
            int originalConnectionTimeout = webServiceSettingStore.getConnectionTimeout();
            int currentConnectionTimeout = StringUtils.isNotBlank(txtConnectionTimeout.getText())
                    ? Integer.parseInt(txtConnectionTimeout.getText())
                    : WebServiceExecutionSettingStore.EXECUTION_DEFAULT_CONNECTION_TIMEOUT_MS;
            boolean hasConnectionTimeoutChanged = currentConnectionTimeout != originalConnectionTimeout;

            int originalSocketTimeout = webServiceSettingStore.getSocketTimeout();
            int currentSocketTimeout = StringUtils.isNotBlank(txtSocketTimeout.getText())
                    ? Integer.parseInt(txtSocketTimeout.getText())
                    : WebServiceExecutionSettingStore.EXECUTION_DEFAULT_SOCKET_TIMEOUT_MS;
            boolean hasSocketTimeoutChanged = currentSocketTimeout != originalSocketTimeout;

            return hasConnectionTimeoutChanged || hasSocketTimeoutChanged;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean canCustomizeRequestTimeout() {
        return LicenseUtil.isNotFreeLicense();
    }
}
