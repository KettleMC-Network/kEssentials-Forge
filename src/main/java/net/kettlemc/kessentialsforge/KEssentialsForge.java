package net.kettlemc.kessentialsforge;

import net.kettlemc.kessentialsforge.command.*;
import net.kettlemc.kessentialsforge.event.*;
import net.kettlemc.kessentialsforge.jda.DiscordBot;
import net.kettlemc.kessentialsforge.service.*;
import net.kettlemc.kessentialsforge.util.ServerRef;
import net.kettlemc.kessentialsforge.i18n.Messages;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import java.nio.file.Files; import java.nio.file.Path;

import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod("kessentials")
public class KEssentialsForge {
    public static KEssentialsForge INSTANCE;
    public final Path configDir;
    public final HomeService homes; public final WarpService warps; public final TpaService tpas; public final SpawnService spawn;
    public final BackService back; public final StateService state; public final ChatService chat; public final ConfigService config; public final KChatService kchat;
    public final CooldownService cd; public final LinkService links; public final LocaleService locales;
    private DiscordBot discord;

    public KEssentialsForge() {
        System.out.print("================================");
        System.out.print("kEssentials Forge wird gestartet");
        System.out.print("================================");
        INSTANCE = this;
        this.configDir = Path.of("config", "kessentials");
        try { Files.createDirectories(configDir); } catch (Exception ignored) {}
        Messages.setConfigDir(configDir);
        this.locales = new LocaleService(configDir);
        this.homes=new HomeService(configDir); this.warps=new WarpService(configDir); this.tpas=new TpaService(); this.spawn=new SpawnService(configDir);
        this.back=new BackService(); this.state=new StateService(configDir); this.chat=new ChatService(configDir); this.config=new ConfigService(configDir);
        this.cd=new CooldownService(); this.links=new LinkService(configDir); this.kchat=new KChatService(configDir);
        this.locales.load();
        Messages.setLocaleProvider(locales::getLocale);
        Messages.setDefaultLocale(locales.getDefaultLocale());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ProtectionEvents());
        MinecraftForge.EVENT_BUS.register(new VanishEvents());
        MinecraftForge.EVENT_BUS.register(new ChatEvents());
        MinecraftForge.EVENT_BUS.register(new FreezeEvents());
        MinecraftForge.EVENT_BUS.register(new JoinLeaveEvents());
        MinecraftForge.EVENT_BUS.register(new KChatEvents());
        MinecraftForge.EVENT_BUS.register(new EnchantingTableEvents());
        MinecraftForge.EVENT_BUS.register(new CommandEvents());
    }
    private void setup(FMLCommonSetupEvent e){ homes.load(); warps.load(); spawn.load(); state.load(); chat.load(); config.load(); links.load(); kchat.load(); }
    @SubscribeEvent public void onRegisterCommands(RegisterCommandsEvent e) {
        HealCommand.register(e.getDispatcher()); FeedCommand.register(e.getDispatcher()); FlyCommand.register(e.getDispatcher()); GameModeCommand.register(e.getDispatcher());
        WorkbenchCommand.register(e.getDispatcher()); AnvilCommand.register(e.getDispatcher()); EnchantCommand.register(e.getDispatcher()); EnderChestCommand.register(e.getDispatcher());
        HomeCommands.register(e.getDispatcher()); WarpCommands.register(e.getDispatcher()); RtpCommand.register(e.getDispatcher()); SuicideCommand.register(e.getDispatcher());
        GodCommand.register(e.getDispatcher()); VanishCommand.register(e.getDispatcher()); BackCommand.register(e.getDispatcher());
        BroadcastCommand.register(e.getDispatcher()); MsgReplyCommands.register(e.getDispatcher()); SocialSpyCommand.register(e.getDispatcher());
        FreezeCommand.register(e.getDispatcher()); InvseeCommands.register(e.getDispatcher()); EnderSeeCommand.register(e.getDispatcher());
        TeleportCommands.register(e.getDispatcher()); TpaCommands.register(e.getDispatcher()); ChatClearCommands.register(e.getDispatcher()); TeleportPlayerAlias.register(e.getDispatcher());
        SpawnCommands.register(e.getDispatcher()); TimeWeatherCommands.register(e.getDispatcher());
        ClearCommand.register(e.getDispatcher()); RepairCommand.register(e.getDispatcher()); ExtinguishCommand.register(e.getDispatcher()); HatCommand.register(e.getDispatcher());
        TopJumpCommands.register(e.getDispatcher()); NearSeenCommands.register(e.getDispatcher()); ArmorSeeCommand.register(e.getDispatcher());
        MaterialCommand.register(e.getDispatcher()); LinkCommand.register(e.getDispatcher()); KChatCommand.register(e.getDispatcher());
        KEssentialsCommand.register(e.getDispatcher());
    }
    @SubscribeEvent public void onServerTick(TickEvent.ServerTickEvent e){
        if (e.phase == TickEvent.Phase.END) {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            tpas.tick();
            state.tick();
            if (server != null) {
                cd.tick(server);
                config.tickRestart(server);
            }
        }
    }
    @SubscribeEvent public void onServerAboutToStart(ServerAboutToStartEvent e) {
        ServerRef.set(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer());
        try {
            this.discord = new DiscordBot(configDir.resolve("discord.properties"));
            if (discord.isEnabled()) {
                discord.start();
                ChatEvents.setDiscord(discord);
            } else {
                ChatEvents.setDiscord(null);
            }
        }
        catch (Exception ex) { System.err.println("[kEssentials] Failed to init Discord: " + ex.getMessage()); }
        if (kchat != null) {
            kchat.applyMotd(e.getServer());
        }
    }
    @SubscribeEvent public void onServerStarting(ServerStartingEvent e) {
        if (kchat != null) {
            kchat.applyMotd(e.getServer());
        }
    }
    @SubscribeEvent public void onServerStarted(ServerStartedEvent e){ ServerRef.set(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer()); if (discord != null && discord.isEnabled()) discord.setGameStatus("Minecraft 1.18.2"); if (kchat != null) kchat.applyMotd(e.getServer()); }
    @SubscribeEvent public void onServerStopping(ServerStoppingEvent e){ try { if (discord != null) { discord.shutdown(); ChatEvents.setDiscord(null); discord = null; } homes.save(); warps.save(); spawn.save(); state.save(); chat.save(); } catch (Exception ignored) {} }
    @SubscribeEvent public void onDeath(LivingDeathEvent e){ if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) back.store(p); }
}
