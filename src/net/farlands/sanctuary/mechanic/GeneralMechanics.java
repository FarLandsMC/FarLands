package net.farlands.sanctuary.mechanic;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import static com.kicas.rp.util.TextUtils.format;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.player.CommandKittyCannon;
import net.farlands.sanctuary.data.Cooldown;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.gui.GuiVillagerEditor;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.ReflectionHelper;
import net.farlands.sanctuary.util.FLUtils;

import net.md_5.bungee.api.chat.BaseComponent;

import net.minecraft.server.v1_16_R1.EntityTypes;
import net.minecraft.server.v1_16_R1.EntityVillager;
import net.minecraft.server.v1_16_R1.EntityVillagerAbstract;

import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.craftbukkit.v1_16_R1.CraftServer;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftVillager;
import org.bukkit.entity.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
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

public class GeneralMechanics extends Mechanic {
    private final Map<UUID, Player> fireworkLaunches;
    private BaseComponent[] joinMessage;

    private static final List<EntityType> LEASHABLE_ENTITIES = Arrays.asList(EntityType.SKELETON_HORSE,
            EntityType.VILLAGER, EntityType.TURTLE, EntityType.PANDA, EntityType.FOX);

    private Cooldown nightSkip;
    private List<UUID> leashedEntities;
    private BukkitTask nightSkipTask;

    public GeneralMechanics() {
        this.fireworkLaunches = new HashMap<>();
        this.joinMessage = new BaseComponent[0];
        this.nightSkip = new Cooldown(200L);
        this.leashedEntities = new ArrayList<>();
        this.nightSkipTask = null;
    }

