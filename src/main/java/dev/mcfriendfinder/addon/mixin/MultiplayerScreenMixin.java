package dev.mcfriendfinder.addon.mixin;

import dev.mcfriendfinder.addon.gui.ServerFinderScreen;
import dev.mcfriendfinder.addon.modules.ServerFinderModule;
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
 * Adds a "Find Servers" button directly to the vanilla Multiplayer screen, so
 * the addon's server browser is discoverable without digging through
 * Meteor's module list first.
 * <p>
 * Mirrors how meteor-client's own {@code JoinMultiplayerScreenMixin} adds its
 * "Accounts"/"Proxies" buttons - injecting into {@code repositionElements}
 * at {@code TAIL} and adding a renderable widget positioned in a screen
 * corner. Depending on your Meteor config, this button may visually overlap
 * with Meteor's own buttons; adjust the position below if so.
 */
@Mixin(JoinMultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    @Unique
    private static final int BUTTON_WIDTH = 100;
    @Unique
    private static final int BUTTON_HEIGHT = 20;
    @Unique
    private static final int MARGIN = 4;

    @Unique
    private Button mcff$findServersButton;

    protected MultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void mcff$onInit(CallbackInfo ci) {
        if (mcff$findServersButton == null) {
            mcff$findServersButton = addRenderableWidget(
                new Button.Builder(Component.literal("Find Servers"), button -> mcff$openServerFinder())
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build()
            );
        }

        mcff$findServersButton.setPosition(MARGIN, this.height - MARGIN - BUTTON_HEIGHT);
    }

    @Unique
    private void mcff$openServerFinder() {
        ServerFinderModule module = Modules.get().get(ServerFinderModule.class);
        this.minecraft.setScreen(new ServerFinderScreen(GuiThemes.get(), module));
    }
}
