package net.farlands.odyssey.mechanic;

import com.snowgears.shop.event.PlayerCreateShopEvent;
import com.snowgears.shop.event.PlayerDestroyShopEvent;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.DataHandler;
import net.farlands.odyssey.data.struct.ItemDistributor;
import net.farlands.odyssey.data.struct.Punishment;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.mechanic.anticheat.AntiCheat;
import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.TextUtils;
import net.farlands.odyssey.util.Utils;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.potion.PotionEffect;


import java.util.*;
import java.util.stream.Collectors;

public class Restrictions extends Mechanic {
    
    private static final List<int[]> PISTON_FILTER = Arrays.asList(
            new int[] {-2, -2, 0}, new int[] {2, -2, 0}, new int[] {0, -2, -2}, new int[] {0, -2, 2},
            new int[] {-1, -2, 0}, new int[] {1, -2, 0}, new int[] {0, -2, -1}, new int[] {0, -2, 1},
            new int[] {-2, -1, 0}, new int[] {2, -1, 0}, new int[] {0, -1, -2}, new int[] {0, -1, 2},
            new int[] {-1, -1, -1}, new int[] {1, -1, -1}, new int[] {-1, -1, 1}, new int[] {1, -1, 1},
            new int[] {-1, 0, 0}, new int[] {1, 0, 0}, new int[] {0, 0, -1}, new int[] {0, 0, 1}
    );
    
    private static final List<Material> DOORS = Arrays.asList(Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR,
            Material.BIRCH_TRAPDOOR, Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
            Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE, Material.JUNGLE_FENCE_GATE,
            Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE);

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        final DataHandler dh = FarLands.getDataHandler();
        OfflineFLPlayer flp = FarLands.getPDH().getFLPlayer(player);
        if(!flp.getRank().isStaff()) {
            player.setGameMode(GameMode.SURVIVAL);
            List<String> notes = FarLands.getPDH().getNotes(player.getUniqueId());
            if(!notes.isEmpty()) {
                FarLands.broadcastStaff(TextUtils.format("&(red)%0 has notes. Click $(command,/notes view %0," +
                        "{&(aqua,underline)here}) to view them.", player.getName()));
            }
            List<OfflineFLPlayer> alts = FarLands.getPDH().getAlts(flp.getUuid());
            List<String> banned = alts.stream().filter(OfflineFLPlayer::isBanned).map(OfflineFLPlayer::getUsername).collect(Collectors.toList()),
                    normal = alts.stream().filter(p -> !p.isBanned()).map(OfflineFLPlayer::getUsername).collect(Collectors.toList());
            if(!banned.isEmpty()) {
                FarLands.broadcastStaff(ChatColor.RED + flp.getUsername() + " shares the same IP as " + banned.size() + " banned player" +
                        (banned.size() > 1 ? "s" : "") + ": " + String.join(", ", banned), isNew ? "alerts" : null);
            }
            if(!normal.isEmpty()) {
                FarLands.broadcastStaff(ChatColor.RED + flp.getUsername() + " shares the same IP as " + normal.size() + " player" +
                        (normal.size() > 1 ? "s" : "") + ": " + String.join(", ", normal), isNew ? "alerts" : null);
            }
            if(isNew) {
                flp.setLastLocation(dh.getPluginData().getSpawn());
                if(!banned.isEmpty()) {
                    flp.punish(Punishment.PunishmentType.BAN_EVASION, null);
                    alts.stream().filter(p -> !p.isBanned()).forEach(a -> a.punish(Punishment.PunishmentType.BAN_EVASION, null));
                    FarLands.broadcastStaff("Punishing " + flp.getUsername() + " for ban evasion, along with the following alts: " +
                            alts.stream().filter(p -> !p.isBanned()).map(OfflineFLPlayer::getUsername).collect(Collectors.joining(", ")), "output");
                    return;
                }
            }
        }

        if(!Utils.deltaEquals(flp.getLastLocation(), Utils.LOC_ZERO.asLocation(), 1.0))
            player.teleport(flp.getLastLocation());

