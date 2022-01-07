package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.Pagination;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CommandHomes extends Command {
    public CommandHomes() {
        super(Rank.INITIATE, Category.HOMES, "List your homes.", "/homes [page]", "homes");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if ((sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) && args.length == 0) {
            sender.sendMessage(ComponentColor.red("You must be in-game to use this command."));
            return true;
        }

        // Someone else's home (staff)
        if (Rank.getRank(sender).isStaff() && args.length > 0 && !NumberUtils.isNumber(args[0]) && args[0].length() > 2) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
            if (flp == null) {
                sender.sendMessage(ComponentColor.red("Player not found."));
                return true;
            }

            if (flp.homes.isEmpty()) {
                ComponentColor.gold("This player does not have any homes.");
                return true;
            }

            // Build a list of homes to paginate
            List<Component> lines = new LinkedList<>();
            flp.homes.forEach(home -> {
                Location location = home.getLocation();
                lines.add(
                    Component.empty()
                        .hoverEvent(HoverEvent.showText(ComponentColor.gray("Go to home %s", home.getName())))
                        .clickEvent(ClickEvent.runCommand("/home " + home.getName() + " " + flp.username))
                        .append(ComponentColor.gold("%s: ", home.getName()))
                        .append(
                            ComponentColor.aqua("%d %d %d", location.getBlockX(),
                            location.getBlockZ(), location.getBlockY())
                        )
                );
            });

            Pagination pagination = new Pagination(
                ComponentColor.aqua(flp.username + "'s homes"),
                "/homes " + flp.username
            ).addLines(lines);

            try {
                pagination.sendPage(args.length == 1 ? 1 : Integer.parseInt(args[1]), sender);
            } catch (Pagination.InvalidPageException | NumberFormatException exception) {
                sender.sendMessage(
                    ComponentColor.red("Invalid page. Valid pages are 1-%d.", pagination.numPages())
                );
                return true;
            }
        }
        // The sender's homes
        else {
            List<Home> homes = FarLands.getDataHandler().getOfflineFLPlayer(sender).homes;
            if (homes.isEmpty()) {
                sender.sendMessage(
                    ComponentColor.gold("You don't have any homes! Set one with ")
                        .append(ComponentColor.aqua("/sethome")
                                .clickEvent(ClickEvent.suggestCommand("/sethome")))
                );
                return true;
            }

            // Build a list of homes to paginate
            List<Component> lines = new LinkedList<>();
            homes.forEach(home -> {
                Location location = home.getLocation();
                lines.add(
                    Component.empty()
                        .hoverEvent(HoverEvent.showText(ComponentColor.gray("Go to home %s", home.getName())))
                        .clickEvent(ClickEvent.runCommand("/home " + home.getName()))
                        .append(ComponentColor.gold("%s: ", home.getName()))
                        .append(
                            ComponentColor.aqua("%d %d %d", location.getBlockX(),
                                location.getBlockZ(), location.getBlockY())
                        )
                );
            });

            Pagination pagination = new Pagination(ComponentColor.aqua("Your homes"), "/homes");
            pagination.addLines(lines);

            try {
                pagination.sendPage(args.length == 0 ? 1 : Integer.parseInt(args[0]), sender);
            } catch (Pagination.InvalidPageException | NumberFormatException exception) {
                sender.sendMessage(
                    ComponentColor.red("Invalid page. Valid pages are 1-%d.", pagination.numPages())
                );
                return true;
            }
        }
        return true;
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage("Usage: " + (Rank.getRank(sender).isStaff() ? "/homes [player] [page]" : getUsage()));
    }

    @Override
    public @NotNull List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 && Rank.getRank(sender).isStaff()
                ? getOnlinePlayers(args[0], sender)
                : Collections.emptyList();
    }
}