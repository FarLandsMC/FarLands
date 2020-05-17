package net.farlands.odyssey;

import com.google.gson.*;

import net.farlands.odyssey.command.CommandHandler;
import net.farlands.odyssey.data.DataHandler;
import net.farlands.odyssey.data.Config;
import net.farlands.odyssey.data.Debugger;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.discord.DiscordHandler;
import net.farlands.odyssey.gui.GuiHandler;
import net.farlands.odyssey.mechanic.MechanicHandler;
import net.farlands.odyssey.scheduling.Scheduler;
import net.farlands.odyssey.util.Logging;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

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
        Rank.createTeams();
        scheduler.start();
        farlandsWorld = (new WorldCreator(DataHandler.WORLDS.get(3))).seed(0xc0ffee).generateStructures(false).createWorld();
        mechanicHandler.registerMechanics();
        discordHandler.startBot();
        Bukkit.getScheduler().runTaskLater(this, () -> Logging.log("Successfully loaded FarLands v" +
                instance.getDescription().getVersion() + "."), 50L);
    }

    @Override
    public void onDisable() {
        scheduler.interrupt();
        discordHandler.setActive(false);
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
        } catch (IOException ex) {
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
}
