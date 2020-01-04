package net.farlands.odyssey.command;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.TextUtils;
import net.farlands.odyssey.util.Utils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Command extends org.bukkit.command.Command {
    protected static final List<String> TRUE_OR_FALSE = Arrays.asList("true", "false");

    protected final List<String> aliases;
    protected final Rank minimumRankRequirement;
    protected final boolean requiresAlias; // Whether we should pass the alias in the arguments

    protected Command(Rank minRank, String description, String usage, boolean requiresAlias, String name, String... aliases) {
        super(name, description, usage, Arrays.stream(aliases).map(String::toLowerCase).collect(Collectors.toList()));
        this.aliases = Arrays.stream(aliases).map(String::toLowerCase).collect(Collectors.toList());
        this.minimumRankRequirement = minRank;
        this.requiresAlias = requiresAlias;
    }

    protected Command(Rank minRank, String description, String usage, String name, String... aliases) {
        this(minRank, description, usage, false, name, aliases);
    }

    // Returns false if the command was used incorrectly, true otherwise
    protected abstract boolean execute(CommandSender sender, String[] args) throws Exception;

    // The only point of this having a return value is to make bukkit happy
    @Override
    public final boolean execute(CommandSender sender, String alias, String[] args0) {
        try {
            // Add the alias to the args array if needed
            String[] args = requiresAlias ? new String[args0.length + 1] : args0;
            if(requiresAlias) {
                args[0] = alias.toLowerCase();
                System.arraycopy(args0, 0, args, 1, args0.length);
            }
            try {
                if(!execute(sender, args))
                    showUsage(sender);
            }catch(TextUtils.SyntaxException ex) {
                sender.sendMessage(ChatColor.RED + ex.getMessage());
            }
        }catch(Throwable ex) {
            sender.sendMessage("There was an error executing this command.");
            StringBuilder error = new StringBuilder();
            error.append(ex.getClass().getName()).append(": ").append(ex.getMessage()).append('\n');
            for(StackTraceElement ste : ex.getStackTrace())
                error.append("    ").append(ste.toString()).append('\n');
            String errorString = error.toString();
            if(showErrorsOnDiscord()) {
                Chat.error("Error executing command " + getName() + " from " + sender.getName());
                FarLands.getDebugger().echo(errorString.length() > 1994 ? errorString.substring(0, 1991) + "..." : errorString);
            }
            ex.printStackTrace(System.out);
        }
        return true;
    }

    @Override
    public boolean isRegistered() {
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return Collections.emptyList();
    }

    public boolean matches(String command) { // Does this command math the given token?
        command = command.toLowerCase();
        return command.equalsIgnoreCase(getName()) || aliases.contains(command);
    }

    public boolean canUse(CommandSender sender) {
        if(Rank.getRank(sender).specialCompareTo(minimumRankRequirement) < 0) {
            sender.sendMessage(ChatColor.RED + "You must be at least rank " + Utils.capitalize(minimumRankRequirement.toString()) +
                    " to use this command.");
            return false;
        }
        return true;
    }

    public boolean showErrorsOnDiscord() {
        return true;
    }

    public boolean requiresVerifiedDiscordSenders() {
        return true;
    }

    public Rank getMinRankRequirement() {
        return minimumRankRequirement;
    }

    protected void showUsage(CommandSender sender) {
        sender.sendMessage("Usage: " + getUsage());
    }

    public static <T> T parseNumber(String input, Function<String, T> parser, T onFail) {
        try {
            return parser.apply(input);
        }catch(Throwable t) {
            return onFail;
        }
    }

    // Excludes vanished players
    public static Player getPlayer(String name, CommandSender sender) {
        Player player = Bukkit.getServer().getPlayer(name);
        if (player == null || (!Rank.getRank(sender).isStaff() && FarLands.getDataHandler().getOfflineFLPlayer(player).isVanished()))
            return null;
        return player;
    }

    // Excludes vanished players
    public static List<String> getOnlinePlayers(String partialName, CommandSender sender) {
        Stream<? extends Player> stream = Bukkit.getOnlinePlayers().stream();
        if (!Rank.getRank(sender).isStaff())
            stream = stream.filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).isVanished());
        return stream.map(Player::getName).filter(name -> name.toLowerCase().startsWith(partialName.toLowerCase()))
                .collect(Collectors.toList());
    }

    public static String joinArgsBeyond(int index, String delim, String[] args) {
        ++index;
        String[] data = new String[args.length - index];
        System.arraycopy(args, index, data, 0, data.length);
        return String.join(delim, data);
    }
}
