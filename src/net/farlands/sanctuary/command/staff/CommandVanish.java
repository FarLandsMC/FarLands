package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.Chat;

import org.bukkit.command.CommandSender;

public class CommandVanish extends Command {
    public CommandVanish() {
        super(Rank.MEDIA, "Toggle on and off vanish mode.", "/vanish", "vanish");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        boolean online = flp.isOnline();
        flp.vanished = !flp.vanished;
        flp.updateSessionIfOnline(false);
        if (flp.vanished) {
            sendFormatted(sender, "&(gold)You are now vanished.");
            if (online) {
                Chat.playerTransition(flp, false);
                flp.lastLogin = System.currentTimeMillis();
            }
        } else {
            sendFormatted(sender, "&(gold)You are no longer vanished.");
            if (online)
                Chat.playerTransition(flp, true);
        }
        if (online)
            FarLands.getDiscordHandler().updateStats();
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.COMMAND_LOG, flp.username + " toggled vanish " +
                (flp.vanished ? "on" : "off"));
        return true;
    }
}
