package net.farlands.odyssey.data;

import net.farlands.odyssey.data.struct.ItemDistributor;
import net.farlands.odyssey.data.struct.Proposal;
import net.farlands.odyssey.util.LocationWrapper;
import net.farlands.odyssey.util.Utils;
import org.bukkit.Location;

import java.util.*;

/**
 * Contains any private data that needs to be serialized. This class is loaded and managed by the data handler.
 */
public class PluginData {
    private int currentMonth;
    private int lastPatch;
    private byte[] lastPatchnotesMD5;
    private LocationWrapper spawn;
    private LocationWrapper pvpIslandSpawn;
    private List<ItemDistributor> itemDistributors;
    private Set<UUID> spawnTraders;
    private Map<String, LocationWrapper> warps;
    private List<Proposal> proposals;

    public PluginData() {
        this.currentMonth = Utils.getMonthInYear();
        this.lastPatch = 0;
        this.lastPatchnotesMD5 = new byte[0];
        this.spawn = Utils.LOC_ZERO;
        this.pvpIslandSpawn = Utils.LOC_ZERO;
        this.itemDistributors = new ArrayList<>();
        this.spawnTraders = new HashSet<>();
        this.warps = new HashMap<>();
        this.proposals = new ArrayList<>();
    }

    public int getCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(int currentMonth) {
        this.currentMonth = currentMonth;
    }

    public int getLastPatch() {
        return lastPatch;
    }

    public void incrementPatch() {
        ++ lastPatch;
    }

    public byte[] getLastPatchnotesMD5() {
        return lastPatchnotesMD5;
    }

    public void setLastPatchnotesMD5(byte[] lastPatchnotesMD5) {
        this.lastPatchnotesMD5 = lastPatchnotesMD5;
    }

    public Location getSpawn() {
        return spawn.asLocation();
    }

    public Location getPvPIslandSpawn() {
        return pvpIslandSpawn.asLocation();
    }

    public List<ItemDistributor> getItemDistributors() {
        return itemDistributors;
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
}
