package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.TextUtils;
import com.kicas.rp.util.Utils;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
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
                sendFormatted(sender, "&(red)You must be online to manage staff chat settings.");
                return true;
            }
            FLPlayerSession session = FarLands.getDataHandler().getSession((Player) sender);

            if (args.length == 1) {
                sendFormatted(sender, "&(red)Usage: /staffchat <toggle-message|toggle-view|set-color>...");
                return true;
            }

            switch (args[1]) {
                // Toggle on/off auto-messaging
                case "toggle-message": {
                    session.autoSendStaffChat = toggledValue(sender, session.autoSendStaffChat, args, 2);
                    sendFormatted(sender, "&(green)Staff chat auto-messaging toggled %0.",
                            session.autoSendStaffChat ? "on" : "off");
                    break;
                }

                case "toggle-view": {
                    session.showStaffChat = toggledValue(sender, session.showStaffChat, args, 2);
                    sendFormatted(sender, "&(green)Staff chat toggled %0.", session.showStaffChat ? "on" : "off");
                    break;
                }

                case "set-color": {
                    if (args.length == 2) {
                        sendFormatted(sender, "&(red)Usage: /staffchat set-color <color>");
                        return true;
                    }

                    ChatColor color = Utils.valueOfFormattedName(args[2], ChatColor.class);
                    if (color == null || color.isFormat() || color == ChatColor.RESET) {
                        sendFormatted(sender, "&(red)Invalid chat color \"%0\", must be a valid color and not a format.", args[2]);
                        return true;
                    }

                    session.handle.staffChatColor = color;
                    sendFormatted(sender, "&(green)Updated your staff chat color to %0%1", color, Utils.formattedName(color));
                    break;
                }

                default:
                    sendFormatted(sender, "&(red)Usage: /staffchat <toggle|set-color>...");
                    return true;
            }


        } else {
            OfflineFLPlayer handle = FarLands.getDataHandler().getOfflineFLPlayer(sender);
            String username = handle == null ? "Console" : handle.username;
            String message = joinArgsBeyond(0, " ", args);
            if (message.isEmpty())
                return true;
            TextUtils.sendFormatted(
                    Bukkit.getConsoleSender(),
                    "&(red)[SC] %0: %1",
                    username,
                    message
            );
            FarLands.getDataHandler().getSessions().stream().filter(session -> session.handle.rank.isStaff() && session.showStaffChat)
                    .forEach(session -> sendFormatted(session.player, "%0[SC] %1: %2", session.handle.staffChatColor, username, message));
            FarLands.getDiscordHandler().sendMessage(DiscordChannel.STAFF_COMMANDS, username + ": " + message);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if ("staffchat".equals(alias)) {
            if (args.length == 1) {
                return TabCompleterBase.filterStartingWith(args[0], Stream.of("toggle-message", "toggle-view", "set-color"));
            } else {
                if ("toggle-message".equals(args[0]) || "toggle-view".equals(args[0]))
                    return TabCompleterBase.filterStartingWith(args[1], Stream.of("on", "off"));
                else
                    return TabCompleterBase.filterStartingWith(args[1], Arrays.stream(ChatColor.values())
                            .filter(color -> color.isColor() && color != ChatColor.RESET).map(Utils::formattedName));
            }
        }

        return Collections.emptyList();
    }

    private static boolean toggledValue(CommandSender sender, boolean currentValue, String[] args, int index) {
        boolean newValue = !currentValue;

        if (args.length == index + 1) {
            if ("on".equalsIgnoreCase(args[index]))
                newValue = true;
            else if ("off".equalsIgnoreCase(args[index]))
                newValue = false;
            else
                sendFormatted(sender, "&(red)Ignoring invalid toggle value \"%0\"", args[index]);
        }

        return newValue;
    }
}
