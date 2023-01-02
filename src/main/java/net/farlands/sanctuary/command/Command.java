package net.farlands.sanctuary.command;

import com.kicas.rp.util.TextUtils;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.Logging;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for all plugin commands.
 */
public abstract class Command extends org.bukkit.command.Command {
    protected final CommandData data;

    /**
     * Get the command's data. Things like usage, requirements, minimum rank, etc.
     *
     * @return command data
     */
    public CommandData data() {
        return this.data;
    }

    protected Command(CommandData data) {
        super(data.name(), data.description(), data.usage(), data.aliases());
        this.data = data;
    }

    @Deprecated
    protected Command(Rank minRank, Category category, String description, String usage, boolean requiresAlias, String name, String... aliases) {
        super(name, description, usage, Arrays.stream(aliases).map(String::toLowerCase).collect(Collectors.toList()));
        this.data = CommandData
            .simple(name, description, usage)
            .category(category)
            .aliases(requiresAlias, aliases)
            .minimumRank(minRank);
    }

    @Deprecated
    protected Command(Rank minRank, Category category, String description, String usage, String name, String... aliases) {
        this(minRank, category, description, usage, false, name, aliases);
    }

    @Deprecated
    protected Command(Rank minRank, String description, String usage, boolean requiresAlias,
                      String name, String... aliases) {
        this(minRank, Category.STAFF, description, usage, requiresAlias, name, aliases);
    }

    @Deprecated
    protected Command(Rank minRank, String description, String usage, String name, String... aliases) {
        this(minRank, description, usage, false, name, aliases);
    }

    // Returns false if the command was used incorrectly, true otherwise
    protected abstract boolean execute(CommandSender sender, String[] args) throws Exception;

