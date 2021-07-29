package net.farlands.sanctuary.mechanic;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.Region;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import com.kicas.rp.event.ClaimCreationEvent;
import com.kicas.rp.event.ClaimResizeEvent;
import com.kicas.rp.util.Pair;
import com.kicas.rp.util.TextUtils;
import com.kicasmads.cs.event.ShopCreateEvent;
import com.kicasmads.cs.event.ShopRemoveEvent;
import com.kicasmads.cs.event.ShopTransactionEvent;

import net.coreprotect.CoreProtect;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.player.CommandKittyCannon;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Punishment;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.anticheat.AntiCheat;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.LocationWrapper;
import net.farlands.sanctuary.util.Logging;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kicas.rp.util.TextUtils.sendFormatted;

public class Restrictions extends Mechanic {

    private final HashSet<UUID> endWarnings = new HashSet<>();

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

            List<Punishment> alertablePunishments =
                flp.punishments.stream()
                    .filter(p -> p.getType().isRejoinAlert() && p.notAlerted())
                    .collect(Collectors.toList());

            if(!alertablePunishments.isEmpty()){
                Logging.broadcastStaff(
                    TextUtils.format(ChatColor.RED + "%0 has joined for the first time since receiving the following punishment%1: %2",
                        flp.username,
                        alertablePunishments.size() == 1 ? "" : "s",
                        alertablePunishments.stream()
                            .map(punishment -> punishment.getType().getHumanName())
                            .collect(Collectors.joining(", "))
                    ),
                    DiscordChannel.ALERTS);
                alertablePunishments.forEach(Punishment::alertSent);
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

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split(" ");
        if (event.getMessage().startsWith("/trigger") && args.length > 1 && !"as_help".equalsIgnoreCase(args[1])) {
            Player player = event.getPlayer();

            // Check for claims
            FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(player.getLocation());
            if (!(flags == null || flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(player, TrustLevel.BUILD, flags))) {
                sendFormatted(player, "&(red)You do not have permission to edit armor stands here.");
                event.setCancelled(true);
            }
        }
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
    public void onShopTransaction(ShopTransactionEvent event) {
        Player player = event.getPlayer();
        int buyAmount = event.getShop().getBuyAmount();
        ItemStack bought = event.getShop().getBuyItem();
        int sellAmount = event.getShop().getSellAmount();
        ItemStack sold = event.getShop().getSellItem();

        CoreProtect.getInstance().getAPI().logContainerTransaction("[SHOP] " + player.getName(), event.getShop().getChestLocation());
        Logging.log(player.getName() + " bought " + sellAmount + " " + sold.getType().toString().toLowerCase()
                + " with " + buyAmount + " " + bought.getType().toString().toLowerCase());
    }

    @EventHandler(ignoreCancelled = true)
    public void onShopDestroyed(ShopRemoveEvent event) {
        FarLands.getDataHandler().getOfflineFLPlayer(event.getShop().getCachedOwner().getId()).removeShop();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortalCreation(PortalCreateEvent event) {
        switch(event.getReason()) {
            case NETHER_PAIR: // Prevent portals forming in spawn
                if(event.getBlocks()
                    .stream()
                    .map(block -> block.getBlock().getLocation())
                    .anyMatch(FLUtils::isInSpawn)
                ) {
                    event.setCancelled(true);
                }
            case FIRE: // Prevent portals forming in the pocket world
                if(event.getWorld().getName().equals("farlands")) {
                    event.setCancelled(true);
                }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String signText = String.join("\n" + ChatColor.GRAY, event.getLines());
        if (!signText.isBlank()) {
            Location loc = event.getBlock().getLocation();
            Logging.broadcastStaff(
                ChatColor.GRAY + event.getPlayer().getName() + " placed a sign at " +
                    loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + ":\n" +
                    signText
            );
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (
                "world_the_end".equals(event.getBlock().getWorld().getName()) && (
                        event.getBlockPlaced().getType() == Material.SLIME_BLOCK ||
                        event.getBlockPlaced().getType() == Material.HONEY_BLOCK
                )
        ) {
            if (!endWarnings.contains(event.getPlayer().getUniqueId())) {
                TextUtils.sendFormatted(event.getPlayer(), "&(red)Flying machine related deaths will not be considered " +
                        "server error and as a result, will {&(bold)not} be restored! " +
                        "Travel safely by riding in a boat or minecart that is attached to the machine.");
                endWarnings.add(event.getPlayer().getUniqueId());
            }
        }
        else if ((
                event.getBlockAgainst().getType() == Material.SLIME_BLOCK ||
                event.getBlockAgainst().getType() == Material.HONEY_BLOCK
            ) && (
                event.getBlockPlaced().getType().name().endsWith("FAN") ||
                event.getBlockPlaced().getType().name().endsWith("CORAL")
        )) {
            Location location = event.getBlock().getLocation();
            AntiCheat.broadcast(event.getPlayer().getName(), "may be attempting to build a duper @ " +
                    location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
            TextUtils.sendFormatted(event.getPlayer(), "&(red)It appears you are building a duping device, " +
                    "this is against the $(hovercmd,/rules,{&(white)view the rules},&(dark_red)/rules) (#1) as duping is a form of exploit. " +
                    "If you ignore this warning and do not remove the machine immediately you will be subject to a punishment.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE &&
                !Rank.getRank(event.getPlayer()).isStaff()) { // Prevent players from teleporting using spectator mode
            event.setCancelled(true);
            return;
        }
        onPlayerMove(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        int max = (int)to.getWorld().getWorldBorder().getSize() >> 1; // size / 2 (rounded down)
        if (Math.abs(to.getX()) > max || Math.abs(to.getZ()) > max) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot leave the world.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClaimCreated(ClaimCreationEvent event) {
        checkClaim(event.getCreator(), event.getRegion(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClaimResized(ClaimResizeEvent event) {
        checkClaim(event.getDelegate(), event.getRegion(), event);
    }

    private void checkClaim(Player delegate, Region region, Cancellable event) {
        Location min = region.getMin(),
                 max = region.getMax();
        final int NETHER_CLAIM_LIMIT = 15000;
        if (region.getWorld().getEnvironment() == World.Environment.NETHER &&
                (
                        Math.abs(min.getBlockX()) > NETHER_CLAIM_LIMIT || Math.abs(min.getBlockZ()) > NETHER_CLAIM_LIMIT ||
                        Math.abs(max.getBlockX()) > NETHER_CLAIM_LIMIT || Math.abs(max.getBlockZ()) > NETHER_CLAIM_LIMIT
                )
        ) {
            if (delegate != null)
                delegate.sendMessage(ChatColor.RED + "You cannot create a claim more than 15k blocks from 0,0.");
            event.setCancelled(true);
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
    public void onPlayerClickInventory(InventoryClickEvent event) {
        if (event.getView().getTopInventory() instanceof HorseInventory) {
            if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(CommandKittyCannon.CANNON.isSimilar(event.getCursor()) ||
                    CommandKittyCannon.CANNON.isSimilar(event.getCurrentItem()));
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
