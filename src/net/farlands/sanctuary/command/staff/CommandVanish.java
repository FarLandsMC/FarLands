package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.Chat;

import net.farlands.sanctuary.util.Logging;
import org.bukkit.ChatColor;
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

        boolean changed = flp.vanished; // make sure we don't "leave" or "join" if our vanish doesn't change
        if (args.length > 0) {
            if ("on".equalsIgnoreCase(args[0]))
                flp.vanished = true;
            else if ("off".equalsIgnoreCase(args[0]))
                flp.vanished = false;

            FLPlayerSession session = flp.getSession();
            if (session != null) {
                session.update(false);
                session.updateVanish();

                Logging.broadcastStaff(
                        ChatColor.YELLOW + sender.getName() + " is " +
                                (flp.vanished ? "now" : "no longer") +
                                " vanished.",
                        DiscordChannel.STAFF_COMMANDS
                );
            }
        }

        boolean update = flp.isOnline() && changed != flp.vanished;
        if (flp.vanished) {
            sendFormatted(sender, "&(gold)You are now vanished.");
            if (update) {
                Chat.playerTransition(flp, false);
                flp.lastLogin = System.currentTimeMillis();
            }
        } else {
            sendFormatted(sender, "&(gold)You are no longer vanished.");
            if (update)
                Chat.playerTransition(flp, true);
        }
        if (update)
            FarLands.getDiscordHandler().updateStats();
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
