package net.farlands.sanctuary.command;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.StringFilter;

import com.kicas.rp.util.ReflectionHelper;
import net.dv8tion.jda.api.entities.Message;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.ChatHandler;
import net.farlands.sanctuary.command.discord.*;
import net.farlands.sanctuary.command.player.*;
import net.farlands.sanctuary.command.player.CommandHelp;
import net.farlands.sanctuary.command.player.CommandList;
import net.farlands.sanctuary.command.player.CommandMe;
import net.farlands.sanctuary.command.staff.*;
import net.farlands.sanctuary.command.staff.CommandDebug;
import net.farlands.sanctuary.command.staff.CommandKick;
import net.farlands.sanctuary.command.staff.CommandFLTrigger;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.Chat;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.FLUtils;

import net.minecraft.commands.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.server.MinecraftServer;

import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.command.VanillaCommandWrapper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Handles command registration, querying, and Discord command execution.
 */
public class CommandHandler extends Mechanic {
    private final List<Command> commands;
    private static final List<String> COMMAND_LOG_BLACKLIST = Arrays.asList(
            "hdb", "headdb", "heads",
            "shops", "searchshops",
            "petblock", "petblocks", "petblockreload",
            "trigger");

    public CommandHandler() {
        this.commands = new ArrayList<>();
    }

