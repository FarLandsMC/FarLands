package net.farlands.sanctuary.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.Region;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import com.kicas.rp.util.Pair;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.mechanic.Restrictions;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Range;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * General util class for many purposes
 */
public final class FLUtils {

    public static final  Random                 RNG                     = new Random();
    public static final  Runnable               NO_ACTION               = () -> {
    };
    public static final  List<ChatColor>        ILLEGAL_COLORS          = Arrays.asList(ChatColor.MAGIC, ChatColor.BLACK);
    private static final ChatColor[]            COLORING                = { ChatColor.DARK_GREEN, ChatColor.GREEN, ChatColor.YELLOW,
                                                                            ChatColor.RED, ChatColor.DARK_RED };
    public static final  double                 DEGREES_TO_RADIANS      = Math.PI / 180;
    // Pattern matching "nicer" legacy hex chat color codes - &#rrggbb
    private static final Pattern                HEX_COLOR_PATTERN_SIX   = Pattern.compile("&#([0-9a-fA-F]{6})");
    // Pattern matching funny's need for 3 char hex
    private static final Pattern                HEX_COLOR_PATTERN_THREE = Pattern.compile("&#([0-9a-fA-F]{3})");
    public static        Map<Worlds, TextColor> WORLD_COLORS            = new ImmutableMap.Builder<Worlds, TextColor>()
        .put(Worlds.OVERWORLD, NamedTextColor.GREEN)
        .put(Worlds.NETHER, NamedTextColor.RED)
        .put(Worlds.END, NamedTextColor.YELLOW) // not possible, but here for completion :P
        .put(Worlds.FARLANDS, NamedTextColor.DARK_GREEN)
        .put(Worlds.POCKET, NamedTextColor.DARK_GREEN)
        .build();

    /**
     * Get a player's 3D head texture URL
     *
     * @param flp The FLP in question
     * @return A URL String
     */
    public static String getHeadUrl(OfflineFLPlayer flp) {
        String skinTexture = FLUtils.getSkinUrl(flp);
        return skinTexture != null
            ? "https://minecraft-heads.com/scripts/3d-head.php?hrh=00&aa=true&headOnly=true&ratio=6&imageUrl=" + skinTexture.substring(skinTexture.lastIndexOf('/') + 1)
            : null;
    }

