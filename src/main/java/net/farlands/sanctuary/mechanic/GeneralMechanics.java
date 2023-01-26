package net.farlands.sanctuary.mechanic;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.Region;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.WorldData;
import com.kicas.rp.data.flagdata.EnumFilter;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import com.kicas.rp.event.ClaimAbandonEvent;
import com.kicas.rp.event.ClaimStealEvent;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.FLShutdownEvent;
import net.farlands.sanctuary.command.player.CommandKittyCannon;
import net.farlands.sanctuary.data.Cooldown;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.SkullCreator;
import net.farlands.sanctuary.gui.GuiVillagerEditor;
import net.farlands.sanctuary.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftVillager;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * General mechanics that don't fit into other categories
 */
public class GeneralMechanics extends Mechanic {

    private final Map<UUID, Player> fireworkLaunches;
    private       Component   joinMessage;

    private static final List<EntityType> LEASHABLE_ENTITIES = List.of(
        EntityType.SKELETON_HORSE,
        EntityType.VILLAGER,
        EntityType.TURTLE,
        EntityType.PANDA,
        EntityType.FOX
    );

    private final Cooldown   nightSkip;
    private final List<UUID> leashedEntities;
    private       BukkitTask nightSkipTask;

    // Recently punished players (since server restart)
    public static final List<OfflineFLPlayer> recentlyPunished = new ArrayList<>();

    public GeneralMechanics() {
        this.fireworkLaunches = new HashMap<>();
        this.joinMessage = Component.empty();
        this.nightSkip = new Cooldown(200L);
        this.leashedEntities = new ArrayList<>();
        this.nightSkipTask = null;
    }

