package net.farlands.sanctuary.command.staff;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.ChatHandler;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.Logging;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class CommandVanish extends Command {
    public CommandVanish() {
        super(Rank.MEDIA, "Toggle on and off vanish mode.", "/vanish <on|off>", "vanish");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        if (args.length == 0) {
            if (flp.vanished) {
                sender.sendMessage( // You are currently vanished.  Use /vanish off to disable.
                    ComponentColor.gold("You are currently vanished.  Use ")
                        .append(ComponentUtils.suggestCommand("/vanish off"))
                        .append(ComponentColor.gold(" to disable."))
                );
            } else {
                sender.sendMessage( // You are not currently vanished.  Use /vanish on to enable.
                    ComponentColor.gold("You are not currently vanished.  Use ")
                        .append(ComponentUtils.suggestCommand("/vanish on"))
                        .append(ComponentColor.gold(" to enable."))
                );
            }
            return true;
        }

        boolean prev = flp.vanished;
        flp.vanished = args[0].equalsIgnoreCase("on");

        if (flp.vanished != prev) { // state changes

            FLPlayerSession session = flp.getSession();
            if (session != null) { // Player is online
                session.update(false);
                session.updateVanish();

                Logging.broadcastStaff(
                    ComponentColor.yellow(
                        "%s is %s vanished",
                        sender.getName(),
                        (flp.vanished ? "now" : "no longer")
                    ),
                    DiscordChannel.STAFF_COMMANDS
                );

                ChatHandler.playerTransition(flp, !flp.vanished);
                if(flp.vanished) {
                    flp.lastLogin = System.currentTimeMillis();
                }
                FarLands.getDiscordHandler().updateStats();
            }

            sender.sendMessage(
                ComponentColor.gold("You are %s vanished.", flp.vanished ? "now" : "no longer")
            );
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], Stream.of("on", "off"))
                : Collections.emptyList();
    }
}
