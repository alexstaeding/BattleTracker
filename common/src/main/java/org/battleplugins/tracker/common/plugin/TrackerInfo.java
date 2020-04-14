package org.battleplugins.tracker.plugin;

import com.google.inject.Inject;
import org.anvilpowered.anvil.api.plugin.PluginInfo;
import org.anvilpowered.anvil.api.util.TextService;

/**
 * Class that holds information about BattleTracker
 * from the pom. Due to how the annotation processor
 * works, this class had to be created.
 *
 * Values here are replaced at runtime.
 *
 * @author Redned
 */
public class TrackerInfo<TString, TCommandSource> implements PluginInfo<TString> {
    public static final String id = "battletracker";
    public static final String name = "BattleTracker";
    public static final String version = "$version";
    public static final String description = "$description";
    public static final String url = "https://github.com/BattlePlugins/BattleTracker";
    public static final String[] authors = {"Redned"};
    public static final String organizationName = "BattlePlugins";
    public static final String buildDate = "$buildDate";
    public TString pluginPrefix;

    @Inject
    public void setPluginPrefix(TextService<TString, TCommandSource> textService) {
        pluginPrefix = textService.builder().green().append("[", name, "] ").build();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String[] getAuthors() {
        return authors;
    }

    @Override
    public String getOrganizationName() {
        return organizationName;
    }

    @Override
    public String getBuildDate() {
        return buildDate;
    }

    @Override
    public TString getPrefix() {
        return pluginPrefix;
    }
}