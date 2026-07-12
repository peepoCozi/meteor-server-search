package dev.mcfriendfinder.addon.modules;

import dev.mcfriendfinder.addon.ServerFinderAddon;
import dev.mcfriendfinder.addon.api.CrackedFilter;
import dev.mcfriendfinder.addon.api.ServerTypeFilter;
import dev.mcfriendfinder.addon.gui.ServerFinderScreen;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ServerConnectEndEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Browses Minecraft servers found by a self-hosted MC Friend Finder scanner
 * (see {@code scanner/} in the repo root) and lets you add them to your
 * vanilla Multiplayer server list, or connect to them directly.
 * <p>
 * This addon does not ship with, and does not depend on, any hosted/shared
 * API instance - {@link #apiBaseUrl} must point at one you (or a friend) run
 * yourselves.
 */
public class ServerFinderModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFilters = settings.createGroup("Default Filters");

    public final Setting<String> apiBaseUrl = sgGeneral.add(new StringSetting.Builder()
        .name("api-base-url")
        .description("Base URL of your self-hosted MC Friend Finder API, e.g. http://your-server:8080. See the project README for how to run one.")
        .defaultValue("")
        .wide()
        .build()
    );

    public final Setting<String> apiKey = sgGeneral.add(new StringSetting.Builder()
        .name("api-key")
        .description("Only needed if your API instance was configured with an [api].api_key.")
        .defaultValue("")
        .wide()
        .build()
    );

    public final Setting<String> versionFilter = sgFilters.add(new StringSetting.Builder()
        .name("version")
        .description("Only show servers whose version name contains this text, e.g. 1.21. Leave blank for any version.")
        .defaultValue("")
        .build()
    );

    public final Setting<Integer> minPlayers = sgFilters.add(new IntSetting.Builder()
        .name("min-players")
        .description("Only show servers with at least this many players online. 0 to disable.")
        .defaultValue(0)
        .min(0)
        .sliderMax(50)
        .build()
    );

    public final Setting<Integer> maxPlayers = sgFilters.add(new IntSetting.Builder()
        .name("max-players")
        .description("Only show servers with at most this many players online. 0 to disable.")
        .defaultValue(0)
        .min(0)
        .sliderMax(200)
        .build()
    );

    public final Setting<CrackedFilter> crackedFilter = sgFilters.add(new EnumSetting.Builder<CrackedFilter>()
        .name("cracked-filter")
        .description("Filter servers by online-mode status.")
        .defaultValue(CrackedFilter.ANY)
        .build()
    );

    public final Setting<ServerTypeFilter> serverTypeFilter = sgFilters.add(new EnumSetting.Builder<ServerTypeFilter>()
        .name("server-type")
        .description("Only show servers detected as running this software (based on their reported version name).")
        .defaultValue(ServerTypeFilter.ANY)
        .build()
    );

    public final Setting<String> motdContains = sgFilters.add(new StringSetting.Builder()
        .name("motd-contains")
        .description("Only show servers whose MOTD contains this text. Leave blank for any.")
        .defaultValue("")
        .build()
    );

    public final Setting<Integer> pageSize = sgFilters.add(new IntSetting.Builder()
        .name("page-size")
        .description("How many servers to fetch per page in the browser screen.")
        .defaultValue(50)
        .min(1)
        .max(200)
        .sliderMax(200)
        .build()
    );

    public final Setting<Boolean> hideJoinedServers = sgFilters.add(new BoolSetting.Builder()
        .name("hide-joined-servers")
        .description("Hide servers you've already connected to, however you connected to them.")
        .defaultValue(false)
        .build()
    );

    /**
     * Address:port of every server the client has connected to, used by
     * {@link #hideJoinedServers}. Populated from {@link ServerConnectEndEvent}
     * (see {@link #onServerConnectEnd}) so it covers every way of joining a
     * server - this screen's Connect button, double-clicking a vanilla
     * Multiplayer list entry, or the direct-connect screen - not just this
     * addon's own UI. Not a user-facing setting - hidden via
     * {@code visible(() -> false)} - but piggybacking on the Setting system
     * means it's saved/loaded automatically along with everything else
     * instead of needing custom persistence code.
     */
    public final Setting<List<String>> joinedServers = sgFilters.add(new StringListSetting.Builder()
        .name("joined-servers")
        .description("Internal bookkeeping for hide-joined-servers. Not meant to be edited directly.")
        .defaultValue()
        .visible(() -> false)
        .build()
    );

    public ServerFinderModule() {
        super(ServerFinderAddon.CATEGORY, "server-finder", "Browse servers found by your self-hosted scanner and add them to your server list.");

        // Subscribed unconditionally (rather than relying on Module's
        // built-in autoSubscribe, which only listens while the module is
        // toggled on) since this module has no on/off behaviour of its own -
        // it's just a GUI launcher - but hide-joined-servers bookkeeping
        // should still work regardless.
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    private void onServerConnectEnd(ServerConnectEndEvent event) {
        markJoined(event.address.getHostString() + ":" + event.address.getPort());
    }

    public void markJoined(String hostAndPort) {
        List<String> joined = joinedServers.get();
        if (joined.contains(hostAndPort)) return;

        List<String> updated = new ArrayList<>(joined);
        updated.add(hostAndPort);
        joinedServers.set(updated);
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton open = theme.button("Open Server Finder");
        open.action = () -> mc.setScreen(new ServerFinderScreen(theme, this));
        return open;
    }
}
