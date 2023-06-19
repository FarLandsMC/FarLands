package net.farlands.sanctuary.command;

import com.google.common.collect.ImmutableMap;
import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.StringFilter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.ChatHandler;
import net.farlands.sanctuary.command.discord.*;
import net.farlands.sanctuary.command.player.*;
import net.farlands.sanctuary.command.staff.*;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.bukkit.craftbukkit.v1_20_R1.command.VanillaCommandWrapper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Handles command registration, querying, and Discord command execution.
 */
public class CommandHandler extends Mechanic {

    private final        List<Command>         commands;
    private final        List<SlashCommand>    slashCommands;
    private final        Set<SlashCommandData> discordSlashCommands;
    private static final List<String>          COMMAND_LOG_BLACKLIST = Arrays.asList(
            "hdb", "headdb", "heads",
            "shops", "searchshops",
            "trigger");

    public CommandHandler() {
        this.commands = new ArrayList<>();
        this.slashCommands = new ArrayList<>();
        this.discordSlashCommands = new HashSet<>();
    }

    @Override
    public void onStartup() {
        // Register your commands here so it works properly. Make sure you extend net.farlands.odyssey.command.Command

        registerCommand(new CommandChain());            // Initiate
        registerCommand(new CommandDelay());            // Initiate

        // Discord Commands
        registerCommand(new CommandAddReactions());     // Jr Builder
        registerCommand(new CommandArchive());          // Admin
        registerCommand(new CommandArtifact());         // Admin (Requires JS Permission)
        registerCommand(new CommandDevReport());        // Initiate
        registerCommand(new CommandGetLog());           // Admin
        registerCommand(new CommandKickme());           // Initiate
        registerCommand(new CommandNotes());            // Jr Builder
        registerCommand(new CommandPropose());          // Builder
        registerCommand(new CommandUploadSchem());      // Builder
        registerCommand(new CommandVerify());           // Initiate

        // Player Commands
        registerCommand(new CommandAFK());              // Initiate
        registerCommand(new CommandBirthday());         // Initiate
        registerCommand(new CommandCensor());           // Initiate
        registerCommand(new CommandColors());           // Adept
        registerCommand(new CommandConfig());           // Initiate
        registerCommand(new CommandCraft());            // Patron
        registerCommand(new CommandDelHome());          // Initiate
        registerCommand(new CommandDiscord());          // Initiate
        registerCommand(new CommandDonate());           // Initiate
        registerCommand(new CommandEat());              // Sponsor
        registerCommand(new CommandEchest());           // Donor
        registerCommand(new CommandEditArmorStand());   // Sponsor
        registerCommand(new CommandEditSign());         // Sage
        registerCommand(new CommandEnd());              // Initiate
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
        registerCommand(new CommandPocket());           // Initiate
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
        registerCommand(new CommandAlts());             // Jr Builder
        registerCommand(new CommandBack());             // Jr_Builder
        registerCommand(new CommandBotSpam());          // Builder
        registerCommand(new CommandBright());           // Jr_Builder
        registerCommand(new CommandBroadcast());        // Builder
        registerCommand(new CommandCustomItem());       // Builder
        registerCommand(new CommandDeath());            // Jr_Builder
        registerCommand(new CommandDeathMute());        // Builder
        registerCommand(new CommandDebug());            // Builder
        registerCommand(new CommandDelWarp());          // Builder
        registerCommand(new CommandEdit());             // Builder
        registerCommand(new CommandEntityCount());      // Jr_Builder
        registerCommand(new CommandEvidenceLocker());   // Jr_Builder
        registerCommand(new CommandFLTrigger());        // Admin
        registerCommand(new CommandFly());              // Media
        registerCommand(new CommandGameMode());         // Jr_Builder
        registerCommand(new CommandGamemodeImmune());   // Admin
        registerCommand(new CommandGod());              // Jr_Builder
        registerCommand(new CommandJS());               // Admin (Requires JS Permission)
        registerCommand(new CommandKick());             // Jr_Builder
        registerCommand(new CommandMined());            // Builder
        registerCommand(new CommandMoveSchems());       // Builder
        registerCommand(new CommandMute());             // Jr_Builder
        registerCommand(new CommandPocketReset());      // Admin
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

        registerDiscordCommands();
    }

