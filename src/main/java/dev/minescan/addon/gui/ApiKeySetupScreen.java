package dev.minescan.addon.gui;

import dev.minescan.addon.api.ApiClient;
import dev.minescan.addon.modules.MineScanModule;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screens.Screen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Small screen for pasting a MineScan User Access Code without digging
 * through Meteor's module settings. Opened from the vanilla Multiplayer
 * menu or from {@link MineScanScreen} when no code is configured yet.
 */
public class ApiKeySetupScreen extends WindowScreen {
    private final MineScanModule module;
    private final Screen returnTo;
    private final Runnable onSaved;

    public ApiKeySetupScreen(GuiTheme theme, MineScanModule module, Screen returnTo) {
        this(theme, module, returnTo, null);
    }

    public ApiKeySetupScreen(GuiTheme theme, MineScanModule module, Screen returnTo, Runnable onSaved) {
        super(theme, "MineScan Access Code");
        this.module = module;
        this.returnTo = returnTo;
        this.onSaved = onSaved;
    }

    @Override
    public void initWidgets() {
        add(theme.label("Paste your User Access Code from Discord (/register)."))
            .expandX();
        add(theme.label("Required to search MineScan servers."))
            .expandX();

        WTextBox keyBox = add(theme.textBox(module.userApiKey.get(), "User Access Code"))
            .expandX()
            .minWidth(280d)
            .widget();

        WHorizontalList buttons = add(theme.horizontalList()).widget();

        WButton save = buttons.add(theme.button("Save")).expandX().widget();
        save.action = () -> saveKey(keyBox.get());

        if (returnTo != null) {
            WButton back = buttons.add(theme.button("Back")).widget();
            back.action = () -> mc.setScreen(returnTo);
        }
    }

    private void saveKey(String raw) {
        String key = ApiClient.normalizeUserApiKey(raw);
        if (key.isEmpty()) {
            return;
        }

        module.userApiKey.set(key);

        if (onSaved != null) {
            onSaved.run();
        } else if (returnTo != null) {
            mc.setScreen(returnTo);
        }
    }
}
