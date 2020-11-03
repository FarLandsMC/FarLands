package net.farlands.sanctuary.mechanic.region;

import com.kicas.rp.data.Region;
import com.kicas.rp.util.Entities;
import com.kicas.rp.util.Pair;

import static net.farlands.sanctuary.mechanic.region.AutumnEvent.skullItem;
import static net.farlands.sanctuary.util.FLUtils.RNG;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.FLShutdownEvent;
import net.farlands.sanctuary.data.Cooldown;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.mechanic.Chat;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.farlands.sanctuary.util.FLUtils;

import net.minecraft.server.v1_16_R3.Items;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagList;
import net.minecraft.server.v1_16_R3.NBTTagString;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutumnEvent extends Mechanic {

    private static final List<GraveZombie> GRAVE_ZOMBIES = Arrays.asList(
            new GraveZombie(new Location(FarLands.getWorld(), -277, 78, 653, 90, 0), "Prehistoric Glitch",
                    "f35042af-4da5-460d-8372-0f1f88d6ccb3",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYz" +
                            "RjMzlhNTAwMjJiYTc2OWM4OWYxNWUwODdjY2RhM2QzMjg4MDg3YmVhNzg2OTRhZTdlYWJiZWQ0OTYxYzJhNCJ9fX0=",
                    0x60A099, 0x2C2E30, 0xD8D9D7),
            new GraveZombie(new Location(FarLands.getWorld(), -312, 71, 680, 0, 0), "The Lost Battler",
                    "eee84000-5db1-4f87-b931-e7cb173a7986",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYj" +
                            "JhZmZlY2UzZWM3MTY5ZDljNzIyZjVmNThjZjk1ZDJmNTE2NDUwZmM5YmQ5YzZlMzA5NzNjMzc1ZGE2ODllMCJ9fX0=",
                    0x442921, 0x2E4C12, 0x541C1E),
            new GraveZombie(new Location(FarLands.getWorld(), -296, 70, 742, 270, 0), "Murderous Tendancies",
                    "efb263d6-eada-45f5-8255-da3583e8ea73",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZG" +
                            "FhZTYwNDA3N2I2M2Q0YzIwN2M0MGI4ODg4NDM3NWEyMzE2NjQ4MzY2YTBkZWFmMzQ5NGU2ZGYzYTNlMTAzNiJ9fX0=",
                    0x100D11, 0xC8C162, 0xCEB290),
            new GraveZombie(new Location(FarLands.getWorld(), -236, 75, 716, 0, 0), "Furry Frenzy",
                    "92012a69-ba91-486e-89b6-f391dc43bd2b",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYz" +
                            "JkYmExMmMyZTE5YjEyMmFiMDQ4ZjdkNGI1NDdmOWM2OWRhM2YzNjYxOGNkNWUzZDFlNWEzNmMyNzJkMWUzMyJ9fX0=",
                    0x544640, 0x4A423D, 0xB48650),
            new GraveZombie(new Location(FarLands.getWorld(), -200, 78, 731, 90, 0), "Split Personality",
                    "0b9ea35f-8049-4782-8c91-2bff0011aea9",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYT" +
                            "djMmFkZDg4YjU2MjIwZDk1YWIyYmExZjkyMjFhNTM1OTIxMjU2MDBlZTkwZTY0NzQyOThjY2YzYmRmYWEyNyJ9fX0=",
                    0x556866, 0x363740, 0xF4F5F0)
    );

    private final List<ItemStack> COLLECTABLES = Arrays.asList(
            generateCollectable("5ab54155-50af-4702-87d0-a10abcac6e79",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2" +
                            "U3MDhhOTI1ZTEwZjI5ZjIyMzJhOTFlNDExODM1Mzc2YTFkYjE1YzhmZDM5NmRiY2I5NWIzZGY5ZGIwNTViMSJ9fX0=",
                    ChatColor.GOLD + ChatColor.BOLD.toString() + "Jack Skellington", "Pumpkin Patch"),
            // https://minecraft-heads.com/custom-heads/decoration/39719-piglin-plushie
            generateCollectable("d3d3a6ce-ad08-449e-8de0-768649419917",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYW" +
                            "ZlYzkxYzBiN2U0YzM4NWM0MTk0OWY4OTVhMzgyODlkNmI1ZTQ3MWNhMzUxNjgwODQyZGY0MjQ1NDg5OGY2ZCJ9fX0=",
                    ChatColor.DARK_RED + ChatColor.BOLD.toString() + "Midas", "Netherscape"),
            generateCollectable("214a80cc-d4eb-418f-add7-279b77450379",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUv" +
                            "NzUyZTdjNjgyZjdhOGNmN2Q3MGNlYmY2MjY5NDI0YjhkN2YxOGQ1ZWJiMzliOTZmNTdkMGUyZDZkN2ZjYTUifX19",
                    ChatColor.AQUA + ChatColor.BOLD.toString() + "Herobrine", "Graveyard"),
            generateCollectable("e823683d-8312-49ad-923c-17f284b9c532",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMj" +
                            "c3YmM5ODBiNWY1NmE3N2M1YWEyNmFkODQ1OTcyOWRiMDdhYmI0NWU1ZmNmNTA3ZTExZjQ3MDAyMGQ1YjM1NyJ9fX0=",
                    ChatColor.YELLOW + ChatColor.BOLD.toString() + "Headless Horseman", "Autumn Woods"),
            generateCollectable("f2b738d9-df0b-4e8f-b4d4-fd29ec0a95af",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOG" +
                            "Q1NGNhYjg2NGE5ZWFjZDg0OWU5ZjFkM2Q3MTc2MzdlMTdkYzdkN2JiYTVkODk0ZGMzMmY2MWYzODdkNDk1NyJ9fX0=",
                    ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Baba Yaga", "Witch Swamp"),
            generateCollectable("e5fd653e-ad72-48aa-8063-e5f7809483d9",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNz" +
                            "A5MzkzNzNjYWZlNGIxZjUzOTdhYWZkMDlmM2JiMTY2M2U3YjYyOWE0MWE3NWZiZGMxODYwYjZiZjhiNDc1ZiJ9fX0=",
                    ChatColor.RED + ChatColor.BOLD.toString() + "Archane's Mount", "Spider Abyss"),
            // https://minecraft-heads.com/custom-heads/decoration/18031-wither-skeleton-plushie
            generateCollectable("3f4b57d5-bfe5-4fe8-be1f-01c1d7f7ca21",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2" +
                            "E3MGM1ZDA3Yjc5YzViZTQzZDBiMjFjNmQ0ZTM3NDcyZGIyYWIzZmE2MzRhNWYxM2NmODlmYjk4N2IyZTUifX19",
                    ChatColor.GRAY + ChatColor.BOLD.toString() + "Blight", "Soul Sand Valley"),
            // https://minecraft-heads.com/custom-heads/decoration/37730-ghast-plushie
            generateCollectable("e4866f95-9631-4a5b-9ee9-4b1c06a02885",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGU" +
                            "4YTM4ZTlhZmJkM2RhMTBkMTliNTc3YzU1YzdiZmQ2YjRmMmU0MDdlNDRkNDAxN2IyM2JlOTE2N2FiZmYwMiJ9fX0=",
                    ChatColor.DARK_AQUA + ChatColor.BOLD.toString() + "Specter", "Haunted Forest")
    );

    // approximation of event boundary to use for events
    private static final Location CENTER = new Location(FarLands.getWorld(), -426.5, 133.5, 801.5);
    private static final Location SPAWN = new Location(FarLands.getWorld(), -489.5, 86, 886.5, -90, 0);
    private static final List<Location> REGION_RESPAWNS = Arrays.asList(
            SPAWN,
            new Location(FarLands.getWorld(), -566,  89,  817, 135, 0), // Pumpkin Patch
            new Location(FarLands.getWorld(), -418, 105, 1036, 135, 0), // Netherscape
            new Location(FarLands.getWorld(), -305,  92,  730, 225, 0), // g-Raveyard
            new Location(FarLands.getWorld(), -421,  68,  703, 180, 0), // Fall Forest
            new Location(FarLands.getWorld(), -162,  72,  862,  90, 0), // Witch Village
            new Location(FarLands.getWorld(), -417,  63,  578, 225, 0), // Spider Abyss
            new Location(FarLands.getWorld(), -571,  74, 1112,  90, 0), // Soul Sand Valley
            new Location(FarLands.getWorld(), -281,  77, 1095, 340, 0)  // Haunted Forest
    );

    private static final Location DROPPER_START = new Location(FarLands.getWorld(), -472.5, 111, 883.5);
    private static final Map<UUID, Boolean> DROPPER_PLAYERS = new HashMap<>();
    private static final Region DROPPER = new Region(
            new Location(FarLands.getWorld(), -470,  49, 887),
            new Location(FarLands.getWorld(), -458, 110, 891)
    );

    private static final HashSet<CreatureSpawnEvent.SpawnReason> ALLOWED_SPAWNS = new HashSet<>(Arrays.asList(
            CreatureSpawnEvent.SpawnReason.DEFAULT, CreatureSpawnEvent.SpawnReason.SHOULDER_ENTITY,
            CreatureSpawnEvent.SpawnReason.SPAWNER_EGG, CreatureSpawnEvent.SpawnReason.CUSTOM
    ));
    private static final Map<Integer, List<Pack>> PACKS = new HashMap<>();
    private static final List<Door> DOORS = new ArrayList<>();
    private static final ItemStack CATAKEY = generateKeyItem("Catacombs", ChatColor.YELLOW);
    private static final ItemStack AIR = new ItemStack(Material.AIR, 0);

    private static final Map<UUID, Map<UUID, Double>> soulbound = new HashMap<>();
    private static final Map<String, Integer> tasks = new ConcurrentHashMap<>();
    private        final Map<UUID, Pair<Location, Integer>> PLAYER_RESPAWNS = new HashMap<>();

    private static final HashMap<String, Cooldown> cooldownBossSpawns = new HashMap<>();
    private static final HashMap<UUID, Long> ptimeStore = new HashMap<>();

    // doors
    static {
        // 0
        DOORS.add(new Door(
                new Location(FarLands.getWorld(), -640, 106, 878),
                new Location(FarLands.getWorld(), -640, 109, 882),
                new Location(FarLands.getWorld(), -623,   7,  888, 90, 0),
                generateKeyItem("Pumpkin", ChatColor.GOLD), 32, true
        ));
        ItemStack key = generateKeyItem("Nether", ChatColor.DARK_RED);
        // 1
        DOORS.add(new Door(
                new Location(FarLands.getWorld(), -391, 17, 1025),
                new Location(FarLands.getWorld(), -391, 29, 1043),
                new Location(FarLands.getWorld(), -377, 14, 1033, -64, 0),
                key, 13, false
        ));
        // 2
        DOORS.add(new Door(
                new Location(FarLands.getWorld(), -432, 38, 1039),
                new Location(FarLands.getWorld(), -432, 53, 1054),
                new Location(FarLands.getWorld(), -453, 25, 1028, -128, 0),
                key, 64, false, "To use this passage, you must be holding at least " + 64 +
                " * " + key.getItemMeta().getDisplayName() + ChatColor.RESET + " in your main hand. You will also need an additional " +
                DOORS.get(1).requiredKeys + " * " + key.getItemMeta().getDisplayName() + ChatColor.RESET + " to enter the boss room"
        ));
        // 3
        DOORS.add(new Door(
                new Location(FarLands.getWorld(), -253, 113, 610),
                new Location(FarLands.getWorld(), -249, 115, 610),
                new Location(FarLands.getWorld(), -240,  17, 719, 180, 0),
                generateKeyItem("Graveyard", ChatColor.AQUA), 32, true
        ));
        key = generateKeyItem("Autumn Forest", ChatColor.YELLOW);
        // 4
        DOORS.add(new Door(
                new Location(FarLands.getWorld(), -493, 64, 835),
                new Location(FarLands.getWorld(), -493, 68, 835),
                new Location(FarLands.getWorld(), -493, 64, 834, 180, 0),
                key, 8, false
        ));
        // 5
        DOORS.add(new Door(
                new Location(FarLands.getWorld(), -428, 66, 835),
                new Location(FarLands.getWorld(), -428, 70, 835),
                new Location(FarLands.getWorld(), -428, 66, 834, 180,   0),
                key, 8, false
        ));
        // 6
        DOORS.add(new Door(
                new Location(FarLands.getWorld(), -156, 74, 900),
                new Location(FarLands.getWorld(), -156, 75, 900),
                new Location(FarLands.getWorld(), -214,  5, 891, 90, 0),
                generateKeyItem("Witch Swamp", ChatColor.LIGHT_PURPLE), 16, true
        ));
        // 7
        DOORS.add(new Door(
                new Location(FarLands.getWorld(), -425, 26, 503),
                new Location(FarLands.getWorld(), -415, 28, 512),
                new Location(FarLands.getWorld(), -406, 22, 433, 35, 0),
                generateKeyItem("Spider Abyss", ChatColor.RED), 24, true
        ));
        // 8
        DOORS.add(new Door(
                new Location(FarLands.getWorld(), -635, 92, 1094),
                new Location(FarLands.getWorld(), -635, 99, 1099),
                new Location(FarLands.getWorld(), -621, 20, 1116, 145, 20),
                generateKeyItem("Soul Sand Valley", ChatColor.GRAY), 16, true
        ));
        // 9
        DOORS.add(new Door(
                new Location(FarLands.getWorld(), -337, 73, 1182),
                new Location(FarLands.getWorld(), -335, 77, 1185),
                new Location(FarLands.getWorld(), -323, 20, 1129, -160, 0),
                generateKeyItem("Haunted Forest", ChatColor.DARK_AQUA), 16, true
        ));
    }

    @Override
    public void onStartup() {
        FarLands.getMechanicHandler().getMechanic(Chat.class).addRotatingMessage("&(gold)There's a server event going on right now!" +
                " Type $(hovercmd,/party,{&(aqua)Click to run the command},{&(aqua)/party}) to warp to the Autumn event!");
        FarLands.getScheduler().scheduleSyncRepeatingTask(() -> {
            List<Integer> keys = new ArrayList<>();
            FarLands.getWorld().getPlayers().forEach(player -> {
                if (player.getGameMode() != GameMode.SURVIVAL)
                    return;

                // O(27n / 2) not O(n^4)
                for (int y = -16; y <= 16; y += 16)
                    for (int z = -16; z <= 16; z += 16)
                        for (int x = -16; x <= 16; x += 16) {
                            int key = generateKey(player.getLocation().clone().add(x, y, z));
                            if (!keys.contains(key))
                                keys.add(key);
                        }

                for (GraveZombie graveZombie : GRAVE_ZOMBIES)
                    if (graveZombie.location.clone().add(0, 1.94, 0).distance(player.getLocation()) < 16 &&
                            graveZombie.cooldown.isComplete()) {
                        summonSkinContender(graveZombie);
                        graveZombie.cooldown.reset();
                    }
            });
            for (int key : keys)
                if (PACKS.containsKey(key)) {
                    for (Pack pack : PACKS.get(key))
                        pack.trySpawn();
                }
        }, 0L, 100L);
    }

    @EventHandler
    public void onFLShutdown(FLShutdownEvent event) {
        tasks.values().forEach(FarLands.getScheduler()::completeTask);
    }

    // https://currentmillis.com/
    public static boolean isActive() {
        return System.currentTimeMillis() < 1606111200000L;
    }
    public static Location getSpawn() {
        return SPAWN;
    }
    public static boolean isInEvent(Location location) {
        if (location.getWorld() != FarLands.getWorld())
            return false;
        return CENTER.distanceSquared(location) < 164000;
    }
    public static boolean canBypass(Player player) {
        return Rank.getRank(player).isStaff() && player.getGameMode() != GameMode.SURVIVAL;
    }
    private static ItemStack generateKeyItem(String name, ChatColor color) {
        ItemStack stack = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta im = stack.getItemMeta();
        im.setDisplayName(ChatColor.RESET + color.toString() + name + " Key" + ChatColor.RESET);
        im.setLore(Collections.singletonList(name + " Boss Key."));
        im.addEnchant(Enchantment.MENDING, 1, true);
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(im);
        return stack;
    }
    private static ItemStack generateCollectable(String owner, String texture, String name, String lore1) {
        return skullItem(owner, new SkullData(texture, name, ChatColor.DARK_PURPLE + ChatColor.BOLD.toString() + lore1,
                ChatColor.DARK_RED + "Autumn 2020", ChatColor.GOLD + "FarLands' Fall 2020"));
    }

    /**
     * Produces a hash such that no key will ever point to a list of locations in different sub chunks
     * Where the following masks show the bit position of chunk XYZ where X, Z is 14 bits and Y is 4
     *
     * Therefore the max chunk value for X, Z is 16383 (world location 262143)
     *               max chunk value for Y    is 15    (as restricted by the game build height 255)
     *
     * X Hash Mask = 0F FF C0 00
     * Y Hash Mask = F0 00 00 00
     * Z Hash Mask = 00 00 3F FF
     *
     * @param location location of the Pack value will be stored in the HashMap
     * @return hash key for this location
     */
    private static int generateKey(Location location) {
        return (location.getBlockX() & 0x3FFF0) << 10 | (location.getBlockY() & 0xF0) << 24 | (location.getBlockZ() & 0x3FFF0) >> 4;
    }
    private static void putPack(Pack pack) {
        int key = generateKey(pack.center);
        if (!PACKS.containsKey(key))
            PACKS.put(key, new ArrayList<>());
        PACKS.get(key).add(pack);
    }
    private static Mob dramaticSpawn(Location location, EntityType entityType, float eyeHeight) {
        Mob mob = (Mob)FarLands.getWorld().spawnEntity(location.clone().subtract(0, eyeHeight - 0.05, 0), entityType);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (int)(eyeHeight / 0.05), 0, true));

        mob.getEquipment().setItemInMainHand(AIR);
        mob.getEquipment().setHelmet(AIR);
        mob.getEquipment().setChestplate(AIR);
        mob.getEquipment().setLeggings(AIR);
        mob.getEquipment().setBoots(AIR);

        return mob;
    }

    // Pack spawns

    // Pumpkin Patch
    static {
        putPack(new Pack(
                AutumnEvent::summonPossessedPumpkin, Husk.class,
                new Location(FarLands.getWorld(), -521, 68, 879),
                12, 12, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonPossessedPumpkin, Husk.class,
                new Location(FarLands.getWorld(), -579, 64, 897),
                8, 8, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonPossessedPumpkin, Husk.class,
                new Location(FarLands.getWorld(), -566, 65, 838),
                12, 16, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonPossessedPumpkin, Husk.class,
                new Location(FarLands.getWorld(), -609, 68, 786),
                15, 16, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonPossessedPumpkin, Husk.class,
                new Location(FarLands.getWorld(), -650, 66, 784),
                11, 10, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonPossessedPumpkin, Husk.class,
                new Location(FarLands.getWorld(), -643, 79, 837),
                8, 6, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonPossessedPumpkin, Husk.class,
                new Location(FarLands.getWorld(), -690, 74, 770),
                6, 6, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonPossessedPumpkin, Husk.class,
                new Location(FarLands.getWorld(), -685, 63, 805),
                10, 12, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonPossessedPumpkin, Husk.class,
                new Location(FarLands.getWorld(), -559, 63, 762),
                14, 12, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonPossessedPumpkin, Husk.class,
                new Location(FarLands.getWorld(), -518, 64, 801),
                10, 14, new Cooldown(60 * 20),
                false));

        putPack(new Pack(
                AutumnEvent::summonAxeMurderer, Vindicator.class,
                new Location(FarLands.getWorld(), -589, 70, 705),
                24, 16, new Cooldown(90 * 20),
                false
        ));
    }
    private static Mob summonPossessedPumpkin(Location location) {
        Husk mob = (Husk)dramaticSpawn(location, EntityType.HUSK, 1.74F);
        mob.setCustomName(ChatColor.GOLD + "Possessed Pumpkin");
        mob.setCustomNameVisible(true);
        mob.setAdult();

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(64);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12);
        mob.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0);
        mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.25);

        if (RNG.nextInt(5) == 0)
            mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));

        if (RNG.nextInt(5) == 0)
            mob.getEquipment().setItemInMainHand(RNG.nextInt(5) == 0
                    ? new ItemStack(Material.SOUL_LANTERN)
                    : new ItemStack(Material.LANTERN));

        mob.getEquipment().setHelmet(new ItemStack(RNG.nextInt(5) == 0 ? Material.JACK_O_LANTERN : Material.CARVED_PUMPKIN));
        if (RNG.nextInt(5) == 0) {
            // https://minecraft-heads.com/custom-heads/plants/40577-evil-jack-o-lantern-soulfire
            mob.getEquipment().setHelmet(skullItem("257c43a6-459d-42e5-956d-9a901406fae0", new SkullData(
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZD" +
                            "VlNmNjYTAyMWE5OTEzY2UwNmQyZTczNzc2YWE4Yjg4NDQ3YWZiODI0Y2Q2NTdmZjhjMmNiODhjZGYwYTNkNSJ9fX0=",
                    mob.getCustomName())
            ));
        } else
            mob.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));

        ItemStack stack = new ItemStack(Material.CHAINMAIL_BOOTS);
        ItemMeta im = stack.getItemMeta();
        im.addEnchant(Enchantment.DEPTH_STRIDER, 3, false);
        stack.setItemMeta(im);
        mob.getEquipment().setBoots(stack);

        return mob;
    }
    private static Mob summonAxeMurderer(Location location) {
        Vindicator mob = (Vindicator)dramaticSpawn(location, EntityType.VINDICATOR, 1.62F);
        mob.setCustomName(ChatColor.GOLD + "Axe Murderer");
        mob.setCustomNameVisible(true);
        mob.setPatrolLeader(false);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(64);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(16);

        mob.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));

        return mob;
    }

    // Netherscape
    static {
        Location location;

        // Fortress
        putPack(new Pack(
                AutumnEvent::summonVolcanicCreation, Blaze.class,
                new Location(FarLands.getWorld(), -551, 78, 971),
                7, 5, new Cooldown(90 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonVolcanicCreation, Blaze.class,
                new Location(FarLands.getWorld(), -543, 84, 1008),
                7, 5, new Cooldown(90 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonVolcanicCreation, Blaze.class,
                new Location(FarLands.getWorld(), -494, 87, 1001),
                3, 5, new Cooldown(90 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonVolcanicCreation, Blaze.class,
                new Location(FarLands.getWorld(), -519, 90, 1025),
                7, 5, new Cooldown(90 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonFortressDefender, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -392, 33, 969),
                1, 3, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonFortressDefender, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -362, 32, 958),
                1, 3, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonFortressDefender, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -484, 48, 994),
                1, 3, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonFortressDefender, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -478, 48, 1005),
                1, 3, new Cooldown(120 * 20),
                false
        ));
        // collides with self
        location = new Location(FarLands.getWorld(), -480, 48, 988);
        putPack(new Pack(
                AutumnEvent::summonFortressDefender, WitherSkeleton.class,
                location,
                1, 1, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonAbomination, PigZombie.class,
                location,
                1, 2, new Cooldown(90 * 20),
                false
        ));
        // collides with -410, 33, 982
        putPack(new Pack(
                AutumnEvent::summonFortressDefender, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -412, 41, 987),
                1, 3, new Cooldown(120 * 20),
                false
        ));
        // collides with -412, 41, 987 and self
        location = new Location(FarLands.getWorld(), -410, 33, 982);
        putPack(new Pack(
                AutumnEvent::summonFortressDefender, WitherSkeleton.class,
                location,
                1, 2, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonAbomination, PigZombie.class,
                location,
                1, 2, new Cooldown(90 * 20),
                false
        ));
        // collides with self
        location = new Location(FarLands.getWorld(), -420, 41, 988);
        putPack(new Pack(
                AutumnEvent::summonFortressDefender, WitherSkeleton.class,
                location,
                1, 2, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonAbomination, PigZombie.class,
                location,
                1, 2, new Cooldown(90 * 20),
                false
        ));
        // collides with self
        location = new Location(FarLands.getWorld(), -447, 37, 955);
        putPack(new Pack(
                AutumnEvent::summonFortressDefender, WitherSkeleton.class,
                location,
                1, 2, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonAbomination, PigZombie.class,
                location,
                1, 2, new Cooldown(120 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonAbomination, PigZombie.class,
                new Location(FarLands.getWorld(), -377, 33, 959),
                1, 3, new Cooldown(90 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonAbomination, PigZombie.class,
                new Location(FarLands.getWorld(), -370, 33, 983),
                1, 3, new Cooldown(90 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonAbomination, PigZombie.class,
                new Location(FarLands.getWorld(), -471, 41, 959),
                1, 3, new Cooldown(90 * 20),
                false
        ));



        // Netherscape
        putPack(new Pack(
                AutumnEvent::summonAbomination, PigZombie.class,
                new Location(FarLands.getWorld(), -427, 65, 1041),
                10, 8, new Cooldown(45 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonAbomination, PigZombie.class,
                new Location(FarLands.getWorld(), -382, 66, 988),
                16, 12, new Cooldown(45 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonAbomination, PigZombie.class,
                new Location(FarLands.getWorld(), -466, 66, 983),
                24, 12, new Cooldown(45 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonAbomination, PigZombie.class,
                new Location(FarLands.getWorld(), -324, 71, 1023),
                16, 12, new Cooldown(45 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonMoltenSlime, MagmaCube.class,
                new Location(FarLands.getWorld(), -547, 77, 937),
                12, 5, new Cooldown(90 * 20),
                true
        ));
        putPack(new Pack(
                AutumnEvent::summonMoltenSlime, MagmaCube.class,
                new Location(FarLands.getWorld(), -459, 77, 944),
                12, 4, new Cooldown(90 * 20),
                true
        ));
        putPack(new Pack(
                AutumnEvent::summonMoltenSlime, MagmaCube.class,
                new Location(FarLands.getWorld(), -376, 77, 1053),
                12, 4, new Cooldown(90 * 20),
                true
        ));

        putPack(new Pack(
                AutumnEvent::summonShriekingSpecter, Ghast.class,
                new Location(FarLands.getWorld(), -439, 80, 976),
                32, 2, new Cooldown(120 * 20),
                true
        ));
    }
    private static Mob summonAbomination(Location location) {
        PigZombie mob = (PigZombie)dramaticSpawn(location, EntityType.ZOMBIFIED_PIGLIN, 1.74F);
        mob.setCustomName(ChatColor.DARK_RED + "Abomination");
        mob.setCustomNameVisible(true);
        mob.setAngry(true);
        mob.setAnger(Integer.MAX_VALUE);
        mob.setAdult();

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(16);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(16); // default 5
        mob.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0);
        mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);

        mob.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));
        if (RNG.nextInt(5) == 0) {
            // https://minecraft-heads.com/custom-heads/monsters/22738-red-netherbrick-pigman
            mob.getEquipment().setHelmet(skullItem("71d7c668-5af3-4438-a6f8-a91f925643f3", new SkullData(
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMz" +
                            "Y5ZjAyYTM2NGFhNzgyODEyMmZjZDQxNjVlYWJkNDMxM2I5OGJhN2NhMDI2YzMxMzcwNWU3ZmE5MzE0NTEwOCJ9fX0=",
                    mob.getCustomName())
            ));
            if (RNG.nextInt(5) == 0)
                mob.getEquipment().setItemInOffHand(new ItemStack(Material.GOLDEN_AXE));
        } else
            // https://minecraft-heads.com/custom-heads/monsters/2309-zombie-pigman
            mob.getEquipment().setHelmet(skullItem("b7ee8dae-7489-4e11-b6bd-4997397d769a", new SkullData(
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzhjNGE2N" +
                            "DFmMDE1N2ZlZTNmMzhkYmM5YmY2OWE0MmE0MjhlNzQ1MDMxM2IxZTRlMGJlMTJiZGFhMDYifX19",
                    mob.getCustomName())
            ));

        return mob;
    }
    private static Mob summonFortressDefender(Location location) {
        WitherSkeleton mob = (WitherSkeleton)dramaticSpawn(location, EntityType.WITHER_SKELETON, 2.1F);
        mob.setCustomName(ChatColor.DARK_RED + "Fortress Defender");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(64);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(20);

        mob.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
        if (RNG.nextInt(5) == 0) {
            mob.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            if (RNG.nextInt(5) == 0) {
                mob.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
                mob.getEquipment().setItemInOffHand(new ItemStack(Material.DIAMOND_SWORD));
            }
        } else
            // https://minecraft-heads.com/custom-heads/humanoid/26718-injured-wither-skeleton
            mob.getEquipment().setHelmet(skullItem("a4058958-fdb7-4090-925b-26829e1e39ea", new SkullData(
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZ" +
                            "GFhNGUyMjk0ZGYzNzBiOWE1MGNiOTI0Y2RkYTc4Zjc0MGIwZmJhZjVhNjg3MTA2MTc4NTA1YzgwYTc5YWRkYyJ9fX0=",
                    mob.getCustomName())
            ));

        return mob;
    }
    private static Mob summonVolcanicCreation(Location location) {
        Blaze mob = (Blaze)FarLands.getWorld().spawnEntity(location, EntityType.BLAZE);
        mob.setCustomName(ChatColor.DARK_RED + "Volcanic Creation");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());

        return mob;
    }
    private static Mob summonShriekingSpecter(Location location) {
        Ghast mob = (Ghast)FarLands.getWorld().spawnEntity(location, EntityType.GHAST);
        mob.setCustomName(ChatColor.DARK_RED + "Shrieking Specter");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());

        return mob;
    }
    private static Mob summonMoltenSlime(Location location) {
        MagmaCube mob = (MagmaCube)FarLands.getWorld().spawnEntity(location, EntityType.MAGMA_CUBE);
        mob.setCustomName(ChatColor.DARK_RED + "Molten Slime");
        mob.setCustomNameVisible(true);
        mob.setSize(RNG.nextInt(2) + (RNG.nextInt(8) == 0 ? 4 : 3));

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20 * mob.getSize());
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(5 * mob.getSize());

        return mob;
    }

    // g-Raveyard
    static {
        putPack(new Pack(
                AutumnEvent::summonDecomposingBones, Skeleton.class,
                new Location(FarLands.getWorld(), -320, 67, 678),
                8, 3, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonDecomposingBones, Skeleton.class,
                new Location(FarLands.getWorld(), -313, 67, 726),
                20, 3, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonDecomposingBones, Skeleton.class,
                new Location(FarLands.getWorld(), -289, 76, 640),
                7, 3, new Cooldown(60 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonMaggot, Silverfish.class,
                new Location(FarLands.getWorld(), -299, 67, 689),
                8, 5, new Cooldown(90 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonMaggot, Silverfish.class,
                new Location(FarLands.getWorld(), -284, 66, 675),
                8, 5, new Cooldown(90 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonMaggot, Silverfish.class,
                new Location(FarLands.getWorld(), -239, 74, 736),
                8, 5, new Cooldown(90 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonMaggot, Silverfish.class,
                new Location(FarLands.getWorld(), -234, 75, 705),
                8, 5, new Cooldown(90 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonRottenCorpse, ZombieVillager.class,
                new Location(FarLands.getWorld(), -298, 69, 707),
                8, 4, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonRottenCorpse, ZombieVillager.class,
                new Location(FarLands.getWorld(), -260, 72, 683),
                24, 4, new Cooldown(60 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonCadaver, Zombie.class,
                new Location(FarLands.getWorld(), -218, 93, 676),
                16, 2, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonCadaver, Zombie.class,
                new Location(FarLands.getWorld(), -270, 98, 631),
                16, 2, new Cooldown(120 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonDarkMonk, Zombie.class,
                new Location(FarLands.getWorld(), -250, 112, 620),
                6, 2, new Cooldown(120 * 20),
                false
        ));
    }
    private static Mob summonDecomposingBones(Location location) {
        Skeleton mob = (Skeleton)dramaticSpawn(location, EntityType.SKELETON, 1.74F);
        mob.setCustomName(ChatColor.AQUA + "Decomposing Bones");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(48);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12);

        mob.getEquipment().setItemInMainHand(RNG.nextInt(5) == 0 ? new ItemStack(Material.DIAMOND_SWORD) : new ItemStack(Material.IRON_SWORD));
        // https://minecraft-heads.com/custom-heads/monsters/36070-mossy-skeleton
        mob.getEquipment().setHelmet(skullItem("21e35a02-de6f-4c28-96ea-ede016f6949f", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTNmY" +
                        "TVlYzk2ZDI1YmY2OTJlNTI4MTA0MDViNGJmOGRjYzY4OTdmYTZjMjBkMzY0NmZlZjNjNjRlMDNjNWI1In19fQ==",
                mob.getCustomName())
        ));

        return mob;
    }
    private static Mob summonRottenCorpse(Location location) {
        ZombieVillager mob = (ZombieVillager)dramaticSpawn(location, EntityType.ZOMBIE_VILLAGER, 1.74F);
        mob.setCustomName(ChatColor.AQUA + "Rotten Corpse");
        mob.setCustomNameVisible(true);
        mob.setAdult();

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(64);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12);
        mob.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0);
        mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.2);

        mob.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));

        return mob;
    }
    private static Mob summonMaggot(Location location) {
        Silverfish mob = (Silverfish)dramaticSpawn(location, EntityType.SILVERFISH, 0.13F);
        mob.setCustomName(ChatColor.AQUA + "Maggot");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(16);

        return mob;
    }
    private static Mob summonCadaver(Location location) {
        Zombie mob = (Zombie)dramaticSpawn(location, EntityType.ZOMBIE, 1.74F);
        mob.setCustomName(ChatColor.AQUA + "Cadaver");
        mob.setCustomNameVisible(true);
        mob.setAdult();

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(20);
        mob.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0);

        mob.getEquipment().setItemInMainHand(
                RNG.nextInt(5) == 0
                        ? new ItemStack(Material.DIAMOND_AXE)
                        : new ItemStack(Material.IRON_AXE)
        );

        // https://minecraft-heads.com/custom-heads/humanoid/4650-ghost
        mob.getEquipment().setHelmet(skullItem("fcd6614c-adb1-4cd8-8361-37eeeb99985c", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWY3YT" +
                        "RmOTVlNWZlOTliNDViZTYxYmIzMzg4MmMxMmE5M2IyMmQyOTdmZDE3NjVhYjIxZTc3NDhkYzZiOGNmMyJ9fX0=",
                mob.getCustomName())
        ));

        return mob;
    }
    private static Mob summonDarkMonk(Location location) {
        Zombie mob = (Zombie)dramaticSpawn(location, EntityType.ZOMBIE, 1.74F);
        mob.setCustomName(ChatColor.AQUA + "Dark Monk");
        mob.setCustomNameVisible(true);
        mob.setAdult();

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(128);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(16);
        mob.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0);
        mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.3);
        mob.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(.5);

        mob.getEquipment().setItemInMainHand(new ItemStack(Material.BLAZE_ROD));
        if (RNG.nextInt(5) == 0)
            mob.getEquipment().setItemInOffHand(new ItemStack(Material.BLAZE_POWDER));

        // https://minecraft-heads.com/custom-heads/humanoid/16064-ghost
        mob.getEquipment().setHelmet(skullItem("ad4335c3-47c1-4e3f-8e0c-20a12ad6b925", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3Rle" +
                        "HR1cmUvMzFjOWU1MjNlOGNkNzVlMTdhNmZmYzVlOGJmMTM4ZWI5NzNlMzgzZWVjMmFmMGQzNTE0MjFhZGNhMzQ0MyJ9fX0=",
                mob.getCustomName()
        )));
        mob.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        mob.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        mob.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));

        return mob;
    }

    // Fall Forest
    static {
        // Scarecrow Fields
        putPack(new Pack(
                AutumnEvent::summonScarecrow, Husk.class,
                new Location(FarLands.getWorld(), -394, 69, 873),
                30, 8, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonScarecrow, Husk.class,
                new Location(FarLands.getWorld(), -333, 65, 876),
                25, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonScarecrow, Husk.class,
                new Location(FarLands.getWorld(), -312, 66, 805),
                40, 8, new Cooldown(120 * 20),
                false
        ));



        // Fall Forest
        putPack(new Pack(
                AutumnEvent::summonForestGolem, Ravager.class,
                new Location(FarLands.getWorld(), -499, 65, 649),
                15, 2, new Cooldown(240 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestGolem, Ravager.class,
                new Location(FarLands.getWorld(), -457, 68, 688),
                15, 2, new Cooldown(240 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestGolem, Ravager.class,
                new Location(FarLands.getWorld(), -453, 62, 656),
                15, 2, new Cooldown(240 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestGolem, Ravager.class,
                new Location(FarLands.getWorld(), -392, 63, 682),
                15, 2, new Cooldown(240 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestGolem, Ravager.class,
                new Location(FarLands.getWorld(), -384, 60, 733),
                15, 2, new Cooldown(240 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestGolem, Ravager.class,
                new Location(FarLands.getWorld(), -356, 66, 664),
                15, 2, new Cooldown(240 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestGolem, Ravager.class,
                new Location(FarLands.getWorld(), -414, 65, 742),
                15, 2, new Cooldown(240 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonForestHuntsman, Illusioner.class,
                new Location(FarLands.getWorld(), -475, 64, 717),
                18, 1, new Cooldown(180 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestHuntsman, Illusioner.class,
                new Location(FarLands.getWorld(), -424, 63, 673),
                18, 2, new Cooldown(180 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestHuntsman, Illusioner.class,
                new Location(FarLands.getWorld(), -435, 69, 706),
                18, 1, new Cooldown(180 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestHuntsman, Illusioner.class,
                new Location(FarLands.getWorld(), -388, 63, 649),
                18, 2, new Cooldown(180 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestHuntsman, Illusioner.class,
                new Location(FarLands.getWorld(), -373, 62, 765),
                18, 1, new Cooldown(180 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonForestMinion, Husk.class,
                new Location(FarLands.getWorld(), -489, 71, 675),
                15, 3, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestMinion, Husk.class,
                new Location(FarLands.getWorld(), -433, 62, 759),
                15, 2, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestMinion, Husk.class,
                new Location(FarLands.getWorld(), -404, 61, 713),
                15, 3, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestMinion, Husk.class,
                new Location(FarLands.getWorld(), -449, 62, 734),
                15, 2, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonForestMinion, Husk.class,
                new Location(FarLands.getWorld(), -404, 62, 774),
                15, 3, new Cooldown(60 * 20),
                false
        ));
    }
    private static Mob summonScarecrow(Location location) {
        Husk mob = (Husk)dramaticSpawn(location, EntityType.HUSK, 1.74F);
        mob.setCustomName(ChatColor.YELLOW + "Scarecrow");
        mob.setCustomNameVisible(true);
        mob.setAdult();

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(24);
        mob.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0);
        mob.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);

        mob.getEquipment().setItemInMainHand(new ItemStack(Material.TRIDENT));

        // https://minecraft-heads.com/custom-heads/decoration/12632-scarecrow
        mob.getEquipment().setHelmet(skullItem("1832e57c-48e7-4531-9e9e-fdfa933b5655", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZlN" +
                        "jUwMmFjNGM4NDdiMWFjMzc4MTBkNjZkMjhjOTFhOGIxOGZkN2Y2MzgzMTI4MjI4NzU1YWE4YzhmNSJ9fX0=",
                mob.getCustomName()
        )));

        return mob;
    }
    private static Mob summonForestHuntsman(Location location) {
        Illusioner mob = (Illusioner)dramaticSpawn(location, EntityType.ILLUSIONER, 1.62F);
        mob.setCustomName(ChatColor.YELLOW + "Forest Huntsman");
        mob.setCustomNameVisible(true);
        mob.setPatrolLeader(false);

        mob.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));

        return mob;
    }
    private static Mob summonForestGolem(Location location) {
        Ravager mob = (Ravager)FarLands.getWorld().spawnEntity(location, EntityType.RAVAGER);
        mob.setCustomName(ChatColor.YELLOW + "Forest Golem");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());

        return mob;
    }
    private static Mob summonForestMinion(Location location) {
        Fox vehicle = (Fox)FarLands.getWorld().spawnEntity(location, EntityType.FOX);
        vehicle.setAdult();


        Husk mob = (Husk)FarLands.getWorld().spawnEntity(location, EntityType.HUSK);
        mob.setCustomName(ChatColor.YELLOW + "Forest Minion");
        mob.setCustomNameVisible(true);
        mob.setBaby();

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(32);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12);
        mob.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0);

        // https://minecraft-heads.com/custom-heads/humanoid/26686-injured-husk
        mob.getEquipment().setHelmet(skullItem("c86e3d82-4bda-4dd8-831f-617b08108987", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODVjM" +
                        "Dk2MTJmMGYyNjVhMWRiMmQ2MmU4MDM1YWI5Y2ZhNWYxYjk0OTAyNGQ5MDQ5NGRlOWNhMDJkNTlmM2YxNSJ9fX0=",
                mob.getCustomName()
        )));

        vehicle.addPassenger(mob);
        return mob;
    }

    // Witch Village
    static {
        putPack(new Pack(
                AutumnEvent::summonHag, Witch.class,
                new Location(FarLands.getWorld(), -252, 69, 796),
                5, 3, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonHag, Witch.class,
                new Location(FarLands.getWorld(), -238, 67, 874),
                5, 3, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonHag, Witch.class,
                new Location(FarLands.getWorld(), -206, 66, 872),
                5, 3, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonHag, Witch.class,
                new Location(FarLands.getWorld(), -237, 69, 909),
                5, 3, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonHag, Witch.class,
                new Location(FarLands.getWorld(), -198, 66, 925),
                5, 3, new Cooldown(60 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonSludge, Slime.class,
                new Location(FarLands.getWorld(), -204, 63, 840),
                12, 5, new Cooldown(90 * 20),
                true
        ));
        putPack(new Pack(
                AutumnEvent::summonSludge, Slime.class,
                new Location(FarLands.getWorld(), -142, 67, 929),
                12, 5, new Cooldown(90 * 20),
                true
        ));
        putPack(new Pack(
                AutumnEvent::summonSludge, Slime.class,
                new Location(FarLands.getWorld(), -221, 64, 897),
                12, 5, new Cooldown(90 * 20),
                true
        ));
        putPack(new Pack(
                AutumnEvent::summonSludge, Slime.class,
                new Location(FarLands.getWorld(), -282, 63, 906),
                12, 5, new Cooldown(90 * 20),
                true
        ));
    }
    private static Mob summonHag(Location location) {
        Witch mob = (Witch)dramaticSpawn(location, EntityType.WITCH, 1.62F);
        mob.setCustomName(ChatColor.LIGHT_PURPLE + "Old Hag");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(128);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());

        return mob;
    }
    private static Mob summonSludge(Location location) {
        Slime mob = (Slime)FarLands.getWorld().spawnEntity(location, EntityType.SLIME);
        mob.setCustomName(ChatColor.LIGHT_PURPLE + "Sludge Heap");
        mob.setCustomNameVisible(true);
        mob.setSize(RNG.nextInt(2) + (RNG.nextInt(8) == 0 ? 4 : 3));

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20 * mob.getSize());
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(5 * mob.getSize());

        return mob;
    }

    // Spider Abyss
    static {
        putPack(new Pack(
                AutumnEvent::summonGoliathSpider, Spider.class,
                new Location(FarLands.getWorld(), -397, 62, 594),
                3, 2, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonGoliathSpider, Spider.class,
                new Location(FarLands.getWorld(), -397, 68, 561),
                3, 3, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonGoliathSpider, Spider.class,
                new Location(FarLands.getWorld(), -401, 65, 516),
                3, 3, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonGoliathSpider, Spider.class,
                new Location(FarLands.getWorld(), -440, 34, 585),
                3, 3, new Cooldown(60 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonGoliathSpider, Spider.class,
                new Location(FarLands.getWorld(), -388, 29, 598),
                3, 3, new Cooldown(60 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonRecluseSpider, CaveSpider.class,
                new Location(FarLands.getWorld(), -408, 31, 627),
                3, 3, new Cooldown(90 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonRecluseSpider, CaveSpider.class,
                new Location(FarLands.getWorld(), -418, 30, 605),
                3, 4, new Cooldown(90 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonRecluseSpider, CaveSpider.class,
                new Location(FarLands.getWorld(), -383, 29, 585),
                3, 4, new Cooldown(90 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonRecluseSpider, CaveSpider.class,
                new Location(FarLands.getWorld(), -393, 29, 521),
                2, 5, new Cooldown(90 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonCommonHouseSpider, Spider.class,
                new Location(FarLands.getWorld(), -382, 31, 551),
                3, 3, new Cooldown(120 * 20),
                false
        ));

        putPack(new Pack(AutumnEvent::summonTarantula, Spider.class,
                new Location(FarLands.getWorld(), -401, 28, 580),
                3, 4, new Cooldown(120 * 20),
                false
        ));

        putPack(new Pack(AutumnEvent::summonNimbleArcher, Skeleton.class,
                new Location(FarLands.getWorld(), -439, 63, 501),
                12, 5, new Cooldown(90 * 20),
                false
        ));

        putPack(new Pack(AutumnEvent::summonCreepySpider, Spider.class,
                new Location(FarLands.getWorld(), -420, 26, 552),
                12, 2, new Cooldown(120 * 20),
                false
        ));
    }
    private static Mob summonGoliathSpider(Location location) {
        Spider mob = (Spider)dramaticSpawn(location, EntityType.SPIDER, 0.65F);
        mob.setCustomName(ChatColor.RED + "Goliath Spider");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(16);

        return mob;
    }
    private static Mob summonRecluseSpider(Location location) {
        CaveSpider mob = (CaveSpider)dramaticSpawn(location, EntityType.CAVE_SPIDER, 0.45F);
        mob.setCustomName(ChatColor.RED + "Recluse Spider");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(8);

        return mob;
    }
    private static Mob summonCommonHouseSpider(Location location) {
        Spider mob = (Spider)dramaticSpawn(location, EntityType.SPIDER, 0.65F);
        mob.setCustomName(ChatColor.RED + "Common House Spider");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(14);

        mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, true));

        return mob;
    }
    private static Mob summonTarantula(Location location) {
        Spider mob = (Spider)dramaticSpawn(location, EntityType.SPIDER, 0.65F);
        mob.setCustomName(ChatColor.RED + "Tarantula");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12);

        for (int i = 0; i < 4; ++i) {
            switch (RNG.nextInt(4)) {
                case 0: {
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 1, true));
                    break;
                }
                case 1: {
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true));
                    break;
                }
                case 2: {
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 1, true));
                    break;
                }
                case 3: {
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, true));
                    break;
                }
            }
            if (RNG.nextBoolean())
                break;
        }

        return mob;
    }
    private static Mob summonNimbleArcher(Location location) {
        Spider vehicle = (Spider)dramaticSpawn(location, EntityType.SPIDER, 0.65F);
        vehicle.setCustomName(ChatColor.RED + "Nimble Spider");

        vehicle.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        vehicle.setHealth(vehicle.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        vehicle.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(16);


        Skeleton mob = (Skeleton)FarLands.getWorld().spawnEntity(location, EntityType.SKELETON);
        mob.setCustomName(ChatColor.RED + "Nimble Archer");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(48);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());

        ItemStack stack = new ItemStack(Material.BOW);
        ItemMeta im = stack.getItemMeta();
        im.addEnchant(Enchantment.ARROW_DAMAGE, 3, true);
        stack.setItemMeta(im);
        mob.getEquipment().setItemInMainHand(stack);

        mob.getEquipment().setHelmet(skullItem("8bdb71d0-4724-48b2-9344-e79480424798",
                new SkullData("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L" +
                        "3RleHR1cmUvY2Q1NDE1NDFkYWFmZjUwODk2Y2QyNThiZGJkZDRjZjgwYzNiYTgxNjczNTcyNjA3OGJmZTM5MzkyN2U1" +
                        "N2YxIn19fQ==", mob.getCustomName())));

        vehicle.addPassenger(mob);
        return mob;
    }
    private static Mob summonCreepySpider(Location location) {
        Spider vehicle = (Spider)dramaticSpawn(location, EntityType.SPIDER, 0.65F);
        vehicle.setCustomName(ChatColor.RED + "Creepy Spider");

        vehicle.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(196);
        vehicle.setHealth(vehicle.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        vehicle.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12);

        vehicle.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true));


        Creeper mob = (Creeper) FarLands.getWorld().spawnEntity(location, EntityType.CREEPER);
        mob.setCustomName(ChatColor.RED + "Golden Creeper");
        mob.setCustomNameVisible(true);
        mob.setPowered(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(8);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());

        vehicle.addPassenger(mob);
        return mob;
    }

    // Soul Sand Valley
    static {
        putPack(new Pack(
                AutumnEvent::summonLostTraveller, Skeleton.class,
                new Location(FarLands.getWorld(), -417, 78, 1162),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonLostTraveller, Skeleton.class,
                new Location(FarLands.getWorld(), -493, 82, 1121),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonLostTraveller, Skeleton.class,
                new Location(FarLands.getWorld(), -612, 60, 1029),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonLostTraveller, Skeleton.class,
                new Location(FarLands.getWorld(), -636, 81, 1043),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonLostTraveller, Skeleton.class,
                new Location(FarLands.getWorld(), -590, 81, 1061),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonLostTraveller, Skeleton.class,
                new Location(FarLands.getWorld(), -652, 85, 1102),
                4, 4, new Cooldown(120 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonWitheredExplorer, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -669, 86, 1064),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonWitheredExplorer, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -638, 77, 1123),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonWitheredExplorer, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -634, 74, 997),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonWitheredExplorer, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -670, 63, 1018),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonWitheredExplorer, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -631, 60, 1061),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonWitheredExplorer, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -601, 63, 1081),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonWitheredExplorer, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -613, 63, 1116),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonWitheredExplorer, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -533, 76, 1098),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonWitheredExplorer, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -447, 81, 1125),
                4, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonWitheredExplorer, WitherSkeleton.class,
                new Location(FarLands.getWorld(), -406, 74, 1119),
                4, 4, new Cooldown(120 * 20),
                false
        ));
    }
    private static Mob summonLostTraveller(Location location) {
        Skeleton mob = (Skeleton)dramaticSpawn(location, EntityType.SKELETON, 1.74F);
        mob.setCustomName(ChatColor.GRAY + "Lost Traveller");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12);

        if (RNG.nextInt(5) == 0)
            if (RNG.nextInt(5) == 0)
                // https://minecraft-heads.com/custom-heads/decoration/33901-skeleton-skull-diamond
                mob.getEquipment().setHelmet(skullItem("135aaf7f-97d8-4cd2-ac79-4e61a41d9017", new SkullData(
                        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1c" +
                                "mUvNGMxMWU0OWE2NDk3ZTc3YjViNDFkN2JiMzRlNTY3MzU5YWEzMjIyNTY5Y2ViOGM1MGU2M2YxYTVhZjdiZjUyOCJ9fX0=",
                        mob.getCustomName()
                )));
            else
                // https://minecraft-heads.com/custom-heads/decoration/33899-skeleton-skull-gold
                mob.getEquipment().setHelmet(skullItem("3a5052de-99dc-4b87-bb47-ccd011a21828", new SkullData(
                        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2I4ND" +
                                "AwN2IyMDQ5Y2Q5Y2VkNDExN2QxMDc5M2IwNzZiYTUwZWJmNDQwNWU2Zjk2ZjU5ZjY3NTliYWJkMzE4NiJ9fX0=",
                        mob.getCustomName()
                )));

        return mob;
    }
    private static Mob summonWitheredExplorer(Location location) {
        WitherSkeleton mob = (WitherSkeleton)dramaticSpawn(location, EntityType.WITHER_SKELETON, 2.1F);
        mob.setCustomName(ChatColor.GRAY + "Withered Explorer");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(8);

        mob.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
        mob.getEquipment().setItemInOffHand(RNG.nextBoolean()
                ? new ItemStack(Material.SOUL_LANTERN)
                : new ItemStack(Material.SOUL_TORCH)
        );

        // https://minecraft-heads.com/custom-heads/decoration/33904-skeleton-skull-burned
        mob.getEquipment().setHelmet(skullItem("95e7a0e0-1219-41ee-964c-59f324034780", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UxZT" +
                        "FlODJiZjQzNzhhN2IxMzkyMjliNTYxYzhmMDExOWJmNTY1NTEyODAxNGQzYzU0MzlkODk4MzAzZjFiMCJ9fX0=",
                mob.getCustomName()
        )));

        return mob;
    }

    // Haunted Forest
    static {
        putPack(new Pack(
                AutumnEvent::summonGostlin, Zombie.class,
                new Location(FarLands.getWorld(), -252, 66, 1134),
                12, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonGostlin, Zombie.class,
                new Location(FarLands.getWorld(), -317, 65, 1114),
                12, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonGostlin, Zombie.class,
                new Location(FarLands.getWorld(), -345, 63, 1140),
                12, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonGostlin, Zombie.class,
                new Location(FarLands.getWorld(), -336, 71, 1174),
                12, 4, new Cooldown(120 * 20),
                false
        ));

        putPack(new Pack(
                AutumnEvent::summonSpore, Husk.class,
                new Location(FarLands.getWorld(), -222, 64, 1108),
                8, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonSpore, Husk.class,
                new Location(FarLands.getWorld(), -195, 65, 1041),
                8, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonSpore, Husk.class,
                new Location(FarLands.getWorld(), -259, 65, 1056),
                8, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonSpore, Husk.class,
                new Location(FarLands.getWorld(), -296, 68, 1166),
                8, 4, new Cooldown(120 * 20),
                false
        ));
        putPack(new Pack(
                AutumnEvent::summonSpore, Husk.class,
                new Location(FarLands.getWorld(), -372, 62, 1145),
                8, 4, new Cooldown(120 * 20),
                false
        ));
    }
    private static Mob summonGostlin(Location location) {
        Zombie mob = (Zombie)dramaticSpawn(location, EntityType.ZOMBIE, 1.74F);
        mob.setCustomName(ChatColor.DARK_AQUA + "Gostlin");
        mob.setCustomNameVisible(true);
        if (RNG.nextBoolean())
            mob.setBaby();

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12);
        mob.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0);

        mob.addPotionEffect(new PotionEffect(PotionEffectType.JUMP,         Integer.MAX_VALUE, 1, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 1, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, true));

        // https://minecraft-heads.com/custom-heads/humanoid/31762-ghost
        mob.getEquipment().setHelmet(skullItem("c0b406af-47f1-403d-af2d-c79aa395f67a", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGZhMT" +
                        "MwMzJmYTkzOWYxODRkYWE2YTRlMTFlNmYzYTkxM2U0OGYyNTA0OTgxNjVjNTY2NWNjZjQ5YzcyYTE0MCJ9fX0=",
                mob.getCustomName()
        )));

        return mob;
    }
    private static Mob summonSpore(Location location) {
        Husk mob = (Husk)FarLands.getWorld().spawnEntity(location, EntityType.HUSK);
        mob.setCustomName(ChatColor.DARK_AQUA + "Spore");
        mob.setCustomNameVisible(true);
        if (RNG.nextBoolean())
            mob.setBaby();

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(32);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12);
        mob.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0);

        mob.addPotionEffect(new PotionEffect(PotionEffectType.JUMP,         Integer.MAX_VALUE, 1, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 1, true));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, true));

        // https://minecraft-heads.com/custom-heads/plants/34786-warped-fungus
        ItemStack spore = skullItem("dbdade19-b08d-414c-b051-54801d6d0f8b", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzQyNTE4Mjc4YjZhODg3MDE0Y2E3ZjczZDY5MGJiM2MwZjg3ZjE5MGMwYTI1ZThhY2ZmZjVkYjIwMWZiYTIxNyJ9fX0=",
                mob.getCustomName()
        ));
        for (int i = 3; --i >= 0; ) {
            switch(RNG.nextInt(3)) {
                case 0: {
                    mob.getEquipment().setHelmet(spore);
                    break;
                }
                case 1: {
                    mob.getEquipment().setItemInMainHand(spore);
                    break;
                }
                case 2: {
                    mob.getEquipment().setItemInOffHand(spore);
                    break;
                }
            }
        }

        return mob;
    }

    // mini bosses
    private static Mob summonSkinContender(GraveZombie graveZombie) {
        Zombie mob = (Zombie)FarLands.getWorld().spawnEntity(graveZombie.location, EntityType.ZOMBIE);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 0, true));
        mob.setCustomName(ChatColor.AQUA + ChatColor.BOLD.toString() + graveZombie.name);
        mob.setCustomNameVisible(true);
        mob.setAdult();

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(256);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(24);
        mob.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0);
        mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.3);
        mob.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(.5);

        mob.getEquipment().setItemInMainHand(AIR);
        mob.getEquipment().setHelmet(AIR);
        mob.getEquipment().setChestplate(AIR);
        mob.getEquipment().setLeggings(AIR);
        mob.getEquipment().setBoots(AIR);

        mob.getEquipment().setHelmet(graveZombie.head);
        mob.getEquipment().setChestplate(graveZombie.chestplate);
        mob.getEquipment().setLeggings(graveZombie.leggings);
        mob.getEquipment().setBoots(graveZombie.boots);

        return mob;
    }
    private static Mob summonCandyWraith(Location location) {
        Bat vehicle = (Bat)FarLands.getWorld().spawnEntity(location, EntityType.BAT);

        vehicle.setInvulnerable(true);
        vehicle.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));


        Stray mob = (Stray)FarLands.getWorld().spawnEntity(location, EntityType.STRAY);
        mob.setCustomName(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Candy Wraith");
        mob.setCustomNameVisible(true);

        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(512);
        mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());

        mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));

        ItemStack stack = new ItemStack(Material.BOW);
        ItemMeta im = stack.getItemMeta();
        im.addEnchant(Enchantment.ARROW_DAMAGE, 10, true);
        stack.setItemMeta(im);
        mob.getEquipment().setItemInMainHand(stack);

        // https://minecraft-heads.com/custom-heads/humanoid/29296-thunder-skeleton
        mob.getEquipment().setHelmet(skullItem("427aa647-eb1f-4e5e-8f14-291fec403b32", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmFi" +
                        "YmUzZGZjY2ViYWIyNDk1OTk0ZGY4MzVhYmZiMjk2YmUwNDE4ZDJmNjIyODUwYmRlMTRjOWU4MjQyMmYxYyJ9fX0=",
                mob.getCustomName()
        )));

        vehicle.addPassenger(mob);
        return mob;
    }

    // Bosses
    static {
        /*
         * make boss class instead of using pack
         * attach to door so we can spawn boss on player enter boss room using the below
         * if cooldown complete trySpawn on boss
         * on boss death start 10m cooldown
         * lock door only during 10m boss cooldown
         */
        putPack(new Pack(
                AutumnEvent::summonCandyWraith, Bat.class,
                new Location(FarLands.getWorld(), -496, 69, 757),
                4, 2, new Cooldown(5 * 60 * 20),
                true
        ));

        cooldownBossSpawns.put(DOORS.get(0).keyType.getItemMeta().getDisplayName(), new Cooldown(10 * 60 * 20));
        putPack(new Pack(
                        AutumnEvent::summonJackSkellington, Skeleton.class,
                        new Location(FarLands.getWorld(), -654, 17, 879, -90, 0),
                        0, 1, new Cooldown(10 * 60 * 20),
                        false
                ){
                    @Override
                    protected List<Mob> spawn(Location location1) {
                        List<Mob> boss = super.spawn(location1);
                        if (boss.isEmpty())
                            return boss;
                        FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(
                                ChatColor.GOLD + ChatColor.BOLD.toString() + "Jack Skellington" + ChatColor.RESET + " has been summoned"
                        ));
                        cooldownBossSpawns.get(DOORS.get(0).keyType.getItemMeta().getDisplayName()).reset();
                        return boss;
                    }

                    @Override
                    boolean trySpawn() {
                        if (!cooldownBossSpawns.get(DOORS.get(0).keyType.getItemMeta().getDisplayName()).isComplete())
                            return false;
                        return super.trySpawn();
                    }
                }
        );

        cooldownBossSpawns.put(DOORS.get(1).keyType.getItemMeta().getDisplayName(), new Cooldown(10 * 60 * 20));
        putPack(new Pack(
                        AutumnEvent::summonMidas, PiglinBrute.class,
                        new Location(FarLands.getWorld(), -347, 18, 1043, 90, 0),
                        0, 1, new Cooldown(10 * 60 * 20),
                        false
                ){
                    @Override
                    protected List<Mob> spawn(Location location1) {
                        List<Mob> boss = super.spawn(location1);
                        if (boss.isEmpty())
                            return boss;
                        FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(
                                ChatColor.DARK_RED + ChatColor.BOLD.toString() + "Midas" + ChatColor.RESET + " has been summoned"
                        ));
                        cooldownBossSpawns.get(DOORS.get(1).keyType.getItemMeta().getDisplayName()).reset();
                        return boss;
                    }

                    @Override
                    boolean trySpawn() {
                        if (!cooldownBossSpawns.get(DOORS.get(1).keyType.getItemMeta().getDisplayName()).isComplete())
                            return false;
                        return super.trySpawn();
                    }
                }
        );

        cooldownBossSpawns.put(DOORS.get(3).keyType.getItemMeta().getDisplayName(), new Cooldown(10 * 60 * 20));
        putPack(new Pack(
                        AutumnEvent::summonHerobrine, Zombie.class,
                        new Location(FarLands.getWorld(), -240, 19, 661, 0, 0),
                        0, 1, new Cooldown(10 * 60 * 20),
                        false
                ){
                    @Override
                    protected List<Mob> spawn(Location location1) {
                        List<Mob> boss = super.spawn(location1);
                        if (boss.isEmpty())
                            return boss;
                        FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(
                                ChatColor.AQUA + ChatColor.BOLD.toString() + "Herobrine" + ChatColor.RESET + " has been summoned"
                        ));
                        cooldownBossSpawns.get(DOORS.get(3).keyType.getItemMeta().getDisplayName()).reset();
                        return boss;
                    }

                    @Override
                    boolean trySpawn() {
                        if (!cooldownBossSpawns.get(DOORS.get(3).keyType.getItemMeta().getDisplayName()).isComplete())
                            return false;
                        return super.trySpawn();
                    }
                }
        );

        cooldownBossSpawns.put(DOORS.get(4).keyType.getItemMeta().getDisplayName(), new Cooldown(10 * 60 * 20));
        putPack(new Pack(
                        AutumnEvent::summonHeadlessHorseman, Stray.class,
                        new Location(FarLands.getWorld(), -452, 12, 794, -106, 0),
                        0, 1, new Cooldown(10 * 60 * 20),
                        false
                ){
                    @Override
                    protected List<Mob> spawn(Location location1) {
                        List<Mob> boss = super.spawn(location1);
                        if (boss.isEmpty())
                            return boss;
                        FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(
                                ChatColor.YELLOW + ChatColor.BOLD.toString() + "The Headless Horseman" + ChatColor.RESET + " has been summoned"
                        ));
                        cooldownBossSpawns.get(DOORS.get(4).keyType.getItemMeta().getDisplayName()).reset();
                        return boss;
                    }

                    @Override
                    boolean trySpawn() {
                        if (!cooldownBossSpawns.get(DOORS.get(4).keyType.getItemMeta().getDisplayName()).isComplete())
                            return false;
                        return super.trySpawn();
                    }
                }
        );

        cooldownBossSpawns.put(DOORS.get(6).keyType.getItemMeta().getDisplayName(), new Cooldown(10 * 60 * 20));
        putPack(new Pack(
                        AutumnEvent::summonBabaYaga, Evoker.class,
                        new Location(FarLands.getWorld(), -258, 6, 891, -90, 0),
                        0, 1, new Cooldown(10 * 60 * 20),
                        false
                ){
                    @Override
                    protected List<Mob> spawn(Location location1) {
                        List<Mob> boss = super.spawn(location1);
                        if (boss.isEmpty())
                            return boss;
                        FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(
                                ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Baba Yaga" + ChatColor.RESET + " has been summoned"
                        ));
                        cooldownBossSpawns.get(DOORS.get(6).keyType.getItemMeta().getDisplayName()).reset();
                        return boss;
                    }

                    @Override
                    boolean trySpawn() {
                        if (!cooldownBossSpawns.get(DOORS.get(6).keyType.getItemMeta().getDisplayName()).isComplete())
                            return false;
                        return super.trySpawn();
                    }
                }
        );

        cooldownBossSpawns.put(DOORS.get(7).keyType.getItemMeta().getDisplayName(), new Cooldown(10 * 60 * 20));
        putPack(new Pack(
                        AutumnEvent::summonArchane, Spider.class,
                        new Location(FarLands.getWorld(), -426, 21, 462, -145, 0),
                        0, 1, new Cooldown(10 * 60 * 20),
                        false
                ){
                    @Override
                    protected List<Mob> spawn(Location location1) {
                        List<Mob> boss = super.spawn(location1);
                        if (boss.isEmpty())
                            return boss;
                        FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(
                                ChatColor.RED + ChatColor.BOLD.toString() + "Archane" + ChatColor.RESET + " has been summoned"
                        ));
                        cooldownBossSpawns.get(DOORS.get(7).keyType.getItemMeta().getDisplayName()).reset();
                        return boss;
                    }

                    @Override
                    boolean trySpawn() {
                        if (!cooldownBossSpawns.get(DOORS.get(7).keyType.getItemMeta().getDisplayName()).isComplete())
                            return false;
                        return super.trySpawn();
                    }
                }
        );

        cooldownBossSpawns.put(DOORS.get(8).keyType.getItemMeta().getDisplayName(), new Cooldown(10 * 60 * 20));
        putPack(new Pack(
                        AutumnEvent::summonBlight, Wither.class,
                        new Location (FarLands.getWorld(), -641, 9, 1084),
                        0, 1, new Cooldown(10 * 60 * 20),
                        false
                ) {
                    @Override
                    protected List<Mob> spawn(Location location1) {
                        List<Mob> boss = super.spawn(location1);
                        if (boss.isEmpty())
                            return boss;
                        FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(
                                ChatColor.GRAY + ChatColor.BOLD.toString() + "Blight" + ChatColor.RESET + " has been summoned"
                        ));
                        cooldownBossSpawns.get(DOORS.get(8).keyType.getItemMeta().getDisplayName()).reset();
                        return boss;
                    }

                    @Override
                    boolean trySpawn() {
                        if (!cooldownBossSpawns.get(DOORS.get(8).keyType.getItemMeta().getDisplayName()).isComplete())
                            return false;
                        return super.trySpawn();
                    }
                }
        );

        cooldownBossSpawns.put(DOORS.get(9).keyType.getItemMeta().getDisplayName(), new Cooldown(10 * 60 * 20));
        putPack(new Pack(
                        AutumnEvent::summonSpecter, Zombie.class,
                        new Location (FarLands.getWorld(), -330, 13, 1115),
                        0, 1, new Cooldown(10 * 60 * 20),
                        false
                ) {
                    @Override
                    protected List<Mob> spawn(Location location1) {
                        List<Mob> boss = super.spawn(location1);
                        if (boss.isEmpty())
                            return boss;
                        FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(
                                ChatColor.DARK_AQUA + ChatColor.BOLD.toString() + "Specter" + ChatColor.RESET + " has been summoned"
                        ));
                        cooldownBossSpawns.get(DOORS.get(9).keyType.getItemMeta().getDisplayName()).reset();
                        return boss;
                    }

                    @Override
                    boolean trySpawn() {
                        if (!cooldownBossSpawns.get(DOORS.get(9).keyType.getItemMeta().getDisplayName()).isComplete())
                            return false;
                        return super.trySpawn();
                    }
                }
        );
    }
    private static Mob summonJackSkellington(Location location) {
        Skeleton boss = (Skeleton)dramaticSpawn(location, EntityType.SKELETON, 1.74F);
        boss.setCustomName(ChatColor.GOLD + ChatColor.BOLD.toString() + "Jack Skellington");
        boss.setCustomNameVisible(true);

        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(2048);
        boss.setHealth(boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        boss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);

        boss.getEquipment().setItemInMainHand(new ItemStack(Material.JACK_O_LANTERN));
        boss.getEquipment().setItemInOffHand(new ItemStack(Material.SNOWBALL));

        boss.getEquipment().setHelmet(skullItem("1cb588a4-a358-4660-abfa-9988021bd4a2", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzY4N" +
                        "2E3NDA5NWJlYzhiZjg1ZjllODEzZjEyNDk3MWExZGRkMTk5MWY3MTFiNzRmOTg0OWNmZmM5MmU0YjFjIn19fQ==",
                boss.getCustomName()
        )));

        ItemStack stack = new ItemStack(Material.CHAINMAIL_BOOTS);
        ItemMeta im = stack.getItemMeta();
        im.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, false);
        stack.setItemMeta(im);
        boss.getEquipment().setBoots(stack);

        boss.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 10, true));

        Pack support = new Pack((location1) -> {
            Skeleton mob = (Skeleton)dramaticSpawn(location1, EntityType.SKELETON, 1.74F);
            mob.setCustomName(ChatColor.GOLD + ChatColor.BOLD.toString() + "Nightmare Skeleton");
            mob.setCustomNameVisible(true);

            mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(32);
            mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
            mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(16);
            mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);

            // needs delay to render on client
            FarLands.getScheduler().scheduleSyncDelayedTask(() -> mob.getEquipment().setItemInMainHand(AIR), 1);

            return mob;
        }, Skeleton.class, location, 3, 2, new Cooldown(0), false) {
            @Override
            boolean trySpawn() {
                if (boss.isDead() || !boss.isValid())
                    return true;
                if (boss.getTarget() != null)
                    spawn(boss.getLocation()).forEach(mob -> mob.setTarget(boss.getTarget()));
                return false;
            }
        };
        tasks.put(boss.getCustomName(), FarLands.getScheduler().scheduleSyncRepeatingTask(() -> {
            if (support.trySpawn()) {
                int taskID = tasks.remove(boss.getCustomName());
                FarLands.getScheduler().getTask(taskID).setComplete(true);
                FarLands.getScheduler().cancelTask(taskID);
            }
        }, 32 * 20, 24 * 20));

        return boss;
    }
    private static Mob summonMidas(Location location) {
        Strider vehicle = (Strider)FarLands.getWorld().spawnEntity(location, EntityType.STRIDER);
        vehicle.setCustomName(ChatColor.DARK_RED + "Wraith");
        vehicle.setAdult();

        vehicle.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(512);
        vehicle.setHealth(vehicle.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        vehicle.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.2);


        PiglinBrute boss = (PiglinBrute)dramaticSpawn(location, EntityType.PIGLIN_BRUTE, 1.74F);
        boss.setCustomName(ChatColor.DARK_RED + ChatColor.BOLD.toString() + "Midas");
        boss.setCustomNameVisible(true);
        boss.setImmuneToZombification(true);

        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(2048);
        boss.setHealth(boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(16);

        boss.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1, true));

        ItemStack stack = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta im = stack.getItemMeta();
        im.addEnchant(Enchantment.KNOCKBACK, 3, false);
        im.addEnchant(Enchantment.FIRE_ASPECT, 8, false);
        stack.setItemMeta(im);
        boss.getEquipment().setItemInMainHand(stack);
        boss.getEquipment().setItemInOffHand(new ItemStack(Material.FIRE_CHARGE));

        stack = new ItemStack(Material.GOLDEN_HELMET);
        im = stack.getItemMeta();
        im.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 5, false);
        stack.setItemMeta(im);
        boss.getEquipment().setHelmet(stack);

        Pack support = new Pack((location1) -> {
            Piglin mob = (Piglin)dramaticSpawn(location1, EntityType.PIGLIN, 1.74F);
            mob.setCustomName(ChatColor.DARK_RED + "Gold Bandit");
            mob.setCustomNameVisible(true);
            mob.setImmuneToZombification(true);
            mob.setCanPickupItems(false);
            mob.setAdult();

            mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(96);
            mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
            mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12);

            mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1, true));

            mob.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));

            return mob;
        }, Piglin.class,
                location,
                4, 4, new Cooldown(0),
                false
        ) {
            @Override
            boolean trySpawn() {
                if (boss.isDead() || !boss.isValid())
                    return true;
                if (boss.getTarget() != null)
                    spawn(boss.getLocation()).forEach(mob -> mob.setTarget(boss.getTarget()));
                return false;
            }
        };
        tasks.put(boss.getCustomName(), FarLands.getScheduler().scheduleSyncRepeatingTask(() -> {
            if (support.trySpawn()) {
                int taskID = tasks.remove(boss.getCustomName());
                FarLands.getScheduler().getTask(taskID).setComplete(true);
                FarLands.getScheduler().cancelTask(taskID);
            }
        }, 10 * 20, (32 + RNG.nextInt(16)) * 20));

        vehicle.addPassenger(boss);
        return boss;
    }
    private static Mob summonHerobrine(Location location) {
        FarLands.getWorld().strikeLightningEffect(location);
        Zombie boss = (Zombie)dramaticSpawn(location, EntityType.ZOMBIE, 1.74F);
        boss.setCustomName(ChatColor.AQUA + ChatColor.BOLD.toString() + "Herobrine");
        boss.setCustomNameVisible(true);
        boss.setAdult();

        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1536);
        boss.setHealth(boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(16);
        boss.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0);
        boss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
        boss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);

        boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 1, true));

        boss.getEquipment().setHelmet(skullItem("f79df048-7881-41b1-9ec4-333a8a8d67f7", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGZmYz" +
                        "k0YmM1OWRjY2M0OGM5N2M0ZThiZGYwMTUyZWE0N2VhNDU1NWVkODUxMTNiN2M2ZThkMzcyYjVjZCJ9fX0=",
                boss.getCustomName()
        )));

        Pack support = new Pack(
                AutumnEvent::summonDarkMonk, Zombie.class,
                location,
                3, 1, new Cooldown(0),
                false
        ) {
            @Override
            boolean trySpawn() {
                if (boss.isDead() || !boss.isValid())
                    return true;
                if (boss.getTarget() != null)
                    spawn(boss.getLocation()).forEach(mob -> mob.setTarget(boss.getTarget()));
                return false;
            }
        };
        tasks.put(boss.getCustomName(), FarLands.getScheduler().scheduleSyncRepeatingTask(() -> {
            if (support.trySpawn()) {
                int taskID = tasks.remove(boss.getCustomName());
                FarLands.getScheduler().getTask(taskID).setComplete(true);
                FarLands.getScheduler().cancelTask(taskID);
            }
        }, 32 * 20, (64 + RNG.nextInt(32)) * 20));
        // mistakes were made // regrets were had
        tasks.put(boss.getCustomName() + 1, FarLands.getScheduler().scheduleSyncRepeatingTask(() -> {
            if (boss.isDead() || !boss.isValid())
                FarLands.getScheduler().cancelTask(tasks.remove(boss.getCustomName() + 1));
            else if (boss.getTarget() != null) {
                if (RNG.nextInt(3) == 0) {
                    FarLands.getWorld().strikeLightningEffect(boss.getTarget().getLocation());
                    boss.getTarget().damage(24, boss);
                } else
                    FarLands.getWorld().strikeLightningEffect(boss.getTarget().getLocation());
            }
        }, 5 * 20, (4 + RNG.nextInt(16)) * 20));

        return boss;
    }
    private static Mob summonHeadlessHorseman(Location location) {
        SkeletonHorse vehicle = (SkeletonHorse)FarLands.getWorld().spawnEntity(location, EntityType.SKELETON_HORSE);
        vehicle.setCustomName(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Dullahan");

        vehicle.setAdult();
        vehicle.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(256);
        vehicle.setHealth(vehicle.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        vehicle.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.4);


        Stray boss = (Stray)FarLands.getWorld().spawnEntity(location, EntityType.STRAY);
        boss.setCustomName(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Headless Horseman");
        boss.setCustomNameVisible(true);

        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1536);
        boss.setHealth(boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());

        ItemStack stack = new ItemStack(Material.BOW);
        ItemMeta im = stack.getItemMeta();
        im.addEnchant(Enchantment.ARROW_DAMAGE,    10, true);
        im.addEnchant(Enchantment.ARROW_KNOCKBACK,  6, true);
        im.addEnchant(Enchantment.ARROW_FIRE,       8, true);
        stack.setItemMeta(im);
        boss.getEquipment().setItemInMainHand(stack);

        boss.getEquipment().setHelmet(skullItem("87f9057b-abd3-45b9-8457-b8507a67ac55", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGM2NT" +
                        "cwZjEyNDI5OTJmNmViYTIzZWU1ODI1OThjMzllM2U3NDUzODMyNzNkZWVmOGIzOTc3NTgzZmUzY2Y1In19fQ==",
                boss.getCustomName()
        )));

        vehicle.addPassenger(boss);
        return boss;
    }
    private static Mob summonBabaYaga(Location location) {
        Evoker boss = (Evoker)dramaticSpawn(location, EntityType.EVOKER, 1.62F);
        boss.setCustomName(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Baba Yaga");
        boss.setCustomNameVisible(true);
        boss.setPatrolLeader(false);

        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1536);
        boss.setHealth(boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        boss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.7);

        Pack support = new Pack((location1) -> {
            Vex mob = (Vex)FarLands.getWorld().spawnEntity(location1, EntityType.VEX);
            // a shiny
            if (RNG.nextInt(8192) == 0) {
                mob.setCustomName(ChatColor.AQUA + "Vexento");
                FarLands.getScheduler().scheduleSyncDelayedTask(() -> mob.getEquipment().setItemInMainHand(new ItemStack(Material.MUSIC_DISC_FAR)), 1);
                FarLands.getScheduler().scheduleSyncDelayedTask(() -> mob.getEquipment().setItemInOffHand(new ItemStack(Material.MUSIC_DISC_WAIT)), 1);
            } else {
                mob.setCustomName(ChatColor.LIGHT_PURPLE + "Yaga Fairy");
                FarLands.getScheduler().scheduleSyncDelayedTask(() -> mob.getEquipment().setItemInMainHand(new ItemStack(Material.PRISMARINE_SHARD)), 1);
            }
            mob.setCustomNameVisible(true);
            mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(8);
            return mob;
        }, Vex.class, location, 2, 3, new Cooldown(0), true) {
            @Override
            boolean trySpawn() {
                if (boss.isDead() || !boss.isValid())
                    return true;
                if (boss.getTarget() != null)
                    spawn(boss.getLocation()).forEach(mob -> mob.setTarget(boss.getTarget()));
                return false;
            }
        };
        tasks.put(boss.getCustomName(), FarLands.getScheduler().scheduleSyncRepeatingTask(() -> {
            if (support.trySpawn()) {
                int taskID = tasks.remove(boss.getCustomName());
                FarLands.getScheduler().getTask(taskID).setComplete(true);
                FarLands.getScheduler().cancelTask(taskID);
            }
        }, 10 * 20, (16 + RNG.nextInt(16)) * 20));
        return boss;
    }
    private static Mob summonArchane(Location location) {
        Spider boss = (Spider)dramaticSpawn(location, EntityType.SPIDER, 0.65F);
        boss.setCustomName(ChatColor.RED + ChatColor.BOLD.toString() + "Archane's Mount");

        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1536);
        boss.setHealth(boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(14);


        Skeleton rider = (Skeleton)FarLands.getWorld().spawnEntity(location, EntityType.SKELETON);
        rider.setCustomName(ChatColor.RED + ChatColor.BOLD.toString() + "Archane");
        rider.setCustomNameVisible(true);

        rider.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(512);
        rider.setHealth(rider.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());

        ItemStack stack = new ItemStack(Material.BOW);
        ItemMeta im = stack.getItemMeta();
        im.addEnchant(Enchantment.ARROW_DAMAGE, 5, true);
        stack.setItemMeta(im);
        rider.getEquipment().setItemInMainHand(stack);

        rider.getEquipment().setHelmet(skullItem("bf0be255-89ce-415a-b7ca-037424dce343", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODMwM" +
                        "Dk4NmVkMGEwNGVhNzk5MDRmNmFlNTNmNDllZDNhMGZmNWIxZGY2MmJiYTYyMmVjYmQzNzc3ZjE1NmRmOCJ9fX0=",
                rider.getCustomName()
        )));

        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestplateMeta = (LeatherArmorMeta)chestplate.getItemMeta();
        chestplateMeta.setColor(Color.fromRGB(0xB02E26));
        chestplate.setItemMeta(chestplateMeta);
        rider.getEquipment().setChestplate(chestplate);

        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta leggingMeta = (LeatherArmorMeta)leggings.getItemMeta();
        leggingMeta.setColor(Color.fromRGB(0xB02E26));
        leggings.setItemMeta(leggingMeta);
        rider.getEquipment().setLeggings(leggings);

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootMeta = (LeatherArmorMeta)boots.getItemMeta();
        bootMeta.setColor(Color.fromRGB(0xB02E26));
        boots.setItemMeta(bootMeta);
        rider.getEquipment().setBoots(boots);

        Pack support = new Pack((location1) -> {
            CaveSpider mob = (CaveSpider)FarLands.getWorld().spawnEntity(location1, EntityType.CAVE_SPIDER);
            mob.setCustomName(ChatColor.RED + "Toxic Minion");
            mob.setCustomNameVisible(true);

            mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(64);
            mob.setHealth(mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
            mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(8);

            return mob;
        }, CaveSpider.class, location, 5, 2, new Cooldown(0), false) {
            @Override
            boolean trySpawn() {
                if (boss.isDead() || !boss.isValid())
                    return true;
                if (boss.getTarget() != null)
                    spawn(boss.getLocation()).forEach(mob -> mob.setTarget(boss.getTarget()));
                return false;
            }
        };
        tasks.put(rider.getCustomName(), FarLands.getScheduler().scheduleSyncRepeatingTask(() -> {
            if (support.trySpawn()) {
                int taskID = tasks.remove(rider.getCustomName());
                FarLands.getScheduler().getTask(taskID).setComplete(true);
                FarLands.getScheduler().cancelTask(taskID);
            }
        }, 10 * 20, (32 + RNG.nextInt(16)) * 20));

        boss.addPassenger(rider);
        return boss;
    }
    private static Mob summonBlight(Location location) {
        Wither boss = (Wither)dramaticSpawn(location, EntityType.WITHER, 3F);
        boss.setCustomName(ChatColor.GRAY + ChatColor.BOLD.toString() + "Blight");
        boss.setCustomNameVisible(true);

        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1536);
        boss.setHealth(boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        boss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);

        return boss;
    }
    private static Mob summonSpecter(Location location) {
        Zombie boss = (Zombie)dramaticSpawn(location, EntityType.ZOMBIE, 1.74F);
        boss.setCustomName(ChatColor.DARK_AQUA + ChatColor.BOLD.toString() + "Specter");
        boss.setCustomNameVisible(true);
        boss.setAdult();

        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(2048);
        boss.setHealth(boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(16);
        boss.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0);
        boss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);

        boss.addPotionEffect(new PotionEffect(PotionEffectType.JUMP,         Integer.MAX_VALUE, 1, true));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 1, true));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, true));

        // https://minecraft-heads.com/custom-heads/humanoid/40551-flame-demon
        boss.getEquipment().setHelmet(skullItem("f5ba9c75-ebf6-4ad8-b5e3-5e5a1d213011", new SkullData(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDRiYT" +
                        "RhYjRmOTNiODJlNjE0MTI4OTgwMWMzOWUwYmMyNzBjYmU1MTc5ZGY3NmU4NWI1NDMwNmMyNzhjMTdkYSJ9fX0=",
                boss.getCustomName()
        )));

        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestplateMeta = (LeatherArmorMeta)chestplate.getItemMeta();
        chestplateMeta.setColor(Color.fromRGB(0xFFFFFF));
        chestplate.setItemMeta(chestplateMeta);
        boss.getEquipment().setChestplate(chestplate);

        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta leggingsMeta = (LeatherArmorMeta)leggings.getItemMeta();
        leggingsMeta.setColor(Color.fromRGB(0xFFFFFF));
        leggings.setItemMeta(leggingsMeta);
        boss.getEquipment().setLeggings(leggings);

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta)boots.getItemMeta();
        bootsMeta.setColor(Color.fromRGB(0xFFFFFF));
        boots.setItemMeta(bootsMeta);
        boss.getEquipment().setBoots(boots);

        Pack support = new Pack(
                AutumnEvent::summonGostlin, Zombie.class,
                location,
                4, 4, new Cooldown(0),
                false
        ) {
            @Override
            boolean trySpawn() {
                if (boss.isDead() || !boss.isValid())
                    return true;
                if (boss.getTarget() != null)
                    spawn(boss.getLocation()).forEach(mob -> mob.setTarget(boss.getTarget()));
                return false;
            }
        };
        tasks.put(boss.getCustomName(), FarLands.getScheduler().scheduleSyncRepeatingTask(() -> {
            if (support.trySpawn()) {
                int taskID = tasks.remove(boss.getCustomName());
                FarLands.getScheduler().getTask(taskID).setComplete(true);
                FarLands.getScheduler().cancelTask(taskID);
            }
        }, 24 * 20, (64 + RNG.nextInt(32)) * 20));

        return boss;
    }

    private void forceExitKillers(UUID uid, String name) {
        soulbound.get(uid).keySet().stream().map(Bukkit.getServer()::getPlayer).forEach(player -> {
            if (player == null)
                return;
            PLAYER_RESPAWNS.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Congratulations on defeating " + name + ChatColor.GREEN +
                    ", you will now be returned to the event map...");
            FarLands.getScheduler().scheduleSyncDelayedTask(() -> player.teleport(SPAWN), 100);
        });
    }

    private String getKillersNames(UUID uid) {
        if (!soulbound.containsKey(uid))
            return "";

        StringBuilder stringBuilder = new StringBuilder();
        UUID[] killers = new UUID[soulbound.get(uid).keySet().size()];
        killers = soulbound.get(uid).keySet().toArray(killers);
        if (killers.length <= 0)
            return "";

        stringBuilder.append("[").append(Bukkit.getPlayer(killers[0]).getName());
        for (int i = 1; i < killers.length; ++i)
            stringBuilder.append(", ").append(Bukkit.getPlayer(killers[i]).getName());
        stringBuilder.append("]");

        return stringBuilder.toString();
    }

    private static int calcDroppedEXP(LivingEntity entity) {
        double exp = 1;
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null)
            exp = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue();
        exp *= entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        return (int) (exp) >> 5;
    }

    /**
     * Handle drops
     * @param event the event
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isInEvent(event.getEntity().getLocation()))
            return;

        event.getDrops().clear();
        event.setDroppedExp(calcDroppedEXP(event.getEntity()));

        if (event.getEntity().getKiller() == null)
            return;

        // get the item to give to players that contributed sufficiently to mobs death
        final String name = event.getEntity().getCustomName() == null ? "" : Chat.removeColorCodes(event.getEntity().getCustomName());
        Pair<ItemStack, Integer> reward = new Pair<>(null, 0);
        switch (name) {
            // Pumpkin Patch
            case "Possessed Pumpkin": {
                reward.setFirst(DOORS.get(0).keyType);
                reward.setSecond(24);
                break;
            }
            case "Axe Murderer": {
                reward.setFirst(DOORS.get(0).keyType);
                reward.setSecond(4);
                break;
            }

            case "Jack Skellington": {
                FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(event.getEntity().getCustomName() +
                        ChatColor.RESET + " was defeated by " + ChatColor.GOLD + getKillersNames(event.getEntity().getUniqueId()) +
                        ChatColor.RESET + " and will not spawn again for 10 minutes!"));
                forceExitKillers(event.getEntity().getUniqueId(), event.getEntity().getCustomName());
                cooldownBossSpawns.get(DOORS.get(0).keyType.getItemMeta().getDisplayName()).reset();
                reward.setFirst(COLLECTABLES.get(0));
                reward.setSecond(1);
                break;
            }

            // Netherscape
            case "Abomination": {
                reward.setFirst(DOORS.get(1).keyType);
                reward.setSecond(24);
                break;
            }
            case "Fortress Defender":
            case "Volcanic Creation":{
                reward.setFirst(DOORS.get(1).keyType);
                reward.setSecond(16);
                break;
            }
            case "Molten Slime": {
                reward.setFirst(DOORS.get(1).keyType);
                // Range between 3 and 5
                int size = ((MagmaCube) event.getEntity()).getSize() - 2;
                // Range between 1 and 3
                reward.setSecond((int) Math.ceil(18f / (size * size))); // 2 5 18
                break;
            }
            case "Shrieking Specter": {
                reward.setFirst(DOORS.get(1).keyType);
                reward.setSecond(1);
                break;
            }

            case "Midas": {
                FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(event.getEntity().getCustomName() +
                        ChatColor.RESET + " was defeated by " + ChatColor.GOLD + getKillersNames(event.getEntity().getUniqueId()) +
                        ChatColor.RESET + " and will not spawn again for 10 minutes!"));
                forceExitKillers(event.getEntity().getUniqueId(), event.getEntity().getCustomName());
                cooldownBossSpawns.get(DOORS.get(1).keyType.getItemMeta().getDisplayName()).reset();
                reward.setFirst(COLLECTABLES.get(1));
                reward.setSecond(1);
                if (event.getEntity().getVehicle() != null)
                    event.getEntity().getVehicle().remove();
                break;
            }

            // Graveyard
            case "Decomposing Bones":
            case "Rotten Corpse": {
                reward.setFirst(DOORS.get(3).keyType);
                reward.setSecond(12);
                break;
            }
            case "Maggot": {
                reward.setFirst(DOORS.get(3).keyType);
                reward.setSecond(8);
                break;
            }
            case "Cadaver":
            case "Dark Monk": {
                reward.setFirst(DOORS.get(3).keyType);
                reward.setSecond(4);
                break;
            }

            case "Herobrine": {
                FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(event.getEntity().getCustomName() +
                        ChatColor.RESET + " was defeated by " + ChatColor.GOLD + getKillersNames(event.getEntity().getUniqueId()) +
                        ChatColor.RESET + " and will not spawn again for 10 minutes!"));
                forceExitKillers(event.getEntity().getUniqueId(), event.getEntity().getCustomName());
                cooldownBossSpawns.get(DOORS.get(3).keyType.getItemMeta().getDisplayName()).reset();
                reward.setFirst(COLLECTABLES.get(2));
                reward.setSecond(1);
                break;
            }

            // Autumn Woods
            case "Scarecrow": {
                reward.setFirst(DOORS.get(4).keyType);
                reward.setSecond(6);
                break;
            }
            case "Forest Huntsman": {
                reward.setFirst(DOORS.get(4).keyType);
                reward.setSecond(8);
                break;
            }
            case "Forest Minion": {
                reward.setFirst(DOORS.get(4).keyType);
                reward.setSecond(10);
                if (event.getEntity().getVehicle() != null)
                    event.getEntity().getVehicle().remove();
                break;
            }
            case "Forest Golem": {
                reward.setFirst(DOORS.get(4).keyType);
                reward.setSecond(4);
                break;
            }

            case "Candy Wraith": {
                reward.setSecond(2);
                reward.setFirst(CATAKEY);
                if (event.getEntity().getVehicle() != null)
                    event.getEntity().getVehicle().remove();
                break;
            }
            case "Headless Horseman": {
                FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(event.getEntity().getCustomName() +
                        ChatColor.RESET + " was defeated by " + ChatColor.GOLD + getKillersNames(event.getEntity().getUniqueId()) +
                        ChatColor.RESET + " and will not spawn again for 10 minutes!"));
                forceExitKillers(event.getEntity().getUniqueId(), event.getEntity().getCustomName());
                cooldownBossSpawns.get(DOORS.get(4).keyType.getItemMeta().getDisplayName()).reset();
                reward.setFirst(COLLECTABLES.get(3));
                reward.setSecond(1);
                if (event.getEntity().getVehicle() != null)
                    event.getEntity().getVehicle().remove();
                break;
            }

            // Witch Swamp
            case "Old Hag": {
                reward.setFirst(DOORS.get(6).keyType);
                reward.setSecond(4);
                break;
            }
            case "Sludge Heap": {
                reward.setFirst(DOORS.get(6).keyType);
                // Range between 3 and 5
                int size = ((MagmaCube) event.getEntity()).getSize() - 2;
                // Range between 1 and 3
                reward.setSecond((int) Math.ceil(9f / (size * size))); // 1 3 9
                break;
            }

            case "Baba Yaga": {
                FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(event.getEntity().getCustomName() +
                        ChatColor.RESET + " was defeated by " + ChatColor.GOLD + getKillersNames(event.getEntity().getUniqueId()) +
                        ChatColor.RESET + " and will not spawn again for 10 minutes!"));
                forceExitKillers(event.getEntity().getUniqueId(), event.getEntity().getCustomName());
                cooldownBossSpawns.get(DOORS.get(6).keyType.getItemMeta().getDisplayName()).reset();
                reward.setFirst(COLLECTABLES.get(4));
                reward.setSecond(1);
                break;
            }

            // Spider Abyss
            case "Goliath Spider": {
                reward.setFirst(DOORS.get(7).keyType);
                reward.setSecond(16);
                break;
            }
            case "Nimble Spider": {
                event.getEntity().getPassengers().forEach(Entity::remove);
                // fall through
            }
            case "Recluse Spider":
            case "Common House Spider": {
                reward.setFirst(DOORS.get(7).keyType);
                reward.setSecond(12);
                break;
            }
            case "Tarantula": {
                reward.setFirst(DOORS.get(7).keyType);
                reward.setSecond(8);
                break;
            }
            case "Creepy Spider": {
                reward.setFirst(DOORS.get(7).keyType);
                reward.setSecond(1);
                break;
            }

            case "Archane's Mount": {
                FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(event.getEntity().getCustomName() +
                        ChatColor.RESET + " was defeated by " + ChatColor.GOLD + getKillersNames(event.getEntity().getUniqueId()) +
                        ChatColor.RESET + " and will not spawn again for 10 minutes!"));
                forceExitKillers(event.getEntity().getUniqueId(), event.getEntity().getCustomName());
                cooldownBossSpawns.get(DOORS.get(7).keyType.getItemMeta().getDisplayName()).reset();
                reward.setFirst(COLLECTABLES.get(5));
                reward.setSecond(1);
                event.getEntity().getPassengers().forEach(Entity::remove);
                break;
            }

            // Soul Sand Valley
            case "Lost Traveller":
            case "Withered Explorer": {
                reward.setFirst(DOORS.get(8).keyType);
                reward.setSecond(8);
                break;
            }

            case "Blight": {
                FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(event.getEntity().getCustomName() +
                        ChatColor.RESET + " was defeated by " + ChatColor.GOLD + getKillersNames(event.getEntity().getUniqueId()) +
                        ChatColor.RESET + " and will not spawn again for 10 minutes!"));
                forceExitKillers(event.getEntity().getUniqueId(), event.getEntity().getCustomName());
                cooldownBossSpawns.get(DOORS.get(8).keyType.getItemMeta().getDisplayName()).reset();
                reward.setFirst(COLLECTABLES.get(6));
                reward.setSecond(1);
                break;
            }

            // Haunted Forest
            case "Spore":
            case "Gostlin": {
                reward.setFirst(DOORS.get(9).keyType);
                reward.setSecond(8);
                break;
            }

            case "Specter": {
                FarLands.getWorld().getPlayers().forEach(player -> player.sendMessage(event.getEntity().getCustomName() +
                        ChatColor.RESET + " was defeated by " + ChatColor.GOLD + getKillersNames(event.getEntity().getUniqueId()) +
                        ChatColor.RESET + " and will not spawn again for 10 minutes!"));
                forceExitKillers(event.getEntity().getUniqueId(), event.getEntity().getCustomName());
                cooldownBossSpawns.get(DOORS.get(9).keyType.getItemMeta().getDisplayName()).reset();
                reward.setFirst(COLLECTABLES.get(7));
                reward.setSecond(1);
                break;
            }
        }

        if (event.getEntity() instanceof Zombie) {
            for (GraveZombie graveZombie : GRAVE_ZOMBIES) {
                if (name.equals(graveZombie.name)) {
                    reward.setSecond(7);
                    reward.setFirst(graveZombie.head);
                }
            }
        }

        if (reward.getSecond() == 0)
            return;
        // give the item
        Map<UUID, Double> monsterSoulbound = soulbound.get(event.getEntity().getUniqueId());
        for (UUID uid : monsterSoulbound.keySet()) {
            Player player = Bukkit.getPlayer(uid);
            if (player == null || RNG.nextInt(reward.getSecond()) > 0)
                continue;
            // damage contributed to death must be greater than 0.73 * max health of mob / # of players that damaged it
            if (monsterSoulbound.get(uid) > 0.73 * event.getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() /
                    soulbound.get(event.getEntity().getUniqueId()).entrySet().size()) {
                FLUtils.giveItem(player, reward.getFirst(), true);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "You received a drop!");
                if (reward.getFirst().isSimilar(CATAKEY))
                    player.sendMessage(ChatColor.GREEN + "The crypt doors are rusty, try dropping this key in front of their doors at -376 61 798");
                for (Door door : DOORS) {
                    if (reward.getFirst().isSimilar(door.keyType) && Arrays.stream(player.getInventory().getStorageContents())
                            .filter(item -> item != null && item.isSimilar(door.keyType)).findFirst().orElse(AIR).getAmount() % door.requiredKeys == 0)
                        player.sendMessage(ChatColor.GREEN + "You have enough keys to use the door near " +
                                door.entrance.getMin().getBlockX() + " " + door.entrance.getMin().getBlockY() + " " + door.entrance.getMin().getBlockZ());
                }
            } else
                player.sendMessage(ChatColor.GRAY + "You didn't do enough damage to receive an item");
        }
    }

    private static Entity getDamager(Entity entity) {
        ProjectileSource shooter = null;
        if (entity instanceof Projectile)
            shooter = ((Projectile) entity).getShooter();
        else if (entity instanceof AreaEffectCloud)
            shooter = ((AreaEffectCloud) entity).getSource();
        if (shooter != null)
            return (Entity) shooter;
        return entity;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
        if (!isInEvent(event.getEntity().getLocation()))
            return;
        Entity damager = getDamager(event.getDamager());
        if (!(damager instanceof Player))
            return;
        final UUID damagedUID = event.getEntity().getUniqueId();
        if (!soulbound.containsKey(damagedUID))
            soulbound.put(damagedUID, new HashMap<>());
        Map<UUID, Double> monsterSoulbound = soulbound.get(damagedUID);
        final UUID attackerUID = damager.getUniqueId();
        if (!monsterSoulbound.containsKey(attackerUID))
            monsterSoulbound.put(attackerUID, event.getDamage());
        else
            monsterSoulbound.put(attackerUID, monsterSoulbound.get(attackerUID) + event.getDamage());
    }

    private void respawnPlayer(Player player) {
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setFireTicks(0);
        player.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(player::removePotionEffect);

        Location respawn = SPAWN;
        if (PLAYER_RESPAWNS.containsKey(player.getUniqueId())) {
            PLAYER_RESPAWNS.get(player.getUniqueId()).setSecond(
                    PLAYER_RESPAWNS.get(player.getUniqueId()).getSecond() - 1
            );
            respawn = PLAYER_RESPAWNS.get(player.getUniqueId()).getFirst();
            if (PLAYER_RESPAWNS.get(player.getUniqueId()).getSecond() == 0)
                PLAYER_RESPAWNS.remove(player.getUniqueId());
        } else {
            double min = Double.MAX_VALUE;
            for (Location rRespawn : REGION_RESPAWNS) {
                if (rRespawn.distanceSquared(player.getLocation()) < min) {
                    min = rRespawn.distanceSquared(player.getLocation());
                    respawn = rRespawn;
                }
            }
        }

        FLUtils.tpPlayer(player, respawn);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamagedByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player && isInEvent(event.getEntity().getLocation())))
            return;
        Player player = (Player) event.getEntity();
        if (player.getHealth() - event.getFinalDamage() > 0.0)
            return;

        Entity damager = getDamager(event.getDamager());
        final String name = damager.getCustomName() == null ? damager.getName() : damager.getCustomName();
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getEntity().getUniqueId());
        FarLands.getWorld().getPlayers().forEach(player1 -> player1.sendMessage(flp.rank.getNameColor() +
                flp.getDisplayName() + ChatColor.RESET + " was killed by " +
                name + ChatColor.RESET + " at the Autumn Event"));

        event.setCancelled(true);
        respawnPlayer(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent)
            return;
        if (!(event.getEntity() instanceof Player && isInEvent(event.getEntity().getLocation())))
            return;

        final Player player = (Player) event.getEntity();
        final Region clone = new Region(DROPPER.getMin().clone().subtract(0, 12, 0), DROPPER.getMax());
        if (clone.contains(player.getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (player.getHealth() - event.getFinalDamage() > 0.0)
            return;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(event.getEntity().getUniqueId());
        FarLands.getWorld().getPlayers().forEach(player1 -> player1.sendMessage(flp.rank.getNameColor() + flp.getDisplayName() +
                ChatColor.RESET + " died at the Autumn Event"));

        event.setCancelled(true);
        respawnPlayer(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isInEvent(event.getFrom()))
            return;

        Player player = event.getPlayer();
        if (player.getVelocity().getY() > -0.1 && DROPPER.contains(player.getLocation()) && !canBypass(player)) {
            if (!DROPPER_PLAYERS.containsKey(player.getUniqueId()))
                DROPPER_PLAYERS.put(player.getUniqueId(), true);
            if (DROPPER_PLAYERS.get(player.getUniqueId())) {
                DROPPER_PLAYERS.put(player.getUniqueId(), false);
                player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 5.0F, 1.0F);
                player.sendMessage(ChatColor.RED + "You did not make it to the bottom of the dropper!");
                FarLands.getScheduler().scheduleSyncDelayedTask(() -> {
                    DROPPER_START.setYaw(player.getLocation().getYaw());
                    DROPPER_START.setPitch(player.getLocation().getPitch());
                    FLUtils.tpPlayer(player, DROPPER_START);
                    DROPPER_PLAYERS.put(player.getUniqueId(), true);
                }, 30);
            }
        }
        for (Door door : DOORS) {
            if (door.entrance.contains(event.getFrom()) || !door.entrance.contains(event.getTo()))
                continue;

            if (!cooldownBossSpawns.get(door.keyType.getItemMeta().getDisplayName()).isComplete()) {
                player.sendMessage(ChatColor.RED + "This boss is on cooldown, try again later");
                if (!door.teleports) {
                    final Location location = event.getFrom();
                    location.setYaw(location.getYaw() + 180);
                    player.teleport(location);
                }
                return;
            }
            if (canBypass(player)) {
                if (door.teleports) {
                    player.sendMessage(ChatColor.GREEN + "Teleporting to Boss Room.");
                    player.teleport(door.exit);
                } else
                    player.sendMessage(ChatColor.GREEN + "Keys accepted, granting passage");
                PLAYER_RESPAWNS.put(player.getUniqueId(), new Pair<>(door.exit, 3));
                return;
            }
            ItemStack stack = player.getInventory().getItemInMainHand();
            if (door.keyType.isSimilar(stack) && stack.getAmount() >= door.requiredKeys) {
                stack.setAmount(stack.getAmount() - door.requiredKeys);
                player.getInventory().setItemInMainHand(stack.getAmount() == 0 ? AIR : stack);
                if (door.teleports) {
                    player.sendMessage(ChatColor.GREEN + "Teleporting to Boss Room.");
                    player.teleport(door.exit);
                } else
                    player.sendMessage(ChatColor.GREEN + "Keys accepted, granting passage");
                PLAYER_RESPAWNS.put(player.getUniqueId(), new Pair<>(door.exit, 3));
            } else {
                player.sendMessage(door.failMessage);
                if (!door.teleports) {
                    final Location location = event.getFrom();
                    location.setYaw(location.getYaw() + 180);
                    player.teleport(location);
                }
            }
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!isInEvent(event.getFrom()) && isInEvent(event.getTo())) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player.getUniqueId());
            if (flp.ptime >= 0)
                ptimeStore.put(flp.uuid, flp.ptime);
            flp.ptime = 13000;
            player.setPlayerTime(flp.ptime, false);
        } else if (isInEvent(event.getFrom()) && !isInEvent(event.getTo())) {
            long ptime = ptimeStore.getOrDefault(player.getUniqueId(), -1L);
            if (ptime >= 0)
                player.setPlayerTime(ptime, false);
            else
                player.resetPlayerTime();
            return;
        }
        if (!isInEvent(event.getFrom()) || canBypass(player))
            return;

        if (PlayerTeleportEvent.TeleportCause.ENDER_PEARL.equals(event.getCause()) ||
                PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT.equals(event.getCause())) {
            player.sendMessage(ChatColor.RED + "You cannot use ender pearls or chorus fruit in the autumn event.");
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player || isInEvent(event.getEntity().getLocation()))
            return;
        if (!Entities.isMonster(event.getDismounted().getType()))
            event.getDismounted().remove();
    }

    @EventHandler(ignoreCancelled = true)
    public void onSilverfishEnterBlock(EntityChangeBlockEvent event) {
        if (!isInEvent(event.getEntity().getLocation()))
            return;
        event.setCancelled(event.getEntityType() == EntityType.SILVERFISH);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodTick(FoodLevelChangeEvent event) {
        if (isInEvent(event.getEntity().getLocation()) && event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if (isInEvent(event.getLocation()) && !ALLOWED_SPAWNS.contains(event.getSpawnReason()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onElytraOpened(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        final Player player = ((Player) event.getEntity());
        if (!isInEvent(event.getEntity().getLocation()) || canBypass(player))
            return;
        player.setGliding(false);
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!isInEvent(event.getRightClicked().getLocation()) || canBypass(event.getPlayer()))
            return;
        if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.LEAD)
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlaced(BlockPlaceEvent event) {
        if (!isInEvent(event.getBlock().getLocation()) || canBypass(event.getPlayer()))
            return;

        event.getPlayer().sendMessage(ChatColor.RED + "You can't place that here.");
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isInEvent(event.getBlock().getLocation()) && event.getBlock().getType() == Material.SLIME_BLOCK) {
            return;
        }
        if (!isInEvent(event.getBlock().getLocation()) || canBypass(event.getPlayer()))
            return;

        if (event.getBlock().getType().getHardness() <= 0 && event.getBlock().isPassable()) {
            FarLands.getScheduler().scheduleSyncDelayedTask(() -> {
                event.getPlayer().sendBlockChange(event.getBlock().getLocation(), Material.AIR.createBlockData());
                if (event.getBlock().getRelative(BlockFace.DOWN).getType().getHardness() <= 0 && event.getBlock().getRelative(BlockFace.DOWN).isPassable())
                    event.getPlayer().sendBlockChange(event.getBlock().getRelative(BlockFace.DOWN).getLocation(), Material.AIR.createBlockData());
                if (event.getBlock().getRelative(BlockFace.UP).getType().getHardness() <= 0 && event.getBlock().getRelative(BlockFace.UP).isPassable())
                    event.getPlayer().sendBlockChange(event.getBlock().getRelative(BlockFace.UP).getLocation(), Material.AIR.createBlockData());
            }, 1);
        } else
            event.getPlayer().sendMessage(ChatColor.RED + "You can't break that here.");
        event.setCancelled(true);
    }

    static ItemStack skullItem(String owner, SkullData data) {
        NBTTagCompound tag = new NBTTagCompound();
        if (data.name != null) {
            NBTTagCompound display = new NBTTagCompound();
            // TODO: unhack this
            display.setString("Name", "{\"text\":\"" + data.name + "\"}");
            NBTTagList lore = new NBTTagList();
            for (String line : data.lore)
                lore.add(NBTTagString.a("{\"text\":\"" + line + "\"}"));
            display.set("Lore", lore);
            tag.set("display", display);
        }
        NBTTagCompound skullOwner = new NBTTagCompound();
        skullOwner.setString("Id", owner);
        NBTTagCompound properties = new NBTTagCompound();
        NBTTagList textures = new NBTTagList();
        NBTTagCompound textures_0 = new NBTTagCompound();
        textures_0.setString("Value", data.textures);
        textures.add(textures_0);
        properties.set("textures", textures);
        skullOwner.set("Properties", properties);
        tag.set("SkullOwner", skullOwner);
        net.minecraft.server.v1_16_R3.ItemStack dropCB = new net.minecraft.server.v1_16_R3.ItemStack(Items.PLAYER_HEAD, 1);
        dropCB.setTag(tag);
        return CraftItemStack.asBukkitCopy(dropCB);
    }
}

class GraveZombie {
    final Cooldown cooldown;
    final Location location;
    final String name;

    final ItemStack head;
    final ItemStack chestplate;
    final ItemStack leggings;
    final ItemStack boots;

    GraveZombie(Location location, String name, String uuid, String texture, int chestRGB, int legsRGB, int bootsRGB) {
        cooldown = new Cooldown(3 * 60 * 20);
        this.location = location.add(0.5, -1.94, 0.5);
        this.name = name;


        this.head = skullItem(uuid, new SkullData(texture,
                ChatColor.BOLD + name,
                ChatColor.DARK_PURPLE + "Halloween Skin",
                ChatColor.GOLD + "Contest Entry",
                ChatColor.DARK_PURPLE + "October 2020",
                ChatColor.GOLD + "Farlands"
        ));

        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestplateMeta = (LeatherArmorMeta)chestplate.getItemMeta();
        chestplateMeta.setColor(Color.fromRGB(chestRGB));
        chestplate.setItemMeta(chestplateMeta);
        this.chestplate = chestplate;

        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta leggingMeta = (LeatherArmorMeta)leggings.getItemMeta();
        leggingMeta.setColor(Color.fromRGB(legsRGB));
        leggings.setItemMeta(leggingMeta);
        this.leggings = leggings;

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootMeta = (LeatherArmorMeta)boots.getItemMeta();
        bootMeta.setColor(Color.fromRGB(bootsRGB));
        boots.setItemMeta(bootMeta);
        this.boots = boots;
    }
}

interface EventMob {
    Mob spawn(Location location);
}

class Pack {
    protected final EventMob mob;
    protected final Class<? extends Mob> mobType;
    protected final Location center;
    protected final int radius;
    protected final int size;
    protected final Cooldown cooldown;
    protected final boolean skySpawn;

    Pack(EventMob mob, Class<? extends Mob> mobType, Location center, int radius, int size, Cooldown cooldown, boolean skySpawn) {
        this.mob = mob;
        this.mobType = mobType;
        this.center = center.clone().add(0.5, 0.5, 0.5);
        this.radius = radius;
        this.size = size;
        this.cooldown = cooldown;
        this.skySpawn = skySpawn;
    }

    protected List<Mob> spawn(Location center) {
        List<Mob> mobs = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            if (radius <= 0) {
                mobs.add(mob.spawn(center));
                continue;
            }
            final int xOff = 1 + RNG.nextInt(radius << 1) - radius,
                    zMax = 1 + (int) Math.sqrt(Math.pow(radius, 2) - Math.pow(xOff, 2)),
                    zOff = 1 + RNG.nextInt(zMax << 1) - zMax;
            if (skySpawn) {
                final int yMax = 1 + (int) Math.sqrt(Math.pow(radius, 2) - Math.sqrt(Math.pow(xOff, 2) + Math.pow(zOff, 2)));
                mob.spawn(center.clone().add(xOff, 1 + RNG.nextInt(yMax << 1), zOff));
            } else {
                Location location = com.kicas.rp.util.Utils.findSafe(center.clone().add(xOff, 0, zOff), 0, 256);
                if (location != null)
                    mobs.add(mob.spawn(location));
            }
        }
        return mobs;
    }

    boolean trySpawn() {
        if (!cooldown.isComplete())
            return false;
        final int checkRange = radius <= 0 ? 32 : radius + 8;
        Collection<Entity> nearbyEntities = FarLands.getWorld().getNearbyEntities(
                center.clone().subtract(0, skySpawn ? checkRange : 0, 0),
                checkRange, checkRange, checkRange);

        int count = 0;
        Player player = null;
        for (Entity entity : nearbyEntities) {
            if (mobType.isAssignableFrom(entity.getClass())) {
                ++count;
                if (radius <= 0 && entity.getLocation().distance(center) > 16) {
                    entity.teleport(center);
                }
            }
            if (entity instanceof Player)
                player = (Player)entity;
        }
        final Player player1 = player;
        if (count <= size >> 2) {
            spawn(center).forEach(mob -> mob.setTarget(player1));
            cooldown.reset();
            return true;
        }
        return false;
    }
}

class Door {
    final Region entrance;
    final Location exit;
    final ItemStack keyType;
    final int requiredKeys;
    final String failMessage;
    final boolean teleports;

    Door(Location min, Location max, Location exit, ItemStack keyType, int requiredKeys, boolean teleports, String failMessage) {
        this.entrance = new Region(min, max);
        this.exit = exit.add(0.5, 0.5, 0.5);
        this.keyType = keyType;
        this.requiredKeys = requiredKeys;
        this.failMessage = failMessage;
        this.teleports = teleports;
    }
    Door(Location min, Location max, Location exit, ItemStack keyType, int requiredKeys, boolean teleports) {
        this.entrance = new Region(min, max);
        this.exit = exit.add(0.5, 0.5, 0.5);
        this.keyType = keyType;
        this.requiredKeys = requiredKeys;
        this.teleports = teleports;
        this.failMessage = "To use this passage, you must be holding at least " + requiredKeys +
                " * " + keyType.getItemMeta().getDisplayName() + ChatColor.RESET + " in your main hand.";
    }
}

class SkullData {
    String textures;
    String name;
    String[] lore;

    SkullData(String textures, String name, String... lore) {
        this.textures = textures;
        this.name = name;
        this.lore = lore;
    }
}
