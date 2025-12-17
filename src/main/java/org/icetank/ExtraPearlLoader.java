package org.icetank;

import com.zenith.plugin.api.Plugin;
import com.zenith.plugin.api.PluginAPI;
import com.zenith.plugin.api.ZenithProxyPlugin;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.icetank.command.PearlLoaderCommand;
import org.icetank.module.ExtraPearlModule;

@Plugin(
    id = org.ic3tank.BuildConstants.PLUGIN_ID,
    version = org.ic3tank.BuildConstants.VERSION,
    description = "ZenithProxy Extra Pearl Loader Plugin",
    url = "https://github.com/rfresh2/ZenithProxyExamplePlugin",
    authors = {"icetank"},
    mcVersions = {"1.21.4"} // to indicate any MC version: @Plugin(mcVersions = "*")
                            // if you touch packet classes, you almost certainly need to pin to a single mc version
)
public class ExtraPearlLoader implements ZenithProxyPlugin {
    // public static for simple access from modules and commands
    // or alternatively, you could pass these around in constructors
    public static ExtraPearlLoaderConfig PLUGIN_CONFIG;
    public static ComponentLogger LOG;

    @Override
    public void onLoad(PluginAPI pluginAPI) {
        LOG = pluginAPI.getLogger();
        LOG.info("Extra Pearl Loader Plugin loading...");
        // initialize any configurations before modules or commands might need to read them
        PLUGIN_CONFIG = pluginAPI.registerConfig("extra-pearl-loader", ExtraPearlLoaderConfig.class);
        pluginAPI.registerModule(new ExtraPearlModule());
        pluginAPI.registerCommand(new PearlLoaderCommand());
        LOG.info("Extra Pearl Loader Plugin loaded.");
    }
}
