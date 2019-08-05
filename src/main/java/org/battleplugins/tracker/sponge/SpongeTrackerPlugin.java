package org.battleplugins.tracker.sponge;

import mc.alk.sponge.plugin.SpongePlugin;
import org.battleplugins.tracker.BattleTracker;
import org.battleplugins.tracker.TrackerInfo;
import org.spongepowered.api.plugin.Plugin;

/**
 * Main class for BattleTracker Sponge.
 *
 * @author Redned
 */
@Plugin(id = "bt", authors = "BattlePlugins", name = TrackerInfo.NAME, version = TrackerInfo.VERSION, description = TrackerInfo.DESCRIPTION, url = TrackerInfo.URL)
public class SpongeTrackerPlugin extends SpongePlugin {

    @Override
    public void onEnable() {
        BattleTracker.setInstance(new BattleTracker(this));
    }

    @Override
    public void onDisable() {

    }
}