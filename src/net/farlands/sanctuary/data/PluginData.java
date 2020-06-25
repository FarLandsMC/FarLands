package net.farlands.sanctuary.data;

import net.farlands.sanctuary.data.struct.PlayerTrade;
import net.farlands.sanctuary.data.struct.Proposal;
import net.farlands.sanctuary.util.LocationWrapper;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Location;

import java.util.*;

/**
 * Contains any private data that needs to be serialized. This class is loaded and managed by the data handler.
 */
public class PluginData {
    public int currentMonth;
    public int lastPatch;
    public int votesUntilParty;
    public long seasonStartTime;
    public byte[] lastPatchnotesMD5;
    public LocationWrapper spawn;
    public LocationWrapper pvpIslandSpawn;
    public Set<UUID> spawnTraders;
    public Map<String, LocationWrapper> warps;
    public List<Proposal> proposals;
    public Map<UUID, PlayerTrade> playerTrades;

    public PluginData() {
        this.currentMonth = FLUtils.getMonthInYear();
        this.lastPatch = 0;
        this.votesUntilParty = 0;
        this.seasonStartTime = System.currentTimeMillis();
        this.lastPatchnotesMD5 = new byte[0];
        this.spawn = null;
        this.pvpIslandSpawn = null;
        this.spawnTraders = new HashSet<>();
        this.warps = new HashMap<>();
        this.proposals = new ArrayList<>();
        this.playerTrades = new HashMap<>();
    }

    public void setSpawn(Location spawn) {
        this.spawn = new LocationWrapper(spawn);
    }

    public void addSpawnTrader(UUID uuid) {
        spawnTraders.add(uuid);
    }

    public boolean isSpawnTrader(UUID uuid) {
        return spawnTraders.contains(uuid);
    }

    public void removeSpawnTrader(UUID uuid) {
        spawnTraders.remove(uuid);
    }

    public Location getWarp(String name) {
        LocationWrapper lw = warps.entrySet().stream().filter(e -> name.equalsIgnoreCase(e.getKey())).map(Map.Entry::getValue).findAny().orElse(null);
        return lw == null ? null : lw.asLocation();
    }

    public void addWarp(String name, Location location) {
        warps.put(name, new LocationWrapper(location));
    }

    public void removeWarp(String name) {
        warps.remove(name);
    }

    public Set<String> getWarpNames() {
        return warps.keySet();
    }

    public void addProposal(String issuer, String message) {
        proposals.add(new Proposal(issuer, message));
    }

    public void removeProposal(Proposal proposal) {
        proposals.remove(proposal);
    }

    public List<Proposal> getProposals() {
        return proposals;
    }

    public Proposal getProposal(long messageID) {
        return proposals.stream().filter(proposal -> proposal.getMessageID() == messageID).findAny().orElse(null);
    }

    public void setTrade(UUID player, String tradeMessage) {
        playerTrades.put(player, new PlayerTrade(player, tradeMessage));
    }

    public void clearTrade(UUID player) {
        playerTrades.remove(player);
    }
}