    @Override
    public void onStartup() {
        // Register your commands here so it works properly. Make sure you extend net.farlands.odyssey.command.Command

        registerCommand(new CommandChain());            // Initiate
        registerCommand(new CommandDelay());            // Initiate

        // Discord Commands
        registerCommand(new CommandAlts());             // Jr Builder
        registerCommand(new CommandArchive());          // Admin
        registerCommand(new CommandArtifact());         // Admin (Requires JS Permission)
        registerCommand(new CommandDevReport());        // Initiate
        registerCommand(new CommandGetLog());           // Admin
        registerCommand(new CommandNotes());            // Jr Builder
        registerCommand(new CommandPropose());          // Builder
        registerCommand(new CommandUploadSchem());      // Builder
        registerCommand(new CommandVerify());           // Initiate

        // Player Commands
        registerCommand(new CommandAFK());              // Initiate
        registerCommand(new CommandBirthday());         // Initiate
        registerCommand(new CommandCensor());           // Initiate
        registerCommand(new CommandColors());           // Adept
        registerCommand(new CommandCraft());            // Patron
        registerCommand(new CommandDelHome());          // Initiate
        registerCommand(new CommandDiscord());          // Initiate
        registerCommand(new CommandDonate());           // Initiate
        registerCommand(new CommandEat());              // Sponsor
        registerCommand(new CommandEchest());           // Donor
        registerCommand(new CommandEditArmorStand());   // Sponsor
        registerCommand(new CommandEditSign());         // Sage
        registerCommand(new CommandExtinguish());       // Patron
        registerCommand(new CommandGivePet());          // Initiate
        registerCommand(new CommandGuideBook());        // Initiate
        registerCommand(new CommandHat());              // Donor
        registerCommand(new CommandHeadDatabase());     // Sponsor
        registerCommand(new CommandHelp());             // Initiate
        registerCommand(new CommandHome());             // Initiate
        registerCommand(new CommandHomes());            // Initiate
        registerCommand(new CommandIgnore());           // Initiate
        registerCommand(new CommandIgnoredList());      // Initiate
        registerCommand(new CommandJoined());           // Initiate
        registerCommand(new CommandKittyCannon());      // Sponsor
        registerCommand(new CommandLastDeath());        // Initiate
        registerCommand(new CommandList());             // Initiate
        registerCommand(new CommandMail());             // Initiate
        registerCommand(new CommandMe());               // Initiate
        registerCommand(new CommandMend());             // Sponsor
        registerCommand(new CommandMessage());          // Initiate
        registerCommand(new CommandNick());             // Adept
        registerCommand(new CommandNightSkip());        // Initiate
        registerCommand(new CommandPackage());          // Knight
        registerCommand(new CommandPackageAccept());    // Initiate
        registerCommand(new CommandPackageView());      // Initiate
        registerCommand(new CommandParticles());        // Donor
        registerCommand(new CommandParty());            // Initiate
        registerCommand(new CommandPatchnotes());       // Initiate
        registerCommand(new CommandPetblock());         // Sponsor
        registerCommand(new CommandPronouns());         // Initiate
        registerCommand(new CommandProposeWarp());      // Initiate
        registerCommand(new CommandPTime());            // Knight
        registerCommand(new CommandPvP());              // Initiate
        registerCommand(new CommandPWeather());         // Sage
        registerCommand(new CommandRanks());            // Initiate
        registerCommand(new CommandRankup());           // Initiate
        registerCommand(new CommandRealName());         // Initiate
        registerCommand(new CommandReboot());           // Initiate
        registerCommand(new CommandRenameItem());       // Sponsor
        registerCommand(new CommandRenameHome());       // Initiate
        registerCommand(new CommandReport());           // Initiate
        registerCommand(new CommandResetHome());        // Initiate
        registerCommand(new CommandRules());            // Initiate
        registerCommand(new CommandSeen());             // Initiate
        registerCommand(new CommandSetHome());          // Initiate
        registerCommand(new CommandSharehome());        // Initiate
        registerCommand(new CommandShovel());           // Initiate
        registerCommand(new CommandShrug());            // Initiate
        registerCommand(new CommandSit());              // Knight
        registerCommand(new CommandSkull());            // Sage
        registerCommand(new CommandSpawn());            // Initiate
        registerCommand(new CommandStack());            // Esquire
        registerCommand(new CommandStats());            // Initiate
        registerCommand(new CommandSwapHome());         // Initiate
        registerCommand(new CommandTimeZone());         // Initiate
        registerCommand(new CommandTogglePackages());   // Initiate
        registerCommand(new CommandTop());              // Initiate
        registerCommand(new CommandTPA());              // Initiate
        registerCommand(new CommandTPAccept());         // Initiate
        registerCommand(new CommandTrade());            // Bard
        registerCommand(new CommandTradepost());        // Initiate
        registerCommand(new CommandVote());             // Initiate
        registerCommand(new CommandVoteParty());        // Initiate
        registerCommand(new CommandVoteRewards());      // Initiate
        registerCommand(new CommandWarp());             // Initiate
        registerCommand(new CommandWarps());            // Initiate
        registerCommand(new CommandWhyLag());           // Initiate
        registerCommand(new CommandWild());             // Initiate

        // Staff Commands
        registerCommand(new CommandActiveEffects());    // Jr_Builder
        registerCommand(new CommandBack());             // Jr_Builder
        registerCommand(new CommandBotSpam());          // Builder
        registerCommand(new CommandBright());           // Jr_Builder
        registerCommand(new CommandBroadcast());        // Builder
        registerCommand(new CommandDeath());            // Jr_Builder
        registerCommand(new CommandDebug());            // Builder
        registerCommand(new CommandDelWarp());          // Builder
        registerCommand(new CommandEdit());             // Builder
        registerCommand(new CommandEntityCount());      // Jr_Builder
        registerCommand(new CommandEvidenceLocker());   // Jr_Builder
        registerCommand(new CommandFLTrigger());        // Admin
        registerCommand(new CommandFly());              // Media
        registerCommand(new CommandGameMode());         // Jr_Builder
        registerCommand(new CommandGod());              // Jr_Builder
        registerCommand(new CommandJS());             // Admin (Requires JS Permission)
        registerCommand(new CommandKick());             // Jr_Builder
        registerCommand(new CommandMined());            // Builder
        registerCommand(new CommandMoveSchems());       // Builder
        registerCommand(new CommandMute());             // Jr_Builder
        registerCommand(new CommandPartyReset());       // Builder
        registerCommand(new CommandPunish());           // Jr_Builder
        registerCommand(new CommandPunishRemove());     // Jr_Builder
        registerCommand(new CommandPurchase());         // Builder
        registerCommand(new CommandRestoreDeath());     // Builder
        registerCommand(new CommandSearchHomes());      // Jr_Builder
        registerCommand(new CommandSetRank());          // Builder
        registerCommand(new CommandSetSpawn());         // Admin
        registerCommand(new CommandSetWarp());          // Builder
        registerCommand(new CommandShutdown());         // Admin
        registerCommand(new CommandSmite());            // Admin
        registerCommand(new CommandStaffChat());        // Jr_Builder (has initiate to send false command)
        registerCommand(new CommandTNTArrow());         // Admin
        registerCommand(new CommandToLocation());       // Jr_Builder
        registerCommand(new CommandToPlayer());         // Jr_Builder
        registerCommand(new CommandVanish());           // Media
        registerCommand(new CommandViewNodes());        // Jr_Builder
    }

    private void registerCommand(Command command) {
        commands.add(command);
        ((CraftServer) Bukkit.getServer()).getCommandMap().register("farlands", command);
    }

    @SuppressWarnings("unchecked")
    public <T extends Command> T getCommand(Class<T> clazz) {
        return (T) commands.stream().filter(c -> clazz.equals(c.getClass())).findAny().orElse(null);
    }

    public Command getCommand(String alias) {
        return commands.stream().filter(cmd -> cmd.matches(alias)).findAny().orElse(null);
    }

    public List<Command> getCommands() {
        return commands;
    }

