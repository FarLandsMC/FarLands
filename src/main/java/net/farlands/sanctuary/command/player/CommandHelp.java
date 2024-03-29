package net.farlands.sanctuary.command.player;

import com.google.common.collect.ImmutableMap;
import com.kicas.rp.RegionProtection;
import com.kicas.rp.command.TabCompleterBase;
import com.kicasmads.cs.Utils;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.Pagination;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.DiscordCompleter;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CommandHelp extends net.farlands.sanctuary.command.Command {

    public CommandHelp() {
        super(
            CommandData.simple("help", "View information on commands and command categories", "/help [category|command]")
                .category(Category.INFORMATIONAL)
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Show list of categories
        if (args.length == 0) {
            Component comp = ComponentColor.gold("Command Categories (Click to view commands):\n")
                .append(Component.join(
                    JoinConfiguration.separator(Component.newline()),
                    Category.player()
                ));
            sender.sendMessage(comp);
            return true;
        }

        Category category = Utils.valueOfFormattedName(args[0], Category.class);

        // Show info for a particular command
        if (category == null || category == Category.STAFF) {
            net.farlands.sanctuary.command.Command command = FarLands.getCommandHandler().getCommand(args[0]);
            if (command == null) {
                return error(sender, "Command or category not found: \"{}\".", args[0]);
            }

            Component comp = ComponentColor.gold(
                "Showing info for command {}:\nUsage: {:white}\nDescription: {:white}\n{}",
                command.getName(),
                usage(command.getUsage()),
                command.getDescription(),
                command.data().getRequirements()
            );
            sender.sendMessage(comp);
            return true;

        } else {
            List<Command> commands;
            if (category == Category.CLAIMS) {
                commands = Bukkit.getServer()
                    .getCommandMap()
                    .getKnownCommands()
                    .values()
                    .stream()
                    .filter(command -> command instanceof PluginCommand pcmd && pcmd.getPlugin() == RegionProtection.getInstance())
                    .distinct() // Originally collected to a set and then back to a stream, my guess is to dedupe, but this is better
                    .sorted(Comparator.comparing(Command::getUsage))
                    .toList();
            } else {
                commands = FarLands.getCommandHandler().getCommands().stream()
                    .filter(command -> command.getCategory().equals(category))
                    .sorted(Comparator.comparing(Command::getUsage))
                    .map(flcmd -> (Command) flcmd)
                    .toList();
            }

            Pagination pagination = new Pagination(
                ComponentColor.gold(category.getAlias()),
                "/help " + category.name().toLowerCase()
            );

            pagination.addLines(
                commands.stream().map(command -> {
                    Component hover = ComponentColor.gray(command.getDescription());
                    if (command instanceof net.farlands.sanctuary.command.Command flcmd) {
                        hover = hover.append(Component.newline().append(flcmd.data().getRequirements()));
                    }
                    return ComponentUtils.hover(usage(command.getUsage()), hover);
                }).toList()
            );

            try {
                pagination.sendPage(args.length > 1 ? parseNumber(args[1], Integer::parseInt, -1) : 1, sender);
            } catch (Pagination.InvalidPageException ex) {
                return error(sender, "Invalid page number, must be an integer between 1 and {}", pagination.numPages());
            }
        }

        return true;
    }

    private Component usage(String usageStr) {
        Component usage = Component.text(usageStr);

        usage = usage.replaceText(
            TextReplacementConfig
                .builder()
                .match("([<\\[])(.+?)([>\\]])") // Format args
                .replacement((mr, u) -> ComponentColor.gold(mr.group(1))
                    .append(Component.join(
                        JoinConfiguration.separator(ComponentColor.gold("|")),
                        Arrays.stream(mr.group(2).split("\\|")).map(ComponentColor::white).toList()
                    ))
                    .append(ComponentColor.gold(mr.group(3)))
                )
                .build()
        );

        usage = usage.replaceText(
            TextReplacementConfig
                .builder()
                .match("^/(.+?) ") // Format command itself (/...)
                .match("^/(.+?)$")
                .replacement((mr, u) -> ComponentColor.gold("/")
                    .append(Component.join(
                        JoinConfiguration.separator(ComponentColor.gray("|")),
                        Arrays.stream(mr.group(1).split("\\|")).map(ComponentColor::gold).toList()
                    ))
                )
                .build()
        );
        return usage;
    }

    @Override
    public @NotNull List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) {
        return args.length <= 1
            ? TabCompleterBase.filterStartingWith(args[0], Category.player().stream().map(Category::name).map(String::toLowerCase))
            : Collections.emptyList();
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        return this.defaultCommand(false)
            .addOption(
                OptionType.STRING,
                "category-or-command",
                "Category or Command",
                false,
                true
            )
            .addOption(
                OptionType.INTEGER,
                "page",
                "Page",
                false,
                false
            );
    }

    @Override
    public @Nullable Map<String, DiscordCompleter> discordAutocompletion() {
        return ImmutableMap.of(
            "help", (option, partial) -> TabCompleterBase
                .filterStartingWith(
                    partial,
                    Category.player().stream().map(Category::name).map(String::toLowerCase)
                ).toArray(String[]::new)
        );
    }
}
