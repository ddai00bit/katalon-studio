package com.kms.katalon.composer.integration.analytics.preferences;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.kms.katalon.application.constants.ApplicationStringConstants;
import com.kms.katalon.application.utils.ApplicationInfo;
import com.kms.katalon.composer.components.dialogs.FieldEditorPreferencePageWithHelp;
import com.kms.katalon.composer.components.event.EventBrokerSingleton;
import com.kms.katalon.composer.components.impl.dialogs.MultiStatusErrorDialog;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.components.services.UISynchronizeService;
import com.kms.katalon.composer.components.util.ColorUtil;
import com.kms.katalon.composer.integration.analytics.constants.ComposerIntegrationAnalyticsMessageConstants;
import com.kms.katalon.composer.integration.analytics.dialog.NewProjectDialog;
import com.kms.katalon.constants.ActivationPreferenceConstants;
import com.kms.katalon.constants.DocumentationMessageConstants;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.integration.analytics.constants.ComposerAnalyticsStringConstants;
import com.kms.katalon.integration.analytics.entity.AnalyticsOrganization;
import com.kms.katalon.integration.analytics.entity.AnalyticsProject;
import com.kms.katalon.integration.analytics.entity.AnalyticsTeam;
import com.kms.katalon.integration.analytics.entity.AnalyticsTokenInfo;
import com.kms.katalon.integration.analytics.handler.AnalyticsAuthorizationHandler;
import com.kms.katalon.integration.analytics.setting.AnalyticsSettingStore;
import com.kms.katalon.plugin.dialog.KStoreLoginDialog;
import com.kms.katalon.preferences.internal.PreferenceStoreManager;
import com.kms.katalon.preferences.internal.ScopedPreferenceStore;
import com.kms.katalon.util.CryptoUtil;

public class AnalyticsPreferencesPage extends FieldEditorPreferencePageWithHelp {

    private Composite container;

    private Composite enablerComposite;

    private Composite mainComposite;

    private Button btnConnect;

    private Button enableAnalyticsIntegration;

    private Text txtServerUrl, txtEmail, txtOrganization;

    private Label lblStatus;
    
    private Label lblStatusAccessProject;

    private Combo cbbProjects;

    private Combo cbbTeams;

    private List<AnalyticsProject> projects = new ArrayList<>();

    private List<AnalyticsTeam> teams = new ArrayList<>();
    
    private AnalyticsProject selectProjectFromConfig;
    
    private AnalyticsTeam selectTeamFromConfig;
    
    private boolean canAccessProject = true;

    private Button btnCreate;

    private AnalyticsSettingStore analyticsSettingStore;

    private Button chckEncrypt;

    private String email, password, serverUrl;

    private AnalyticsOrganization organization;

    public AnalyticsPreferencesPage() {
        analyticsSettingStore = new AnalyticsSettingStore(
                ProjectController.getInstance().getCurrentProject().getFolderLocation());
        try {
            boolean encryptionEnabled = analyticsSettingStore.isEncryptionEnabled();
            serverUrl = analyticsSettingStore.getServerEndpoint(analyticsSettingStore.isEncryptionEnabled());
            email = analyticsSettingStore.getEmail(encryptionEnabled);
            password = analyticsSettingStore.getPassword(encryptionEnabled);
            organization = analyticsSettingStore.getOrganization();
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        enablerComposite = new Composite(container, SWT.NONE);
        enablerComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        enablerComposite.setLayout(new GridLayout());

        enableAnalyticsIntegration = new Button(enablerComposite, SWT.CHECK);
        enableAnalyticsIntegration.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        enableAnalyticsIntegration
                .setText(ComposerIntegrationAnalyticsMessageConstants.LBL_ENABLE_ANALYTICS_INTEGRATION);

        mainComposite = new Composite(container, SWT.NONE);
        mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        GridLayout glMainComposite = new GridLayout(1, false);
        glMainComposite.marginWidth = 0;
        glMainComposite.marginHeight = 0;
        mainComposite.setLayout(glMainComposite);

        createAuthenticationGroup();
        createSelectGroup();

        addListeners();
        initialize();

        return container;
    }

    private void createAuthenticationGroup() {
        Group grpAuthentication = new Group(mainComposite, SWT.NONE);
        grpAuthentication.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        GridLayout glGrpAuthentication = new GridLayout(2, false);
        glGrpAuthentication.horizontalSpacing = 15;
        grpAuthentication.setLayout(glGrpAuthentication);
        grpAuthentication.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_AUTHENTICATE_GROUP);

        Label lblServerUrl = new Label(grpAuthentication, SWT.NONE);
        lblServerUrl.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_SERVER_URL);

