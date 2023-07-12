package net.farlands.sanctuary.command.staff;

import com.kicas.rp.command.TabCompleterBase;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class CommandStaffChat extends Command {

    public CommandStaffChat() {
        super(Rank.INITIATE, "Send a message to only online staff.", "/c <message>", true, "c", "ac", "staffchat");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!Rank.getRank(sender).isStaff()) { // Try to make it look like an invalid command
            sender.sendMessage("Unknown command. Type \"/help\" for help.");
            return true;
        }

        if ("staffchat".equals(args[0])) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ComponentColor.red("You must be online to manage staff chat settings."));
                return true;
            }

            FLPlayerSession session = FarLands.getDataHandler().getSession((Player) sender);
            if (args.length == 1) {
                sender.sendMessage(ComponentColor.red("Usage: /staffchat <toggle-message|toggle-view|set-color>..."));
                return true;
            }

            switch (args[1]) {
                // Toggle on/off auto-messaging
                case "toggle-message": {
                    session.autoSendStaffChat = toggledValue(sender, session.autoSendStaffChat, args, 2);
                    sender.sendMessage(
                        ComponentColor.green(
                            "Staff chat auto-messaging toggled {}.",
                            session.autoSendStaffChat ? "on" : "off"
                        )
                    );
                    break;
                }

                case "toggle-view": {
                    session.showStaffChat = toggledValue(sender, session.showStaffChat, args, 2);
                    sender.sendMessage(
                        ComponentColor.green(
                            "Staff chat toggled {}.",
                            session.showStaffChat ? "on" : "off"
                        )
                    );
                    break;
                }

                case "set-color": {
                    if (args.length == 2) {
                        sender.sendMessage(ComponentColor.red("Usage: /staffchat set-color <color>"));
                        return true;
                    }

                    String name = args[2].replaceAll("-", "_").toLowerCase();
                    NamedTextColor color = NamedTextColor.NAMES.value(name);
                    if (color == null) {
                        sender.sendMessage(ComponentColor.red("Invalid chat color \"{}\", must be a valid color.", args[2]));
                        return true;
                    }

                    session.handle.staffChatColor = color;
                    sender.sendMessage(
                        ComponentColor.green("Update your staff chat color to ")
                            .append(Component.text(color.toString().replaceAll("_", "-")).color(color))
                    );
                    break;
                }

                default:
                    sender.sendMessage(ComponentColor.red("Usage: /staffchat <toggle|set-color>..."));
                    return true;
            }
        } else {
            OfflineFLPlayer handle = FarLands.getDataHandler().getOfflineFLPlayer(sender);
            String username = handle == null ? "Console" : handle.username;
            String message = joinArgsBeyond(0, " ", args);
            if (message.isEmpty()) {
                return true;
            }

            Component component = ComponentColor.red("[SC] ")
                .append(Component.text(username))
                .append(Component.text(": "))
                .append(Component.text(message));

            Bukkit.getConsoleSender().sendMessage(component);
            FarLands.getDataHandler()
                .getSessions()
                .stream()
                .filter(session -> session.handle.rank.isStaff() && session.showStaffChat)
                .forEach(session -> session.player.sendMessage(component.color(session.handle.staffChatColor)));
            FarLands.getDiscordHandler().sendMessage(DiscordChannel.STAFF_COMMANDS, component);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff()) {
            return Collections.emptyList();
        }

        if (!"staffchat".equals(alias)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return TabCompleterBase.filterStartingWith(args[0], Stream.of("toggle-message", "toggle-view", "set-color"));
        }

        if ("toggle-message".equals(args[0]) || "toggle-view".equals(args[0])) {
            return TabCompleterBase.filterStartingWith(args[1], Stream.of("on", "off"));
        }

        return TabCompleterBase.filterStartingWith(args[1], NamedTextColor.NAMES.values().stream()
            .map(s -> s.toString().replaceAll("_", "-")));
    }

    private static boolean toggledValue(CommandSender sender, boolean currentValue, String[] args, int index) {
        boolean newValue = !currentValue;

        if (args.length == index + 1) {
            if ("on".equalsIgnoreCase(args[index])) {
                newValue = true;
            } else if ("off".equalsIgnoreCase(args[index])) {
                newValue = false;
            } else {
                sender.sendMessage(ComponentColor.red("Ignoring invalid toggle value \"{}\"", args[index]));
            }
        }

        return newValue;
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        return null;
    }

    @Override
    public @NotNull List<SlashCommandData> discordCommands() {
        return Collections.emptyList();
    }
}
