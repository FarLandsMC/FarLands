package net.farlands.sanctuary.mechanic;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
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
import com.kicasmads.cs.ChestShops;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent.ItemFrameChangeAction;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.FLShutdownEvent;
import net.farlands.sanctuary.command.player.CommandKittyCannon;
import net.farlands.sanctuary.data.Cooldown;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.data.pdc.JSONDataType;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.SkullCreator;
import net.farlands.sanctuary.gui.GuiVillagerEditor;
import net.farlands.sanctuary.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.type.Vault;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * General mechanics that don't fit into other categories
 */
public class GeneralMechanics extends Mechanic {

    private final Map<UUID, Player> fireworkLaunches;
    private final TimedSet<UUID>    recentCrossDimTp;
    private       Component         joinMessage;

    private static final List<EntityType> LEASHABLE_ENTITIES = List.of(
        EntityType.SKELETON_HORSE,
        EntityType.VILLAGER,
        EntityType.TURTLE,
        EntityType.PANDA,
        EntityType.FOX
    );

    private static final Set<Material> DRAGON_EGG_BREAKABLE = Set.of(
        Material.BEDROCK,
        Material.END_PORTAL_FRAME
    );

    private final Cooldown   nightSkip;
    private final List<UUID> leashedEntities;
    private       BukkitTask nightSkipTask;

    // Recently punished players (since server restart)
    public static final List<OfflineFLPlayer> recentlyPunished = new ArrayList<>();

