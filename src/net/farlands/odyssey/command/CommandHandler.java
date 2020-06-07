package net.farlands.odyssey.command;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.StringFilter;
import net.dv8tion.jda.api.entities.Message;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.discord.*;
import net.farlands.odyssey.command.player.*;
import net.farlands.odyssey.command.player.CommandList;
import net.farlands.odyssey.command.player.CommandMe;
import net.farlands.odyssey.command.staff.*;
import net.farlands.odyssey.command.staff.CommandDebug;
import net.farlands.odyssey.command.staff.CommandKick;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.discord.DiscordChannel;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.mechanic.Mechanic;
import net.farlands.odyssey.util.Logging;
import net.farlands.odyssey.util.ReflectionHelper;
import net.farlands.odyssey.util.FLUtils;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.craftbukkit.v1_15_R1.command.VanillaCommandWrapper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandHandler extends Mechanic {
    private final List<Command> commands;

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
        registerCommand(new CommandArtifact());         // Admin
        registerCommand(new CommandDevReport());        // Initiate
        registerCommand(new CommandGetLog());           // Admin
        registerCommand(new CommandNotes());            // Jr Builder
        registerCommand(new CommandPropose());          // Builder
        registerCommand(new CommandUploadSchem());      // Builder
        registerCommand(new CommandVerify());           // Initiate

        // Player Commands
        registerCommand(new CommandAFK());              // Initiate
        registerCommand(new CommandCensor());           // Initiate
        registerCommand(new CommandColors());           // Adept
        registerCommand(new CommandCraft());            // Patron
        registerCommand(new CommandDelHome());          // Initiate
        registerCommand(new CommandDiscord());          // Initiate
        registerCommand(new CommandDonate());           // Initiate
        registerCommand(new CommandEchest());           // Donor
        registerCommand(new CommandExtinguish());       // Patron
        registerCommand(new CommandGivePet());          // Initiate
        registerCommand(new CommandGuideBook());        // Initiate
        registerCommand(new CommandHat());              // Donor
        registerCommand(new CommandHome());             // Initiate
        registerCommand(new CommandHomes());            // Initiate
        registerCommand(new CommandIgnore());           // Initiate
        registerCommand(new CommandIgnoredList());      // Initiate
        registerCommand(new CommandJoined());           // Initiate
        registerCommand(new CommandList());             // Initiate
        registerCommand(new CommandMail());             // Initiate
        registerCommand(new CommandMe());               // Initiate
        registerCommand(new CommandMessage());          // Initiate
        registerCommand(new CommandNick());             // Adept
        registerCommand(new CommandPackage());          // Knight
        registerCommand(new CommandParticles());        // Donor
        registerCommand(new CommandPatchnotes());       // Initiate
        registerCommand(new CommandProposeWarp());      // Initiate
        registerCommand(new CommandPTime());            // Knight
        registerCommand(new CommandPvP());              // Initiate
        registerCommand(new CommandPWeather());         // Sage
        registerCommand(new CommandRanks());            // Initiate
        registerCommand(new CommandRankup());           // Initiate
        registerCommand(new CommandRealName());         // Initiate
        registerCommand(new CommandReboot());           // Initiate
        registerCommand(new CommandReport());           // Initiate
        registerCommand(new CommandResetHome());        // Initiate
        registerCommand(new CommandRules());            // Initiate
        registerCommand(new CommandSeen());             // Initiate
        registerCommand(new CommandSetHome());          // Initiate
        registerCommand(new CommandShovel());           // Initiate
        registerCommand(new CommandShrug());            // Initiate
        registerCommand(new CommandSit());              // Knight
        registerCommand(new CommandSkull());            // Sage
        registerCommand(new CommandSpawn());            // Initiate
        registerCommand(new CommandStack());            // Esquire
        registerCommand(new CommandStats());            // Initiate
        registerCommand(new CommandTop());              // Initiate
        registerCommand(new CommandTPA());              // Initiate
        registerCommand(new CommandTPAccept());         // Initiate
        registerCommand(new CommandVote());             // Initiate
        registerCommand(new CommandVoteParty());        // Initiate
        registerCommand(new CommandWarp());             // Initiate
        registerCommand(new CommandWarps());            // Initiate
        registerCommand(new CommandWhyLag());           // Initiate
        registerCommand(new CommandWild());             // Initiate

        // Staff Commands
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
        registerCommand(new CommandFly());              // Media
        registerCommand(new CommandGameMode());         // Jr_Builder
        registerCommand(new CommandGod());              // Jr_Builder
        //registerCommand(new CommandJS());               // Admin (must be js user)
        registerCommand(new CommandKick());             // Jr_Builder
        registerCommand(new CommandMined());            // Builder
        registerCommand(new CommandMoveSchems());       // Builder
        registerCommand(new CommandMute());             // Jr_Builder
        registerCommand(new CommandPunish());           // Jr_Builder
        registerCommand(new CommandPurchase());         // Mod
        registerCommand(new CommandRestoreDeath());     // Builder
        registerCommand(new CommandSetRank());          // Builder
        registerCommand(new CommandSetSpawn());         // Admin
        registerCommand(new CommandSetWarp());          // Builder
        registerCommand(new CommandShutdown());         // Admin
        registerCommand(new CommandSmite());            // Admin
        registerCommand(new CommandStaffChat());        // Jr_Builder (has initiate to send false command)
        registerCommand(new CommandTNTArrow());         // Builder
        registerCommand(new CommandToPlayer());         // Jr_Builder
        registerCommand(new CommandToLocation());       // Jr_Builder
        registerCommand(new CommandVanish());           // Media
    }

    private void registerCommand(Command command) {
        commands.add(command);
        ((CraftServer)Bukkit.getServer()).getCommandMap().register("farlands", command);
    }

    @SuppressWarnings("unchecked")
    public <T extends Command> T getCommand(Class<T> clazz) {
        return (T)commands.stream().filter(c -> clazz.equals(c.getClass())).findAny().orElse(null);
    }

    public List<Command> getCommands() {
        return commands;
    }

    // Returns true if the command was handled
    @SuppressWarnings("unchecked")
    public boolean handleDiscordCommand(DiscordSender sender, Message message) {
        String fullCommand = message.getContentDisplay();
        // Notify staff
        Logging.broadcastStaff(ChatColor.GREEN + sender.getName() + ": " + ChatColor.GRAY + fullCommand);
        String command = fullCommand.substring(fullCommand.startsWith("/") ? 1 : 0, FLUtils.indexOfDefault(fullCommand.indexOf(' '), fullCommand.length())).trim();
        final String[] args = fullCommand.contains(" ") ? fullCommand.substring(fullCommand.indexOf(' ') + 1).split(" ") : new String[0];
        Command c = commands.stream().filter(cmd -> cmd.matches(command)).findAny().orElse(null);
        if((!(c instanceof DiscordCommand)) && FarLands.getDiscordHandler().getChannel(DiscordChannel.IN_GAME).getIdLong() != message.getChannel().getIdLong() &&
                FarLands.getDiscordHandler().getChannel(DiscordChannel.STAFF_COMMANDS).getIdLong() != message.getChannel().getIdLong()) {
            return false;
        }
        if(Rank.getRank(sender).specialCompareTo(Rank.MEDIA) >= 0 && shouldLog(c))
            FarLands.getDiscordHandler().sendMessage(DiscordChannel.COMMAND_LOG, sender.getName() + ": " + fullCommand);
        if(c == null) {
            Map<String, org.bukkit.command.Command> knownCommands =
                    (Map<String, org.bukkit.command.Command>) ReflectionHelper.getFieldValue
                            ("knownCommands", SimpleCommandMap.class, ((CraftServer) Bukkit.getServer())
                                    .getCommandMap());
            String cmd = message.getContentRaw().replaceAll("^(/+)", "").toLowerCase();
            String finalCmd = cmd.substring(0, cmd.contains(" ") ? cmd.indexOf(' ') : cmd.length());
            org.bukkit.command.Command bukkitCommand = knownCommands.get(finalCmd);
            if(bukkitCommand instanceof VanillaCommandWrapper) {
                if(!(boolean)ReflectionHelper.invoke("testPermission", VanillaCommandWrapper.class, bukkitCommand, sender))
                    return false;
                Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
                    MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
                    CommandListenerWrapper wrapper = new CommandListenerWrapper(sender,
                            server.getWorldServer(DimensionManager.OVERWORLD) == null ? Vec3D.a
                                    : new Vec3D(server.getWorldServer(DimensionManager.OVERWORLD).getSpawn()), Vec2F.a,
                            server.getWorldServer(DimensionManager.OVERWORLD), sender.isOp() ? 4 : 0, sender.getName(),
                            new ChatComponentText(sender.getName()), server, null);
                    Object dispatcher = ReflectionHelper.getFieldValue("dispatcher", VanillaCommandWrapper.class,
                            bukkitCommand);
                    Method toDispatcher = ReflectionHelper.getMethod("toDispatcher", VanillaCommandWrapper.class,
                            String[].class, String.class);
                    ReflectionHelper.invoke("a", CommandDispatcher.class, dispatcher, wrapper,
                            ReflectionHelper.invoke(toDispatcher, bukkitCommand, args, bukkitCommand.getName()),
                            ReflectionHelper.invoke(toDispatcher, bukkitCommand, args, finalCmd));
                });
                return true;
            }
            if(bukkitCommand != null)
                bukkitCommand.execute(sender, finalCmd, args);
            return true;
        }
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            if (shouldNotExecute(c, sender))
                return;
            if (c.requiresVerifiedDiscordSenders() && !sender.isVerified())
                return;
            if (!c.canUse(sender))
                return;
            String[] argsCopy = args;
            if (c instanceof DiscordCommand && ((DiscordCommand) c).requiresMessageID()) {
                String[] newArgs = new String[args.length + 1];
                newArgs[0] = message.getChannel().getId() + ":" + message.getId();
                System.arraycopy(args, 0, newArgs, 1, args.length);
                argsCopy = newArgs;
            }
            c.execute(sender, command, argsCopy);
            if (c instanceof DiscordCommand && ((DiscordCommand) c).deleteOnUse())
                message.delete().queue();
        });
        return true;
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Rank senderRank = Rank.getRank(event.getPlayer());
        if((event.getMessage().startsWith("/minecraft:") && !senderRank.isStaff()) || event.getMessage().startsWith("/farlands:")) {
            event.setCancelled(true);
            return;
        }
        if(event.getMessage().startsWith("/trust"))
            event.getPlayer().sendMessage(ChatColor.GOLD + "Be careful trusting player on your claim as they are your responsibility.");

        Player player = event.getPlayer();
        String fullCommand = event.getMessage();

        FarLands.getMechanicHandler().getMechanic(Chat.class).spamUpdate(player, fullCommand);
        String command = fullCommand.substring(fullCommand.startsWith("/") ? 1 : 0, FLUtils.indexOfDefault(fullCommand.indexOf(' '), fullCommand.length())).trim();
        String[] args = fullCommand.contains(" ") ? fullCommand.substring(fullCommand.indexOf(' ') + 1).split(" ") : new String[0];
        Command c = commands.stream().filter(cmd -> cmd.matches(command)).findAny().orElse(null);
        // Notify staff of usage
        if(!(c != null && (CommandStaffChat.class.equals(c.getClass()) || CommandMessage.class.equals(c.getClass()))))
            Logging.broadcastStaff(ChatColor.RED + player.getName() + ": " + ChatColor.GRAY + fullCommand);
        if(senderRank.specialCompareTo(Rank.MEDIA) >= 0 && shouldLog(c))
            FarLands.getDiscordHandler().sendMessage(DiscordChannel.COMMAND_LOG, event.getPlayer().getName() + ": " + fullCommand);
        if(c == null)
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

    @EventHandler(priority=EventPriority.HIGH)
    public void onServerCommand(ServerCommandEvent event) {
        if(event.getCommand().startsWith("/farlands:")) {
            event.setCancelled(true);
            return;
        }

        CommandSender sender = event.getSender();
        String fullCommand = event.getCommand();

        String command = fullCommand.substring(fullCommand.startsWith("/") ? 1 : 0, FLUtils.indexOfDefault(fullCommand.indexOf(' '), fullCommand.length())).trim();
        String[] args = fullCommand.contains(" ") ? fullCommand.substring(fullCommand.indexOf(' ') + 1).split(" ") : new String[0];
        Command c = commands.stream().filter(cmd -> cmd.matches(command)).findAny().orElse(null);
        // Notify staff of usage
        if(!((c != null && (CommandStaffChat.class.equals(c.getClass()) || CommandMessage.class.equals(c.getClass()))) ||
                sender instanceof BlockCommandSender))
            Logging.broadcastStaff(ChatColor.RED + sender.getName() + ": " + ChatColor.GRAY + fullCommand);
        if(c == null)
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

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(event.getEntity());
        if (session.handle.rank == Rank.INITIATE)
            session.setCommandCooldown(getCommand(CommandWild.class), Rank.INITIATE.getWildCooldown() * 60L * 20L);
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
