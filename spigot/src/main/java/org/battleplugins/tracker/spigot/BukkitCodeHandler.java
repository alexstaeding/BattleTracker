package org.battleplugins.tracker.spigot;

import lombok.AllArgsConstructor;

import mc.alk.battlecore.bukkit.platform.BattleBukkitCodeHandler;

import org.battleplugins.tracker.BattleTracker;
import org.battleplugins.tracker.spigot.listener.PvEListener;
import org.battleplugins.tracker.spigot.listener.PvPListener;
import org.battleplugins.tracker.spigot.listener.TrackerListener;
import org.battleplugins.tracker.spigot.plugins.BTPlaceholderExtension;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Handler for version-dependent Bukkit code.
 *
 * @author Redned
 */
public class BukkitCodeHandler extends BattleBukkitCodeHandler {

    @Override
    public void onEnable() {
        super.onEnable();

        Plugin plugin = (Plugin) this.plugin.getPlatformPlugin();
        if (this.plugin.getTrackerManager().isTrackingPvE()) {
            Bukkit.getServer().getPluginManager().registerEvents(new PvEListener(this.plugin), plugin);
        }

        if (this.plugin.getTrackerManager().isTrackingPvP()) {
            Bukkit.getServer().getPluginManager().registerEvents(new PvPListener(this.plugin), plugin);
        }

        Bukkit.getServer().getPluginManager().registerEvents(new TrackerListener(this.plugin), plugin);

        if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BTPlaceholderExtension(this.plugin).register();
        }
    }
}
