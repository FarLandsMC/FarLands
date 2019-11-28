package net.farlands.odyssey.mechanic;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.RandomAccessDataHandler;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.DataHandler;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.util.TextUtils;
import net.farlands.odyssey.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles the processing of events necessary to the operation of the FarLands plugin.
 */
public class FLSystem extends Mechanic {
    @Override
    public void onStartup() {
        FarLands.getScheduler().scheduleSyncRepeatingTask(this::update, 50L, 5L * 60L * 20L); // Every five minutes update stuff
        Rank.createTeams();

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            DataHandler dh = FarLands.getDataHandler();
            if(dh.arePatchnotesDifferent()) {
                try {
                    FarLands.getDiscordHandler().sendMessageRaw("announcements", "@everyone Patch **#" + dh.getCurrentPatch() +
                            "** has been released!\n```" + Chat.removeColorCodes(new String(dh.getResource("patchnotes.txt"), "UTF-8")) + "```");
                    FarLands.getPDH().update("UPDATE playerdata SET flags=flags&253");
                }catch(IOException ex) {
                    FarLands.error("Failed to post patchnotes to #announcements");
                    ex.printStackTrace(System.out);
                }
            }
        }, 100L);

        int gcCycleTime = FarLands.getFLConfig().getGcCycleTime();
        if(gcCycleTime > 0)
            Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), System::gc, 20L * 60L * 5L, 20L * 60L * gcCycleTime);
    }

    @Override
    public void onShutdown() {
        update();
    }

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        FarLands.getDataHandler().getRADH().store(System.currentTimeMillis(), "playtime", player.getUniqueId().toString());
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            Map<String, ItemStack> packages = FarLands.getDataHandler().getAndRemovePackages(player.getUniqueId());
            if(!packages.isEmpty()) {
                player.spigot().sendMessage(TextUtils.format("&(gold)Receiving {&(aqua)%0} $(inflect,noun,0,package) from {&(aqua)%1}",
                        packages.size(), packages.keySet().stream().map(sender -> "{" + sender + "}").collect(Collectors.joining(", "))));
                packages.values().forEach(item -> Utils.giveItem(player, item, true));
            }
            FarLands.getPDH().getFLPlayer(player).update();
        });
        FarLands.getDiscordHandler().updateStats();
    }

    @Override
    public void onPlayerQuit(Player player) {
        final DataHandler dh = FarLands.getDataHandler();
        FLPlayer flp = FarLands.getPDH().getFLPlayer(player);
        flp.updatePlaytime(player);
        dh.getRADH().delete("playtime", flp.getUuid().toString());
        dh.getRADH().delete("back", flp.getUuid().toString());
        dh.getRADH().delete("bakTPEventIgnore", flp.getUuid().toString());
        dh.getRADH().delete("staffchat", flp.getUuid().toString());
        flp.setLastLocation(player.getLocation());
        if(!flp.isVanished())
            flp.setLastLogin(System.currentTimeMillis());
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            FarLands.getPDH().uncachePlayer(player.getUniqueId());
            FarLands.getDiscordHandler().updateStats();
        }, 5L);
    }

    @EventHandler
    public void onVillagerAcquireTrades(VillagerAcquireTradeEvent event) {
        if(FarLands.getDataHandler().getPluginData().isSpawnTrader(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onVillagerChangeCareer(VillagerCareerChangeEvent event) {
        if(FarLands.getDataHandler().getPluginData().isSpawnTrader(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onVillagerReplenishTrades(VillagerReplenishTradeEvent event) {
        if(FarLands.getDataHandler().getPluginData().isSpawnTrader(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled=true)
    @SuppressWarnings("unchecked")
    public void onTeleport(PlayerTeleportEvent event) {
        RandomAccessDataHandler radh = FarLands.getDataHandler().getRADH();
        String uuid = event.getPlayer().getUniqueId().toString();
        if((boolean)radh.retrieveAndStoreIfAbsent(false, "backTPEventIgnore", uuid)) {
            radh.store(false, "backTPEventIgnore", uuid);
            return;
        }
        List<Location> backLocations = (List<Location>)radh.retrieveAndStoreIfAbsent(new ArrayList<Location>(5), "back", uuid);
        if(backLocations.size() >= 5)
            backLocations.remove(0);
        backLocations.add(event.getFrom());
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
    @SuppressWarnings("unchecked")
    public void onPlayerDeath(PlayerDeathEvent event) {
        List<Location> backLocations = (List<Location>)FarLands.getDataHandler().getRADH().retrieveAndStoreIfAbsent(
                new ArrayList<Location>(5), "back", event.getEntity().getUniqueId().toString());
        if(backLocations.size() >= 5)
            backLocations.remove(0);
        backLocations.add(event.getEntity().getLocation());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        FarLands.getGuiHandler().onInventoryClick(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        FarLands.getGuiHandler().onInventoryClose(event);
    }

    private void update() {
        final DataHandler dh = FarLands.getDataHandler();
        FarLands.getPDH().getCached().forEach(FLPlayer::update);
        dh.getPluginData().getProposals().removeIf(proposal -> {
            proposal.update();
            return proposal.isResolved();
        });
        FarLands.getPDH().saveCache(false);
    }
}
