package dev.mcfriendfinder.addon;

import com.mojang.logging.LogUtils;
import dev.mcfriendfinder.addon.commands.ServerFinderCommand;
import dev.mcfriendfinder.addon.modules.ServerFinderModule;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class ServerFinderAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Friend Finder");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Server Search");

        Modules.get().add(new ServerFinderModule());
        Commands.add(new ServerFinderCommand());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "dev.mcfriendfinder.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("peepoCozi", "meteor-server-search");
    }
}