    @Override
    public void onStartup() {
        try {
            joinMessage = ComponentUtils.parse(FarLands.getDataHandler().getDataTextFile("join-message.txt"))
                .replaceText(
                    TextReplacementConfig
                        .builder()
                        .matchLiteral("$$DISCORD$$")
                        .replacement(ComponentUtils.link(
                            FarLands.getFLConfig().discordInvite,
                            FarLands.getFLConfig().discordInvite,
                            NamedTextColor.GRAY
                        ))
                        .build()
                );
        } catch (IOException | IllegalFormatException ex) {
            Logging.error("Failed to load join message!");
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), () ->
            Bukkit.getOnlinePlayers().forEach(player -> {
                OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
                if (flp.hasParticles() && !flp.vanished && GameMode.SPECTATOR != player.getGameMode()) {
                    flp.particles.spawn(player);
                }
            }), 0L, 60L);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), () ->
            Bukkit.getWorld("world").getEntities().stream().filter(e -> EntityType.DROPPED_ITEM == e.getType())
                .map(e -> (Item) e).filter(e -> Material.SLIME_BALL == e.getItemStack().getType() &&
                                                e.isValid() && e.getLocation().getChunk().isSlimeChunk())
                .forEach(e -> {
                    e.getWorld().playSound(e.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.0F);
                    e.setVelocity(new org.bukkit.util.Vector(0.0, 0.4, 0.0));
                }), 0L, 100L);
    }

    @EventHandler
    public void onFLShutdown(FLShutdownEvent event) {
        Bukkit.getOnlinePlayers()
            .forEach(pl -> {
                pl.kick(ComponentColor.gold("The server is restarting..."), PlayerKickEvent.Cause.RESTART_COMMAND);
            });
    }

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        player.sendMessage(joinMessage);

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () ->
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 0.6929134F), 45L);
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () ->
            player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 0.85F, 1.480315F), 95L);
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 0.5F);
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
            if (!flp.viewedPatchnotes) {
                player.sendMessage(
                    ComponentColor.gold("Patch ")
                        .append(ComponentColor.aqua("#%s", FarLands.getDataHandler().getCurrentPatch()))
                        .append(ComponentColor.gold(" has been released! View changes with "))
                        .append(ComponentUtils.command("/patchnotes"))
                );
            }
            if (flp.birthday != null && flp.birthday.isToday()) {
                player.sendMessage(ComponentColor.gold("Happy Birthday"));
            }
            flp.updateDeaths(); // Just to be sure that deaths count is accurate
        }, 125L);

        if (isNew) {
            Logging.broadcast(p -> {
                Player pl = p.handle.getOnlinePlayer();
                if (!player.getUniqueId().equals(p.handle.uuid)) {
                    if (pl != null) {
                        pl.playSound(pl.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5.0F, 1.0F);
                    }
                    return true;
                } else {
                    return false;
                }
            }, "<gold><bold> > </bold> Welcome <green>%s</green> to FarLands!", player.getName());
            player.chat("/chain {guidebook} {shovel}");
            player.sendMessage(
                ComponentColor.gold("Welcome to FarLands! Please read ")
                    .append(ComponentUtils.command("/rules"))
                    .append(ComponentColor.gold(" before playing. To get started, you can use "))
                    .append(ComponentUtils.command("/wild"))
                    .append(ComponentColor.gold(" to teleport to a random location on the map.  Also, feel free to join our community on Discord by clicking "))
                    .append(ComponentUtils.link("here", FarLands.getFLConfig().discordInvite))
            );

            Rank rank = Rank.getRank(player);
            if (rank == Rank.PATRON || rank == Rank.SPONSOR) {
                FLUtils.giveItem(player, FarLands.getDataHandler().getItem("patronCollectable"), false);
            }
        }

        if ("world".equals(player.getWorld().getName())) {
            updateNightSkip(true);
        }
    }

    @Override
    public void onPlayerQuit(Player player) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(player);
        if (session.seatExit != null) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                vehicle.eject();
                vehicle.remove();
            }
            player.teleport(session.seatExit);
            session.seatExit = null;
        }

        session.removeVanishPlaytime();
        updateNightSkip(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player)) {
            return;
        }

        FLPlayerSession session = FarLands.getDataHandler().getSession((Player) event.getExited());
        if (session.seatExit != null) {
            event.setCancelled(true);
            event.getVehicle().eject();
            event.getVehicle().remove();
            event.getExited().teleport(session.seatExit);
            session.seatExit = null;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SURVIVAL && event.getBlock().getType().name().endsWith("SHULKER_BOX")) {
            event.setDropItems(false);
            ItemStack stack = new ItemStack(event.getBlock().getType());
            BlockStateMeta blockStateMeta = (BlockStateMeta) stack.getItemMeta();
            String customName = ((ShulkerBox) event.getBlock().getState()).getCustomName();
            if (customName != null && !customName.isEmpty()) {
                blockStateMeta.setDisplayName(customName);
            }
            ShulkerBox blockState = (ShulkerBox) blockStateMeta.getBlockState();
            blockState.getInventory().setContents(((ShulkerBox) event.getBlock().getState()).getInventory().getContents());
            blockStateMeta.setBlockState(blockState);
            stack.setItemMeta(blockStateMeta);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), stack);
        }

        // Exit a sitting player if the block below them is broken
        for (FLPlayerSession session : FarLands.getDataHandler().getSessions()) {
            Location location = event.getBlock().getLocation();
            if (session.seatExit != null && session.seatExit.getBlockX() == location.getBlockX()
                && session.seatExit.getBlockY() == location.add(0, 1, 0).getBlockY()
                && session.seatExit.getBlockZ() == location.getBlockZ()) {
                session.player.getVehicle().eject();
                session.player.getVehicle().remove();
                //session.player.teleport(session.seatExit);
                // ^ throws error, not sure why but we don't need it since they're falling anyway
                session.seatExit = null;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockDropItems(BlockDropItemEvent event) {
        if (event.getBlockState() instanceof Beehive && !event.getItems().isEmpty()) {
            int beeCount = ((Beehive) event.getBlockState()).getEntityCount();
            Item itemEntity = event.getItems().get(0);
            ItemStack hiveStack = itemEntity.getItemStack();
            ItemMeta meta = hiveStack.getItemMeta();
            meta.lore(List.of(ComponentColor.gold("Bee Count: ").append(ComponentColor.aqua(beeCount + ""))));
            hiveStack.setItemMeta(meta);
            itemEntity.setItemStack(hiveStack);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String[] lines = event.lines().stream().map(ComponentUtils::toText).toArray(String[]::new);
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer());
        for (int i = 0; i < lines.length; ++i) {
            event.line(i, ComponentUtils.parse(lines[i], flp));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return; // Ignore offhand packet
        }

        if (Rank.getRank(event.getPlayer()).specialCompareTo(Rank.SPONSOR) >= 0 && (event.getAction() == Action.RIGHT_CLICK_BLOCK ||
                                                                                    event.getAction() == Action.RIGHT_CLICK_AIR)) {
            ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
            if (CommandKittyCannon.CANNON.isSimilar(stack)) {
                CommandKittyCannon.fireCannon(event.getPlayer());
            }
        }

        if (event.getClickedBlock() == null || event.isCancelled()) {
            return;
        }

        // Pick up dragon egg
        Player player = event.getPlayer();
        if (Material.DRAGON_EGG == event.getClickedBlock().getType()) {
            FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(event.getClickedBlock().getLocation());
            event.setCancelled(true);
            if (
                flags == null || flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(event.getPlayer(), TrustLevel.BUILD, flags) ||
                event.getPlayer().isOp() && flags.<EnumFilter.MaterialFilter>getFlagMeta(RegionFlag.DENY_BREAK).isBlocked(Material.DRAGON_EGG)
            ) {
                event.getClickedBlock().setType(Material.AIR);
                FLUtils.giveItem(event.getPlayer(), new ItemStack(Material.DRAGON_EGG), false);
                event.getPlayer().playSound(event.getClickedBlock().getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
            }
            return;
        }

        // Tell players where a portal links
        if (player.isSneaking() && event.getAction() == Action.RIGHT_CLICK_BLOCK &&
            event.getClickedBlock().getType() == Material.NETHER_PORTAL) {
            Location location = event.getClickedBlock().getLocation();
            player.sendMessage(
                ComponentColor.darkPurple(
                    "This portal best links to %s in the %s.",
                    location.getWorld().getName().equals("world") ?
                        (location.getBlockX() >> 3) + " " + location.getBlockY() + " " + (location.getBlockZ() >> 3) :          // x / 8
                        (location.getBlockX() << 3) + "(+7) " + location.getBlockY() + " " + (location.getBlockZ() << 3) + "(+7)",  // x * 8
                    location.getWorld().getName().equals("world") ? "Nether" : "Overworld"
                )
            );
            return;
        }

        // If a player right-clicks the ground with a rocket and is wearing an elytra, use the rocket to launch them
        // into the air
        ItemStack chestplate = player.getInventory().getChestplate();
        if (GameMode.SPECTATOR != player.getGameMode() &&
            Material.FIREWORK_ROCKET == event.getMaterial() && Action.RIGHT_CLICK_BLOCK == event.getAction() &&
            Material.ELYTRA == (chestplate == null ? null : chestplate.getType()) && !player.isGliding()) {
            event.setCancelled(true);
            if (GameMode.CREATIVE != player.getGameMode()) {
                PlayerInventory inv = player.getInventory();
                ItemStack hand = inv.getItemInMainHand();
                if (hand.getAmount() == 1) {
                    inv.setItemInMainHand(null);
                } else {
                    hand.setAmount(hand.getAmount() - 1);
                }
            }
            Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
            firework.addPassenger(player);
            fireworkLaunches.put(firework.getUniqueId(), player);
            return;
        }

        // Tell players how many bees are in a hive
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getState() instanceof Beehive) {
            int beeCount = ((Beehive) event.getClickedBlock().getState()).getEntityCount();
            player.sendMessage(
                ComponentColor.gold("There %s ", beeCount == 1 ? "is" : "are")
                    .append(ComponentColor.aqua(beeCount + ""))
                    .append(ComponentColor.gold(" bee%s in this hive.", beeCount == 1 ? "" : "s"))
            );
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity ent = event.getRightClicked();
        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (EntityType.VILLAGER == event.getRightClicked().getType() && GameMode.CREATIVE == event.getPlayer().getGameMode() &&
            Material.BLAZE_ROD == hand.getType() && Rank.getRank(event.getPlayer()).isStaff()) {
            event.setCancelled(true);
            FarLands.getDataHandler().getPluginData().addSpawnTrader(ent.getUniqueId());
            (new GuiVillagerEditor((CraftVillager) ent)).openGui(event.getPlayer());
        } else if (LEASHABLE_ENTITIES.contains(event.getRightClicked().getType()) && !FLUtils.isInSpawn(event.getRightClicked().getLocation()) &&
                   event.getRightClicked() instanceof LivingEntity) {
            final LivingEntity entity = (LivingEntity) ent;
            if (Material.LEAD == hand.getType()) {
                if (entity.isLeashed()) {
                    return;
                }
                event.setCancelled(true); // Don't open any GUIs

                // Prevent double lead usage with nitwits
                if (leashedEntities.contains(entity.getUniqueId())) {
                    return;
                }
                leashedEntities.add(entity.getUniqueId());
                FarLands.getScheduler().scheduleSyncDelayedTask(() -> leashedEntities.remove(entity.getUniqueId()), 5L);


                if (hand.getAmount() > 1) {
                    hand.setAmount(hand.getAmount() - 1);
                } else {
                    hand.setAmount(0);
                    hand.setType(Material.AIR);
                }
                Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> entity.setLeashHolder(event.getPlayer()));
            } else if (entity.isLeashed()) {
                event.setCancelled(true);
                entity.setLeashHolder(null);
                entity.getWorld().dropItem(entity.getLocation(), new ItemStack(Material.LEAD));
            }
        } else if (FarLands.getDataHandler().getPluginData().isSpawnTrader(event.getRightClicked().getUniqueId())) {
            event.setCancelled(true);
            Villager trader = (Villager) event.getRightClicked();
            Villager dupe = (Villager) trader.getWorld().spawnEntity(new Location(trader.getWorld(), 0, 0, 0), EntityType.VILLAGER);

            // There's got to be a better way to do this
            dupe.customName(trader.customName());
            dupe.setVillagerLevel(trader.getVillagerLevel());
            dupe.setVillagerExperience(trader.getVillagerExperience());
            dupe.setProfession(trader.getProfession());
            dupe.setReputations(trader.getReputations());
            dupe.setRestocksToday(trader.getRestocksToday());
            dupe.setRecipes(trader.getRecipes());

            event.getPlayer().openMerchant(dupe, true);
        } else if (ent instanceof Tameable pet) {
            if (!(pet.isTamed() && pet.getOwner() != null && (event.getPlayer().getUniqueId().equals(pet.getOwner().getUniqueId()) ||
                                                              Rank.getRank(event.getPlayer()).isStaff()))) {
                return;
            }

            Player petRecipient = FarLands.getDataHandler().getSession(event.getPlayer()).givePetRecipient.getValue();
            FarLands.getDataHandler().getSession(event.getPlayer()).givePetRecipient.discard();
            if (petRecipient == null) {
                return;
            }
            if (FarLands.getDataHandler().getOfflineFLPlayer(petRecipient).getIgnoreStatus(event.getPlayer()).includesPackages()) {
                pet.remove(); // fake the pet teleporting
            }

            pet.setOwner(petRecipient);
            event.getPlayer().sendMessage("Successfully transferred pet to " + petRecipient.getName());
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            updateNightSkip(false);
        }

        // Teleport Leashed entities
        event.getPlayer().getNearbyEntities(10.0, 10.0, 10.0).stream()
            .filter(e -> e instanceof LivingEntity)
            .map(e -> (LivingEntity) e)
            .filter(e -> e.isLeashed() && event.getPlayer().equals(e.getLeashHolder()))
            .forEach(e -> {
                e.setLeashHolder(null);
                final boolean persistent = FLUtils.isPersistent(e);
                FLUtils.setPersistent(e, true);
                Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    e.getLocation().getChunk().load();
                    Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                        e.teleport(event.getTo());
                        e.setLeashHolder(event.getPlayer());
                        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> FLUtils.setPersistent(e, persistent), 1);
                    }, 1);
                }, 1);
            });
    }

    // Send items from the end to the correct location
    @EventHandler(ignoreCancelled = true)
    public void onEntityTeleport(EntityPortalEvent event) {
        if (
            event.getEntityType() != EntityType.PLAYER &&
            "world_the_end".equals(event.getFrom().getWorld().getName()) &&
            "world".equals(event.getTo().getWorld().getName())
        ) {
            Location end = new Location(event.getFrom().getWorld(), 0, 58, 0);
            double minDist = 576, // (24 * 24) only players close to 0 0
                dist;
            Player player = null;
            for (Player player1 : event.getFrom().getWorld().getPlayers()) {
                if ((dist = end.distanceSquared(player1.getLocation())) < minDist) {
                    minDist = dist;
                    player = player1;
                }
            }
            if (player != null && player.getBedSpawnLocation() != null) {
                event.setTo(player.getBedSpawnLocation());
            } else {
                event.setTo(FarLands.getDataHandler().getPluginData().spawn.asLocation());
            }
        }
    }

    @EventHandler
    public void onFireworkExplode(FireworkExplodeEvent event) {
        if (fireworkLaunches.containsKey(event.getEntity().getUniqueId())) {
            Player player = fireworkLaunches.get(event.getEntity().getUniqueId());
            if (player.isValid()  && !Worlds.FARLANDS.matches(player.getWorld())) {
                player.setGliding(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        updateNightSkip(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            FLPlayerSession session = FarLands.getDataHandler().getSession(player);
            session.unsit();
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL && session.fallDamageImmune) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        switch (event.getEntityType()) {
            case ENDER_DRAGON -> {
                event.setDroppedExp(4000);
                Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    Block block = event.getEntity().getWorld().getBlockAt(0, 75, 0);
                    block.setType(Material.DRAGON_EGG);
                    block.getWorld().getNearbyPlayers(block.getLocation(), 50, 50, 50)
                        .forEach(e -> e.sendMessage(ComponentColor.gray("As the dragon dies, an egg forms below.")));
                }, 15L * 20L);
            }
            case VILLAGER -> FarLands.getDataHandler().getPluginData().removeSpawnTrader(event.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(event.getEntity().getLocation());
        if (flags != null && flags.isAdminOwned() && !(flags instanceof WorldData)) {
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () ->
                Bukkit.getOfflinePlayer(event.getEntity().getUniqueId()).decrementStatistic(Statistic.DEATHS), 5L);
        }

        Player player = event.getEntity();
        Player killer = event.getEntity().getKiller();

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            FarLands.getDataHandler().getOfflineFLPlayer(player).updateDeaths();
        }, 10L);
        // Drop a player skull with lore when a player is killed by another player
        if (killer != null && killer != player) {
            ItemStack skull = SkullCreator.skullFromUuid(player.getUniqueId());
            ItemMeta meta = skull.getItemMeta();
            meta.displayName(ComponentColor.red(player.getName() + "'s Head"));
            Component message = event.deathMessage();
            if (message != null) {
                meta.lore(List.of(message));
            }
            skull.setItemMeta(meta);
            killer.getWorld().dropItemNaturally(player.getLocation(), skull);
        }
    }

    @EventHandler
    public void onAnvilPrepared(PrepareAnvilEvent event) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getView().getPlayer());
        if (flp.rank.specialCompareTo(Rank.SPONSOR) >= 0) {
            ItemStack result = event.getResult();
            if (result != null) {
                ItemMeta meta = result.getItemMeta();
                String rawName = ComponentUtils.toText(meta.displayName());
                if (rawName.isEmpty()) return;
                meta.displayName(ComponentUtils.parse(rawName, flp));
                result.setItemMeta(meta);
            }
        }
    }

    private void updateNightSkip(boolean sendBroadcast) {
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            int dayTime = (int) (Worlds.OVERWORLD.getWorld().getTime() % 24000);
            if (12541 > dayTime || dayTime > 23458) {
                return;
            }

            if (nightSkipTask != null && !Bukkit.getScheduler().isCurrentlyRunning(nightSkipTask.getTaskId())) {
                nightSkipTask = null;
            }

            List<Player> online = Bukkit.getOnlinePlayers().stream()
                .filter(player -> "world".equals(player.getWorld().getName()))
                .map(player -> (Player) player)
                .filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).vanished)
                .filter(player -> !FarLands.getDataHandler().getSession(player).afk)
                .toList();

            int sleeping = (int) online.stream().filter(Player::isSleeping).count();
            if (sleeping == 0) {
                return;
            }

            int required = (online.size() + 1) / 2;
            if (sleeping < required) {
                if (sendBroadcast && nightSkip.isComplete()) {
                    nightSkip.reset();
                    Logging.broadcastFormatted(
                        "<!bold><gold>%s more %s to sleep to skip the night.",
                        false,
                        (required - sleeping) + "",
                        (required - sleeping) == 1 ? "player need" : "players needs");
                }
            } else if (nightSkipTask == null) {
                Logging.broadcastFormatted("<!bold><gold>Skipping the night...", false);
                nightSkipTask = Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    World world = Bukkit.getWorld("world");
                    world.setTime(1000L);
                    world.setStorm(false);
                }, 30L);
            }
        }, 2L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClaimStolen(ClaimStealEvent event) {
        TrustMeta trustMeta = event.getRegion().getAndCreateFlagMeta(RegionFlag.TRUST);
        Map<TrustLevel, List<UUID>> trustList = trustMeta.getTrustList();
        int trustedCount = (int) trustList.values().stream().map(List::size).mapToDouble(n -> n).sum();

        String trustListString = trustedCount == 0 ? "none" :
            "\n  Access: " + uuidsToStealInfo(trustList.get(TrustLevel.ACCESS)) +
            "\n  Container: " + uuidsToStealInfo(trustList.get(TrustLevel.CONTAINER)) +
            "\n  Build: " + uuidsToStealInfo(trustList.get(TrustLevel.BUILD)) +
            "\n  Management: " + uuidsToStealInfo(trustList.get(TrustLevel.MANAGEMENT)) +
            "\n  CoOwner: " + uuidsToStealInfo(event.getRegion().getCoOwners());
        Location location = event.getRegion().getMin();

        FarLands.getDebugger().echo(event.getNewOwner().getName() + " stole claim:" +
                                    "\nPrevious Owner: " + uuidsToStealInfo(Collections.singletonList(event.getPreviousOwner())) +
                                    "\nCoordinates: " + "x: " + location.getBlockX() + " y: " + location.getBlockY() + " z: " + location.getBlockZ() +
                                    "\nTrustlist: " + trustListString
        );
    }

    private String uuidsToStealInfo(List<UUID> uuids) {
        return uuids.size() > 0 ? uuids.stream().map(uuid -> {
            OfflineFLPlayer player = FarLands.getDataHandler().getOfflineFLPlayer(uuid);
            if (player == null) {
                return "Unknown Player";
            }
            return player.username + "(Last Seen: " + TimeInterval.formatTime(System.currentTimeMillis() -
                                                                              player.getLastLogin(), true, TimeInterval.DAY) + ")";
        }).collect(Collectors.joining(", ")) : "none";
    }

    @EventHandler(ignoreCancelled = true)
    public void onClaimAbandoned(ClaimAbandonEvent event) {
        if (event.getRegions().size() == 0) {
            return;
        }

        switch (event.getRegions().size()) {
            case 0:
                return;
            case 1:
                Region region = event.getRegions().get(0);
                Location min = region.getMin();
                Location max = region.getMax();
                FarLands.getDebugger().echo(
                    String.format(
                        "%s abandoned " + (event.isAll() ? "all claims" : "claim") + ": %s\nBounds: %s %s %s | %s %s %s\nRecently Stolen: %b",
                        event.getPlayer().getName(),
                        region.getDisplayName(),
                        min.getBlockX(),
                        min.getBlockY(),
                        min.getBlockZ(),
                        max.getBlockX(),
                        max.getBlockY(),
                        max.getBlockZ(),
                        region.isRecentlyStolen()
                    )
                );
                return;
            default:
                StringBuilder sb = new StringBuilder(event.getPlayer().getName() + " abandoned " + (event.isAll() ? "all " : "") + "claims:\nName | Coords | Stolen");
                event.getRegions().forEach(rg -> {
                    Location loc = rg.getMin();
                    sb.append("\n")
                        .append(rg.getDisplayName())
                        .append(" | ")
                        .append(loc.getBlockX()).append(" ")
                        .append(loc.getBlockY()).append(" ")
                        .append(loc.getBlockZ()).append(" ")
                        .append(" | ")
                        .append(rg.isRecentlyStolen());
                });
                FarLands.getDebugger().echo(sb.toString());
        }
    }
}
