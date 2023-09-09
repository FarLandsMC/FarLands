package net.farlands.sanctuary.mechanic;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.Region;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import com.kicas.rp.event.ClaimCreationEvent;
import com.kicas.rp.event.ClaimResizeEvent;
import com.kicas.rp.util.Pair;
import com.kicasmads.cs.event.ShopCreateEvent;
import com.kicasmads.cs.event.ShopRemoveEvent;
import com.kicasmads.cs.event.ShopTransactionEvent;
import com.squareup.moshi.Types;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import net.coreprotect.CoreProtect;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.player.CommandKittyCannon;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.data.pdc.JSONDataType;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Punishment;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.anticheat.AntiCheat;
import net.farlands.sanctuary.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles restrictions on players
 */
public class Restrictions extends Mechanic {

    private final Set<UUID> endWarnings        = new HashSet<>(); // Warnings for flying machines in the end
    public final  Set<UUID> mediaFlyProtection = new HashSet<>(); // Players who have temporary flight from exiting a claim
    public final  HashMap<UUID, List<String>> currentSignEditors = new HashMap<>(); // Players who are currently editing a sign

    @Override
    public void onStartup() {
        // Fix nether claim heights
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> { // Set the height of all claims in the nether to 128 (top bedrock)
            RegionProtection.getDataManager().getRegionsInWorld(Bukkit.getWorld("world_nether")).forEach(region -> {
                if (region.isAdminOwned()) {
                    return;
                }

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

            if (!notes.isEmpty()) { // If they have notes, announce them to online staff
                Logging.broadcastStaff(
                    ComponentColor.red(
                        "{} has notes. Hover {} to view them.",
                        flp,
                        ComponentUtils.hover(
                            ComponentColor.aqua(" here"),
                            ComponentColor.gray(String.join("\n", notes))
                        )
                    )
                );
            }

            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> { // Give staff information about the player's potential alts
                List<OfflineFLPlayer> alts = FarLands.getDataHandler().getOfflineFLPlayers().stream()
                    .filter(otherFlp -> flp.lastIP.equals(otherFlp.lastIP) && !flp.uuid.equals(otherFlp.uuid)).toList();
                List<String> banned = alts.stream().filter(OfflineFLPlayer::isBanned).map(flp0 -> flp0.username).collect(Collectors.toList()),
                    unbanned = alts.stream().filter(p -> !p.isBanned()).map(flp0 -> flp0.username).collect(Collectors.toList());
                if (!banned.isEmpty()) {
                    Logging.broadcastStaff(ChatColor.RED + flp.username + " shares the same IP as " + banned.size() + " banned player" +
                                           (banned.size() > 1 ? "s" : "") + ": " + String.join(", ", banned), isNew ? DiscordChannel.ALERTS : null);
                    Logging.broadcastStaff(
                        ComponentColor.red(
                            flp.username +
                            " shares the same IP as " +
                            banned.size() + " banned player" +
                            (banned.size() > 1 ? "s" : "") +
                            ": " +
                            String.join(", ", banned)),
                        isNew ? DiscordChannel.ALERTS : null
                    );
                }
                if (!unbanned.isEmpty()) {
                    Logging.broadcastStaff(
                        ComponentColor.red(
                            flp.username +
                            " shares the same IP as " +
                            unbanned.size() + " player" +
                            (unbanned.size() > 1 ? "s" : "") +
                            ": " +
                            String.join(", ", unbanned)),
                        isNew ? DiscordChannel.ALERTS : null
                    );
                }
                if (isNew && flp.lastIP != null && !flp.lastIP.trim().isEmpty()) {
                    flp.lastLocation = FarLands.getDataHandler().getPluginData().spawn;
                    if (!banned.isEmpty()) {
                        Logging.broadcastStaff(
                            ComponentColor.red(
                                "Punishing " + flp.username + " for ban evasion"
                                + (unbanned.isEmpty() ? "." : ", along with the following alts: " + String.join(", ", unbanned))
                            ),
                            DiscordChannel.NOTEBOOK
                        );
                        flp.punish(Punishment.PunishmentType.BAN_EVASION, null);
                        alts.stream().filter(p -> !p.isBanned()).forEach(a -> a.punish(Punishment.PunishmentType.BAN_EVASION, null));
                    }
                }
            }, 60L);

            List<Punishment> alertablePunishments = flp.punishments.stream()
                .filter(p -> p.getType().isRejoinAlert() && p.notAlerted())
                .toList();

            if (!alertablePunishments.isEmpty()) { // Alert of certain punishments
                Logging.broadcastStaff(
                    ComponentColor.red(
                        "{} has joined for the first time since receiving the following punishment{}:",
                        flp.username,
                        alertablePunishments.size() == 1 ? "" : "s",
                        alertablePunishments.stream()
                            .map(punishment -> punishment.getType().getHumanName())
                            .collect(Collectors.joining(", "))
                    ),
                    DiscordChannel.ALERTS
                );
                alertablePunishments.forEach(Punishment::alertSent);
            }


        }

        if (isNew) { // Teleport to spawn if a new player
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), flp::moveToSpawn, 5L);
        } else if (flp.lastLocation != null) { // Otherwise, tp to their last location
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> player.teleport(flp.getLastLocation()), 5L);
        }
    }

    @EventHandler
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) { // Handles bans
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getUniqueId());

        if (flp == null && !FarLands.getDataHandler().allowNewPlayers()) { // Server is currently blocking new players
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ComponentColor.red("You cannot join the server right now. Try again in 5-10 minutes."));
        }

        if (flp != null) {
            flp.lastIP = event.getAddress().getHostAddress();
            if (flp.isBanned()) { // Give the player a ban message
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, flp.getCurrentPunishmentMessage());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!event.isBedSpawn()) {
            LocationWrapper spawn = FarLands.getDataHandler().getPluginData().spawn;
            if (spawn == null) {
                event.getPlayer().sendMessage(ComponentColor.red("Server spawn not set! Please contact an owner, " +
                                                                 "administrator, or developer and notify them of this problem."));
                return;
            }
            event.setRespawnLocation(spawn.asLocation());
        }
    }

    @Override // Removes all infinite potion effects.
    public void onPlayerQuit(Player player) {
        player.getActivePotionEffects().stream().filter(pe -> pe.getDuration() >= 100 * 60 * 20) // 100m for bad omen
            .map(PotionEffect::getType).forEach(player::removePotionEffect);
        this.currentSignEditors.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split(" ");
        if (event.getMessage().startsWith("/trigger") && args.length > 1 && !"as_help".equalsIgnoreCase(args[1])) { // /trigger command -- used for armour stand statues
            Player player = event.getPlayer();

            // Check for claims
            FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(player.getLocation());
            if (!(flags == null || flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(player, TrustLevel.BUILD, flags))) { // Check if the player has build trust
                player.sendMessage(ComponentColor.red("You do not have permission to edit armor stands here."));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onShopCreation(ShopCreateEvent event) {
        Player player = event.getPlayer();
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
        if (!flp.canAddShop()) { // Limit the amount of shops that a player can create
            event.setCancelled(true);
            player.sendMessage(ComponentColor.red("You have reached the maximum number of shops you can build. " +
                                                  "Rank up to gain more shops."));
            return;
        }
        flp.addShop();
    }

    @EventHandler(ignoreCancelled = true)
    public void onShopTransaction(ShopTransactionEvent event) {
        String player = event.getPlayer().getName();
        int buyAmount = event.getShop().getBuyAmount();
        ItemStack bought = event.getShop().getBuyItem();
        int sellAmount = event.getShop().getSellAmount();
        ItemStack sold = event.getShop().getSellItem();

        // Add simple logging for shop transactions
        CoreProtect.getInstance().getAPI().logContainerTransaction("[SHOP] " + player, event.getShop().getChestLocation());
        Logging.log("%s bought %d %s with %d %s".formatted(
            player,
            sellAmount, sold.getType().toString().toLowerCase(),
            buyAmount, bought.getType().toString().toLowerCase()
        ));
    }

    @EventHandler(ignoreCancelled = true)
    public void onShopDestroyed(ShopRemoveEvent event) {
        FarLands.getDataHandler().getOfflineFLPlayer(event.getShop().getOwner()).removeShop();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortalCreation(PortalCreateEvent event) {
        // Prevent portals forming in spawn
        if (event.getReason() == PortalCreateEvent.CreateReason.NETHER_PAIR
            && event.getBlocks()
                .stream()
                .map(block -> block.getBlock().getLocation())
                .anyMatch(FLUtils::isInSpawn)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerOpenSign(PlayerOpenSignEvent event) {
        if (event.getCause() == PlayerOpenSignEvent.Cause.INTERACT) {
            Sign sign = event.getSign();
            SignSide side = sign.getSide(event.getSide());
            event.setCancelled(true);

            // This would fit better in GeneralMechanics, but the logic is easier when it's here.
            NamespacedKey key = FLUtils.nsKey(event.getSide().toString().toLowerCase() + "_raw");
            String[] unstyled = sign.getPersistentDataContainer().get(key, new JSONDataType<>(String[].class));

            if (unstyled != null) {
                for (int i = 0; i < unstyled.length; i++) {
                    String l = unstyled[i];
                    side.line(i, Component.text(l));
                }
            }

            sign.update();
            // For some reason, we need to wait two ticks after updating before we can actually open the GUI
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> event.getPlayer().openSign(sign, event.getSide()), 2L);
            this.currentSignEditors.put(event.getPlayer().getUniqueId(), side.lines().stream().map(ComponentUtils::toText).toList());
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        List<Component> lines = event.lines()
            .stream()
            .map(ComponentUtils::toText)
            .filter(s -> !s.isBlank())
            .map(ComponentColor::gray)
            .toList();

        List<String> prevContent = this.currentSignEditors.remove(event.getPlayer().getUniqueId());
        List<String> currContent = lines.stream().map(ComponentUtils::toText).toList();

        if (
            !currContent.equals(prevContent) // Sign content changed
            && !(prevContent == null && lines.isEmpty()) // and didn't place empty sign
        ) {
            Location loc = event.getBlock().getLocation();
            Component content = Component.join(JoinConfiguration.newlines(), lines);
            Logging.broadcastStaff(
                ComponentColor.gray(
                    "{} {} a sign at {} {} {}",
                    event.getPlayer().getName(),
                    prevContent == null
                    ? "placed"
                    : lines.isEmpty()
                        ? "cleared"
                        : "edited",
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ()
                ).append(
                    lines.isEmpty()
                    ? Component.text(".")
                    : ComponentUtils.toText(lines.get(0)).equalsIgnoreCase("[shop]")
                        ? ComponentUtils.format(": {}", ComponentUtils.hover(ComponentColor.aqua("[shop]"), content)) // Show abbreviated message if a shop
                        : ComponentUtils.format(":\n{}", content)
                )
            );
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // Prevent players from placing collectable items in survival
        if (
            FarLands.getDataHandler().isCollectable(event.getItemInHand())
            && event.getPlayer().getGameMode() != GameMode.CREATIVE
        ) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                ComponentColor.red(
                    "Collectables can not be placed{}.",
                    Rank.getRank(event.getPlayer()).isStaff() ? " in survival mode" : ""
                )
            );
        }

        if (
            "world_the_end".equals(event.getBlock().getWorld().getName()) && (
                event.getBlockPlaced().getType() == Material.SLIME_BLOCK ||
                event.getBlockPlaced().getType() == Material.HONEY_BLOCK
            )
        ) {
            if (!this.endWarnings.contains(event.getPlayer().getUniqueId())) { // Give players end warnings
                event.getPlayer().sendMessage(
                    ComponentColor.red(
                            "Flying machine related deaths will not be considered server error and as a result, will "
                        )
                        .append(Component.text("not", Style.style(TextDecoration.BOLD)))
                        .append(ComponentColor.red(" be restored! \nTravel safely by riding in a boat or minecart that is attached to the machine."))
                );
                this.endWarnings.add(event.getPlayer().getUniqueId());
            }
        } else if (( // Alert to staff any attempts to build a duper
                       event.getBlockAgainst().getType() == Material.SLIME_BLOCK ||
                       event.getBlockAgainst().getType() == Material.HONEY_BLOCK
                   ) && (
                       event.getBlockPlaced().getType().name().endsWith("FAN") ||
                       event.getBlockPlaced().getType().name().endsWith("CORAL")
                   )) {
            Location location = event.getBlock().getLocation();
            AntiCheat.broadcast(event.getPlayer().getName(), "may be attempting to build a duper @ " +
                                                             location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
            event.getPlayer().sendMessage(
                ComponentColor.red("It appears that you are building a duping device,  this is against the ")
                    .append(ComponentUtils.command("/rules").color(NamedTextColor.DARK_RED))
                    .append(ComponentColor.red(
                        " (#1) as duping is a form of exploit. " +
                        "\nIf you ignore this warning and do not remove the machine immediately, you will be subject to a punishment"
                    ))
            );
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
        int max = (int) to.getWorld().getWorldBorder().getSize() / 2;
        if (Math.abs(to.getX()) > max || Math.abs(to.getZ()) > max) { // Prevent the player from leaving the world border
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot leave the world.");
        }

        // ------------------------
        // Media flight restriction
        // ------------------------
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer());
        if (flp.rank == Rank.MEDIA) {
            if (!event.getPlayer().isFlying()) {
                this.mediaFlyProtection.remove(flp.uuid); // remove the temp protection if they stop flying
            }
            if (
                event.getPlayer().isFlying()
                && !FLUtils.canMediaFly(event.getPlayer(), event.getTo()) // Check if the player can fly in the given location
            ) {
                Region rgFrom = RegionProtection.getDataManager().getHighestPriorityRegionAt(event.getFrom());
                if (rgFrom != null && (rgFrom.isOwner(flp.uuid) || rgFrom.<TrustMeta>getFlagMeta(RegionFlag.TRUST)
                    .hasTrust(event.getPlayer(), TrustLevel.ACCESS, rgFrom))) { // Leaving a region they own or have at in
                    event.getPlayer().sendMessage(ComponentColor.red("Flight is disabled outside of claims that " +
                                                                     "you own or have access trust in. Flight stopping in 5 seconds."));
                    this.mediaFlyProtection.add(flp.uuid); // Add them to the temp protection list
                    Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                        this.mediaFlyProtection.remove(flp.uuid);
                        FarLands.getDataHandler().getSession(event.getPlayer()).giveFallImmunity(5); // Give time to fall if they're really high up
                    }, 5 * 20L);
                }
            }

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

    /**
     * Validate that a claim is allowed to be created by the player
     */
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
            if (delegate != null) {
                delegate.sendMessage(ChatColor.RED + "You cannot create a claim more than 15k blocks from 0,0.");
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        Material result = event.getInventory().getContents()[0].getType();

        if (result == Material.SHIELD) { // Fix shield duping bug
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

            event.setCancelled(CommandKittyCannon.CANNON.isSimilar(event.getCursor()) || // Prevent kitty cannon being used as horse armor
                               CommandKittyCannon.CANNON.isSimilar(event.getCurrentItem()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if ( // Prevent kitty cannon from being used as horse armour
            CommandKittyCannon.CANNON.isSimilar(event.getPlayer().getInventory().getItem(event.getHand()))
            && event.getRightClicked().getType().name().endsWith("HORSE")
        ) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent the kitty cannon from being used in a grindstone
     * <p>
     * Paper complains about using {@link org.bukkit.event.inventory.PrepareGrindstoneEvent},
     * so we're using {@link PrepareResultEvent} and checking the inventory
     */
    @EventHandler
    public void onPrepareResult(PrepareResultEvent event) {
        if (
            event.getInventory() instanceof GrindstoneInventory
            && event.getInventory().contains(CommandKittyCannon.CANNON)
        ) {
            event.setResult(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        if (event.isSigning() && "Statues".equals(event.getNewBookMeta().getTitle())) { // Prevent creation of statues book
            event.getPlayer().sendMessage(ChatColor.RED + "You are not allowed to manually create the armor stand " +
                                          "editor book, use /editarmorstand instead.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();

        if (
            target instanceof ItemFrame frame // Target is an item frame
            && frame.getItem().getType() == Material.ELYTRA // and the frame has elytra
            && FLUtils.inEndCity(frame.getLocation()) // and in an end city
            && (damager instanceof Projectile || damager instanceof Player) // and it was hit by a projectile or a player
        ) {

            Player player = damager instanceof Player p ? p : null; // Set player to the player or null if there was no player

            if (damager instanceof Projectile proj) { // The item frame was hit by a projectile
                ProjectileSource shooter = proj.getShooter();

                if (shooter instanceof Player p) { // If the shooter was a player, use the player
                    player = p;
                } else { // Otherwise, find the closest player within 32 blocks of the projectile
                    Location projloc = proj.getLocation();
                    player = (Player) proj
                        .getNearbyEntities(32, 32, 32)
                        .stream()
                        .filter(e -> e instanceof Player)
                        .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(projloc)))
                        .orElse(null);
                }
            }


            PersistentDataContainer pdc = frame.getPersistentDataContainer();
            ParameterizedType setUuidType = Types.newParameterizedType(Set.class, UUID.class);
            JSONDataType<Set> dataType = new JSONDataType<>(Set.class, setUuidType);

            if (player != null) { // This should always be true, but who knows ¯\_(ツ)_/¯
                if (player.getGameMode() == GameMode.CREATIVE) {
                    return; // If the player is in creative, let them do as they wish
                }
                if (pdc.has(FLUtils.nsKey("placedbyplayer"))) {
                    return; // Frame placed by player, so ignore it
                }
                OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);

                // Get the "chessboard distance" from (0, 0)
                // This is better than traditional distance as it makes it into a "square" shape, where both (25000, 25000) and (25000, 0) are 25000 blocks from the centre
                double dist = FLUtils.chessboardDistance(player.getLocation(), Worlds.END.getLocation(0, 0, 0));
                int max = (int) ((dist / 25000) * 100);

                if (flp.elytrasObtained >= max) { // They cannot take the elytra
                    Command.error(
                        player,
                        "You cannot collect more than {} elytra{} at this distance from main end island, you must go farther out." +
                        "\nThis helps to make it fair to new players.",
                        max,
                        max == 1 ? "" : "s"
                    );
                    NamespacedKey takeAttempts = FLUtils.nsKey("takeattempts");
                    event.setCancelled(true);
                    Set<UUID> attempts = pdc.get(takeAttempts, dataType);
                    FLUtils.smokeItemFrame(player, frame);
                    if (
                        attempts == null
                        || !attempts.contains(flp.uuid)
                    ) {
                        Set<UUID> updated = new HashSet<>();
                        if (attempts != null) updated.addAll(attempts);
                        updated.add(flp.uuid);
                        pdc.set(takeAttempts, dataType, updated);
                        ItemStack stack = FarLands.getDataHandler().getItem("elytraLimitRocket", true);
                        if (stack.getType() != Material.AIR) {
                            Command.success(player, "Have a rocket to help!");
                            FLUtils.giveItem(player, stack, true);
                        }
                    }
                } else { // They can take the elytra
                    flp.elytrasObtained += 1;
                    NamespacedKey takenBy = FLUtils.nsKey("takenby");
                    pdc.set(takenBy, PersistentDataType.STRING, flp.uuid.toString());
                }
            }
        }

    }

    /**
     * Cancel Item Frames popping off in the end cities - for the elytra restriction
     */
    @EventHandler
    public void onEntityDeath(HangingBreakEvent event) {
        Entity entity = event.getEntity();
        if (
            event.getCause() != HangingBreakEvent.RemoveCause.ENTITY
            && entity instanceof ItemFrame frame
        ) {
            Location frameLoc = frame.getLocation();
            if (!FLUtils.inEndCity(frameLoc)) return;
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChangeItemFrame(PlayerItemFrameChangeEvent event) {
        ItemFrame frame = event.getItemFrame();
        if (
            event.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.PLACE
            && FLUtils.inEndCity(frame.getLocation())
            && event.getPlayer().getGameMode() == GameMode.SURVIVAL
        ) {
            event.setCancelled(true);
            FLUtils.smokeItemFrame(event.getPlayer(), frame);
        }
    }

    @EventHandler
    public void onItemFrame(HangingPlaceEvent event) {
        Hanging entity = event.getEntity();
        if (event.getPlayer() != null && event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            entity.getPersistentDataContainer()
                .set(FLUtils.nsKey("spawnedByPlayer"), PersistentDataType.BYTE, (byte) 1);
        }
    }

}