    public GeneralMechanics() {
        this.fireworkLaunches = new HashMap<>();
        this.recentCrossDimTp = new TimedSet<>(20L);
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
            Bukkit.getWorld("world").getEntities()
                .stream()
                .filter(e -> EntityType.ITEM == e.getType())
                .map(e -> (Item) e)
                .filter(e ->
                            Material.SLIME_BALL == e.getItemStack().getType()
                            && e.isValid()
                            && !e.getScoreboardTags().contains("chestShopDisplay")
                            && e.getLocation().getChunk().isSlimeChunk()
                )
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
                    ComponentColor.gold(
                        "Patch {:aqua} has been released! View changes with {}",
                        "#" + FarLands.getDataHandler().getCurrentPatch(),
                        ComponentUtils.command("/patchnotes")
                    )
                );
            }
            if (flp.birthday != null && flp.birthday.isToday()) {
                player.sendMessage(ComponentColor.gold("Happy Birthday!"));
            }
            flp.updateDeaths(); // Just to be sure that deaths count is accurate
        }, 125L);

        if (isNew) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
            if (flp.vanished) {
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
                    ComponentColor.gold(
                        "Welcome to FarLands! Please read {} before playing.  To get started, you can " +
                        "use {} to teleport to a random location on the map.  Also feel free to join our " +
                        "community on Discord by clicking {}!",
                        ComponentUtils.command("/rules"),
                        ComponentUtils.command("/wild"),
                        ComponentUtils.link("here", FarLands.getFLConfig().discordInvite)
                    )
                );
            }

            flp.giveCollectables(Rank.INITIATE, flp.rank);
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

    /**
     * Make an item frame invis if it gets hit with a splash potion of invis and it has an item in it.
     */
    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        // Check if the potion is invis
        boolean isInvis = event.getPotion()
            .getEffects()
            .stream()
            .anyMatch(pe -> pe.getType().equals(PotionEffectType.INVISIBILITY));

        if (isInvis) {
            event.getEntity()
                // Numbers for range from https://minecraft.fandom.com/wiki/Splash_Potion#Using
                .getNearbyEntities(8.25 / 2, 4.25 / 2, 8.25 / 2)
                .stream()
                .filter(e -> e instanceof ItemFrame) // if it's an item frame
                .map(e -> (ItemFrame) e)
                .filter(f -> f.getItem().getType() != Material.AIR) // and if it has an item in them
                .forEach(f -> f.setVisible(false)); // Then set it invisible
        }
    }

    /**
     * Make item frames visible when an item is removed from them
     */
    @EventHandler
    public void onItemFrameChange(PlayerItemFrameChangeEvent event) {
        if (event.getAction() == ItemFrameChangeAction.REMOVE) {
            event.getItemFrame().setVisible(true);
        }
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
            Component name = ((ShulkerBox) event.getBlock().getState()).customName();
            if (name != null) {
                blockStateMeta.displayName(name);
            }
            ShulkerBox blockState = (ShulkerBox) blockStateMeta.getBlockState();
            blockState.getInventory().setContents(((ShulkerBox) event.getBlock().getState()).getInventory().getContents());
            blockStateMeta.setBlockState(blockState);
            stack.setItemMeta(blockStateMeta);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), stack);
        }

        if (
            event.getPlayer().getActiveItem().containsEnchantment(Enchantment.SILK_TOUCH)
            && event.getBlock().getType().name().startsWith("SUSPICIOUS_")
        ) {
            event.setDropItems(true);
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
            meta.lore(List.of(ComponentColor.gold("Bee Count: {:aqua}", beeCount).decoration(TextDecoration.ITALIC, false)));
            hiveStack.setItemMeta(meta);
            itemEntity.setItemStack(hiveStack);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String[] lines = event.lines().stream().map(ComponentUtils::toText).toArray(String[]::new);

        // Save the raw style such that we can restore it when editing
        Sign state = (Sign) event.getBlock().getState();
        PersistentDataContainer pdc = state.getPersistentDataContainer();
        pdc.set(
            FLUtils.nsKey(event.getSide().toString().toLowerCase() + "_raw"),
            new JSONDataType<>(String[].class),
            lines
        );
        state.update();

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

        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(
            event.getClickedBlock() == null
                ? event.getPlayer().getLocation()
                : event.getClickedBlock().getLocation()
        );
        TrustMeta trust = flags == null ? null : flags.getFlagMeta(RegionFlag.TRUST);

        if ( // Change the shape of a rail by shift + right click
            event.getPlayer().isSneaking()
            && event.getItem() == null
            && event.getClickedBlock() != null
            && event.getClickedBlock().getBlockData() instanceof Rail block
            && trust != null
            && trust.hasTrust(event.getPlayer(), TrustLevel.BUILD, flags)
        ) {
            Rail.Shape shape = block.getShape();
            Rail.Shape nextShape;
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                nextShape = switch (shape) {
                    case ASCENDING_NORTH, ASCENDING_SOUTH -> Rail.Shape.NORTH_SOUTH;
                    case ASCENDING_EAST, ASCENDING_WEST -> Rail.Shape.EAST_WEST;
                    default -> {
                        List<Rail.Shape> valid = block.getShapes() // ImmutableCollections should be deterministic in iteration, so we don't need to sort here.
                            .stream()
                            .filter(s -> !s.name().startsWith("ASCENDING_")) // We don't want them to toggle ascending because then it'd be an endless loop
                            .toList();

                        yield valid.get((valid.indexOf(shape) + 1) % valid.size());
                    }
                }
                ;
            } else {
                nextShape = switch (shape) {
                    case ASCENDING_NORTH, ASCENDING_SOUTH -> Rail.Shape.NORTH_SOUTH;
                    case ASCENDING_EAST, ASCENDING_WEST -> Rail.Shape.EAST_WEST;
                    default -> {
                        List<Rail.Shape> valid = block.getShapes() // ImmutableCollections should be deterministic in iteration, so we don't need to sort here.
                            .stream()
                            .filter(s -> !s.name().startsWith("ASCENDING_")) // We don't want them to toggle ascending because then it'd be an endless loop
                            .sorted(Collections.reverseOrder())
                            .toList();

                        yield valid.get((valid.indexOf(shape) + 1) % valid.size());
                    }
                };
            }
            block.setShape(nextShape); // Change the shape
            event.getClickedBlock().setBlockData(block);
        }

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer());
        if (
            flp.rank.specialCompareTo(Rank.SPONSOR) >= 0
            && event.getAction().isRightClick()
        ) {
            ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
            if (CommandKittyCannon.CANNON.isSimilar(stack)) {
                CommandKittyCannon.fireCannon(event.getPlayer());
            }
        }

        if (event.getClickedBlock() == null || event.isCancelled()) return;

        if (
            !event.getPlayer().isSneaking()
            && event.getClickedBlock().getState() instanceof org.bukkit.block.Vault vault
            && event.getPlayer().getLocation().distanceSquared(event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5)) <= 9
        ) {
            Block block = event.getClickedBlock();
            Vault vaultData = (Vault) block.getBlockData();
            boolean ominousVault = vaultData.isOminous();
            if (vaultData.getTrialSpawnerState() == Vault.State.INACTIVE) {
                PersistentDataContainer pdc = vault.getPersistentDataContainer();
                Long l = pdc.get(FLUtils.nsKey("last_opened"), PersistentDataType.LONG);
                var needed = (ominousVault ? 10 : 5) * 60 * 1000; // 10 minutes for ominous, 5 for normal
                if (l != null) {
                    var elapsed = new Date().getTime() - l;
                    if (needed > elapsed) {
                        event.getPlayer().sendMessage(
                            ComponentColor.red(
                                "You can use this vault in {}.",
                                needed - elapsed < 1000
                                    ? "a moment"
                                    : TimeInterval.formatTimeComponent(needed - elapsed, false)
                            )
                        );
                        event.setCancelled(true);
                        return;
                    }
                }
                NamespacedKey reward_loot = NamespacedKey.minecraft("chests/trial_chambers/%s".formatted(ominousVault ? "reward_ominous" : "reward"));
                Location loc = block.getLocation();
                block.setType(Material.STONE);
                // Note: the reason we are using setblock rather than doing it properly is that there is no API to change these vaules at the moment.
                String cmd = "setblock %d %d %d minecraft:vault[facing=\"%s\",ominous=\"%s\"]{config:{key_item:{count:1,id:\"%s\"},loot_table:\"%s\"}} replace"
                    .formatted(
                        loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                        vaultData.getFacing().name().toLowerCase(),
                        ominousVault,
                        (ominousVault ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY).getKey(),
                        reward_loot
                    );
                Bukkit.dispatchCommand(Bukkit.createCommandSender(c -> {}), cmd);
                event.getPlayer().sendMessage(ComponentColor.green("Vault reset."));
                event.setCancelled(true);
            }

            boolean ominousKey = false;

            if (
                event.getItem() != null
                && (event.getItem().getType() == Material.TRIAL_KEY || (ominousKey = event.getItem().getType() == Material.OMINOUS_TRIAL_KEY))
                && ominousVault == ominousKey
            ) {
                Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
                    if (block.getState() instanceof org.bukkit.block.Vault vault2) {
                        PersistentDataContainer pdc2 = vault2.getPersistentDataContainer();
                        pdc2.set(
                            FLUtils.nsKey("last_opened"),
                            PersistentDataType.LONG,
                            new Date().getTime()
                        );
                        vault2.update();
                    }
                });
            }
        }

        if ( // Unwax a sign by shift + right click with an axe
            event.getPlayer().isSneaking()
            && event.getItem() != null
            && event.getItem().getType().name().endsWith("_AXE")
            && event.getClickedBlock().getState() instanceof Sign sign
            && sign.isWaxed()
            && ChestShops.getDataHandler().getShop(sign.getLocation()) == null
            && trust != null
            && trust.hasTrust(event.getPlayer(), TrustLevel.BUILD, flags)
        ) {
            sign.setWaxed(false);
            sign.update(true);

            ItemUtils.damageItem(event.getItem(), 1); // Apply some damage to the axe
            event.getPlayer().playSound( // Play a nice sound
                                         event.getClickedBlock().getLocation(),
                                         Sound.ITEM_AXE_STRIP,
                                         SoundCategory.BLOCKS,
                                         .25f,
                                         2.0f
            );
            event.getClickedBlock()
                .getWorld()
                .playEffect(
                    event.getClickedBlock().getLocation(),
                    Effect.COPPER_WAX_OFF,
                    0
                );
            event.getPlayer().swingMainHand(); // Swing the hand to make it look natural
        }

        // Pick up dragon egg
        Player player = event.getPlayer();
        if (Material.DRAGON_EGG == event.getClickedBlock().getType()) {
            event.setCancelled(true);
            if (
                trust == null
                || trust.hasTrust(event.getPlayer(), TrustLevel.BUILD, flags)
                || event.getPlayer().isOp() && flags.<EnumFilter.MaterialFilter>getFlagMeta(RegionFlag.DENY_BREAK).isBlocked(Material.DRAGON_EGG)
            ) {
                event.getClickedBlock().setType(Material.AIR);
                ItemUtils.giveItem(event.getPlayer(), new ItemStack(Material.DRAGON_EGG), false);
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
                    "This portal best links to {} in the {}.",
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
        if (
            flp.fireworkLaunch
            && GameMode.SPECTATOR != player.getGameMode()
            && Material.FIREWORK_ROCKET == event.getMaterial() && Action.RIGHT_CLICK_BLOCK == event.getAction()
            && Material.ELYTRA == (chestplate == null ? null : chestplate.getType())
            && !player.isGliding()
        ) {
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
            Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK_ROCKET);
            firework.addPassenger(player);
            fireworkLaunches.put(firework.getUniqueId(), player);
            return;
        }

        // Tell players how many bees are in a hive
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getState() instanceof Beehive) {
            int beeCount = ((Beehive) event.getClickedBlock().getState()).getEntityCount();
            player.sendMessage(
                ComponentColor.gold(
                    "There {} {:aqua} bee{} in this hive.",
                    beeCount == 1 ? "is" : "are",
                    beeCount,
                    beeCount == 1 ? "" : "s"
                )
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
            new GuiVillagerEditor((Villager) ent).openGui(event.getPlayer());
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
            event.getPlayer().sendMessage(ComponentColor.green("Successfully transferred pet to {}.", petRecipient.getName()));
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            updateNightSkip(false);
        }

        if (
            !event.getFrom().getWorld().equals(event.getTo().getWorld())
            && !event.getCause().toString().endsWith("_PORTAL")
        ) {
            this.recentCrossDimTp.add(event.getPlayer().getUniqueId());
        }

        // Teleport Leashed entities
        event.getPlayer().getNearbyEntities(10.0, 10.0, 10.0).stream()
            .filter(e -> e instanceof LivingEntity)
            .map(e -> (LivingEntity) e)
            .filter(e -> e.isLeashed() && event.getPlayer().equals(e.getLeashHolder()))
            .forEach(e -> {
                e.setLeashHolder(null);
                final boolean persistent = e.isPersistent();
                e.setPersistent(true);
                Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    e.getLocation().getChunk().load();
                    Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                        e.teleport(event.getTo());
                        e.setLeashHolder(event.getPlayer());
                        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> e.setPersistent(persistent), 1);
                    }, 1);
                }, 1);
            });
    }

    // Send items from the end to the correct location
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityTeleport(EntityPortalEvent event) {
        if (
            event.getEntityType() != EntityType.PLAYER &&
            "world_the_end".equals(event.getFrom().getWorld().getName()) &&
            "world".equals(event.getTo().getWorld().getName())
        ) {
            if (event.getEntity().getPortalCooldown() > 0) return; // this is mostly just to prevent the event from being spammed
            Location end = new Location(event.getFrom().getWorld(), 0, 58, 0);
            double minDist = 576; // (24 * 24) only players close to 0 0
            double dist;
            Player player = null;
            for (Player player1 : event.getFrom().getWorld().getPlayers()) {
                if ((dist = end.distanceSquared(player1.getLocation())) < minDist) {
                    minDist = dist;
                    player = player1;
                }
            }
            if (player != null && player.getBedSpawnLocation() != null) {
                event.setTo(player.getBedSpawnLocation());
                player.sendMessage(
                    ComponentColor.gold(
                        "{} has been sent to your bed location.",
                        Component.translatable(event.getEntityType().translationKey()).hoverEvent(event.getEntity().asHoverEvent())
                    )
                );
            } else {
                event.setTo(FarLands.getDataHandler().getPluginData().spawn.asLocation());
            }
            event.getEntity().setPortalCooldown(300); // this is mostly just to prevent the event from being spammed
        }
    }

    @EventHandler
    public void onFireworkExplode(FireworkExplodeEvent event) {
        if (fireworkLaunches.containsKey(event.getEntity().getUniqueId())) {
            Player player = fireworkLaunches.get(event.getEntity().getUniqueId());
            if (player.isValid() && !Worlds.FARLANDS.matches(player.getWorld())) {
                player.setGliding(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        updateNightSkip(true);
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer());
        if (flp.homes.isEmpty()) {
            flp.addHome("home", event.getPlayer().getBedSpawnLocation());
            event.getPlayer().sendMessage(
                ComponentColor.gold(
                    "Your home has been set at this location, you can return to it with {}.",
                    ComponentUtils.command("/home")
                )
            );
        }
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
                if (event.getEntity().getWorld().getEnderDragonBattle().hasBeenPreviouslyKilled()) {
                    Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                        Block block = event.getEntity().getWorld().getBlockAt(0, 75, 0);
                        block.setType(Material.DRAGON_EGG);
                        block.getWorld().getNearbyPlayers(block.getLocation(), 50, 50, 50)
                            .forEach(e -> e.sendMessage(ComponentColor.gray("As the dragon dies, an egg forms below.")));
                    }, 15L * 20L);
                }
            }
            case ENDERMAN -> {
                // Make enderpearls dropped by endermen in the end despawn after 1 minute in order to reduce lag
                if (!Worlds.END.matches(event.getEntity().getWorld())) return;

                Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    event.getEntity()
                        .getNearbyEntities(1, 1, 1)
                        .stream()
                        .filter(
                            e -> e instanceof Item i
                                 && i.getItemStack().getType() == Material.ENDER_PEARL
                                 && i.getTicksLived() < 2 // it's a new pearl
                        )
                        .forEach(i -> i.setTicksLived(4 * 60 * 20)); // Item despawns once ticks lived gets to 5 * 60 * 20
                }, 1L);
            }
            case VILLAGER ->
                FarLands.getDataHandler().getPluginData().removeSpawnTrader(event.getEntity().getUniqueId());
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
            meta.displayName(ComponentColor.red("{}'s Head", player.getName()));
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
                        s -> s.player != null && Worlds.OVERWORLD.matches(s.player.getWorld()),
                        "<!bold><gold>%d more %s to sleep to skip the night.",
                        false,
                        (required - sleeping),
                        (required - sleeping) == 1 ? "player needs" : "players need");
                }
            } else if (nightSkipTask == null) {
                Logging.broadcastFormatted(
                    s -> s.player != null && Worlds.OVERWORLD.matches(s.player.getWorld()),
                    "<!bold><gold>Skipping the night...",
                    false
                );
                nightSkipTask = Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    World world = Worlds.OVERWORLD.getWorld();
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

    /**
     * Handle Dragon Egg breaking bedrock and stuff
     */
    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().isEmpty() || event.getBlocks().stream().noneMatch(b -> b.getPistonMoveReaction() == PistonMoveReaction.MOVE)) {
            Block blockAbove = event.getBlock().getRelative(event.getDirection()).getRelative(BlockFace.UP);
            if (blockAbove.getType() != Material.DRAGON_EGG) return;
            Block toBreak = blockAbove.getRelative(BlockFace.DOWN, 2);
            while (toBreak.getType().isAir() && toBreak.getY() >= toBreak.getWorld().getMinHeight()) {
                toBreak = toBreak.getRelative(BlockFace.DOWN);
            }
            if (
                DRAGON_EGG_BREAKABLE.contains(toBreak.getType())
                && !( // Exit end portal
                    Worlds.END.matches(toBreak.getWorld())
                    && Math.abs(toBreak.getY()) <= 5
                    && Math.abs(toBreak.getX()) <= 5
                )
                &&
                toBreak.getY() > toBreak.getWorld().getMinHeight()
            ) {
                toBreak.breakNaturally(true);
                blockAbove.setType(Material.AIR);
            }
        }
    }

    /**
     * Prevent teleporting from activating the 7km achievement
     */
    @EventHandler
    public void onAdvancementCriterionGrant(PlayerAdvancementCriterionGrantEvent event) {
        if (event.getAdvancement().getKey().equals(NamespacedKey.minecraft("nether/fast_travel"))) {
            if (this.recentCrossDimTp.contains(event.getPlayer().getUniqueId())) {
                FarLands.getDebugger().echo("Cancelled 7km nether achievement for " + event.getPlayer() + " due to teleportation.");
                event.setCancelled(true);
            }
        }
    }
}