    // The only point of this having a return value is to make bukkit happy
    @Override
    public final boolean execute(@NotNull CommandSender sender, @NotNull String alias, String[] args0) {
        try {
            // Add the alias to the args array if needed
            String[] args = this.data.requiresAlias() ? new String[args0.length + 1] : args0;
            if (this.data.requiresAlias()) {
                args[0] = alias.toLowerCase();
                System.arraycopy(args0, 0, args, 1, args0.length);
            }
            try {
                if (!execute(sender, args))
                    showUsage(sender);
            } catch (TextUtils.SyntaxException ex) {
                error(sender, ex.getMessage());
            }
        } catch (Throwable ex) {
            error(sender, "There was an error executing this command.");
            StringBuilder error = new StringBuilder();
            error.append(ex.getClass().getName()).append(": ").append(ex.getMessage()).append('\n');
            for (StackTraceElement ste : ex.getStackTrace())
                error.append("    ").append(ste.toString()).append('\n');
            String errorString = error.toString();
            if (showErrorsOnDiscord()) {
                Logging.error("Error executing command " + getName() + " from " + sender.getName() + ": `" +
                        alias.toLowerCase() + " " + String.join(" ", args0) + "`");
                FarLands.getDebugger().echo(errorString.length() > 1994 ? errorString.substring(0, 1991) + "..." : errorString);
            }
            ex.printStackTrace();
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
        return command.equalsIgnoreCase(getName()) || this.data.aliases().contains(command);
    }

    public boolean canUse(CommandSender sender) {
        return canUse(sender, true);
    }

    public boolean canUse(CommandSender sender, boolean alertSender) {
        if (this.data.canUse(sender)) {
            return true;
        }
        if (alertSender) {
            this.data.sendRequirements(sender);
        }
        return false;
    }

    public boolean showErrorsOnDiscord() {
        return true;
    }

    public boolean requiresVerifiedDiscordSenders() {
        return true;
    }

    public Rank getMinRankRequirement() {
        return this.data.minimumRank();
    }

    public Category getCategory() {
        return this.data.category();
    }

    /**
     * @return The SlashCommand to be registered with Discord -- null for no command registration
     */
    public @Nullable SlashCommandData discordCommand() {
        if (this.data.minimumRank().isStaff() || this.data.requiresAlias()) return null;
        return defaultCommand(true);
    }

    /**
     * @return A list of slash commands to be registered -- intended use is for aliases
     */
    public @NotNull List<SlashCommandData> discordCommands() {
        if (this.discordCommand() != null || this.data.minimumRank().isStaff() || !this.data.requiresAlias()) return Collections.emptyList();
        return defaultCommands(true);
    }

    /**
     * @param defaultArgs If the command should have one option - "args"
     * @return A single slash command representing this Command
     */
    protected @NotNull SlashCommandData defaultCommand(boolean defaultArgs) {
        SlashCommandData cmd = Commands.slash(this.getName().toLowerCase(), this.description.substring(0, Math.min(this.description.length(), 100)));
        if(!defaultArgs) return cmd;
        return cmd.addOption(OptionType.STRING, "args", "Args for the command", false, false);
    }



    /**
     * @return A list of slash commands to be registered -- optionally including args
     */
    protected @NotNull List<SlashCommandData> defaultCommands(boolean defaultArgs) {
        return Stream.concat(
                Stream.of(this.getName()),
                this.data
                    .aliases()
                    .stream()
            )
            .map(a -> Commands.slash(a, this.description.substring(0, Math.min(this.description.length(), 100))))
            .map(c -> defaultArgs ? c.addOption(OptionType.STRING, "args", "Args for the command", false, false) : c)
            .toList();
    }

    /**
     * @return Map<CommandName : (OptionName) -> String[]>
     */
    public @Nullable Map<String, DiscordCompleter> discordAutocompletion() {
        return null;
    }

    protected void showUsage(CommandSender sender) {
        sender.sendMessage("Usage: " + getUsage());
    }

    public static <T> T parseNumber(String input, Function<String, T> parser, T onFail) {
        try {
            return parser.apply(input);
        } catch (Throwable t) {
            return onFail;
        }
    }

    // Excludes vanished players
    public static Player getPlayer(String name, CommandSender sender) {
        Player player = Bukkit.getServer().getPlayer(name);
        if (player == null || (!Rank.getRank(sender).isStaff() && FarLands.getDataHandler().getOfflineFLPlayer(player).vanished))
            return null;
        return player;
    }

    // Excludes vanished players
    public static List<String> getOnlinePlayers(String partialName, CommandSender sender) {
        Stream<? extends Player> stream = Bukkit.getOnlinePlayers().stream();
        if (!Rank.getRank(sender).isStaff())
            stream = stream.filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).vanished);
        return stream.map(Player::getName).filter(name -> name.toLowerCase().startsWith(partialName.toLowerCase()))
                .collect(Collectors.toList());
    }

    public static @NotNull String joinArgsBeyond(int index, String delim, String[] args) {
        ++index;
        String[] data = new String[args.length - index];
        System.arraycopy(args, index, data, 0, data.length);
        return String.join(delim, data);
    }

    /**
     * Show an error message to the sender (red)
     * @param message Formatted with {@link String#format(String, Object...)}
     * @param replacements Replacements for {@link String#format(String, Object...)}
     * @return true - So that it can be used in a single line: <code>return {@link Command#error(CommandSender, String, Object...)};</code>
     */
    public static boolean error(CommandSender sender, String message, Object... replacements) {
        sender.sendMessage(ComponentColor.red(message, replacements));
        return true;
    }

    /**
     * Show a success message to the sender (green)
     * @param message Formatted with {@link String#format(String, Object...)}
     * @param replacements Replacements for {@link String#format(String, Object...)}
     * @return true - So that it can be used in a single line: <code>return {@link Command#success(CommandSender, String, Object...)};</code>
     */
    public static boolean success(CommandSender sender, String message, Object... replacements) {
        sender.sendMessage(ComponentColor.green(message, replacements));
        return true;
    }

    /**
     * Show an info message to the sender (gold)
     * @param message Formatted with {@link String#format(String, Object...)}
     * @param replacements Replacements for {@link String#format(String, Object...)}
     * @return true - So that it can be used in a single line: <code>return {@link Command#info(CommandSender, String, Object...)};</code>
     */
    public static boolean info(CommandSender sender, String message, Object... replacements) {
        sender.sendMessage(ComponentColor.gold(message, replacements));
        return true;
    }

}