    // Returns true if the command was handled
    @SuppressWarnings("unchecked")
    public boolean handleDiscordCommand(DiscordSender sender, Message message) {
        String rawStringCommand = message.getContentDisplay();

        // Notify staff
        Logging.broadcastStaff(ChatColor.GREEN + sender.getName() + ": " + ChatColor.GRAY + Chat.colorize(rawStringCommand));

        // Parse out the command name
        String commandName = rawStringCommand.substring(
                rawStringCommand.startsWith("/") ? 1 : 0,
                FLUtils.indexOfDefault(rawStringCommand.indexOf(' '), rawStringCommand.length())
        ).trim();

        // Get the args
        final String[] args = rawStringCommand.contains(" ")
                ? rawStringCommand.substring(rawStringCommand.indexOf(' ') + 1).split(" ")
                : new String[0];

        Command command = getCommand(commandName);
        // Ensure the command was sent in a channel where we accept commands
        if (
                (!(command instanceof DiscordCommand)) &&
                        FarLands.getDiscordHandler().getChannel(DiscordChannel.IN_GAME).getIdLong() != message.getChannel().getIdLong() &&
                        FarLands.getDiscordHandler().getChannel(DiscordChannel.STAFF_COMMANDS).getIdLong() != message.getChannel().getIdLong()
        ) {
            return false;
        }

        // Add command to the extended audit log
        if (Rank.getRank(sender).specialCompareTo(Rank.MEDIA) >= 0 && shouldLog(command))
            FarLands.getDiscordHandler().sendMessage(DiscordChannel.COMMAND_LOG, sender.getName() + ": " + rawStringCommand);

        if (command == null) {
            // Get the list of registered commands
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) ReflectionHelper.getFieldValue(
                    "knownCommands",
                    SimpleCommandMap.class,
                    ((CraftServer) Bukkit.getServer()).getCommandMap()
            );

            // Try to find a command
            org.bukkit.command.Command bukkitCommand = knownCommands.get(commandName);

            // See if it's a vanilla command
            if (bukkitCommand instanceof VanillaCommandWrapper) {
                // Ensure the sender has permission
                if (!(boolean) ReflectionHelper.invoke("testPermission", VanillaCommandWrapper.class, bukkitCommand, sender))
                    return false;

                Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
                    MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();

                    CommandListenerWrapper wrapper = new CommandListenerWrapper(
                            sender,
                            // Position
                            server.getWorldServer(World.f) == null
                                    ? Vec3D.a
                                    : Vec3D.b(server.getWorldServer(World.f).getSpawn()), // World.f = World.OVERWORLD
                            Vec2F.a, // Rotation Vec2F.a = Vec2f.ORIGIN
                            server.getWorldServer(World.f), // World
                            sender.isOp() ? 4 : 0, // Permission level
                            // Name (required twice apparently)
                            sender.getName(),
                            new ChatComponentText(sender.getName()),
                            server,
                            null
                    );

                    // Dispatcher for the command (thing that runs it)
                    Object dispatcher = ReflectionHelper.getFieldValue(
                            "dispatcher",
                            VanillaCommandWrapper.class,
                            bukkitCommand
                    );

                    // Method that actually sends the command to the dispatcher
                    Method toDispatcher = ReflectionHelper.getMethod(
                            "toDispatcher",
                            VanillaCommandWrapper.class,
                            String[].class,
                            String.class
                    );

                    // Run the vanilla command
                    ReflectionHelper.invoke(
                            "a",
                            CommandDispatcher.class,
                            dispatcher,
                            wrapper,
                            ReflectionHelper.invoke(toDispatcher, bukkitCommand, args, bukkitCommand.getName()),
                            ReflectionHelper.invoke(toDispatcher, bukkitCommand, args, commandName)
                    );
                });

