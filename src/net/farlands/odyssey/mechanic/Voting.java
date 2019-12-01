package net.farlands.odyssey.mechanic;

import com.vexsoftware.votifier.model.VotifierEvent;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.PluginData;
import net.farlands.odyssey.data.struct.ItemReward;
import net.farlands.odyssey.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class Voting extends Mechanic {
    private int votesUntilParty;

    public Voting() {
        this.votesUntilParty = FarLands.getFLConfig().getVoteConfig().getVotePartyRequirement();
    }

    public int getVotesUntilParty() {
        return votesUntilParty;
    }

    @EventHandler
    public void onVote(VotifierEvent event) {
        OfflineFLPlayer flp = FarLands.getPDH().getFLPlayerMatching(event.getVote().getUsername());
        if(flp == null) // They need to have logged in before
            return;
        PluginData pd = FarLands.getDataHandler().getPluginData();
        int current = Utils.getMonthInYear();
        if(current != pd.getCurrentMonth()) {
            pd.setCurrentMonth(current);
            flp.setMonthVotes(0);
            FarLands.getPDH().getCached().forEach(flp0 -> flp0.setMonthVotes(0));
            FarLands.getPDH().update("UPDATE playerdata SET monthVotes=0");
        }
        flp.addVote();
        -- votesUntilParty;
        FarLands.broadcastFormatted("&(gold){&(aqua)%0} just voted $(link,%1,{&(aqua,underline)here}) and received a reward!" +
                (votesUntilParty > 0 ? " {&(aqua)%2} more $(inflect,noun,2,vote) until a vote party!" : ""), true, flp.getUsername(),
                FarLands.getFLConfig().getVoteConfig().getVoteLink(), votesUntilParty);
        FarLands.getPDH().saveFLPlayer(flp);

        updateMonthlyVotes();
        updateVoteParty();
    }

    private void updateMonthlyVotes() {
        try {
            ResultSet rs = FarLands.getPDH().query("SELECT uuid FROM playerdata ORDER BY (monthVotes*65536+totalVotes) DESC LIMIT 1");
            UUID top = rs.next() ? Utils.getUuid(rs.getBytes("uuid"), 0) : null;
            if(top == null)
                return;
            rs.close();
            rs = FarLands.getPDH().query("SELECT uuid FROM playerdata WHERE (flags&4)!=0");
            UUID voter = rs.next() ? Utils.getUuid(rs.getBytes("uuid"), 0) : null;
            rs.close();
            if(top.equals(voter))
                return;
            if(voter != null)
                FarLands.getPDH().removeFlag(voter, 2);
            FarLands.getPDH().setFlag(top, 2);

            OfflineFLPlayer voterFlp = FarLands.getPDH().getCached(voter), topFlp = FarLands.getPDH().getCached(top);
            if(voterFlp != null) {
                voterFlp.setTopVoter(false);
                voterFlp.tryUpdateOnline(false);
            }
            if(topFlp != null) {
                topFlp.setTopVoter(true);
                topFlp.tryUpdateOnline(false);
            }

            Player player = Bukkit.getPlayer(top);
            if(player != null)
                player.sendMessage(ChatColor.GREEN + "You are now the top voter of the month!");
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void updateVoteParty() {
        if(votesUntilParty == 0) {
            doVoteParty();
            votesUntilParty = FarLands.getFLConfig().getVoteConfig().getVotePartyRequirement();
        }
    }

    public void doVoteParty() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            ItemStack stack = ItemReward.randomReward(FarLands.getDataHandler().getVotePartyRewards(),
                    FarLands.getFLConfig().getVoteConfig().getVotePartyDistribWeight());
            player.sendMessage(ChatColor.GOLD + "Vote party! Receiving " + ChatColor.AQUA + Utils.itemName(stack));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 0.6929134F);
            Utils.giveItem(player, stack, true);
        });
    }
}
