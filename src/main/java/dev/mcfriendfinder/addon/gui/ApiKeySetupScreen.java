package dev.mcfriendfinder.addon.gui;

import dev.mcfriendfinder.addon.api.ApiClient;
import dev.mcfriendfinder.addon.modules.ServerFinderModule;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screens.Screen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Small screen for pasting a MineScan User API Key without digging through
 * Meteor's module settings. Opened from the vanilla Multiplayer menu or
 * from {@link ServerFinderScreen} when no key is configured yet.
 */
public class ApiKeySetupScreen extends WindowScreen {
    private final ServerFinderModule module;
    private final Screen returnTo;
    private final Runnable onSaved;

    public ApiKeySetupScreen(GuiTheme theme, ServerFinderModule module, Screen returnTo) {
        this(theme, module, returnTo, null);
    }

    public ApiKeySetupScreen(GuiTheme theme, ServerFinderModule module, Screen returnTo, Runnable onSaved) {
        super(theme, "MineScan API Key");
        this.module = module;
        this.returnTo = returnTo;
        this.onSaved = onSaved;
    }

    @Override
    public void initWidgets() {
        add(theme.label("Paste your User API Key from Discord (/register)."))
            .expandX();
        add(theme.label("Required to search MineScan servers."))
            .expandX();

        WTextBox keyBox = add(theme.textBox(module.userApiKey.get(), "User API Key"))
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
