package net.farlands.sanctuary;

import com.squareup.moshi.Moshi;
import net.farlands.sanctuary.advancement.AdvancementHandler;
import net.farlands.sanctuary.command.CommandHandler;
import net.farlands.sanctuary.data.*;
import net.farlands.sanctuary.discord.DiscordHandler;
import net.farlands.sanctuary.gui.GuiHandler;
import net.farlands.sanctuary.mechanic.MechanicHandler;
import net.farlands.sanctuary.scheduling.Scheduler;
import net.farlands.sanctuary.util.Logging;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
    private final AdvancementHandler advancementHandler;
    private World farlandsWorld;

    private static final Moshi moshi = createMoshi();
    private static FarLands instance;

    public static Moshi createMoshi() {
        Moshi.Builder builder = new Moshi.Builder();
        CustomAdapters.register(builder);
        return builder.build();
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
        this.advancementHandler = new AdvancementHandler();
    }

    public static FarLands getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {

        //Reload the data cache after all advancements have been added
        Bukkit.reloadData();

        // TODO: reinstate seed
        // farlandsWorld = (new WorldCreator(DataHandler.WORLDS.get(3))).seed(0xc0ffee).generateStructures(false).createWorld();
        farlandsWorld = (new WorldCreator(DataHandler.WORLDS.get(3))).generateStructures(true).createWorld();
        dataHandler.preStartup();
        Rank.createTeams();
        scheduler.start();
        mechanicHandler.registerMechanics();
        discordHandler.startBot();
        Bukkit.getScheduler().runTaskLater(this, () -> Logging.log("Successfully loaded FarLands v" +
                instance.getDescription().getVersion() + "."), 50L);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "headdatabase:hdb r");
            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "petblocks:petblockreload");
        }, 60L);
    }

    @Override
    public void onDisable() {
        scheduler.interrupt();
        if (discordHandler.isActive()) {
            discordHandler.getNativeBot().shutdown();
        }
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
            Logging.error("Failed to execute script " + script);
            ex.printStackTrace();
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

    public static AdvancementHandler getAdvancementHandler() {
        return instance.advancementHandler;
    }

    public static Debugger getDebugger() {
        return instance.debugger;
    }

    public static World getWorld() {
        return instance.farlandsWorld;
    }

    public static Moshi getMoshi() {
        return moshi;
    }

    /**
     * Get a {@link NamespacedKey} with the FarLands namespace. Do not include {@code farlands:} in the parameter.
     *
     * @param key the key for the {@link NamespacedKey}
     * @return new {@link NamespacedKey}
     * @throws IllegalArgumentException if the parameter is invalid
     */
    public static @NotNull NamespacedKey namespacedKey(final @NotNull String key) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(key, instance);
        if (namespacedKey == null) {
            throw new IllegalArgumentException("Invalid NamespacedKey: " + key);
        }
        return namespacedKey;
    }
}
