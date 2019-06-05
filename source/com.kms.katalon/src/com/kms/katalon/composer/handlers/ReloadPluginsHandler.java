package com.kms.katalon.composer.handlers;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.kms.katalon.core.util.internal.ExceptionsUtil;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.kms.katalon.composer.components.event.EventBrokerSingleton;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.components.services.UISynchronizeService;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.constants.StringConstants;
import com.kms.katalon.plugin.dialog.KStorePluginsDialog;
import com.kms.katalon.plugin.models.KStoreClientAuthException;
import com.kms.katalon.plugin.models.KStorePlugin;
import com.kms.katalon.plugin.models.KStoreUsernamePasswordCredentials;
import com.kms.katalon.plugin.models.ReloadItem;
import com.kms.katalon.plugin.service.PluginService;
import com.kms.katalon.plugin.store.PluginPreferenceStore;

public class ReloadPluginsHandler extends RequireAuthorizationHandler {

    private static final long DIALOG_CLOSED_DELAY_MILLIS = 500L;

    @CanExecute
    public boolean canExecute() {
        return true;
    }

    @Execute
    public void execute() {
        reloadPlugins(false);
    }

    public void reloadPlugins(boolean silenceMode) {
        PluginPreferenceStore store = new PluginPreferenceStore();
        List<ReloadItem>[] resultHolder = new List[1];
        Job reloadPluginsJob = new Job("Reloading plugins...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    KStoreUsernamePasswordCredentials[] credentials = new KStoreUsernamePasswordCredentials[1];
                    UISynchronizeService.syncExec(() -> {
                        try {
                            credentials[0] = getUsernamePasswordCredentials();
                        } catch (KStoreClientAuthException e) {
                            LoggerSingleton.logError(e);
                        }
                    });
                    if (credentials[0] != null) {
                        LoggerSingleton.logInfo("Credentials found. Reloading plugins.");
                        resultHolder[0] = PluginService.getInstance().reloadPlugins(credentials[0], monitor);
                        if (!store.hasReloadedPluginsBefore()) {
                            store.markFirstTimeReloadPlugins();
                        }
                    } else {
                        LoggerSingleton.logError("Credentials not found.");
                        return Status.CANCEL_STATUS;
                    }
                } catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                } catch (Exception e) {
                    LoggerSingleton.logError(e);
                    return new Status(Status.ERROR, "com.kms.katalon", "Error reloading plugins",
                            new Exception(ExceptionsUtil.getStackTraceForThrowable(e)));
                }
                LoggerSingleton.logInfo("Reloaded plugins successfully.");
                return Status.OK_STATUS;
            }
        };

        reloadPluginsJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                EventBrokerSingleton.getInstance().getEventBroker().post(EventConstants.WORKSPACE_PLUGIN_LOADED, null);

                if (!reloadPluginsJob.getResult().isOK()) {
                    LoggerSingleton.logError("Failed to reload plugins.");
                    return;
                }

                List<ReloadItem> results = resultHolder[0];

                if (silenceMode && !checkExpire(results)) {
                    return;
                }

                Executors.newSingleThreadExecutor().submit(() -> {
                    try {
                        // wait for Reloading Plugins dialog to close
                        TimeUnit.MILLISECONDS.sleep(DIALOG_CLOSED_DELAY_MILLIS);
                    } catch (InterruptedException ignored) {}
                    UISynchronizeService.syncExec(() -> openResultDialog(resultHolder[0]));
                });
            }
        });

        reloadPluginsJob.setUser(true);
        reloadPluginsJob.schedule();
    }

    private boolean checkExpire(List<ReloadItem> reloadItems) {
        return reloadItems.stream().filter(i -> {
            KStorePlugin plugin = i.getPlugin();
            return plugin.isExpired() || (plugin.isTrial() && plugin.getRemainingDay() <= 14);
        }).findAny().isPresent();
    }

    private void openResultDialog(List<ReloadItem> result) {
        if (result.size() > 0) {
            KStorePluginsDialog dialog = new KStorePluginsDialog(Display.getCurrent().getActiveShell(), result);
            dialog.open();
        } else {
            MessageDialog.openInformation(Display.getCurrent().getActiveShell(), StringConstants.INFO,
                    StringConstants.HAND_INFO_NO_PLUGINS_FOUND);
        }
    }
}
