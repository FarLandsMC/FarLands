package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;
import net.minecraft.server.v1_14_R1.AdvancementDisplay;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_14_R1.advancement.CraftAdvancement;
import org.bukkit.entity.Player;

public class CommandRankup extends Command {
    public CommandRankup() {
        super(Rank.INITIATE, "Check how long until you rank up.", "/rankup", "rankup");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        Rank nextRank = flp.rank.getNextRank();
        flp.updateSessionIfOnline(false);
        if (!flp.rank.equals(nextRank)) {
            if (!nextRank.isPlaytimeObtainable()) {
                sender.sendMessage(ChatColor.GOLD + "You can no longer rank up from playtime.");
                return true;
            }

            if (!nextRank.hasPlaytime(flp)) {
                sender.sendMessage(ChatColor.GOLD + "You will rank up to " + nextRank.getColor() +
                        nextRank.getSymbol() + ChatColor.GOLD + " in " + TimeInterval.formatTime(
                        (nextRank.getPlayTimeRequired() * 3600 - flp.secondsPlayed) * 1000L, false));
            }

            if (sender instanceof Player && !nextRank.completedAdvancement((Player) sender)) {
                AdvancementDisplay ad = ((CraftAdvancement) nextRank.getAdvancement()).getHandle().c();
                if (ad != null) {
                    sender.sendMessage(ChatColor.GOLD + "You must complete the advancement " + ChatColor.AQUA +
                            ad.a().getText() + ChatColor.GOLD + " to rankup.");
                }
            }
        }
        return true;
    }
}
