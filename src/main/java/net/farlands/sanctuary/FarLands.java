package net.farlands.sanctuary;

import com.squareup.moshi.Moshi;
import hu.trigary.advancementcreator.Advancement;
import hu.trigary.advancementcreator.AdvancementFactory;
import hu.trigary.advancementcreator.Rewards;
import hu.trigary.advancementcreator.shared.ItemObject;
import hu.trigary.advancementcreator.trigger.ImpossibleTrigger;
import hu.trigary.advancementcreator.trigger.PlayerKilledEntityTrigger;
import net.farlands.sanctuary.command.CommandHandler;
import net.farlands.sanctuary.data.*;
import net.farlands.sanctuary.discord.DiscordHandler;
import net.farlands.sanctuary.gui.GuiHandler;
import net.farlands.sanctuary.mechanic.MechanicHandler;
import net.farlands.sanctuary.scheduling.Scheduler;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
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
    }

    public static FarLands getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {

        //This line configures the followings: create the NamespacedKeys in this plugin's namespace,
        //activate the advancements but don't instantly reload the data cache
        AdvancementFactory factory = new AdvancementFactory(this, true, false);

        //Create a root advancement which is also automatically unlocked (with a player head icon)
        Advancement root = factory.getRoot("newbie/root", "Getting Started", "Newbie Advancements", Material.PLAYER_HEAD, "block/dirt");

        //One of the most common advancements, the requirement is that you obtain an item:
        Advancement wood = factory.getItem("newbie/wood", root, "Chopper", "Chop down a tree", Material.OAK_LOG);
        Advancement workbench = factory.getItem("newbie/workbench", wood, "Crafter", "Craft yourself a crafting table", Material.CRAFTING_TABLE);
        Advancement sword = factory.getAnyItem("newbie/sword", workbench, "Armed to Teeth", "Craft a sword", Material.WOODEN_SWORD, Material.STONE_SWORD);

        //I could still use a factory, but I wanted to give an example of how development works without it:
        new Advancement(new NamespacedKey(this, "newbie/kill"), new ItemObject().setItem(Material.STONE_SWORD),
            Component.text("Harvester"), Component.text("Put your weapon to good use"))
            .addTrigger("kill", new PlayerKilledEntityTrigger())
            .makeChild(sword.key())
            .setFrame(Advancement.Frame.GOAL)
            .setRewards(new Rewards().setExperience(10))
            .setToast(true)
            .activate(false);

        new Advancement(new NamespacedKey(this, "nether/quenching-fire"), new ItemObject().setItem(Material.BLAZE_ROD),
            Component.text("Quenching Fire", NamedTextColor.GOLD), Component.text("Throw a fire resistance potion on a Blaze"))
            .addTrigger("throw", new ImpossibleTrigger())
            .makeChild(NamespacedKey.minecraft("nether/obtain_blaze_rod"))
            .setRewards(new Rewards().setExperience(100))
            .setToast(true)
            .setAnnounce(true)
            .activate(false);

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

    public static Debugger getDebugger() {
        return instance.debugger;
    }

    public static World getWorld() {
        return instance.farlandsWorld;
    }

    public static Moshi getMoshi() {
        return moshi;
    }
}