        if(isNew)
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> player.teleport(dh.getPluginData().getSpawn()), 5L);
    }

    @EventHandler
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) { // Handles bans
        OfflineFLPlayer flp = FarLands.getPDH().getFLPlayer(event.getUniqueId());
        if(flp != null && flp.isBanned())
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, flp.getCurrentPunishmentMessage());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if(!event.isBedSpawn()) {
            Location spawn = FarLands.getDataHandler().getPluginData().getSpawn();
            if(Utils.deltaEquals(spawn, Utils.LOC_ZERO.asLocation(), 1e-8D)) {
                event.getPlayer().sendMessage(ChatColor.RED + "Server spawn not set! Please contact an owner, administrator, or developer and notify them of this problem.");
                return;
            }
            event.setRespawnLocation(spawn);
        }
    }

    @Override // Removes all infinite potion effects.
    public void onPlayerQuit(Player player) {
        player.getActivePotionEffects().stream().filter(pe -> pe.getDuration() >= 100 * 60 * 20)
                .map(PotionEffect::getType).forEach(player::removePotionEffect);
    }

    @EventHandler(ignoreCancelled=true)
    public void onShopCreation(PlayerCreateShopEvent event) {
        Player player = event.getPlayer();
        OfflineFLPlayer flp = FarLands.getPDH().getFLPlayer(player);
        if(!flp.canAddShop()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You have reached the maximum number of shops you can build. Rank up to gain more shops.");
            return;
        }
        flp.addShop();
    }

    @EventHandler(ignoreCancelled=true)
    public void onShopDestroyed(PlayerDestroyShopEvent event) {
        FarLands.getPDH().getFLPlayer(event.getShop().getOwnerUUID()).removeShop();
    }

    @EventHandler(ignoreCancelled=true) // Prevent players from teleporting using spectator mode
    public void onTeleport(PlayerTeleportEvent event) {
        event.setCancelled(event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE && !Rank.getRank(event.getPlayer()).isStaff());
    }

    @EventHandler(ignoreCancelled=true) // Prevent portals from forming in spawn
    public void onPortalCreation(PortalCreateEvent event) {
        if(PortalCreateEvent.CreateReason.NETHER_PAIR.equals(event.getReason()) && event.getBlocks().stream()
                .map(block -> block.getBlock().getLocation()).anyMatch(Utils::isInSpawn)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) { // Block 0 tick farms
        event.setCancelled(PISTON_FILTER.stream().map(off -> event.getBlock().getRelative(off[0], off[1], off[2]))
                .anyMatch(block -> block.getType() == Material.MOVING_PISTON));
    }

    @EventHandler(ignoreCancelled=true)
    public void onItemTransferred(InventoryMoveItemEvent event) {
        FarLands.getDataHandler().getPluginData().getItemDistributors().forEach(id -> id.accept(event));
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        StringBuilder sb = new StringBuilder();
        for(String line : event.getLines()) {
            if (!line.isEmpty())
                sb.append(line).append("\n");
        }
        String text = sb.toString().trim();
        if(!text.isEmpty()) {
            FarLands.broadcastStaff(TextUtils.format("&(gray)%0 placed a sign at %1x, %2z:\n%3",
                    event.getPlayer().getName(), event.getBlock().getLocation().getBlockX(),
                    event.getBlock().getLocation().getBlockZ(), text));
        }
    }

    @EventHandler(ignoreCancelled=true) // Prevent players from igniting TNT or opening shulker boxes inside claims via right-clicking
    @SuppressWarnings("unchecked")
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if(Action.RIGHT_CLICK_BLOCK.equals(event.getAction())) {
            if(Utils.isInSpawn(event.getClickedBlock().getLocation()) && !Rank.getRank(player).isStaff() &&
                    DOORS.contains(event.getClickedBlock().getType())) {
                player.sendMessage(ChatColor.RED + "You cannot change that here.");
                event.setCancelled(true);
                return;
            }
            if (event.getClickedBlock().getRelative(event.getBlockFace()).getType() == Material.END_PORTAL &&
                event.getMaterial().name().endsWith("_BUCKET")) {
                event.setCancelled(true);
                return;
            }
            if(GameMode.CREATIVE.equals(player.getGameMode()) &&
                    Rank.getRank(player).isStaff() && Material.IRON_NUGGET.equals(event.getMaterial())) {
                if(FarLands.getDataHandler().getPluginData().getItemDistributors().stream()
                        .anyMatch(id -> id.hasSourceAt(event.getClickedBlock().getLocation()))) {
                    player.sendMessage(ChatColor.RED + "This chest is already a source for an item distributer.");
                    event.setCancelled(true);
                    return;
                }
                RandomAccessDataHandler radh = FarLands.getDataHandler().getRADH();
                Pair<Integer, ItemDistributor> stage = (Pair<Integer, ItemDistributor>)radh
                        .retrieveAndStoreIfAbsent(new Pair<>(0, new ItemDistributor()), "distribMaker",
                        player.getUniqueId().toString());
                radh.setCooldown(400L, "distribMakerTimeout", player.getUniqueId().toString(),
                        () -> radh.delete("distribMaker", player.getUniqueId().toString()));
                switch(stage.getFirst()) {
                    case 0:
                        if(Material.CHEST.equals(event.getClickedBlock().getType())) {
                            stage.getSecond().setSource(event.getClickedBlock().getLocation());
                            player.sendMessage(ChatColor.GREEN + "Source chest set.");
                            stage.setFirst(1);
                        }else
                            player.sendMessage(ChatColor.RED + "Please click a chest to set as the source.");
                        break;
                    case 1:
                        if(Material.CHEST.equals(event.getClickedBlock().getType())) {
                            stage.getSecond().setPublic(event.getClickedBlock().getLocation());
                            player.sendMessage(ChatColor.GREEN + "Public chest set.");
                            stage.setFirst(2);
                        }else
                            player.sendMessage(ChatColor.RED + "Please click a chest to set as the public chest.");
                        radh.resetCooldown("distribMakerTimeout", player.getUniqueId().toString());
                        break;
                    case 2:
                        if(Material.CHEST.equals(event.getClickedBlock().getType())) {
                            stage.getSecond().setPrivate(event.getClickedBlock().getLocation());
                            player.sendMessage(ChatColor.GREEN + "Private chest set, item distributor registered.");
                            radh.removeCooldown("distribMakerTimeout", player.getUniqueId().toString());
                            radh.delete("distribMaker", player.getUniqueId().toString());
                            FarLands.getDataHandler().getPluginData().getItemDistributors().add(stage.getSecond());
                        }else
                            player.sendMessage(ChatColor.RED + "Please click a chest to set as the private chest.");
                        break;
                }
                event.setCancelled(true);
            }
        }else if(Action.LEFT_CLICK_BLOCK.equals(event.getAction())) {
            if(GameMode.CREATIVE.equals(player.getGameMode()) &&
                    Rank.getRank(player).isStaff() && Material.IRON_NUGGET.equals(event.getMaterial()) &&
                    FarLands.getDataHandler().getPluginData().getItemDistributors()
                            .removeIf(id -> id.hasSourceAt(event.getClickedBlock().getLocation()))) {
                player.sendMessage(ChatColor.GREEN + "Removed item distributor.");
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if("world_the_end".equals(event.getBlock().getWorld().getName()) &&
                event.getBlockPlaced().getType() == Material.SLIME_BLOCK) {
            event.getPlayer().sendMessage(ChatColor.RED + "You can't place slime blocks in the end.");
            event.setCancelled(true);
        } else if (event.getBlockAgainst().getType() == Material.SLIME_BLOCK &&
                (event.getBlockPlaced().getType().name().endsWith("FAN") ||
                    event.getBlockPlaced().getType().name().endsWith("CORAL"))) {
            final Location loc = event.getBlock().getLocation();
            AntiCheat.broadcast(event.getPlayer().getName(), "may be attempting to build a duper @ " +
                    loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
            TextUtils.sendFormatted(event.getPlayer(),"&(red)It appears you are building a duping device, " +
                    "this is against the $(hovercmd,/rules,{&(white)view the rules},&(dark_red)/rules) (#1) as duping is a form of exploit. " +
                    "If you ignore this warning and do not remove the machine immediately you will be subject to a punishment.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if(to.getY() > 126.0 && "world_nether".equals(to.getWorld().getName()) && !Rank.getRank(event.getPlayer()).isStaff()) {
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot go on top of the nether. This is in the rules, your items will not be restored.");
            event.getPlayer().setHealth(0.0);
            return;
        }
        int max = (int)event.getTo().getWorld().getWorldBorder().getSize() / 2;
        if(Math.abs(to.getX()) > max || Math.abs(to.getZ()) > max)  {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot leave the world.");
        }
    }
}