    /**
     * Get a player's skin texture URL
     *
     * @param flp The FLP in question
     * @return A URL String
     */
    public static String getSkinUrl(OfflineFLPlayer flp) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + flp.uuid); // get the player's texture object as JSON
            InputStreamReader reader = new InputStreamReader(url.openStream());
            JsonObject textureProperty = JsonParser.parseReader(reader).getAsJsonObject().get("properties")
                .getAsJsonArray().get(0).getAsJsonObject(); // get .properties[0] as Object
            return JsonParser.parseString(
                    new String(Base64.getDecoder()
                                   .decode(textureProperty.get("value").getAsString()) // get .value as String
                    )
                ).getAsJsonObject()
                .get("textures").getAsJsonObject()
                .get("SKIN").getAsJsonObject()
                .get("url").getAsString(); // get .texture.SKIN.url as String
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Get the url of the latest paper release
     *
     * @return URL String for the latest paper download
     */
    public static String getLatestReleaseUrl() {
        try {

            InputStreamReader reader;
            JsonArray arr;

            // Get the latest version
            URL versionsUrl = new URL("https://papermc.io/api/v2/projects/paper");
            reader = new InputStreamReader(versionsUrl.openStream());
            arr = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("versions").getAsJsonArray(); // get .versions as Array
            String version = arr.get(arr.size() - 1).getAsString(); // get the last index of the versions array

            // get the latest build
            URL buildsUrl = new URL("https://papermc.io/api/v2/projects/paper/versions/" + version);
            reader = new InputStreamReader(buildsUrl.openStream());
            arr = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("builds").getAsJsonArray(); // get .builds as Array
            String build = arr.get(arr.size() - 1).getAsString(); // get the last index of the builds array

            // form the download url
            return String.format("https://papermc.io/api/v2/projects/paper/versions/%1$s/builds/%2$s/downloads/paper-%1$s-%2$s.jar", version, build);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if an entity will persist (not despawn)
     * @deprecated Switch to {@link Entity#isPersistent()}
     */
    @Deprecated
    public static boolean isPersistent(Entity entity) {
        return entity.isPersistent();
    }

    /**
     * Toggle an entity's persistence (ability to not despawn)
     * @deprecated Switch to {@link Entity#setPersistent(boolean)}
     */
    @Deprecated
    public static void setPersistent(Entity entity, boolean persistent) {
        entity.setPersistent(persistent);
    }

    public static List<MerchantRecipe> copyRecipeList(List<MerchantRecipe> list) {
        List<MerchantRecipe> copy = new ArrayList<>();
        list.forEach(recipe -> {
            MerchantRecipe r = new MerchantRecipe(
                recipe.getResult(),
                recipe.getUses(),
                recipe.getMaxUses(),
                recipe.hasExperienceReward(),
                recipe.getVillagerExperience(),
                recipe.getPriceMultiplier(),
                recipe.getDemand(),
                recipe.getSpecialPrice(),
                recipe.shouldIgnoreDiscounts()
            );

            recipe.getIngredients().forEach(r::addIngredient);

            copy.add(r);
        });
        return copy;
    }

    /**
     * Get the current MSPT of the server
     */
    public static double serverMspt() {
        long totalMspt = 0;
        long[] mspts = Bukkit.getServer().getTickTimes();
        for (long v : mspts) {
            totalMspt += v;
        }
        return totalMspt / (mspts.length * 1000000.0);
    }

    /**
     * Returns the item the player is holding in the given hand.
     *
     * @param player the player.
     * @param hand   the hand.
     * @return the item the player is holding in the given hand.
     */
    public static ItemStack heldItem(Player player, EquipmentSlot hand) {
        return hand == EquipmentSlot.HAND
            ? player.getInventory().getItemInMainHand()
            : player.getInventory().getItemInOffHand();
    }

    /**
     * Get the material of an item stack -- null safe
     */
    public static @Nonnull
    Material material(@Nullable ItemStack stack) {
        return stack == null ? Material.AIR : stack.getType();
    }

    /**
     * Check if a provided location is within any of the provided bounds
     */
    public static boolean isWithin(Location loc, List<Pair<Location, Location>> bounds) {
        for (Pair<Location, Location> subregion : bounds) {
            if (isWithin(loc, subregion)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if any point between two provided locations is within the bounds
     */
    public static boolean passedThrough(Location from, Location to, List<Pair<Location, Location>> bounds) {
        if (isWithin(from, bounds)) {
            return false;
        }
        if (isWithin(to, bounds)) {
            return true;
        } else { // Take small steps and check each one, account for high velocities
            double xs = to.getX() - from.getX(), ys = to.getY() - from.getY(), zs = to.getZ() - from.getZ();
            double div = Stream.of(xs, ys, zs).map(Math::abs).max(Double::compare).get() * 1.1;
            xs /= div;
            ys /= div;
            zs /= div;
            Location loc = from.clone();
            while (loc.distanceSquared(to) > 0.95) {
                loc = loc.add(xs, ys, zs);
                if (isWithin(loc, bounds)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Get text enclosed in brackets
     *
     * @param start  Initial index to check from
     * @param string String to check
     * @return Pair&lt;Content inside of brackets, index of char after brackets&gt;
     */
    public static Pair<String, Integer> getEnclosed(int start, String string) {
        boolean curved = string.charAt(start) == '('; // ()s or {}s
        int depth = 1, i = start + 1;
        while (depth > 0) { // Exits when there are no pairs of open brackets
            if (i == string.length()) // Avoid index out of bound errors
            {
                return new Pair<>(null, -1);
            }
            char c = string.charAt(i++);
            if (c == (curved ? ')' : '}')) // We've closed off a pair
            {
                --depth;
            } else if (c == (curved ? '(' : '{')) // We've started a pair
            {
                ++depth;
            }
        }
        // Return the stuff inside the brackets, and the index of the char after the last bracket
        return new Pair<>(string.substring(start + 1, i - 1), i);
    }

    /**
     * Get the color of a specific value using COLORING array
     */
    public static ChatColor color(double value, double[] coloring) {
        for (int i = 0; i < coloring.length; ++i) {
            if (value <= coloring[i]) {
                return COLORING[i];
            }
        }
        return COLORING[COLORING.length - 1];
    }

    /**
     * Get a random item from the provided list
     */
    public static <T> T selectRandom(@Nonnull List<T> list) {
        return list.isEmpty() ? null : list.get(RNG.nextInt(list.size()));
    }

    /**
     * Convert HSV to RGB
     *
     * @param h Hue
     * @param s Saturation
     * @param v Value
     * @return [R, G, B]
     */
    public static int[] hsv2rgb(double h, double s, double v) {
        double c = v * s;
        double x = c * (1 - Math.abs(((h / 60.0) % 2.0) - 1));
        double m = v - c;
        double[] prgb;
        if (h < 60) {
            prgb = new double[]{ c, x, 0 };
        } else if (h < 120) {
            prgb = new double[]{ x, c, 0 };
        } else if (h < 180) {
            prgb = new double[]{ 0, c, x };
        } else if (h < 240) {
            prgb = new double[]{ 0, x, c };
        } else if (h < 300) {
            prgb = new double[]{ x, 0, c };
        } else {
            prgb = new double[]{ c, 0, x };
        }
        return new int[]{ (int) ((prgb[0] + m) * 255), (int) ((prgb[1] + m) * 255), (int) ((prgb[2] + m) * 255) };
    }

    /**
     * Get a weighted boolean
     */
    public static boolean randomChance(@Range(from = 0, to = 1) double chance) {
        return RNG.nextDouble() < chance;
    }

    public static void changeBlocksAsync(Player player, Map<Block, WrappedBlockData> changes) {
        if (changes.isEmpty()) return;

        Map<Chunk, Map<Block, WrappedBlockData>> byChunk = new HashMap<>();
        for (Block block : changes.keySet()) // We have to split it up by chunk because that's how the packet works.
        {
            byChunk.computeIfAbsent(block.getChunk(), k -> new HashMap<>()).put(block, changes.get(block));
        }

        int delay = 1;
        final Chunk[] chunks = byChunk.keySet().toArray(new Chunk[0]);
        for (int i = 0; i < chunks.length; i += 4) { // Send packets for the blocks modified in each chunk.
            final int i0 = i;
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                for (int j = i0; j < Math.min(chunks.length, i0 + 4); ++j) {
                    Map<Block, WrappedBlockData> send = byChunk.get(chunks[j]);
                    PacketContainer pc = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
                    MultiBlockChangeInfo[] blockData = new MultiBlockChangeInfo[send.size()];
                    for (int k = 0; k < blockData.length; k++) {
                        Block key = new ArrayList<>(send.keySet()).get(k);
                        blockData[k] = new MultiBlockChangeInfo(key.getLocation(), send.get(key));
                    }

                    pc.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(chunks[j].getX(), chunks[j].getZ()));
                    pc.getMultiBlockChangeInfoArrays().write(0, blockData);
                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player, pc);
                    } catch (InvocationTargetException ex) {
                        ex.printStackTrace();
                    }
                }
            }, delay);
            delay += 4;
        }
    }

    public static void changeBlocks(Player player, Map<Block, WrappedBlockData> changes) {
        if (changes.isEmpty()) return;

        Map<Chunk, Map<Block, WrappedBlockData>> byChunk = new HashMap<>();
        for (Block block : changes.keySet()) // We have to split it up by chunk because that's how the packet works.
        {
            byChunk.computeIfAbsent(block.getChunk(), k -> new HashMap<>()).put(block, changes.get(block));
        }

        try {
            for (Chunk chunk : byChunk.keySet()) { // Send packets for the blocks modified in each chunk.
                Map<Block, WrappedBlockData> send = byChunk.get(chunk);
                PacketContainer pc = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
                MultiBlockChangeInfo[] blockData = new MultiBlockChangeInfo[send.size()];
                for (int i = 0; i < blockData.length; i++) {
                    Block key = new ArrayList<>(send.keySet()).get(i);
                    blockData[i] = new MultiBlockChangeInfo(key.getLocation(), send.get(key));
                }

                pc.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(chunk.getX(), chunk.getZ()));
                pc.getMultiBlockChangeInfoArrays().write(0, blockData);
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, pc);
            }
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get the {@link CompoundTag} of the given {@link ItemStack}
     */
    public static CompoundTag getTag(ItemStack stack) {
        return stack == null ? null : CraftItemStack.asNMSCopy(stack).getTag();
    }

    /**
     * Create a duplicate itemstack with the given tags
     */
    public static ItemStack applyTag(CompoundTag nbt, ItemStack stack) {
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        nmsStack.setTag(nbt);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    /**
     * Get a value from a map if it exists, otherwise add a new one.
     *
     * @param map   The map to manipulate
     * @param key   The key to use
     * @param value The default value to return (and add to the map)
     * @return The value that was in the map or the default value
     */
    public static <K, V> V getAndPutIfAbsent(Map<K, V> map, K key, V value) {
        V val = map.get(key);
        if (val == null) {
            map.put(key, value);
            return value;
        } else {
            return val;
        }
    }

    /**
     * Get a "random" number that has a bias
     */
    public static int biasedRandom(int bound, double bias) {
        double k0 = -(bound * bound) / bias, k1 = -(bias * bias + 2 * bias + 1) / (4 * bias), k2 = (bound * (bias + 1)) / (2 * bias);
        return (int) (-Math.sqrt(k0 * (RNG.nextDouble() + k1)) + k2);
    }

    /**
     * Get a random integer between the bounds
     *
     * @param min Lower bound
     * @param max Upper bound
     */
    public static int randomInt(int min, int max) {
        return RNG.nextInt(max - min) + min;
    }

    /**
     * Get a random double between the bounds
     *
     * @param min Lower bound
     * @param max Upper bound
     */
    public static double randomDouble(double min, double max) {
        return (max - min) * RNG.nextDouble() + min;
    }

    /**
     * Convert a date to string using the format provided
     *
     * @param date   Milliseconds since epoch
     * @param format Format for {@link SimpleDateFormat}
     * @return
     */
    public static String dateToString(long date, String format) {
        return (new SimpleDateFormat(format)).format(new Date(date));
    }

    /**
     * Serialize a {@link UUID} as bytes
     */
    public static void serializeUuid(UUID uuid, byte[] dest, int index) {
        System.arraycopy(FLUtils.serializeLong(uuid.getMostSignificantBits()), 0, dest, index, 8);
        System.arraycopy(FLUtils.serializeLong(uuid.getLeastSignificantBits()), 0, dest, index + 8, 8);
    }


    /**
     * Serialize a {@link UUID} as bytes
     */
    public static byte[] serializeUuid(UUID uuid) {
        byte[] serUuid = new byte[16];
        System.arraycopy(FLUtils.serializeLong(uuid.getMostSignificantBits()), 0, serUuid, 0, 8);
        System.arraycopy(FLUtils.serializeLong(uuid.getLeastSignificantBits()), 0, serUuid, 8, 8);
        return serUuid;
    }

    /**
     * Deserialize a {@link UUID} from bytes
     */
    public static UUID getUuid(byte[] bytes, int index) {
        return new UUID(getLong(bytes, index), getLong(bytes, index + 8));
    }

    /**
     * Serialize a long as a bytes
     */
    public static byte[] serializeLong(long num) {
        return new byte[]{
            (byte) ((num >> 56) & 0xFF),
            (byte) ((num >> 48) & 0xFF),
            (byte) ((num >> 40) & 0xFF),
            (byte) ((num >> 32) & 0xFF),
            (byte) ((num >> 24) & 0xFF),
            (byte) ((num >> 16) & 0xFF),
            (byte) ((num >> 8) & 0xFF),
            (byte) (num & 0xFF)
        };
    }

    /**
     * Deserialize a long from bytes
     */
    public static long getLong(byte[] bytes, int index) {
        return (((long) bytes[index]) & 0xFF) << 56 |
               (((long) bytes[index + 1]) & 0xFF) << 48 |
               (((long) bytes[index + 2]) & 0xFF) << 40 |
               (((long) bytes[index + 3]) & 0xFF) << 32 |
               (((long) bytes[index + 4]) & 0xFF) << 24 |
               (((long) bytes[index + 5]) & 0xFF) << 16 |
               (((long) bytes[index + 6]) & 0xFF) << 8 |
               (((long) bytes[index + 7]) & 0xFF);
    }

    /**
     * Get the item name from an {@link ItemStack}
     */
    public static String itemName(ItemStack stack) {
        String custom = stack.getItemMeta().getDisplayName();
        return custom.isEmpty() ? stack.getAmount() + " * " + capitalize(stack.getType().toString().replaceAll("_", " ")) : custom;
    }

    /**
     * Get a {@link Location} from a {@link CompoundBinaryTag}
     */
    public static Location locationFromNBT(CompoundBinaryTag nbt) {
        return new Location(
            Bukkit.getWorld(UUID.fromString(nbt.getString("world"))),
            nbt.getDouble("x"),
            nbt.getDouble("y"),
            nbt.getDouble("z"),
            nbt.getFloat("yaw"),
            nbt.getFloat("pitch")
        );
    }

    /**
     * Get a {@link CompoundBinaryTag} from a {@link Location}
     */
    public static CompoundBinaryTag locationToNBT(Location location) {
        return CompoundBinaryTag.builder()
            .putString("world", location.getWorld().getUID().toString())
            .putDouble("x", location.getX())
            .putDouble("y", location.getY())
            .putDouble("z", location.getZ())
            .putFloat("yaw", location.getYaw())
            .putFloat("pitch", location.getPitch())
            .build();
    }

    /**
     * Convert from a byte[] to an {@link ItemStack}
     */
    public static ItemStack itemStackFromNBT(byte[] bytes) {
        return bytes == null ? null : ItemStack.deserializeBytes(bytes);
    }

    /**
     * Convert from an {@link ItemStack} to a byte[]
     */
    public static byte[] itemStackToNBT(ItemStack stack) {
        return stack.serializeAsBytes();
    }


    /**
     * Check if a provided location is within two bounds
     */
    public static boolean isWithin(Location loc, Pair<Location, Location> bounds) {
        if (loc == null || bounds == null || bounds.getFirst() == null || bounds.getSecond() == null ||
            !loc.getWorld().equals(bounds.getFirst().getWorld())) {
            return false;
        }
        Location f = bounds.getFirst(), s = bounds.getSecond();
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return f.getX() < x && x < s.getX() && f.getY() < y && y < s.getY() && f.getZ() < z && z < s.getZ();
    }

    /**
     * Get current calendar month
     */
    public static @Range(from = 0, to = 11) int getMonthInYear() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        return cal.get(Calendar.MONTH);
    }

    /**
     * Give a player an item, optionally send messages
     */
    public static void giveItem(Player player, ItemStack stack, boolean sendMessage) {
        giveItem(player, player.getInventory(), player.getLocation(), stack, sendMessage);
    }

    /**
     * Give a player an item, attempting to place it in the provided inventory and then dropping it at the provided location if full
     */
    public static void giveItem(CommandSender recipient, Inventory inv, Location location, ItemStack stack, boolean sendMessage) {
        if (inv.firstEmpty() > -1) {
            inv.addItem(stack.clone());
        } else {
            location.getWorld().dropItem(location, stack);
            if (sendMessage) {
                recipient.sendMessage(ChatColor.RED + "Your inventory was full, so you dropped the item.");
            }
        }
    }

    /**
     * Check for materials surrounding the location
     */
    public static boolean checkNearby(Location location, Material... materials) {
        List<Material> types = Arrays.asList(materials);
        return Arrays.stream(BlockFace.values()).map(bf -> location.getBlock().getRelative(bf).getType()).anyMatch(types::contains);
    }

    /**
     * Teleport a player to a location (gives temporary resistance)
     */
    public static void tpPlayer(final Player player, final Location location) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 140, 7));
        player.teleport(location);
        player.setFallDistance(0);
        player.setVelocity(new Vector(0, 0.3, 0));
    }

    /**
     * Constrain a number between two bounds
     *
     * @param d   the number to constrain
     * @param min Lower bound
     * @param max Upper bound
     */
    public static double constrain(double d, double min, double max) {
        return d < min ? min : (Math.min(d, max));
    }

    /**
     * Truncate a double to three decimal places
     */
    public static String toStringTruncated(double d) {
        return toStringTruncated(d, 3);
    }

    /**
     * Truncate a double to a certain number of decimal places
     */
    public static String toStringTruncated(double d, int precision) {
        String fp = Double.toString(d);
        return fp.contains(".") ? fp.substring(0, Math.min(fp.lastIndexOf('.') + precision + 1, fp.length())) +
                                  (fp.contains("E") ? fp.substring(fp.lastIndexOf('E')) : "") : fp;
    }

    /**
     * Get the first key that matches the provided value in the provided map
     */
    public static <K, V> K getKey(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Check if a location is within the provided radius around spawn
     */
    public static boolean isInSpawn(Location loc, double radius) {
        LocationWrapper spawn = FarLands.getDataHandler().getPluginData().spawn;
        return spawn != null && "world".equals(loc.getWorld().getName()) && loc.distance(spawn.asLocation()) < radius;
    }

    /**
     * Check if the given location is within 600 blocks of spawn
     */
    public static boolean isInSpawn(Location loc) {
        return isInSpawn(loc, 600);
    }

    /**
     * Take the case pattern of one string and apply it to the other
     *
     * @param original String to get case pattern from
     * @param other    String to apply it to
     */
    public static String matchCase(String original, String other) {
        String[] ogwords = original.split(" "), owords = other.split(" ");
        for (int i = 0; i < Math.min(ogwords.length, owords.length); ++i) {
            char[] ogwc = ogwords[i].toCharArray(), owc = owords[i].toCharArray();
            for (int j = 0; j < Math.min(ogwc.length, owc.length); ++j) {
                if (Character.isUpperCase(ogwc[j])) {
                    owc[j] = Character.toUpperCase(owc[j]);
                } else {
                    owc[j] = Character.toLowerCase(owc[j]);
                }
            }
            owords[i] = new String(owc);
        }
        return String.join(" ", owords);
    }

    /**
     * Value of string that returns null rather than throwing an exception
     */
    public static <T> T safeValueOf(Function<String, T> valueOf, String name) {
        try {
            return valueOf.apply(name);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Capitalize the first letter of every word (split on spaces)
     *
     * @param x String to capitalize
     * @return Capitalized string
     */
    public static String capitalize(String x) {
        if (x == null || x.isEmpty()) {
            return x;
        }
        String[] split = x.split(" ");
        for (int i = 0; i < split.length; ++i) {
            if (!split[i].isEmpty()) {
                split[i] = Character.toUpperCase(split[i].charAt(0)) + split[i].substring(1).toLowerCase();
            }
        }
        return String.join(" ", split);
    }

    /**
     * Combine two UUIDs
     */
    public static UUID combineUUIDs(UUID a, UUID b) {
        return new UUID(a.getMostSignificantBits() ^ b.getMostSignificantBits(), a.getLeastSignificantBits() ^ b.getLeastSignificantBits());
    }

    public static boolean deltaEquals(Location a, Location b, double delta) {
        double dx = a.getX() - b.getX(), dy = a.getY() - b.getY(), dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz) < delta;
    }

    public static boolean deltaEquals(double a, double b, double delta) {
        double d = a - b;
        return (d < 0.0 ? -d : d) < delta;
    }

    /**
     * Use a default value if the provided number is &lt; 0
     * <br>
     * Useful for preventing out of bounds on array indexes
     *
     * @param index The index
     * @param def   Default value
     * @return The index or default value
     */
    public static int indexOfDefault(int index, @Range(from = 0, to = Integer.MAX_VALUE) int def) {
        return index < 0 ? def : index;
    }

    /**
     * Convert a list of pairs to a map
     *
     * @param entries All pairs
     * @param <K>     Key type
     * @param <V>     Value type
     */
    @SafeVarargs
    public static <K, V> Map<K, V> asMap(Pair<K, V>... entries) {
        if (entries.length == 0) {
            return Collections.emptyMap();
        }
        Map<K, V> map = new HashMap<>(entries.length);
        for (Pair<K, V> entry : entries) {
            map.put(entry.getFirst(), entry.getSecond());
        }
        return map;
    }

    /**
     * Hash a byte[] with MD5 algo
     */
    public static byte[] hash(byte[] data) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new InternalError(ex);
        }
        return md.digest(data);
    }

    /**
     * Remove color codes from a provided string
     */
    public static String removeColorCodes(String message) {
        return ComponentUtils.toText(ComponentUtils.parse(message));
    }

    /**
     * Apply color codes within the provided string
     *
     * @param rank    The rank (used for permsission checks)
     * @param message The message to check
     * @return The updated message
     * @deprecated <strong><em>Please switch to Components!</em></strong>
     */
    @Deprecated
    public static String applyColorCodes(Rank rank, String message) {
        if (rank == null || rank.specialCompareTo(Rank.ADEPT) < 0) {
            return removeColorCodes(message);
        }

        message = colorize(message);

        message = ChatColor.translateAlternateColorCodes('&', message);

        if (!rank.isStaff()) {
            for (ChatColor color : ILLEGAL_COLORS) {
                message = message.replaceAll(ChatColor.COLOR_CHAR + Character.toString(color.getChar()), "");
            }
        }
//        message = colorize(message);
        return message;
    }

    /**
     * Colorize a string using the color codes
     *
     * @deprecated <strong><em>Please switch to Components!</em></strong>
     */
    @Deprecated
    public static String colorize(String string) {
        // Do 6 char first since the 3 char pattern will also match 6 char occurrences
        StringBuffer sb6 = new StringBuffer();
        Matcher matcher6 = HEX_COLOR_PATTERN_SIX.matcher(string);
        while (matcher6.find()) {
            StringBuilder replacement = new StringBuilder(14).append("&x");
            for (char character : matcher6.group(1).toCharArray()) {
                replacement.append('&').append(character);
            }
            if (getLuma(replacement.toString()) > 16) {
                matcher6.appendReplacement(sb6, replacement.toString());
            } else {
                matcher6.appendReplacement(sb6, "");
            }
        }
        matcher6.appendTail(sb6);
        string = sb6.toString();

        // Now convert 3 char to the same format ex. &#363 -> &x&3&3&6&6&3&3
        StringBuffer sb3 = new StringBuffer();
        Matcher matcher3 = HEX_COLOR_PATTERN_THREE.matcher(string);
        while (matcher3.find()) {
            StringBuilder replacement = new StringBuilder(14).append("&x");
            for (char character : matcher3.group(1).toCharArray()) {
                replacement.append('&').append(character).append('&').append(character);
            }
            if (getLuma(replacement.toString()) > 16) {
                matcher3.appendReplacement(sb3, replacement.toString());
            } else {
                matcher3.appendReplacement(sb3, "");
            }
        }
        matcher3.appendTail(sb3);

        // Translate '&' to '§'
        return ChatColor.translateAlternateColorCodes('&', sb3.toString());
    }

    /**
     * Get luminescence from the provided hex string
     *
     * @param color hex string, either #rrggbb or bungee format
     */
    public static double getLuma(String color) {
        int r, g, b;
        color = color.replaceAll("[&x#]", "");

        r = Integer.valueOf(color.substring(0, 2), 16);
        g = Integer.valueOf(color.substring(2, 4), 16);
        b = Integer.valueOf(color.substring(4, 6), 16);
        return (0.2126 * r) + (0.7152 * g) + (0.0722 * b);
    }

    /**
     * Create a simple string in the format of "world - x y z"
     */
    public static String toSimpleString(Location location) {
        return String.format("%s - %s %s %s", Worlds.getByName(location.getWorld().getName()), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Get the coords of the provided location in the format of "x, y, z, world"
     */
    public static String coords(Location location) {
        return String.format("%s, %s, %s, %s", location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
    }

    /**
     * Check if a media player can fly at their location
     *
     * @param player The player to check
     * @return If the player is able to fly at the location as media
     */
    public static boolean canMediaFly(Player player) {
        return canMediaFly(player, player.getLocation());
    }

    /**
     * Check if the provided player can fly at the provided location
     */
    public static boolean canMediaFly(Player player, Location loc) {
        Region rg = RegionProtection.getDataManager().getHighestPriorityRegionAt(loc);
        return rg != null
               && (
                   rg.isOwner(player.getUniqueId())
                   || rg.isAllowed(RegionFlag.FLIGHT)
                   || rg.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(player, TrustLevel.ACCESS, rg)
               )
               || FarLands.getMechanicHandler()
                   .getMechanic(Restrictions.class)
                   .mediaFlyProtection
                   .contains(player.getUniqueId());
    }

    /**
     * Create a new List&lt;T&gt; with all elements provided
     *
     * @param initial Initial list
     * @param others  Elements to add
     * @return Immutable list
     */
    @SafeVarargs
    public static <T> List<T> join(List<T> initial, T... others) {
        List<T> list = new ArrayList<>(initial);
        list.addAll(Arrays.asList(others));
        return Collections.unmodifiableList(list);
    }
}
