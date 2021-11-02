package net.farlands.sanctuary.command.player;

import io.papermc.paper.advancement.AdvancementDisplay;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.TimeInterval;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
                sender.sendMessage(ComponentColor.gold("You can no longer rank up from playtime."));
                return true;
            }

            if (!nextRank.hasPlaytime(flp)) {
                sender.sendMessage(
                    ComponentColor.gold("You will rank up to ")
                        .append(Component.text(nextRank.getName())
                                    .color(TextColor.color(nextRank.getColor().getColor().getRGB()))
                        )
                        .append(Component.text(" in " + TimeInterval.formatTime(((nextRank.getPlayTimeRequired() - flp.totalSeasonVotes) * 3600L - flp.secondsPlayed) * 1000L, false)))
                );
            }
            if (!nextRank.completedAdvancement(sender)) {
                AdvancementDisplay advDisplay = nextRank.getAdvancement().getDisplay();
                sender.sendMessage(
                    ComponentColor.gold("You must complete the advancement ")
                        .append(
                            ComponentColor.green("[")
                                .append(advDisplay.title())
                                .append(Component.text("]"))
                                .hoverEvent(
                                    HoverEvent.showText(
                                        advDisplay.title()
                                            .append(Component.text("\n"))
                                            .append(advDisplay.description())
                                            .color(NamedTextColor.GREEN)
                                    )
                                )
                        )
                        .append(Component.text(" to rankup."))
                );
            }
        }

        return true;
    }
}
