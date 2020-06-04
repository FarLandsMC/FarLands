package net.farlands.odyssey.mechanic;

import com.kicas.rp.util.TextUtils;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.Punishment;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.discord.DiscordChannel;
import net.farlands.odyssey.mechanic.anticheat.AntiCheat;
import net.farlands.odyssey.util.LocationWrapper;
import net.farlands.odyssey.util.Logging;
import net.farlands.odyssey.util.FLUtils;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.potion.PotionEffect;


import java.util.*;
import java.util.stream.Collectors;

public class Restrictions extends Mechanic {

    private static final List<int[]> PISTON_FILTER = Arrays.asList(
            new int[]{-2, -2, 0}, new int[]{2, -2, 0}, new int[]{0, -2, -2}, new int[]{0, -2, 2},
            new int[]{-1, -2, 0}, new int[]{1, -2, 0}, new int[]{0, -2, -1}, new int[]{0, -2, 1},
            new int[]{-2, -1, 0}, new int[]{2, -1, 0}, new int[]{0, -1, -2}, new int[]{0, -1, 2},
            new int[]{-1, -1, -1}, new int[]{1, -1, -1}, new int[]{-1, -1, 1}, new int[]{1, -1, 1},
            new int[]{-1, 0, 0}, new int[]{1, 0, 0}, new int[]{0, 0, -1}, new int[]{0, 0, 1}
    );

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
        if (!flp.rank.isStaff()) {
            player.setGameMode(GameMode.SURVIVAL);
            List<String> notes = flp.notes;
            if (!notes.isEmpty()) {
                Logging.broadcastStaff(TextUtils.format("&(red)%0 has notes. Hover $(command,{&(gray)%1}," +
                        "{&(aqua,underline)here}) to view them.", player.getName(), String.join("\n", notes)));
            }
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
            if (isNew) {
                flp.lastLocation = FarLands.getDataHandler().getPluginData().spawn;
                if (!banned.isEmpty()) {
                    Logging.broadcastStaff(TextUtils.format("Punishing %0 for ban evasion%1", flp.username,
                            unbanned.isEmpty() ? "." : ", along with the following alts: " + String.join(", ", unbanned)),
                            DiscordChannel.NOTEBOOK);
                    flp.punish(Punishment.PunishmentType.BAN_EVASION, null);
                    alts.stream().filter(p -> !p.isBanned()).forEach(a -> a.punish(Punishment.PunishmentType.BAN_EVASION, null));
                    return;
                }
            }
        }

        if (isNew) {
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
        if (flp != null && flp.isBanned())
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, flp.getCurrentPunishmentMessage());
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

    /*@EventHandler(ignoreCancelled = true)
    public void onShopCreation(PlayerCreateShopEvent event) {
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
    public void onShopDestroyed(PlayerDestroyShopEvent event) {
        FarLands.getDataHandler().getOfflineFLPlayer(event.getShop().getOwnerUUID()).removeShop();
    }*/

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

    @EventHandler(ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) { // Block 0 tick farms
        event.setCancelled(PISTON_FILTER.stream().map(off -> event.getBlock().getRelative(off[0], off[1], off[2]))
                .anyMatch(block -> block.getType() == Material.MOVING_PISTON));
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

    @EventHandler(ignoreCancelled = true)
    // Prevent players from igniting TNT or opening shulker boxes inside claims via right-clicking
    @SuppressWarnings("unchecked")
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (Action.RIGHT_CLICK_BLOCK == event.getAction()) {
            if (event.getClickedBlock().getRelative(event.getBlockFace()).getType() == Material.END_PORTAL &&
                    event.getMaterial().name().endsWith("_BUCKET")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
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
        if (to.getY() >= 128 && "world_nether".equals(to.getWorld().getName()) && !Rank.getRank(event.getPlayer()).isStaff()) {
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot go on top of the nether. This is in the rules.");
            event.setCancelled(true);
            return;
        }
        int max = (int)event.getTo().getWorld().getWorldBorder().getSize() >> 1; // size / 2 (rounded down)
        if (Math.abs(to.getX()) > max || Math.abs(to.getZ()) > max) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot leave the world.");
        }
    }
}
