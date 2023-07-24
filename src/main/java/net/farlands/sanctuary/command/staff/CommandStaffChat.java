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
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
            return error(sender, "Unknown command. Type \"/help\" for help.");
        }

        if ("staffchat".equals(args[0])) {
            if (!(sender instanceof Player)) {
                return error(sender, "You must be online to manage staff chat settings.");
            }

            FLPlayerSession session = FarLands.getDataHandler().getSession((Player) sender);
            if (args.length == 1) {
                return error(sender, "Usage: /staffchat <toggle-message|toggle-view|set-color>...");
            }

            switch (args[1]) {
                // Toggle on/off auto-messaging
                case "toggle-message" -> {
                    session.autoSendStaffChat = toggledValue(sender, session.autoSendStaffChat, args, 2);
                    success(sender, "Staff chat auto-messaging toggled {::?on:off}", session.autoSendStaffChat);
                }
                case "toggle-view" -> {
                    session.showStaffChat = toggledValue(sender, session.showStaffChat, args, 2);
                    success(sender, "Staff chat toggled {::?on:off}", session.showStaffChat);
                }
                case "set-color" -> {
                    if (args.length == 2) {
                        return error(sender, "Usage: /staffchat set-color <color|#hex>");
                    }

                    TextColor color = FLUtils.parseColor(args[2]);
                    if (color == null) {
                        return error(sender, "Invalid chat color \"{}\", must be a valid color.", args[2]);
                    }

                    session.handle.staffChatColor = color;
                    success(sender, "Updated your staff chat color to {}.", ComponentColor.color(color, FLUtils.colorToString(color)));
                }
                default -> {
                    return error(sender, "Usage: /staffchat <toggle|set-color>...");
                }
            }
        } else {
            OfflineFLPlayer handle = FarLands.getDataHandler().getOfflineFLPlayer(sender);
            String username = handle == null ? "Console" : handle.username;
            String message = joinArgsBeyond(0, " ", args);
            if (message.isEmpty()) {
                return true;
            }

            Component component = ComponentColor.red("[SC] {}: {}", username, message);

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

        return TabCompleterBase.filterStartingWith(args[1], NamedTextColor.NAMES.values().stream().map(s -> s.toString().replaceAll("_", "-")));
    }

    private static boolean toggledValue(CommandSender sender, boolean currentValue, String[] args, int index) {
        boolean newValue = !currentValue;

        if (args.length == index + 1) {
            if ("on".equalsIgnoreCase(args[index])) {
                newValue = true;
            } else if ("off".equalsIgnoreCase(args[index])) {
                newValue = false;
            } else {
                error(sender, "Invalid toggle value: {}", args[index]);
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
