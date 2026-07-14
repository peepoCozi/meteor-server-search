package dev.minescan.addon;

import com.mojang.logging.LogUtils;
import dev.minescan.addon.commands.MineScanCommand;
import dev.minescan.addon.modules.MineScanModule;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class MineScanAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("MineScan");

    @Override
    public void onInitialize() {
        LOG.info("Initializing MineScan");

        Modules.get().add(new MineScanModule());
        Commands.add(new MineScanCommand());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "dev.minescan.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("peepoCozi", "MineScan-Addon");
    }
}