        txtServerUrl = new Text(grpAuthentication, SWT.BORDER);
        txtServerUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        txtServerUrl.setEnabled(false);

        Label lblOrganization = new Label(grpAuthentication, SWT.NONE);
        lblOrganization.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_ORGANIZATION);
        
        txtOrganization = new Text(grpAuthentication, SWT.BORDER);
        txtOrganization.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        txtOrganization.setEnabled(false);

        Label lblEmail = new Label(grpAuthentication, SWT.NONE);
        lblEmail.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_EMAIL);

        txtEmail = new Text(grpAuthentication, SWT.BORDER);
        txtEmail.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        txtEmail.setEnabled(false);

        chckEncrypt = new Button(grpAuthentication, SWT.CHECK);
        chckEncrypt.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true, 2, 1));
        chckEncrypt.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_ENABLE_ANTHENTICATION_ENCRYPTION);
        chckEncrypt.setEnabled(false);
    }

    private void createSelectGroup() {
        Group grpSelect = new Group(mainComposite, SWT.NONE);
        grpSelect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        GridLayout glGrpSelect = new GridLayout(4, false);
        grpSelect.setLayout(glGrpSelect);
        grpSelect.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_SELECT_GROUP);

        Label lblTeam = new Label(grpSelect, SWT.NONE);
        lblTeam.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_TEAM);

        cbbTeams = new Combo(grpSelect, SWT.READ_ONLY);
        cbbTeams.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        cbbTeams.setEnabled(false);

        Label lblProject = new Label(grpSelect, SWT.NONE);
        lblProject.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_PROJECT);

        cbbProjects = new Combo(grpSelect, SWT.READ_ONLY);
        cbbProjects.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        cbbProjects.setEnabled(false);

        btnCreate = new Button(grpSelect, SWT.NONE);
        btnCreate.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        btnCreate.setText(ComposerIntegrationAnalyticsMessageConstants.BTN_NEW_PROJECT);
        btnCreate.setEnabled(false);

        Composite compConnect = new Composite(grpSelect, SWT.NONE);
        compConnect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
        GridLayout glConnect = new GridLayout(4, false);
        compConnect.setLayout(glConnect);

        GridData gdBtn = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gdBtn.widthHint = 100;
        
        btnConnect = new Button(compConnect, SWT.NONE);
        btnConnect.setLayoutData(gdBtn);
        btnConnect.setText(ComposerIntegrationAnalyticsMessageConstants.BTN_CONNECT);
        btnConnect.setEnabled(false);

        lblStatus = new Label(compConnect, SWT.NONE);
        lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        lblStatusAccessProject = new Label(compConnect, SWT.NONE);
        lblStatusAccessProject.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        lblStatusAccessProject.setForeground(ColorUtil.getTextErrorColor());
    }

    @Override
    protected void initialize() {
        super.initialize();
        if (isNoInfo()) {
            katalonLogin();
        } else {
            fillData();
        }
    }

    @Override
    protected void createFieldEditors() {
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        if (!isInitialized()) {
            return;
        }
        changeEnabled();
        updateDataStore();
    }

    @Override
    public boolean performOk() {
        
        if (!isInitialized()) {
            return true;
        }
        
        boolean integrationEnabled = enableAnalyticsIntegration.getSelection();
        
        if (!integrationEnabled) {
            updateDataStore();
            return true;
        }

        if (cbbTeams.getSelectionIndex() == -1) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), ComposerAnalyticsStringConstants.ERROR,
                    ComposerIntegrationAnalyticsMessageConstants.REPORT_MSG_MUST_SET_TEAM);
            return false;
        }
        
        if (cbbProjects.getSelectionIndex() == -1) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), ComposerAnalyticsStringConstants.ERROR,
                    ComposerIntegrationAnalyticsMessageConstants.REPORT_MSG_MUST_SET_PROJECT);
            return false;
        }

        if (cbbProjects.getSelectionIndex() == -1) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), ComposerAnalyticsStringConstants.ERROR,
                    ComposerIntegrationAnalyticsMessageConstants.REPORT_MSG_MUST_SET_PROJECT);
            return false;
        }

        if (StringUtils.isEmpty(txtEmail.getText())
                || StringUtils.isEmpty(txtServerUrl.getText())) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), ComposerAnalyticsStringConstants.ERROR,
                    ComposerIntegrationAnalyticsMessageConstants.REPORT_MSG_MUST_ENTER_REQUIRED_INFORMATION);
            return false;
        }

        updateDataStore();
        return super.performOk();
    }

    @Override
    public boolean performCancel() {
        try {
            analyticsSettingStore.enableIntegration(isIntegratedSuccessfully());
            IEventBroker eventBroker = EventBrokerSingleton.getInstance().getEventBroker();
            eventBroker.post(EventConstants.IS_INTEGRATED, isIntegratedSuccessfully());
        } catch (IOException e) {
            LoggerSingleton.logError(e);
        }
        return super.performCancel();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (FieldEditor.VALUE.equals(event.getProperty())) {
            handleFieldEditorValueChanged(event);
        }
        super.propertyChange(event);
    }

    private void handleFieldEditorValueChanged(PropertyChangeEvent event) {
        if (event.getSource() == enableAnalyticsIntegration) {
            changeEnabled();
        }
    }

    private void fillData() {
        try {
            boolean encryptionEnabled = analyticsSettingStore.isEncryptionEnabled();
            enableAnalyticsIntegration.setSelection(encryptionEnabled);

            cbbTeams.setItems();
            cbbProjects.setItems();

            chckEncrypt.setSelection(encryptionEnabled);
            txtEmail.setText(email);
            txtServerUrl.setText(serverUrl);
            txtOrganization.setText(analyticsSettingStore.getOrganization().getName());
            
            selectProjectFromConfig = analyticsSettingStore.getProject();
            selectTeamFromConfig = analyticsSettingStore.getTeam();
            
            teams.clear();
            projects.clear();
            
            if (selectTeamFromConfig != null) {
                teams.add(selectTeamFromConfig);
                cbbTeams.setItems(AnalyticsAuthorizationHandler.getTeamNames(teams).toArray(new String[teams.size()]));
                int indexSelectTeam = AnalyticsAuthorizationHandler.getDefaultTeamIndex(analyticsSettingStore, teams);
                cbbTeams.select(indexSelectTeam);
                setProjectsBasedOnTeam(teams.get(indexSelectTeam), projects);
                if (selectProjectFromConfig != null) {
                    projects.add(selectProjectFromConfig);
                }
            }

            if (enableAnalyticsIntegration.getSelection()) {
                Executors.newFixedThreadPool(1).submit(() -> {
                    UISynchronizeService.syncExec(
                            () -> {
                            	enableObject(false);
                                setProgressMessage(ComposerIntegrationAnalyticsMessageConstants.MSG_DLG_PRG_CONNECTING_TO_SERVER, false);
                            });
                    UISynchronizeService.syncExec(() -> {
                        try {
                            AnalyticsTokenInfo tokenInfo = AnalyticsAuthorizationHandler.getToken(serverUrl, email, password, analyticsSettingStore);
                            if (tokenInfo == null) {
                                return;
                            }
                            getTeam(serverUrl, organization.getId(), tokenInfo);
                            if (teams != null && !teams.isEmpty()) {
                                if (!checkUserCanAccessProject()) {
                                    canAccessProject = false;
                                    projects.clear();
                                    teams.add(selectTeamFromConfig);
                                    projects.add(selectProjectFromConfig);
                                    lblStatusAccessProject.setText(ComposerIntegrationAnalyticsMessageConstants.VIEW_ERROR_MSG_PROJ_USER_CAN_NOT_ACCESS_PROJECT);
                                } else {
                                    getProject(serverUrl, teams.get(AnalyticsAuthorizationHandler.getDefaultTeamIndex(analyticsSettingStore, teams)), tokenInfo);
                                }
                                cbbTeams.setItems(
                                        AnalyticsAuthorizationHandler.getTeamNames(teams).toArray(new String[teams.size()]));
                                int indexSelectTeam = AnalyticsAuthorizationHandler.getDefaultTeamIndex(analyticsSettingStore, teams);
                                cbbTeams.select(indexSelectTeam);
                                setProjectsBasedOnTeam(teams.get(indexSelectTeam), projects);
                            }
                        } catch (Exception e) {
                            LoggerSingleton.logError(e);
                        }
                        enableObject(true);
                    });
                });
            }

            ScopedPreferenceStore preferenceStore = PreferenceStoreManager
                    .getPreferenceStore(ActivationPreferenceConstants.ACTIVATION_INFO_STORAGE);
            String preferenceEmail = preferenceStore.getString(ActivationPreferenceConstants.ACTIVATION_INFO_EMAIL);
            String preferencePassword = preferenceStore
                    .getString(ActivationPreferenceConstants.ACTIVATION_INFO_PASSWORD);

            if (!StringUtils.isEmpty(preferenceEmail) && !StringUtils.isEmpty(preferencePassword)) {
                txtEmail.setText(CryptoUtil.decode(CryptoUtil.getDefault(preferenceEmail)));
                password = CryptoUtil.decode(CryptoUtil.getDefault(preferencePassword));
                // empty preference store password
                preferenceStore.setValue(ActivationPreferenceConstants.ACTIVATION_INFO_PASSWORD, StringUtils.EMPTY);
            }
            changeEnabled();
        } catch (IOException | GeneralSecurityException e) {
            LoggerSingleton.logError(e);
            MultiStatusErrorDialog.showErrorDialog(e, ComposerAnalyticsStringConstants.ERROR, e.getMessage());
        }
    }

    private void getTeam(String serverUrl, Long orgId, AnalyticsTokenInfo tokenInfo) {
        try {
            boolean encryptionEnabled = analyticsSettingStore.isEncryptionEnabled();
            teams = AnalyticsAuthorizationHandler.getTeams(analyticsSettingStore.getServerEndpoint(encryptionEnabled), orgId, tokenInfo);
            setProgressMessage("", false);
        } catch (Exception e) {
            setProgressMessage(ComposerIntegrationAnalyticsMessageConstants.MSG_REQUEST_TOKEN_ERROR, true);
        }
    }
    
    private void getProject(final String serverUrl, final AnalyticsTeam team, AnalyticsTokenInfo tokenInfo) {
        try {
            projects = AnalyticsAuthorizationHandler.getProjects(serverUrl, team, tokenInfo);
            setProgressMessage("", false);
        } catch (Exception e) {
            setProgressMessage(ComposerIntegrationAnalyticsMessageConstants.MSG_REQUEST_TOKEN_ERROR, true);
        }
    }
    
    private void setProgressMessage(String message, boolean isError) {
        lblStatus.setText(message);
        if (isError) {
            lblStatus.setForeground(ColorUtil.getTextErrorColor());
        } else {
            lblStatus.setForeground(ColorUtil.getTextRunningColor());
        }
        lblStatus.getParent().layout();
    }

    private boolean checkUserCanAccessProject() throws IOException {
        AnalyticsTeam currentTeam = analyticsSettingStore.getTeam();

        if (currentTeam.getId() != null) {
            long currentTeamId = currentTeam.getId();
            for (AnalyticsTeam team : teams) {
                long teamId = team.getId();
                if (teamId == currentTeamId) {
                    return true;
                }
            }
            return false;
        } 
        return true;
    }

    private void changeEnabled() {
        boolean isAnalyticsIntegrated = enableAnalyticsIntegration.getSelection();
        btnConnect.setEnabled(isAnalyticsIntegrated);
        chckEncrypt.setEnabled(isAnalyticsIntegrated);
        cbbProjects.setEnabled(isAnalyticsIntegrated);
        cbbTeams.setEnabled(isAnalyticsIntegrated);
        if (canAccessProject && isAnalyticsIntegrated) {
            btnCreate.setEnabled(true);
        } else {
            btnCreate.setEnabled(false);
        }
    }

    private boolean isIntegratedSuccessfully() {
        if (!isInitialized()) {
            return false;
        }

        boolean isAnalyticsIntegrated = enableAnalyticsIntegration.getSelection();
        return isAnalyticsIntegrated && !teams.isEmpty();
    }

    private void updateDataStore() {
        try {
            boolean encryptionEnabled = enableAnalyticsIntegration.getSelection();
            analyticsSettingStore.enableIntegration(isIntegratedSuccessfully());
            analyticsSettingStore.enableEncryption(encryptionEnabled);
            if (!teams.isEmpty()) {
                analyticsSettingStore.setTeam(teams.get(cbbTeams.getSelectionIndex()));
                if (!projects.isEmpty()) {
                    analyticsSettingStore.setProject(projects.get(cbbProjects.getSelectionIndex()));
                }
            }

            IEventBroker eventBroker = EventBrokerSingleton.getInstance().getEventBroker();
            eventBroker.post(EventConstants.IS_INTEGRATED, isIntegratedSuccessfully());
        } catch (IOException e) {
            LoggerSingleton.logError(e);
            MultiStatusErrorDialog.showErrorDialog(e, ComposerAnalyticsStringConstants.ERROR, e.getMessage());
        }
    }

    private boolean isNoInfo() {
        if (email.isEmpty() || password.isEmpty() || organization.getId() == null) {
            return true;
        }
        return false;
    }

    private boolean katalonLogin() {
        try {
            Shell shell = Display.getCurrent().getActiveShell();
            KStoreLoginDialog dialog = new KStoreLoginDialog(shell);
            if (dialog.open() == Dialog.OK) {
                email = dialog.getUsername();
                password = dialog.getPassword();
                organization = analyticsSettingStore.getOrganization();

                ApplicationInfo.setAppProperty(ApplicationStringConstants.ARG_EMAIL, email, true);
                String encryptedPassword = CryptoUtil.encode(CryptoUtil.getDefault(password));
                ApplicationInfo.setAppProperty(ApplicationStringConstants.ARG_PASSWORD, encryptedPassword, true);
                dialog.close();
                fillData();
                return true;
            }
        } catch (Exception e) {
            LoggerSingleton.logError(e);
        }
        return false;
    }

    private void addListeners() {
        enableAnalyticsIntegration.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (isNoInfo()) {
                    boolean result = katalonLogin();
                    enableAnalyticsIntegration.setSelection(result);
                } else {
                    changeEnabled();
                    if (enableAnalyticsIntegration.getSelection()) {
                       connect();
                    }
                }
            }
        });

        btnConnect.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                connect();
            }
        });

        cbbTeams.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                cbbProjects.setEnabled(false);
                Executors.newFixedThreadPool(1).submit(() -> {
                    UISynchronizeService.syncExec(() -> setProgressMessage(ComposerIntegrationAnalyticsMessageConstants.MSG_DLG_PRG_GETTING_PROJECTS, false));
                    UISynchronizeService.syncExec(() -> {
                        AnalyticsTeam selectTeamFromUser = teams.get(cbbTeams.getSelectionIndex());
                        if (!canAccessProject) {
                            if (selectTeamFromUser.equals(selectTeamFromConfig)) {
                                setProgressMessage("", false);
                                cbbProjects.setEnabled(true);
                                return;
                            } else {
                                teams.remove(selectTeamFromConfig);
                                cbbTeams.setItems(AnalyticsAuthorizationHandler.getTeamNames(teams).toArray(new String[teams.size()]));
                                int indexSelectTeam = teams.indexOf(selectTeamFromUser);
                                cbbTeams.select(indexSelectTeam);
                                lblStatusAccessProject.setText("");
                                canAccessProject = true;
                            }
                        }

                        AnalyticsTokenInfo tokenInfo = AnalyticsAuthorizationHandler.getToken(serverUrl, email, password, analyticsSettingStore);

                        getProject(serverUrl, selectTeamFromUser, tokenInfo);

                        setProjectsBasedOnTeam(selectTeamFromUser, projects);
                        cbbProjects.setEnabled(true);
                        setProgressMessage("", false);
                    });
                });
            }
        });

        btnCreate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                AnalyticsTeam team = null;
                if (teams != null && teams.size() > 0) {
                    team = teams.get(cbbTeams.getSelectionIndex());
                }

                NewProjectDialog dialog = new NewProjectDialog(btnCreate.getDisplay().getActiveShell(), serverUrl,
                        email, password, team);
                if (dialog.open() == Dialog.OK) {
                    AnalyticsProject createdProject = dialog.getAnalyticsProject();
                    if (createdProject != null) {
                        cbbProjects.setEnabled(false);
                        Executors.newFixedThreadPool(1).submit(() -> {
                            UISynchronizeService.syncExec(() -> setProgressMessage(ComposerIntegrationAnalyticsMessageConstants.MSG_DLG_PRG_RETRIEVING_PROJECTS, false));
                            UISynchronizeService.syncExec(() -> {
                                AnalyticsTokenInfo tokenInfo = AnalyticsAuthorizationHandler.getToken(serverUrl, email, password, analyticsSettingStore);
                                getProject(serverUrl, teams.get(cbbTeams.getSelectionIndex()), tokenInfo);
                                if (projects == null) {
                                    cbbProjects.setEnabled(true);
                                    setProgressMessage(ComposerIntegrationAnalyticsMessageConstants.MSG_REQUEST_TOKEN_ERROR, true);
                                    return;
                                }
                                cbbProjects.setItems(AnalyticsAuthorizationHandler.getProjectNames(projects)
                                        .toArray(new String[projects.size()]));
                                cbbProjects.select(
                                        AnalyticsAuthorizationHandler.getProjectIndex(createdProject, projects));
                                cbbProjects.setEnabled(true);
                                setProgressMessage("", false);
                            });
                        });
                    }
                }
            }
        });
    }
    
    private void connect() {
        enableObject(false);

        cbbTeams.setItems();
        cbbProjects.setItems();

        lblStatusAccessProject.setText("");

        Executors.newFixedThreadPool(1).submit(() -> {
            UISynchronizeService.syncExec(
                    () ->  setProgressMessage(ComposerIntegrationAnalyticsMessageConstants.MSG_DLG_PRG_CONNECTING_TO_SERVER, false));
            AnalyticsTokenInfo tokenInfo = AnalyticsAuthorizationHandler.getToken(serverUrl, email, password,
                    analyticsSettingStore);
            if (tokenInfo == null) {
                return;
            }
            UISynchronizeService.syncExec(() -> {
                getTeam(serverUrl, organization.getId(), tokenInfo);
                if (!teams.isEmpty()) {
                    getProject(serverUrl,
                            teams.get(AnalyticsAuthorizationHandler.getDefaultTeamIndex(analyticsSettingStore, teams)),
                            tokenInfo);

                    cbbTeams.setItems(
                            AnalyticsAuthorizationHandler.getTeamNames(teams).toArray(new String[teams.size()]));
                    int indexSelectTeam = AnalyticsAuthorizationHandler.getDefaultTeamIndex(analyticsSettingStore, teams);
                    cbbTeams.select(indexSelectTeam);

                    setProjectsBasedOnTeam(teams.get(indexSelectTeam), projects);
                    lblStatusAccessProject.setText("");
                } else {
                    MessageDialog.openWarning(Display.getCurrent().getActiveShell(),
                            ComposerAnalyticsStringConstants.WARN,
                            ComposerIntegrationAnalyticsMessageConstants.REPORT_WARNING_MSG_NO_TEAM);
                }
                enableObject(true);
            });
         });
    }
    
    private void enableObject(boolean isEnable) {
        cbbTeams.setEnabled(isEnable);
        cbbProjects.setEnabled(isEnable);
        btnConnect.setEnabled(isEnable);
        btnCreate.setEnabled(isEnable);
    }

    private void setProjectsBasedOnTeam(AnalyticsTeam team, List<AnalyticsProject> projects) {
        if (projects != null && !projects.isEmpty()) {
            cbbProjects.setItems(
                    AnalyticsAuthorizationHandler.getProjectNames(projects).toArray(new String[projects.size()]));
            cbbProjects.select(AnalyticsAuthorizationHandler.getDefaultProjectIndex(analyticsSettingStore, projects));
        } else {
            cbbProjects.clearSelection();
            cbbProjects.removeAll();
        }
        String role = team.getRole();
        if (role.equals("USER")) {
            btnCreate.setEnabled(false);
        } else {
            btnCreate.setEnabled(true);
        }
    }

    protected boolean isInitialized() {
        return enableAnalyticsIntegration != null;
    }

    @Override
    public boolean hasDocumentation() {
        return true;
    }

    @Override
    public String getDocumentationUrl() {
        return DocumentationMessageConstants.SETTINGS_KATALON_ANALYTICS;
    }
}