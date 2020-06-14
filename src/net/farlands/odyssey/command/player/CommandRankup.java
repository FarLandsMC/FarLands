package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;

import net.minecraft.server.v1_15_R1.AdvancementDisplay;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_15_R1.advancement.CraftAdvancement;
import org.bukkit.entity.Player;

public class CommandRankup extends Command {
    public CommandRankup() {
        super(Rank.INITIATE, "Check how long until you rank up.", "/rankup", "rankup");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Trigger the rank-up if possible
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        Rank nextRank = flp.rank.getNextRank();
        flp.updateSessionIfOnline(false);

        // Rank-up failed so notify the player of the remaining time
        if (!flp.rank.equals(nextRank)) {
            if (!nextRank.isPlaytimeObtainable()) {
                sendFormatted(sender, "&(gold)You can no longer rank up from playtime.");
                return true;
            }

            if (!nextRank.hasPlaytime(flp)) {
                sendFormatted(
                        sender,
                        "&(gold)You will rank up to {%0%1} in %2",
                        nextRank.getColor(),
                        nextRank.getName(),
                        TimeInterval.formatTime(((nextRank.getPlayTimeRequired() - flp.totalSeasonVotes) * 3600 - flp.secondsPlayed) * 1000L, false)
                );
            }

            if (sender instanceof Player && !nextRank.completedAdvancement((Player) sender)) {
                AdvancementDisplay advancementDisplay = ((CraftAdvancement) nextRank.getAdvancement()).getHandle().c();
                if (advancementDisplay != null) {
                    sendFormatted(sender, "&(gold)You must complete the advancement {&(aqua)%0} to rankup.",
                            advancementDisplay.a().getText());
                }
            }
        }

        return true;
    }
}
