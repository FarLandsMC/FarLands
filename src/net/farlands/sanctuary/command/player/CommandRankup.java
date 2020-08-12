package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.TimeInterval;

import net.minecraft.server.v1_16_R1.*;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_16_R1.advancement.CraftAdvancement;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

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

            if (!nextRank.completedAdvancement(sender)) {
                ((CraftPlayer) sender).getHandle().sendMessage(
                        new ChatComponentText("You must complete the advancement ").setChatModifier(FLUtils.chatModifier("gold"))
                                .addSibling(((CraftAdvancement) nextRank.getAdvancement()).getHandle().j())
                                .addSibling(new ChatComponentText(" to rankup.").setChatModifier(FLUtils.chatModifier("gold"))),
                        new UUID(0L, 0L)
                );
            }
        }

        return true;
    }
}
