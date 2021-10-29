package net.farlands.sanctuary.mechanic;

import com.vexsoftware.votifier.model.VotifierEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.VoteConfig;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.PluginData;
import net.farlands.sanctuary.data.struct.ItemReward;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;

/**
 * Handles plugin events related to voting.
 */
public class Voting extends Mechanic {
    private final PluginData pluginData;
    private final VoteConfig voteConfig;

    public Voting() {
        this.pluginData = FarLands.getDataHandler().getPluginData();
        this.voteConfig = FarLands.getFLConfig().voteConfig;
    }

    public int getVotesUntilParty() {
        return pluginData.votesUntilParty;
    }

    @EventHandler
    public void onVote(VotifierEvent event) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(event.getVote().getUsername());
        if (flp == null) // They need to have logged in before
            return;
        int currentMonth = FLUtils.getMonthInYear();
        if (currentMonth != pluginData.currentMonth) {
            pluginData.currentMonth = currentMonth;
            FarLands.getDataHandler().getOfflineFLPlayers().forEach(otherFlp -> otherFlp.monthVotes = 0);
        }
        flp.addVote();
        --pluginData.votesUntilParty;
        Logging.broadcastFormatted("&(gold){&(aqua)%0} just voted $(link,%1,{&(aqua,underline)here}) and received a reward!" +
                        (pluginData.votesUntilParty > 0 ? " {&(aqua)%2} more $(inflect,noun,2,vote) until a vote party!" : ""),
                false, flp.username, voteConfig.voteLink, pluginData.votesUntilParty);
        EmbedBuilder eb = new EmbedBuilder()
            .setTitle(flp.username + " just voted here and received a reward!", voteConfig.voteLink)
            .setColor(ChatColor.YELLOW.asBungee().getColor());

        if (pluginData.votesUntilParty > 0) {
            eb.setDescription(pluginData.votesUntilParty + " more vote" + (pluginData.votesUntilParty == 1 ? "" : "s") + " until a vote party!");
        }

        FarLands.getDiscordHandler().sendMessageEmbed(DiscordChannel.IN_GAME, eb);

        updateTopVoter();
        updateVoteParty();
    }

    private void updateTopVoter() {
        OfflineFLPlayer currentTop = FarLands.getDataHandler().getOfflineFLPlayers().stream()
                .filter(flp -> flp.topVoter)
                .findAny()
                .orElse(null);
        OfflineFLPlayer actualTop = FarLands.getDataHandler().getOfflineFLPlayers().stream()
                .filter(flp -> !flp.rank.isStaff())
                .max(Comparator.comparingInt(flp -> (flp.monthVotes << 26) + (flp.totalSeasonVotes << 13) + flp.totalVotes))
                .orElse(null);

        if (currentTop != null) {
            if (actualTop.uuid == currentTop.uuid)
                return;
            else {
                currentTop.topVoter = false;
                currentTop.updateSessionIfOnline(false);
            }
        }

        actualTop.topVoter = true;
        Player actualTopPlayer = actualTop.getOnlinePlayer();
        if (actualTopPlayer != null) {
            actualTop.updateSessionIfOnline(false);
            actualTopPlayer.sendMessage(ChatColor.GREEN + "You are now the top voter of the month!");
        }
    }

    private void updateVoteParty() {
        if (pluginData.votesUntilParty == 0)
            doVoteParty();
        if (pluginData.votesUntilParty <= 0)
            pluginData.votesUntilParty = voteConfig.votePartyRequirement;
    }

    public void doVoteParty() {
        Bukkit.getOnlinePlayers()
                .stream()
                .filter(player -> FarLands.getDataHandler().getOfflineFLPlayer(player).acceptVoteRewards)
                .forEach(player -> {
            ItemStack stack = ItemReward.randomReward(voteConfig.votePartyRewards, voteConfig.votePartyDistribWeight).getFirst();
            player.sendMessage(ChatColor.GOLD + "Vote party! Receiving " + ChatColor.AQUA + FLUtils.itemName(stack));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 0.6929134F);
            FLUtils.giveItem(player, stack, true);
        });
    }
}