                return true;
            }

            // Run the bukkit command
            if (bukkitCommand != null)
                bukkitCommand.execute(sender, commandName, args);

            return true;
        }

        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            // Handle event cancellations and regions
            if (shouldNotExecute(command, sender))
                return;

            // Check verification
            if (command.requiresVerifiedDiscordSenders() && !sender.isVerified())
                return;

            // Check permission
            if (!command.canUse(sender))
                return;

            String[] argsCopy = args;

            // Add the message info to the args if needed
            if (command instanceof DiscordCommand && ((DiscordCommand) command).requiresMessageID()) {
                String[] newArgs = new String[args.length + 1];
                newArgs[0] = message.getChannel().getId() + ":" + message.getId();
                System.arraycopy(args, 0, newArgs, 1, args.length);
                argsCopy = newArgs;
            }

            command.execute(sender, commandName, argsCopy);

            // Delete the discord message if necessary
            if (command instanceof DiscordCommand && ((DiscordCommand) command).deleteOnUse())
                message.delete().queue();
        });

        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Rank senderRank = Rank.getRank(event.getPlayer());
        if ((event.getMessage().startsWith("/minecraft:") && !senderRank.isStaff()) || event.getMessage().startsWith("/farlands:")) {
            event.setCancelled(true);
            return;
        }

        if (event.getMessage().startsWith("/petblock") && event.getMessage().contains("rename") && Chat.getMessageFilter().isProfane(event.getMessage())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot set your pet's name to that.");
            return;
        }

        if (event.getMessage().startsWith("/trust"))
            event.getPlayer().sendMessage(ChatColor.GOLD + "Be careful trusting player on your claim as they are your responsibility.");

        if (event.getMessage().trim().equalsIgnoreCase("/co cancel")) {
            FLPlayerSession session = FarLands.getDataHandler().getSession(event.getPlayer());
            if (!session.canCoRollback()) {
                session.allowCoRollback();
                event.getPlayer().chat("/co cancel");
                session.resetCoRollback();
                event.setCancelled(true);
                return;
            }
        }

        Player player = event.getPlayer();
        String fullCommand = event.getMessage();

        ChatHandler.handleSpam(player, fullCommand);
        String command = fullCommand.substring(
                fullCommand.startsWith("/") ? 1 : 0,
                FLUtils.indexOfDefault(fullCommand.indexOf(' '), fullCommand.length())
        ).trim();
        String[] args = fullCommand.contains(" ")
                ? fullCommand.substring(fullCommand.indexOf(' ') + 1).split(" ")
                : new String[0];
        Command c = getCommand(command);
        // Notify staff of usage
        if (!(c != null && (CommandStaffChat.class.equals(c.getClass()) || CommandMessage.class.equals(c.getClass()) ||
                CommandEditArmorStand.class.equals(c.getClass()))))
            Logging.broadcastStaff(ChatColor.RED + player.getName() + ": " + ChatColor.GRAY + Chat.colorize(fullCommand));
        if (senderRank.specialCompareTo(Rank.MEDIA) >= 0 && shouldLog(c) &&
                !COMMAND_LOG_BLACKLIST.contains(command.toLowerCase()))
            FarLands.getDiscordHandler().sendMessage(
                    DiscordChannel.COMMAND_LOG, event.getPlayer().getName() + ": " + fullCommand
            );
        if (c == null)
            return;
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            if (shouldNotExecute(c, player))
                return;
            if (!c.canUse(player))
                return;
            c.execute(player, command, args);
        });

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onServerCommand(ServerCommandEvent event) {
        if (event.getCommand().startsWith("/farlands:")) {
            event.setCancelled(true);
            return;
        }

        CommandSender sender = event.getSender();
        String fullCommand = event.getCommand();

        String command = fullCommand.substring(fullCommand.startsWith("/") ? 1 : 0, FLUtils.indexOfDefault(fullCommand.indexOf(' '), fullCommand.length())).trim();
        String[] args = fullCommand.contains(" ") ? fullCommand.substring(fullCommand.indexOf(' ') + 1).split(" ") : new String[0];
        Command c = getCommand(command);
        // Notify staff of usage
        if (!((c != null && (CommandStaffChat.class.equals(c.getClass()) || CommandMessage.class.equals(c.getClass()) ||
                CommandEditArmorStand.class.equals(c.getClass()))) || sender instanceof BlockCommandSender))
            Logging.broadcastStaff(ChatColor.RED + sender.getName() + ": " + ChatColor.GRAY + Chat.colorize(fullCommand));
        if (c == null)
            return;
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            if (shouldNotExecute(c, sender))
                return;
            if (!c.canUse(sender))
                return;
            c.execute(sender, command, args);
        });

        event.setCancelled(true);
    }

    @EventHandler // reset initiate wild cooldown on death
    public void onPlayerDeath(PlayerDeathEvent event) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(event.getEntity());
        if (session.handle.rank == Rank.INITIATE)
            session.completeCooldown(getCommand(CommandWild.class));
    }

    private boolean shouldNotExecute(Command command, CommandSender sender) {
        // Comply with RP
        if (sender instanceof Player && !FarLands.getDataHandler().getOfflineFLPlayer(sender).rank.isStaff()) {
            FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(((Player) sender).getLocation());
            if (flags != null && flags.<StringFilter>getFlagMeta(RegionFlag.DENY_COMMAND).isBlocked(command.getName()))
                return true;
        }

        FLCommandEvent event = new FLCommandEvent(command, sender);
        FarLands.getInstance().getServer().getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    private boolean shouldLog(Command command) {
        return command == null || command.getMinRankRequirement().specialCompareTo(Rank.MEDIA) >= 0 &&
                !(command instanceof CommandPropose || command instanceof CommandKick ||
                        command instanceof CommandMute || command instanceof CommandPunish ||
                        command instanceof CommandStaffChat || command instanceof CommandSetRank ||
                        command instanceof CommandJS);
    }
}
