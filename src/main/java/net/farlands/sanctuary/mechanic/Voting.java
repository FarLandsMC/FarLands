package net.farlands.sanctuary.mechanic;

import com.vexsoftware.votifier.model.VotifierEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Config;
import net.farlands.sanctuary.data.PluginData;
import net.farlands.sanctuary.data.struct.ItemReward;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

/**
 * Handles plugin events related to voting.
 */
public class Voting extends Mechanic {

    private final PluginData        pluginData;
    private final Config.VoteConfig voteConfig;

    public Voting() {
        this.pluginData = FarLands.getDataHandler().getPluginData();
        this.voteConfig = FarLands.getFLConfig().voteConfig;
    }

    @Override
    public void onStartup() {

        // Schedule a task to reset `votesToday` for every player every 24 hours starting at UTC midnight
        LocalDateTime from = LocalDateTime.now(ZoneId.of("UTC"));
        LocalDateTime to = LocalDate.now(ZoneId.of("UTC")).atTime(0, 0);

        long seconds = ChronoUnit.SECONDS.between(from, to);
        if (seconds < 0) to = to.plusDays(1); // If we are already past the time specified, go to the next day
        seconds = ChronoUnit.SECONDS.between(from, to);

        Bukkit.getScheduler().runTaskTimer( // Timer rather than delay for servers like the dev server, which don't restart daily
            FarLands.getInstance(),
            () -> {
                FarLands.getDataHandler().getOfflineFLPlayers().forEach(flp -> flp.votesToday = 0);
            },
            seconds * 20, // Ticks before running for the first time
            24 * 60 * 60 * 20 // 24 hours * 60 minutes * 60 seconds * 20 ticks (ticks in one day)
        );

    }

    public int getVotesUntilParty() {
        return this.pluginData.votesUntilParty;
    }

    @EventHandler
    public void onVote(VotifierEvent event) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(event.getVote().getUsername());
        if (flp == null) { // Player has not logged in before (current season or any other)
            return;
        }
        if (flp.debugging) { // Show the vote if the player who voted is in debug mode
            FarLands.getDebugger().echo(event.getVote().toString());
        }
        if (this.voteConfig.voteLinks.get(event.getVote().getServiceName()) == null) { // Vote service not in the configuration
            FarLands.getDebugger().echo("Vote service not recognised: " + event.getVote());
            return;
        }
        String url = this.voteConfig.voteLinks.get(event.getVote().getServiceName());
        String host;

        try {
            host = new URL(url).getHost();
        } catch (MalformedURLException e) {
            host = event.getVote().getServiceName();
        }

        int currentMonth = FLUtils.getMonthInYear();
        if (currentMonth != this.pluginData.currentMonth) {
            this.pluginData.currentMonth = currentMonth;
            FarLands.getDataHandler().getOfflineFLPlayers().forEach(otherFlp -> otherFlp.monthVotes = 0);
        }
        flp.addVote();
        --this.pluginData.votesUntilParty;

        TextComponent.Builder builder = ((TextComponent) ComponentColor.gold(
            "{} just voted at {} and received a reward!",
            flp,
            ComponentUtils.link(host, url, NamedTextColor.AQUA)
        )).toBuilder();

        if (this.pluginData.votesUntilParty > 0) {
            builder.append(ComponentColor.gold(
                " {:aqua} more {} until a vote party!",
                this.pluginData.votesUntilParty,
                this.pluginData.votesUntilParty == 1 ? "" : "s")
            );
        }
        Logging.broadcastIngame(builder.build(), false);

        EmbedBuilder eb = new EmbedBuilder()
            .setTitle(MarkdownProcessor.escapeMarkdown(flp.username + " just voted at " + host + " and received a reward!"), url)
            .setColor(NamedTextColor.YELLOW.value());

        if (this.pluginData.votesUntilParty > 0) {
            eb.setDescription(this.pluginData.votesUntilParty + " more vote" + (this.pluginData.votesUntilParty == 1 ? "" : "s") + " until a vote party!");
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
            .max(Comparator.<OfflineFLPlayer>
                    comparingInt(f -> f.monthVotes)
                     .thenComparingInt(f1 -> f1.totalVotes)
                     .thenComparingInt(f2 -> f2.totalSeasonVotes))
            .orElse(null);

        if (currentTop != null) {
            if (actualTop.uuid == currentTop.uuid) {
                return;
            } else {
                currentTop.topVoter = false;
                currentTop.updateSessionIfOnline(false);
            }
        }

        actualTop.topVoter = true;
        Player actualTopPlayer = actualTop.getOnlinePlayer();
        if (actualTopPlayer != null) {
            actualTop.updateSessionIfOnline(false);
            actualTopPlayer.sendMessage(ComponentColor.green("You are now the top voter of the month!"));
        }
    }

    /**
     * Update the votes needed and do one if <= # of votes
     */
    private void updateVoteParty() {
        if (this.pluginData.votesUntilParty == 0) {
            doVoteParty();
        }
        if (this.pluginData.votesUntilParty <= 0) {
            this.pluginData.votesUntilParty = this.voteConfig.votePartyRequirement;
        }
    }

    /**
     * Handle a vote party, granting everyone online a random reward
     */
    public void doVoteParty() {
        Bukkit.getOnlinePlayers()
            .stream()
            .filter(player -> FarLands.getDataHandler().getOfflineFLPlayer(player).acceptVoteRewards)
            .forEach(player -> {
                ItemStack stack = ItemReward.randomReward(this.voteConfig.votePartyRewards(), this.voteConfig.votePartyDistribWeight).getFirst();
                player.sendMessage(ComponentColor.gold("Vote Party! Receiving {}.", stack));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 0.6929134F);
                FLUtils.giveItem(player, stack, true);
            });
    }
}