    @Override
    public void onStartup() {
        try {
            joinMessage = format(FarLands.getDataHandler().getDataTextFile("join-message.txt"), FarLands.getFLConfig().discordInvite);
        } catch (IOException ex) {
            Logging.error("Failed to load join message!");
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), () ->
                Bukkit.getOnlinePlayers().forEach(player -> {
                    OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
                    if (flp.hasParticles() && !flp.vanished && GameMode.SPECTATOR != player.getGameMode())
                        flp.particles.spawn(player);
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

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        player.spigot().sendMessage(joinMessage);

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () ->
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 0.6929134F), 45L);
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () ->
                player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 0.85F, 1.480315F), 95L);
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 0.5F);
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
            if (!flp.viewedPatchnotes)
                sendFormatted(player,"&(gold)Patch {&(aqua)#%0} has been released! View changes with " +
                        "$(hovercmd,/patchnotes,{&(gray)Click to Run},&(aqua)/patchnotes)", FarLands.getDataHandler().getCurrentPatch());
        }, 125L);

        if (isNew) {
            Logging.broadcast(p -> {
                Player pl = p.handle.getOnlinePlayer();
                if (!player.getUniqueId().equals(p.handle.uuid)) {
                    if (pl != null)
                        pl.playSound(pl.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5.0F, 1.0F);
                    return true;
                } else
                    return false;
            }, "&(gold){&(bold)>} Welcome {&(green)%0} to FarLands!", player.getName());
            player.chat("/chain {guidebook} {shovel}");
            sendFormatted(player, "&(gold)Welcome to FarLands! Please read $(hovercmd,/rules,&(aqua)Click " +
                    "to view the server rules.,&(aqua)our rules) before playing. To get started, you can use " +
                    "$(hovercmd,/wild,&(aqua)Click to go to a random location.,&(aqua)/wild) to teleport to a " +
                    "random location on the map. Also, feel free to join our community on discord by clicking " +
                    "$(link,%0,&(aqua)here.)", FarLands.getFLConfig().discordInvite);

            Rank rank = Rank.getRank(player);
            if (rank == Rank.PATRON || rank == Rank.SPONSOR)
                FLUtils.giveItem(player, FarLands.getFLConfig().patronCollectable.getStack(), false);
        }

        if ("world".equals(player.getWorld().getName()))
            updateNightSkip(true);
    }

    @Override
    public void onPlayerQuit(Player player) {
        Location exit = FarLands.getDataHandler().getSession(player).seatExit;
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            vehicle.eject();
            vehicle.remove();
        }
        if (exit != null) {
            player.teleport(exit);
            FarLands.getDataHandler().getSession(player).seatExit = null;
        }
        updateNightSkip(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SURVIVAL && event.getBlock().getType().name().endsWith("SHULKER_BOX")) {
            event.setDropItems(false);
            ItemStack stack = new ItemStack(event.getBlock().getType());
            BlockStateMeta blockStateMeta = (BlockStateMeta) stack.getItemMeta();
            String customName = ((ShulkerBox)event.getBlock().getState()).getCustomName();
            if (customName != null && !customName.isEmpty())
                blockStateMeta.setDisplayName(customName);
            ShulkerBox blockState = (ShulkerBox) blockStateMeta.getBlockState();
            blockState.getInventory().setContents(((ShulkerBox) event.getBlock().getState()).getInventory().getContents());
            blockStateMeta.setBlockState(blockState);
            stack.setItemMeta(blockStateMeta);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), stack);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockDropItems(BlockDropItemEvent event) {
        if (event.getBlockState() instanceof Beehive && !event.getItems().isEmpty()) {
            int beeCount = ((Beehive) event.getBlockState()).getEntityCount();
            Item itemEntity = event.getItems().get(0);
            ItemStack hiveStack = itemEntity.getItemStack();
            ItemMeta meta = hiveStack.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add("Bee count: " + beeCount);
            meta.setLore(lore);
            hiveStack.setItemMeta(meta);
            itemEntity.setItemStack(hiveStack);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String[] lines = event.getLines();
        for (int i = 0; i < lines.length; ++i)
            event.setLine(i, Chat.applyColorCodes(Rank.getRank(event.getPlayer()), lines[i]));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND)
            return; // Ignore offhand packet

        if (Rank.getRank(event.getPlayer()).specialCompareTo(Rank.SPONSOR) >= 0 && (event.getAction() == Action.RIGHT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_AIR)) {
            ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
            if (CommandKittyCannon.CANNON.isSimilar(stack))
                CommandKittyCannon.fireCannon(event.getPlayer());
        }

        if (event.getClickedBlock() == null || event.isCancelled())
            return;

        // Pick up dragon egg
        Player player = event.getPlayer();
        if (Material.DRAGON_EGG == event.getClickedBlock().getType()) {
            event.setCancelled(true);
            event.getClickedBlock().setType(Material.AIR);
            FLUtils.giveItem(event.getPlayer(), new ItemStack(Material.DRAGON_EGG), false);
            event.getPlayer().playSound(event.getClickedBlock().getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
            return;
        }

        // Tell players where a portal links
        if (player.isSneaking() && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.NETHER_PORTAL) {
            Location location = event.getClickedBlock().getLocation();
            sendFormatted(player, "&(dark_purple)This portal best links to %0 in the %1.",
                    location.getWorld().getName().equals("world") ?
                    (location.getBlockX() >> 3) + " "      + location.getBlockY() + " " + (location.getBlockZ() >> 3) :           // x / 8
                    (location.getBlockX() << 3) + "(+15) " + location.getBlockY() + " " + (location.getBlockZ() << 3) + "(+15)",  // x * 8
                    location.getWorld().getName().equals("world") ? "Nether" : "Overworld");
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
                if (hand.getAmount() == 1)
                    inv.setItemInMainHand(null);
                else
                    hand.setAmount(hand.getAmount() - 1);
            }
            Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
            firework.addPassenger(player);
            fireworkLaunches.put(firework.getUniqueId(), player);
            return;
        }

        // Tell players how many bees are in a hive
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getState() instanceof Beehive) {
            int beeCount = ((Beehive) event.getClickedBlock().getState()).getEntityCount();
            sendFormatted(
                    player,
                    "&(gold)There %0 {&(aqua)%1} $(inflect,noun,1,bee) in this hive.",
                    beeCount == 1 ? "is" : "are",
                    beeCount
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
                if (entity.isLeashed())
                    return;
                event.setCancelled(true); // Don't open any GUIs

                // Prevent double lead usage with nitwits
                if (leashedEntities.contains(entity.getUniqueId()))
                    return;
                leashedEntities.add(entity.getUniqueId());
                FarLands.getScheduler().scheduleSyncDelayedTask(() -> leashedEntities.remove(entity.getUniqueId()), 5L);


                if (hand.getAmount() > 1)
                    hand.setAmount(hand.getAmount() - 1);
                else {
                    hand.setAmount(0);
                    hand.setType(Material.AIR);
                }
                Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> entity.setLeashHolder(event.getPlayer()));
            } else if (entity.isLeashed()) {
                event.setCancelled(true);
                entity.setLeashHolder(null);
                Item item = (Item) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.DROPPED_ITEM);
                item.setItemStack(new ItemStack(Material.LEAD));
            }
        } else if (FarLands.getDataHandler().getPluginData().isSpawnTrader(event.getRightClicked().getUniqueId())) {
            event.setCancelled(true);
            EntityVillager handle = ((CraftVillager) event.getRightClicked()).getHandle(), duplicate = new EntityVillager(EntityTypes.VILLAGER, handle.world);
            duplicate.setPosition(0.0, 0.0, 0.0);
            duplicate.setCustomName(handle.getCustomName());
            duplicate.setVillagerData(handle.getVillagerData());
            ReflectionHelper.setFieldValue("trades", EntityVillagerAbstract.class, duplicate, FLUtils.copyRecipeList(handle.getOffers()));
            event.getPlayer().openMerchant(new CraftVillager((CraftServer) Bukkit.getServer(), duplicate), true);
        } else if (ent instanceof Tameable) {
            Tameable pet = (Tameable) ent;
            if (!(pet.isTamed() && pet.getOwner() != null && (event.getPlayer().getUniqueId().equals(pet.getOwner().getUniqueId()) ||
                    Rank.getRank(event.getPlayer()).isStaff())))
                return;

            Player petRecipient = FarLands.getDataHandler().getSession(event.getPlayer()).givePetRecipient.getValue();
            FarLands.getDataHandler().getSession(event.getPlayer()).givePetRecipient.discard();
            if (petRecipient == null)
                return;
            if (FarLands.getDataHandler().getOfflineFLPlayer(petRecipient).isIgnoring(event.getPlayer()))
                pet.remove(); // fake the pet teleporting

            pet.setOwner(petRecipient);
            event.getPlayer().sendMessage("Successfully transferred pet to " + petRecipient.getName());
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) { // Allow teleporting with leashed entities
        if (event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            event.getPlayer().getNearbyEntities(10.0, 10.0, 10.0).stream().filter(e -> e instanceof LivingEntity)
                    .map(e -> (LivingEntity) e).filter(e -> e.isLeashed() && event.getPlayer().equals(e.getLeashHolder())).forEach(e -> {
                e.setLeashHolder(null);
                Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    e.getLocation().getChunk().load();
                    Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                        e.teleport(event.getTo());
                        e.setLeashHolder(event.getPlayer());
                    }, 1);
                }, 1);
            });
        } else
            updateNightSkip(false);
    }

    // Send items from the end to the correct location
    @EventHandler(ignoreCancelled = true)
    public void onEntityTeleport(EntityPortalEvent event) {
        if (event.getEntity().getType() == EntityType.DROPPED_ITEM &&
                "world_the_end".equals(event.getFrom().getWorld().getName()) &&
                "world".equals(event.getTo().getWorld().getName())) {
            event.setTo(FarLands.getDataHandler().getPluginData().spawn.asLocation());
        }
    }

    @EventHandler
    public void onFireworkExplode(FireworkExplodeEvent event) {
        if (fireworkLaunches.containsKey(event.getEntity().getUniqueId())) {
            Player player = fireworkLaunches.get(event.getEntity().getUniqueId());
            if (player.isValid() && !"farlands".equals(player.getWorld().getName()))
                player.setGliding(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        updateNightSkip(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player))
            return;
        FLPlayerSession session = FarLands.getDataHandler().getSession((Player) event.getExited());
        if (session.seatExit != null) {
            event.setCancelled(true);
            event.getVehicle().eject();
            event.getVehicle().remove();
            event.getExited().teleport(session.seatExit);
            session.seatExit = null;
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        switch (event.getEntityType()) {
            case ENDER_DRAGON:
                event.setDroppedExp(4000);
                Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    Block block = event.getEntity().getWorld().getBlockAt(0, 75, 0);
                    block.setType(Material.DRAGON_EGG);
                    block.getWorld().getNearbyEntities(block.getLocation(), 50, 50, 50)
                            .forEach(e -> sendFormatted(e, "&(gray)As the dragon dies, an egg forms below."));
                }, 15L * 20L);
                break;
            case VILLAGER:
                FarLands.getDataHandler().getPluginData().removeSpawnTrader(event.getEntity().getUniqueId());
                break;
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(event.getEntity().getLocation());
        if (flags != null && flags.isAdminOwned()) {
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                Bukkit.getOfflinePlayer(event.getEntity().getUniqueId()).decrementStatistic(Statistic.DEATHS);
            }, 5L);
        }
    }

    @EventHandler
    public void onAnvilPrepared(PrepareAnvilEvent event) {
        Rank rank = Rank.getRank(event.getView().getPlayer());
        if (rank.specialCompareTo(Rank.SPONSOR) >= 0) {
            ItemStack result = event.getResult();
            if (result != null) {
                ItemMeta meta = result.getItemMeta();
                meta.setDisplayName(Chat.applyColorCodes(rank, meta.getDisplayName()));
                result.setItemMeta(meta);
            }
        }
    }

    private void updateNightSkip(boolean sendBroadcast) {
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            int dayTime = (int) (Bukkit.getWorld("world").getTime() % 24000);
            if (12541 > dayTime || dayTime > 23458)
                return;

            if (nightSkipTask != null && !Bukkit.getScheduler().isCurrentlyRunning(nightSkipTask.getTaskId()))
                nightSkipTask = null;

            List<Player> online = Bukkit.getOnlinePlayers().stream()
                    .filter(player -> "world".equals(player.getWorld().getName()))
                    .map(player -> (Player) player)
                    .filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).vanished)
                    .collect(Collectors.toList());
            int sleeping = (int) online.stream().filter(Player::isSleeping).count();
            if (sleeping == 0)
                return;

            int required = (online.size() + 1) / 2;
            if (sleeping < required) {
                if (sendBroadcast && nightSkip.isComplete()) {
                    nightSkip.reset();
                    Logging.broadcastFormatted("%0 &(gold)more $(inflect,noun,0,player) $(inflect,verb,0,need) " +
                            "to sleep to skip the night.", false, required - sleeping);
                }
            } else if (nightSkipTask == null) {
                Logging.broadcastFormatted("&(gold)Skipping the night...", false);
                nightSkipTask = Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    World world = Bukkit.getWorld("world");
                    world.setTime(1000L);
                    world.setStorm(false);
                }, 30L);
            }
        }, 2L);
    }
}
