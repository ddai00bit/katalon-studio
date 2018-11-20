package com.kms.katalon.platform;

import com.katalon.platform.api.Application;
import com.katalon.platform.api.service.ExtensionManager;
import com.katalon.platform.api.service.PluginManager;

public class ApplicationImpl implements Application {
    
    private final PluginManager pluginManager = new PluginManagerImpl();
    
    private final ExtensionManager extensionManager = new ExtensionManagerImpl();

    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public ExtensionManager getExtensionManager() {
        return extensionManager;
    }
}
