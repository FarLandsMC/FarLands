package net.farlands.sanctuary.mechanic;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.util.Pair;
import com.kicas.rp.util.TextUtils;

import com.kicasmads.cs.event.ShopCreateEvent;
import com.kicasmads.cs.event.ShopRemoveEvent;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.Punishment;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.anticheat.AntiCheat;
import net.farlands.sanctuary.util.LocationWrapper;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.potion.PotionEffect;


import java.util.*;
import java.util.stream.Collectors;

public class Restrictions extends Mechanic {
    @Override
    public void onStartup() {
        // Fix nether claim heights
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            RegionProtection.getDataManager().getRegionsInWorld(Bukkit.getWorld("world_nether")).forEach(region -> {
                if (region.isAdminOwned())
                    return;

                Pair<Location, Location> bounds = region.getBounds();
                bounds.getSecond().setY(128);
                region.setBounds(bounds);
            });
        }, 100L);
    }

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
        if (!flp.rank.isStaff()) {
            player.setGameMode(GameMode.SURVIVAL);
            List<String> notes = flp.notes;
            if (!notes.isEmpty()) {
                Logging.broadcastStaff(TextUtils.format("&(red)%0 has notes. Hover $(hover,{&(gray){%1}}," +
                        "{&(aqua,underline)here}) to view them.", player.getName(), String.join("\n", notes)));
            }
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                List<OfflineFLPlayer> alts = FarLands.getDataHandler().getOfflineFLPlayers().stream()
                        .filter(otherFlp -> flp.lastIP.equals(otherFlp.lastIP) && !flp.uuid.equals(otherFlp.uuid))
                        .collect(Collectors.toList());
                List<String> banned = alts.stream().filter(OfflineFLPlayer::isBanned).map(flp0 -> flp0.username).collect(Collectors.toList()),
                        unbanned = alts.stream().filter(p -> !p.isBanned()).map(flp0 -> flp0.username).collect(Collectors.toList());
                if (!banned.isEmpty()) {
                    Logging.broadcastStaff(ChatColor.RED + flp.username + " shares the same IP as " + banned.size() + " banned player" +
                            (banned.size() > 1 ? "s" : "") + ": " + String.join(", ", banned), isNew ? DiscordChannel.ALERTS : null);
                }
                if (!unbanned.isEmpty()) {
                    Logging.broadcastStaff(ChatColor.RED + flp.username + " shares the same IP as " + unbanned.size() + " player" +
                            (unbanned.size() > 1 ? "s" : "") + ": " + String.join(", ", unbanned), isNew ? DiscordChannel.ALERTS : null);
                }
                if (isNew && flp.lastIP != null && !flp.lastIP.trim().isEmpty()) {
                    flp.lastLocation = FarLands.getDataHandler().getPluginData().spawn;
                    if (!banned.isEmpty()) {
                        Logging.broadcastStaff(TextUtils.format("Punishing %0 for ban evasion%1", flp.username,
                                unbanned.isEmpty() ? "." : ", along with the following alts: " + String.join(", ", unbanned)),
                                DiscordChannel.NOTEBOOK);
                        flp.punish(Punishment.PunishmentType.BAN_EVASION, null);
                        alts.stream().filter(p -> !p.isBanned()).forEach(a -> a.punish(Punishment.PunishmentType.BAN_EVASION, null));
                    }
                }
            }, 60L);
        }

        if (isNew ||
            (
                player.getWorld().getEnvironment() == World.Environment.NETHER &&
                    (Math.abs(player.getLocation().getX()) > 5000 ||
                    Math.abs(player.getLocation().getZ()) > 5000)
            )
        ) {
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                LocationWrapper spawn = FarLands.getDataHandler().getPluginData().spawn;
                if (spawn != null)
                    player.teleport(spawn.asLocation());
            }, 5L);
        } else if (flp.lastLocation != null) {
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> player.teleport(flp.getLastLocation()), 5L);
        }
    }

    @EventHandler
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) { // Handles bans
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getUniqueId());
        if (flp == null && !FarLands.getDataHandler().allowNewPlayers())
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "You cannot join the server right now. Try again in 5-10 minutes.");
        if (flp != null) {
            flp.lastIP = event.getAddress().getHostAddress();
            if (flp.isBanned())
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, flp.getCurrentPunishmentMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!event.isBedSpawn()) {
            LocationWrapper spawn = FarLands.getDataHandler().getPluginData().spawn;
            if (spawn == null) {
                event.getPlayer().sendMessage(ChatColor.RED + "Server spawn not set! Please contact an owner, " +
                        "administrator, or developer and notify them of this problem.");
                return;
            }
            event.setRespawnLocation(spawn.asLocation());
        }
    }

    @Override // Removes all infinite potion effects.
    public void onPlayerQuit(Player player) {
        player.getActivePotionEffects().stream().filter(pe -> pe.getDuration() >= 100 * 60 * 20) // 100m for bad omen
                .map(PotionEffect::getType).forEach(player::removePotionEffect);
    }

    @EventHandler(ignoreCancelled = true)
    public void onShopCreation(ShopCreateEvent event) {
        Player player = event.getPlayer();
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
        if (!flp.canAddShop()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You have reached the maximum number of shops you can build. " +
                    "Rank up to gain more shops.");
            return;
        }
        flp.addShop();
    }

    @EventHandler(ignoreCancelled = true)
    public void onShopDestroyed(ShopRemoveEvent event) {
        FarLands.getDataHandler().getOfflineFLPlayer(event.getShop().getOwner()).removeShop();
    }

    @EventHandler(ignoreCancelled = true) // Prevent players from teleporting using spectator mode
    public void onTeleport(PlayerTeleportEvent event) {
        event.setCancelled(event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE &&
                !Rank.getRank(event.getPlayer()).isStaff());
    }

    @EventHandler(ignoreCancelled = true) // Prevent portals from forming in spawn
    public void onPortalCreation(PortalCreateEvent event) {
        if (PortalCreateEvent.CreateReason.NETHER_PAIR == event.getReason() && event.getBlocks().stream()
                .map(block -> block.getBlock().getLocation()).anyMatch(FLUtils::isInSpawn)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        StringBuilder sb = new StringBuilder();
        for (String line : event.getLines()) {
            if (!line.isEmpty())
                sb.append(line).append("\n");
        }
        String text = sb.toString().trim();
        if (!text.isEmpty()) {
            Location location = event.getBlock().getLocation();
            Logging.broadcastStaff(ChatColor.GRAY + event.getPlayer().getName() + " placed a sign at " +
                    location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + ":\n" +
                    text);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // TODO: Add honey to check or warn players flying machines are unsafe
        if ("world_the_end".equals(event.getBlock().getWorld().getName()) &&
                event.getBlockPlaced().getType() == Material.SLIME_BLOCK) {
            event.getPlayer().sendMessage(ChatColor.RED + "You can't place slime blocks in the end.");
            event.setCancelled(true);
        } else if (event.getBlockAgainst().getType() == Material.SLIME_BLOCK &&
                (event.getBlockPlaced().getType().name().endsWith("FAN") ||
                        event.getBlockPlaced().getType().name().endsWith("CORAL"))) {
            final Location location = event.getBlock().getLocation();
            AntiCheat.broadcast(event.getPlayer().getName(), "may be attempting to build a duper @ " +
                    location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
            TextUtils.sendFormatted(event.getPlayer(), "&(red)It appears you are building a duping device, " +
                    "this is against the $(hovercmd,/rules,{&(white)view the rules},&(dark_red)/rules) (#1) as duping is a form of exploit. " +
                    "If you ignore this warning and do not remove the machine immediately you will be subject to a punishment.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        onPlayerMove(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        /*if (to.getY() >= 128 && "world_nether".equals(to.getWorld().getName()) && !Rank.getRank(event.getPlayer()).isStaff()) {
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot go on top of the nether. This is in the rules.");
            event.setCancelled(true);
            return;
        }*/
        if ((Math.abs(to.getX()) > 5000 || Math.abs(to.getZ()) > 5000) && to.getWorld().getEnvironment() == World.Environment.NETHER
                && !Rank.getRank(event.getPlayer()).isStaff()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "The nether border has temporarily been restricted due to world gen changes in 1.16.2");
            return;
        }

        int max = (int)to.getWorld().getWorldBorder().getSize() >> 1; // size / 2 (rounded down)
        if (Math.abs(to.getX()) > max || Math.abs(to.getZ()) > max) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot leave the world.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null)
            return;
        Material result = event.getInventory().getContents()[0].getType();

        if (result == Material.SHIELD) {
            event.getInventory().getContents()[0].setAmount(1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        if (event.isSigning() && "Statues".equals(event.getNewBookMeta().getTitle())) {
            event.getPlayer().sendMessage(ChatColor.RED + "You are not allowed to manually create the armor stand " +
                    "editor book, use /editarmorstand instead.");
            event.setCancelled(true);
        }
    }
}