    private void registerDiscordCommands() {

        // Register Slash Commands
        List<SlashCommandData> slashCommands = discordSlashCommands
            .stream()
            .map(c -> c.setGuildOnly(true)) // Force it to guild only (disables global cache and commands in dm)
            .limit(Commands.MAX_SLASH_COMMANDS)
            .toList();
        FarLands.getDiscordHandler().registerSlashCommands(slashCommands);

        FarLands.getDiscordHandler().registerAutocompleters(ImmutableMap.of( // Register some default auto-completers
            "*", (option, partial) -> {
                switch (option.toLowerCase()) {
                    case "playername", "player-name", "username", "user-name" -> {
                        return Bukkit.getOnlinePlayers()
                            .stream()
                            .filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).vanished)
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                            .toArray(String[]::new);

                    }
                }
                return new String[0];
            }
        ));

        FarLands.getDiscordHandler().getNativeBot().retrieveCommands().queue(cmds -> Logging.log("Registered " + cmds.size() + " slash commands."));
    }

    private void registerCommand(Command command) {
        if(!(command instanceof PlayerCommand)) {
            discordSlashCommands
                .addAll( // Add slash commands to set
                         command.discordCommand() == null
                             ? command.discordCommands()
                             : List.of(command.discordCommand())
                );

            // Register Autocompletion
            Map<String, DiscordCompleter> ac = command.discordAutocompletion();
            FarLands.getDiscordHandler().registerAutocompleters(ac);
        }
        commands.add(command);
        ((CraftServer) Bukkit.getServer()).getCommandMap().register("farlands", command);
    }

    private void registerCommand(SlashCommand command) {
        discordSlashCommands.add(command.commandData());
        slashCommands.add(command);
    }

    @SuppressWarnings("unchecked")
    public <T extends Command> T getCommand(Class<T> clazz) {
        return (T) commands.stream().filter(c -> clazz.equals(c.getClass())).findAny().orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends SlashCommand> T getSlashCommand(Class<T> clazz) {
        return (T) slashCommands.stream().filter(c -> clazz.equals(c.getClass())).findAny().orElse(null);
    }

    public Command getCommand(String alias) {
        return commands.stream().filter(cmd -> cmd.matches(alias)).findAny().orElse(null);
    }

    public SlashCommand getSlashCommand(String name) {
        return slashCommands
            .stream()
            .filter(cmd -> cmd.commandData().getName().equalsIgnoreCase(name))
            .findAny()
            .orElse(null);
    }

    public SlashCommand getSlashCommand(SlashCommandInteraction interaction) {
        return getSlashCommand(interaction.getName());
    }

    public List<Command> getCommands() {
        return commands;
    }

    // Returns true if the command was handled
    @SuppressWarnings("unchecked")
    public boolean handleDiscordCommand(DiscordSender sender, Message message) {
        String rawStringCommand = message.getContentDisplay();

        // Notify staff
        logCommand(sender, rawStringCommand, sender.getChannel());

        // Parse out the command name
        String commandName = rawStringCommand.substring(
                rawStringCommand.startsWith("\\/")
                    ? 2
                    : rawStringCommand.startsWith("/")
                        ? 1
                        : 0,
                FLUtils.indexOfDefault(rawStringCommand.indexOf(' '), rawStringCommand.length())
        ).trim();

        // Get the args
        final String[] args = rawStringCommand.contains(" ")
                ? rawStringCommand.substring(rawStringCommand.indexOf(' ') + 1).split(" ")
                : new String[0];

        Command command = getCommand(commandName);
        // Ensure the command was sent in a channel where we accept commands
        if (
                !(command instanceof DiscordCommand)
                && DiscordChannel.IN_GAME.id() != message.getChannel().getIdLong()
                && DiscordChannel.STAFF_COMMANDS.id() != message.getChannel().getIdLong()
        ) {
            return false;
        }

        // Add command to the extended audit log
        if (sender.isVerified() && Rank.getRank(sender).specialCompareTo(Rank.MEDIA) >= 0 && shouldLog(command)) {
            FarLands.getDiscordHandler().sendMessage(DiscordChannel.COMMAND_LOG, sender.getName() + ": " + rawStringCommand);
        }

        if (command == null) {

            // Try to find a command
            org.bukkit.command.Command bukkitCommand = Bukkit.getServer().getCommandMap().getCommand(commandName);

            // See if it's a vanilla command
            if (bukkitCommand instanceof VanillaCommandWrapper cmd) {
                // Ensure the sender has permission
                if (!cmd.testPermission(sender)) {
                    return false;
                }

                cmd.execute(sender, commandName, args);

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
            if (sender.isVerified() && !command.canUse(sender))
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

    // Returns true if the command was handled
    @SuppressWarnings("unchecked")
    public void handleDiscordCommand(DiscordSender sender, SlashCommandInteraction interaction, String commandStr) {

        // Notify staff
        logCommand(sender, commandStr, sender.getChannel());

        // Parse out the command name
        String commandName = commandStr.substring(commandStr.startsWith("/")
                        ? 1
                        : 0,
                FLUtils.indexOfDefault(commandStr.indexOf(' '), commandStr.length())
        ).trim();

        // Get the args
        final String[] args = commandStr.contains(" ")
                ? commandStr.substring(commandStr.indexOf(' ') + 1).split(" ")
                : new String[0];

        Command command = getCommand(commandName);
        if (command == null) throw new IllegalArgumentException("Command not found.");
        // Ensure the command was sent in a channel where we accept commands
        if (
                !(command instanceof DiscordCommand)
                && DiscordChannel.IN_GAME.id() != interaction.getChannel().getIdLong()
                && DiscordChannel.STAFF_COMMANDS.id() != interaction.getChannel().getIdLong()
        ) {
            interaction.reply("Please use " + DiscordChannel.IN_GAME + " for sending commands.")
                .setEphemeral(true)
                .queue();
            return;
        }

        // Add command to the command log
        if (sender.isVerified() && Rank.getRank(sender).specialCompareTo(Rank.MEDIA) >= 0 && shouldLog(command)) {
            FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.COMMAND_LOG, sender.getName() + ": " + commandStr);
        }

        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            // Handle event cancellations and regions
            if (shouldNotExecute(command, sender))
                return;

            // Check verification
            if (command.requiresVerifiedDiscordSenders() && !sender.isVerified())
                return;

            // Check permission
            if (sender.isVerified() && !command.canUse(sender))
                return;

            String[] argsCopy = args;

            // Add the message info to the args if needed
            if (command instanceof DiscordCommand && ((DiscordCommand) command).requiresMessageID()) {
                String[] newArgs = new String[args.length + 1];
                newArgs[0] = interaction.getChannel().getId() + ":" + interaction.getId();
                System.arraycopy(args, 0, newArgs, 1, args.length);
                argsCopy = newArgs;
            }

            command.execute(sender, commandName, argsCopy);

            // Delete the discord message if necessary (No way to delete, so make ephemeral)
            if (command instanceof DiscordCommand && ((DiscordCommand) command).deleteOnUse())
                sender.ephemeral(true);
        });

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Rank senderRank = Rank.getRank(event.getPlayer());
        if ((event.getMessage().startsWith("/minecraft:") && !senderRank.isStaff()) || event.getMessage().startsWith("/farlands:")) {
            event.setCancelled(true);
            return;
        }

        if (event.getMessage().startsWith("/trust"))
            event.getPlayer().sendMessage(ComponentColor.gold("Be careful trusting player on your claim as they are your responsibility."));

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
            logCommand(player, fullCommand, null);
        if (senderRank.specialCompareTo(Rank.MEDIA) >= 0 && shouldLog(c) &&
                !COMMAND_LOG_BLACKLIST.contains(command.toLowerCase()))
            FarLands.getDiscordHandler().sendMessage(
                    DiscordChannel.COMMAND_LOG, event.getPlayer().getName() + ": " + fullCommand
            );
        if (c == null) return;
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            if (shouldNotExecute(c, player)) return;
            if (!c.canUse(player)) return;
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
            logCommand(sender, fullCommand, null);
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

    /**
     * Alert ingame staff when a command is run
     * @param name Name of the sender
     * @param command Command to log
     * @param discordChannel Discord channel in which the message was sent -- null for in-game
     */
    public void logCommand(String name, String command, @Nullable TextChannel discordChannel) {
        TextColor col = NamedTextColor.GREEN;
        if (discordChannel == null) {
            col = NamedTextColor.RED;
        } else if (discordChannel.getIdLong() == DiscordChannel.STAFF_COMMANDS.id()) {
            col = NamedTextColor.DARK_AQUA;
        }
        Logging.broadcastStaff(
            Component.text(name + ": ")
                .color(col)
                .append(ComponentColor.gray(command))
        );
    }

    /**
     * Alert ingame staff when a command is run
     * @param sender Sender of the command
     * @param command Command to log
     * @param discordChannel Discord channel in which the message was sent -- null for in-game
     */
    public void logCommand(CommandSender sender, String command, @Nullable TextChannel discordChannel) {
        logCommand(sender.getName(), command, discordChannel);
    }

    /**
     * Alert ingame staff when a command is run
     * @param sender Sender of the command
     * @param command Command to log
     * @param discordChannel Discord channel in which the message was sent -- null for in-game
     */
    public void logCommand(OfflineFLPlayer sender, String command, @Nullable TextChannel discordChannel) {
        logCommand(sender.username, command, discordChannel);
    }

    private final List<Class<? extends Command>> commandExcludes = List.of(
        CommandPropose.class,
        CommandKick.class,
        CommandMute.class,
        CommandPunish.class,
        CommandStaffChat.class,
        CommandSetRank.class,
        CommandJS.class
    );

    private boolean shouldLog(Command command) {
        return command == null || command.getMinRankRequirement().specialCompareTo(Rank.MEDIA) >= 0 &&
                !commandExcludes.contains(command.getClass());
    }

    public List<SlashCommand> slashCommands() {
        return slashCommands;
    }
}
