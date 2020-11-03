package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.util.TextUtils;
import com.kicasmads.cs.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ReflectionHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandHelp extends net.farlands.sanctuary.command.Command {
    private static final int COMMANDS_PER_PAGE = 8;

    public CommandHelp() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "View information on available commands.",
                "/help [category|command] [page]", "help");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(CommandSender sender, String[] args) {
        // Show list of categories
        if (args.length == 0) {
            TextUtils.sendFormatted(
                    sender,
                    "&(gold)Commands are organized by category:\n" + Stream.of(Category.VALUES)
                            .filter(category -> category != Category.STAFF)
                            .map(category -> "$(hovercmd,/help " + Utils.formattedName(category) + "," +
                                    "{&(gray)Click to view this category}," + category.getAlias() + "): " +
                                    "{&(white)" + category.getDescription() + "}")
                            .collect(Collectors.joining("\n"))
            );
            return true;
        }

        Category category = Utils.valueOfFormattedName(args[0], Category.class);

        // Show info for a particular command
        if (category == null || category == Category.STAFF) {
            Command command = FarLands.getCommandHandler().getCommand(args[0]);
            if (command == null) {
                sender.sendMessage(ChatColor.RED + "Command or category not found: " + args[0]);
                return true;
            }

            TextUtils.sendFormatted(
                    sender,
                    "&(gold)Showing info for command {&(aqua)%0}:\nUsage: %1\nDescription: {&(white)%2}",
                    command.getName(),
                    formatUsage(command.getUsage()),
                    command.getDescription()
            );
        } else {
            List<Command> commands;
            if (category == Category.CLAIMS) {
                Map<String, Command> knownCommands = (Map<String, org.bukkit.command.Command>) ReflectionHelper.getFieldValue(
                        "knownCommands",
                        SimpleCommandMap.class,
                        ((CraftServer) Bukkit.getServer()).getCommandMap()
                );
                Set<Command> commandSet = knownCommands.values().stream()
                        .filter(command -> command instanceof PluginCommand &&
                                ((PluginCommand)command).getPlugin() == RegionProtection.getInstance())
                        .collect(Collectors.toSet());
                commands = commandSet.stream()
                        .sorted(Comparator.comparing(Command::getUsage))
                        .collect(Collectors.toList());
            } else {
                commands = FarLands.getCommandHandler().getCommands().stream()
                        .filter(command -> command.getCategory().equals(category))
                        .sorted(Comparator.comparing(Command::getUsage))
                        .collect(Collectors.toList());
            }

            int page = 0;
            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "Invalid page number: " + args[1]);
                    return true;
                }
            }

            if (page < 0) {
                sender.sendMessage(ChatColor.RED + "Invalid page number: " + args[1]);
                return true;
            }

            int maxPageIndex = (commands.size() - 1) / COMMANDS_PER_PAGE;
            if (page > maxPageIndex) {
                TextUtils.sendFormatted(sender, "&(red)This category only has %0 $(inflect,noun,0,page).", maxPageIndex + 1);
                return true;
            }

            TextUtils.sendFormatted(
                    sender,
                    "&(gold)[%0] %1 - Page %2/%3 [%4]\n%5",
                    page == 0 ? "{&(gray)Prev}" : "$(command,/help " + args[0] + " " + (page - 1) + ",{&(aqua)Prev})",
                    category.getAlias(),
                    page + 1,
                    maxPageIndex + 1,
                    page == maxPageIndex ? "{&(gray)Next}" : "$(command,/help " + args[0] + " " + (page + 1) + ",{&(aqua)Next})",
                    commands.stream().skip(page * COMMANDS_PER_PAGE).limit(COMMANDS_PER_PAGE)
                            .map(command -> "$(hover,{&(gray)" + command.getDescription() + "},{" + formatUsage(command.getUsage()) + "})")
                            .collect(Collectors.joining("\n"))
            );
        }

        return true;
    }

    private static String formatUsage(String usage) {
        return Arrays.stream(usage.split(" ")).map(arg -> {
            if (arg.startsWith("<") && arg.endsWith(">") || arg.startsWith("[") && arg.endsWith("]")) {
                String inner = arg.substring(1, arg.length() - 1).replaceAll("\\|", "{&(gold)|}").replaceAll("=", "{&(gold)=}");
                return arg.substring(0, 1) + "{&(white)" + inner + "}" + arg.substring(arg.length() - 1);
            } else
                return arg.replaceAll("\\|", "{&(gray)|}");
        }).collect(Collectors.joining(" "));
    }
}
