package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.TimeInterval;
import org.bukkit.entity.Player;

public class CommandRankup extends PlayerCommand {
    public CommandRankup() {
        super(Rank.INITIATE, Category.PLAYER_SETTINGS_AND_INFO, "Check the requirements for your next rank up.", "/rankup", "rankup");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        // Trigger the rank-up if possible
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        Rank nextRank = flp.rank.getNextRank();
        flp.updateSessionIfOnline(false);

        // Rank-up failed so notify the player of the remaining time
        if (!flp.rank.equals(nextRank)) {
            if (!nextRank.isPlaytimeObtainable()) {
                return info(sender, "You can no longer rank up from playtime.");
            }

            if (!nextRank.hasPlaytime(flp)) {
                info(sender,
                     "You will rank up to {} in {}",
                     nextRank,
                     TimeInterval.formatTime(((nextRank.getPlayTimeRequired() - flp.totalSeasonVotes) * 3600L - flp.secondsPlayed) * 1000L, false)
                );
            }
            if (!nextRank.completedAdvancement(sender)) {
                info(sender, "You must complete the advancement {} to rankup.", nextRank.getAdvancement());
            }
        }

        return true;
    }
}
