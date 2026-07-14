package dev.minescan.addon.mixin;

import dev.minescan.addon.gui.MineScanScreen;
import dev.minescan.addon.modules.MineScanModule;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a "MineScan" button directly to the vanilla Multiplayer screen, so
 * MineScan is usable without digging through Meteor's module list first.
 * <p>
 * Mirrors how meteor-client's own {@code JoinMultiplayerScreenMixin} adds its
 * "Accounts"/"Proxies" buttons - injecting into {@code repositionElements}
 * at {@code TAIL} and adding renderable widgets positioned in a screen
 * corner. Depending on your Meteor config, these buttons may visually overlap
 * with Meteor's own buttons; adjust the position below if so.
 */
@Mixin(JoinMultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    @Unique
    private static final int MINESCAN_BUTTON_WIDTH = 80;
    @Unique
    private static final int BUTTON_HEIGHT = 20;
    @Unique
    private static final int MARGIN = 4;

    @Unique
    private Button minescan$browseButton;

    protected MultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void minescan$onInit(CallbackInfo ci) {
        if (minescan$browseButton == null) {
            minescan$browseButton = addRenderableWidget(
                new Button.Builder(Component.literal("MineScan"), button -> minescan$openBrowser())
                    .size(MINESCAN_BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build()
            );
        }

        int y = this.height - MARGIN - BUTTON_HEIGHT;
        minescan$browseButton.setPosition(MARGIN, y);
    }

    @Unique
    private void minescan$openBrowser() {
        MineScanModule module = Modules.get().get(MineScanModule.class);
        this.minecraft.setScreen(new MineScanScreen(GuiThemes.get(), module));
    }
}
