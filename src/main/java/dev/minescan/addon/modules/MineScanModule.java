package dev.minescan.addon.modules;

import dev.minescan.addon.MineScanAddon;
import dev.minescan.addon.api.CrackedFilter;
import dev.minescan.addon.api.ServerTypeFilter;
import dev.minescan.addon.api.WhitelistFilter;
import dev.minescan.addon.gui.MineScanScreen;
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
 * Browses Minecraft servers found by a MineScan scanner (see
 * {@code scanner/} in the repo root) and lets you add them to your vanilla
 * Multiplayer server list, or connect to them directly.
 * <p>
 * By default every request goes to {@link #DEFAULT_API_BASE_URL}. Users who
 * run their own scanner can enable {@link #selfHostedScanner} to reveal and
 * edit the API Base URL (and optional Server Password).
 * <p>
 * The addon itself never scans or searches anything on its own - every
 * search/filter is a single request to whichever API instance is
 * configured, which does all the actual querying. Abuse protection (rate
 * limiting, mandatory per-user key) lives entirely server-side in {@code
 * scanner/api}, since that's the only place it can't be bypassed by a
 * modified client.
 * <p>
 * Every request requires {@link #userApiKey} (displayed to users as the
 * "User Access Code"), obtained by joining the project's Discord and
 * running {@code /register} there - the addon itself has no way to mint
 * one. Note this is addon-only: the Discord bot's own commands (including
 * its {@code /search}) don't require it. {@link #serverPassword} is a separate, optional
 * secret only needed against a self-hosted / 3rd-party instance that
 * configured one; it is never used against the default MineScan API.
 */
public class MineScanModule extends Module {
    public static final String DEFAULT_API_BASE_URL = "https://api.minescan.net";

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFilters = settings.createGroup("Default Filters");

    public final Setting<Boolean> selfHostedScanner = sgGeneral.add(new BoolSetting.Builder()
        .name("self-hosted-scanner")
        .description("Enable to point the addon at your own (or a friend's) scanner instance instead of the default MineScan API.")
        .defaultValue(false)
        .build()
    );

    public final Setting<String> apiBaseUrl = sgGeneral.add(new StringSetting.Builder()
        .name("api-base-url")
        .description("Base URL of your self-hosted MineScan API. Only shown when Self-Hosted Scanner is enabled.")
        .defaultValue(DEFAULT_API_BASE_URL)
        .wide()
        .visible(selfHostedScanner::get)
        .build()
    );

    /**
     * Displayed to users as "User Access Code" (renamed from "User API
     * Key"). The setting's internal {@code name()} is left unchanged so
     * upgrading users don't lose their already-saved value.
     */
    public final Setting<String> userApiKey = sgGeneral.add(new StringSetting.Builder()
        .name("user-api-key")
        .description("Required for every request. Get your User Access Code by joining our Discord and running /register there.")
        .defaultValue("")
        .wide()
        .build()
    );

    public final Setting<String> serverPassword = sgGeneral.add(new StringSetting.Builder()
        .name("server-password")
        .description("Only needed if your self-hosted instance configured an [api].server_password. Not used for the default MineScan API.")
        .defaultValue("")
        .wide()
        .visible(selfHostedScanner::get)
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

    public final Setting<WhitelistFilter> whitelistFilter = sgFilters.add(new EnumSetting.Builder<WhitelistFilter>()
        .name("whitelist-filter")
        .description("Filter servers by detected whitelist status. Online-mode servers always show as \"Unknown\" - see the README's ethics notice for why.")
        .defaultValue(WhitelistFilter.ANY)
        .build()
    );

    public final Setting<String> addressFilter = sgFilters.add(new StringSetting.Builder()
        .name("address")
        .description("Only show servers at this exact IP address. Leave blank for any.")
        .defaultValue("")
        .build()
    );

    public final Setting<String> playerFilter = sgFilters.add(new StringSetting.Builder()
        .name("player")
        .description("Only show servers where this username has ever been seen. Leave blank for any.")
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

    public MineScanModule() {
        super(MineScanAddon.CATEGORY, "minescan", "Browse servers indexed by MineScan and add them to your server list.");

        // Subscribed unconditionally (rather than relying on Module's
        // built-in autoSubscribe, which only listens while the module is
        // toggled on) since this module has no on/off behaviour of its own -
        // it's just a GUI launcher - but hide-joined-servers bookkeeping
        // should still work regardless.
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    /**
     * Always the default MineScan URL unless the user explicitly enabled
     * Self-Hosted Scanner - so hiding the setting also enforces it.
     */
    public String getEffectiveApiBaseUrl() {
        if (!selfHostedScanner.get()) {
            return DEFAULT_API_BASE_URL;
        }
        String configured = apiBaseUrl.get() == null ? "" : apiBaseUrl.get().strip();
        return configured.isEmpty() ? DEFAULT_API_BASE_URL : configured;
    }

    /**
     * Server Password is only meaningful for self-hosted instances.
     */
    public String getEffectiveServerPassword() {
        if (!selfHostedScanner.get()) {
            return "";
        }
        return serverPassword.get() == null ? "" : serverPassword.get();
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
        WButton open = theme.button("Browse Servers");
        open.action = () -> mc.setScreen(new MineScanScreen(theme, this));
        return open;
    }
}
