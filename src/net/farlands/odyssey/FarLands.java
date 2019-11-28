package net.farlands.odyssey;

import com.google.gson.*;
import net.farlands.odyssey.command.CommandHandler;
import net.farlands.odyssey.data.DataHandler;
import net.farlands.odyssey.data.Config;
import net.farlands.odyssey.data.Debugger;
import net.farlands.odyssey.data.PlayerDataHandler;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.discord.DiscordHandler;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.gui.GuiHandler;
import net.farlands.odyssey.mechanic.MechanicHandler;
import net.farlands.odyssey.scheduling.Scheduler;
import net.farlands.odyssey.util.TextUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Plugin class and "main" class for the plugin.
 */
public class FarLands extends JavaPlugin {
    private final Scheduler scheduler;
    private final Debugger debugger;
    private final DataHandler dataHandler;
    private final MechanicHandler mechanicHandler;
    private final CommandHandler commandHandler;
    private final DiscordHandler discordHandler;
    private final GuiHandler guiHandler;
    private World farlandsWorld;

    private static Gson gson;
    private static FarLands instance;

    static {
        gson = (new GsonBuilder()).setPrettyPrinting().create();
    }

    public FarLands() {
        instance = this;
        this.scheduler = new Scheduler();
        this.debugger = new Debugger();
        this.dataHandler = new DataHandler(getDataFolder());
        this.mechanicHandler = new MechanicHandler();
        this.commandHandler = new CommandHandler();
        this.discordHandler = new DiscordHandler();
        this.guiHandler = new GuiHandler();
    }

    public static FarLands getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        scheduler.start();
        farlandsWorld = (new WorldCreator(DataHandler.WORLDS.get(3))).seed(0xc0ffee).generateStructures(false).createWorld();
        mechanicHandler.registerMechanics();
        discordHandler.startBot();
        Bukkit.getScheduler().runTaskLater(this, () -> log("Successfully loaded FarLands v2."), 50L);
    }

    @Override
    public void onDisable() {
        discordHandler.setActive(false);
        dataHandler.onShutdown();
        scheduler.interrupt();
    }

    public static void executeScript(String script, String... args) {
        String[] command = new String[args.length + 3];
        command[0] = "nohup";
        command[1] = "sh";
        command[2] = System.getProperty("user.dir") + "/" + script;
        System.arraycopy(args, 0, command, 3, args.length);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        try {
            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(false);
            processBuilder.start();
        }catch(IOException ex) {
            System.out.println("Failed to execute script " + script);
            ex.printStackTrace(System.out);
        }
    }

    public static Scheduler getScheduler() {
        return instance.scheduler;
    }

    public static DataHandler getDataHandler() {
        return instance.dataHandler;
    }

    public static PlayerDataHandler getPDH() {
        return instance.dataHandler.getPDH();
    }

    public static Config getFLConfig() { // Shortcut method
        return instance.dataHandler.getConfig();
    }

    public static MechanicHandler getMechanicHandler() {
        return instance.mechanicHandler;
    }

    public static CommandHandler getCommandHandler() {
        return instance.commandHandler;
    }

    public static DiscordHandler getDiscordHandler() {
        return instance.discordHandler;
    }

    public static GuiHandler getGuiHandler() {
        return instance.guiHandler;
    }

    public static Debugger getDebugger() {
        return instance.debugger;
    }

    public static World getWorld() {
        return instance.farlandsWorld;
    }

    public static Gson getGson() {
        return gson;
    }

    public static void broadcastIngame(BaseComponent[] message) {
        Bukkit.getOnlinePlayers().stream().map(Player::spigot).forEach(spigot -> spigot.sendMessage(message));
        Bukkit.getConsoleSender().spigot().sendMessage(message);
    }

    public static void broadcastFormatted(String message, boolean sendToDiscord, Object... values) {
        BaseComponent[] formatted = TextUtils.format("{&(gold,bold) > }&(aqua)" + message, values);
        broadcastIngame(formatted);
        if(sendToDiscord)
            FarLands.getDiscordHandler().sendMessage("ingame", formatted);
    }

    public static void broadcast(String message, boolean applyColors) {
        broadcastIngame(TextComponent.fromLegacyText(applyColors ? Chat.applyColorCodes(message) : Chat.removeColorCodes(message)));
        FarLands.getDiscordHandler().sendMessage("ingame", message);
    }

    public static void broadcast(Predicate<FLPlayer> filter, String message, boolean applyColors) {
        Bukkit.getOnlinePlayers().stream().map(getPDH()::getFLPlayer).filter(filter)
                .forEach(flp -> flp.getOnlinePlayer().sendMessage(applyColors ? Chat.applyColorCodes(message) : Chat.removeColorCodes(message)));
        Bukkit.getConsoleSender().sendMessage(applyColors ? Chat.applyColorCodes(message) : Chat.removeColorCodes(message));
        FarLands.getDiscordHandler().sendMessage("ingame", message);
    }

    public static void broadcastStaff(BaseComponent[] message, String discordChannel) { // Set the channel to null to not send to discord
        Bukkit.getOnlinePlayers().stream().map(getPDH()::getFLPlayer).filter(flp -> flp.getRank().isStaff() &&
                FarLands.getDataHandler().getRADH().retrieveBoolean(true, "staffChatToggle", flp.getUuid().toString()))
                .forEach(flp -> flp.getOnlinePlayer().spigot().sendMessage(message));
        Bukkit.getConsoleSender().spigot().sendMessage(message);
        if(discordChannel != null)
            instance.discordHandler.sendMessage(discordChannel, message);
    }

    public static void broadcastStaff(BaseComponent[] message) {
        broadcastStaff(message, null);
    }

    public static void broadcastStaff(String message, String discordChannel) {
        broadcastStaff(TextComponent.fromLegacyText(message), discordChannel);
    }

    public static void broadcastStaff(String message) {
        broadcastStaff(TextComponent.fromLegacyText(message), null);
    }

    public static void log(Object x) {
        Bukkit.getLogger().info("[FLv2] - " + x);
    }

    public static void error(Object x) {
        String msg = Objects.toString(x);
        Bukkit.getLogger().severe("[FLv2] - " + msg);
        instance.debugger.echo("Error", msg);
        instance.discordHandler.sendMessageRaw("output", msg);
    }
}
