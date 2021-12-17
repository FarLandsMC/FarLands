package net.farlands.sanctuary.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kicas.rp.util.Pair;
import com.kicas.rp.util.ReflectionHelper;
import com.kicas.rp.util.TextUtils2;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.player.CommandShrug;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatHexColor;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.item.trading.MerchantRecipe;
import net.minecraft.world.item.trading.MerchantRecipeList;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_18_R1.CraftServer;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
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
 * General utility methods.
 */
public final class FLUtils {
    public static final Random RNG = new Random();
    public static final Runnable NO_ACTION = () -> { };
    public static final List<ChatColor> ILLEGAL_COLORS = Arrays.asList(ChatColor.MAGIC, ChatColor.BLACK);
    private static final ChatColor[] COLORING = {ChatColor.DARK_GREEN, ChatColor.GREEN, ChatColor.YELLOW,
        ChatColor.RED, ChatColor.DARK_RED};
    public static final double DEGREES_TO_RADIANS = Math.PI / 180;
    // Pattern matching "nicer" legacy hex chat color codes - &#rrggbb
    private static final Pattern HEX_COLOR_PATTERN_SIX = Pattern.compile("&#([0-9a-fA-F]{6})");
    // Pattern matching funny's need for 3 char hex
    private static final Pattern HEX_COLOR_PATTERN_THREE = Pattern.compile("&#([0-9a-fA-F]{3})");

    private FLUtils() { }

    public static String getSkinUrl(OfflineFLPlayer flp) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + flp.uuid);
            InputStreamReader reader = new InputStreamReader(url.openStream());
            JsonObject textureProperty = JsonParser.parseReader(reader).getAsJsonObject().get("properties")
                .getAsJsonArray().get(0).getAsJsonObject();
            return JsonParser.parseString(
                new String(Base64.getDecoder()
                    .decode(textureProperty.get("value").getAsString())
                )
            ).getAsJsonObject()
                .get("textures").getAsJsonObject()
                .get("SKIN").getAsJsonObject()
                .get("url").getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean isPersistent(Entity entity) {
        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
        if (handle instanceof EntityInsentient e)
            return e.persist;
        return false;
    }

    public static void setPersistent(Entity entity, boolean persistent) {
        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
        if (handle instanceof EntityInsentient)
            ((EntityInsentient) handle).setPersistenceRequired(persistent); // ca = persistent field
    }

    public static ChatModifier chatModifier(String color) {
        try {
            Constructor<ChatModifier> constructor = ChatModifier.class.getDeclaredConstructor(
                    ChatHexColor.class,
                    Boolean.class,
                    Boolean.class,
                    Boolean.class,
                    Boolean.class,
                    Boolean.class,
                    ChatClickable.class,
                    ChatHoverable.class,
                    String.class,
                    MinecraftKey.class
            );

            constructor.setAccessible(true);
            return constructor.newInstance(ChatHexColor.a(color), null, null, null, null, null, null, null, null, null);
        } catch (Throwable t) {
            return null;
        }
    }

    public static MerchantRecipeList copyRecipeList(MerchantRecipeList list) {
        MerchantRecipeList copy = new MerchantRecipeList();
        list.forEach(recipe -> {
            MerchantRecipe r = new MerchantRecipe(
                    recipe.a, // buyItem1
                    recipe.b, // buyItem2
                    recipe.c, // sellItem
                    recipe.d, // uses
                    recipe.e, // maxUses
                    recipe.j, // xp
                    recipe.i, // priceMultiplier
                    (int) ReflectionHelper.getFieldValue("h", MerchantRecipe.class, recipe) // demand
            );

            ReflectionHelper.setNonFinalFieldValue("f", MerchantRecipe.class, r, ReflectionHelper.getFieldValue("f", Recipe.class, recipe));
            copy.add(r);
        });
        return copy;
    }

    public static double serverMspt() {
        long totalMspt = 0;
        long[] mspts = ((CraftServer)Bukkit.getServer()).getServer().p; // p = mspts field
        for (long v : mspts)
            totalMspt += v;
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
        return EquipmentSlot.HAND.equals(hand) ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
    }

    public static Material material(ItemStack stack) {
        return stack == null ? Material.AIR : stack.getType();
    }

    public static boolean isWithin(Location loc, List<Pair<Location, Location>> region) {
        for (Pair<Location, Location> subregion : region) {
            if (isWithin(loc, subregion))
                return true;
        }
        return false;
    }

    public static boolean passedThrough(Location from, Location to, List<Pair<Location, Location>> region) {
        if (isWithin(from, region))
            return false;
        if (isWithin(to, region))
            return true;
        else { // Take small steps and check each one, account for high velocities
            double xs = to.getX() - from.getX(), ys = to.getY() - from.getY(), zs = to.getZ() - from.getZ();
            double div = Stream.of(xs, ys, zs).map(Math::abs).max(Double::compare).get() * 1.1;
            xs /= div;
            ys /= div;
            zs /= div;
            Location loc = from.clone();
            while (loc.distanceSquared(to) > 0.95) {
                loc = loc.add(xs, ys, zs);
                if (isWithin(loc, region))
                    return true;
            }
            return false;
        }
    }

    // Get text enclosed in brackets
    public static Pair<String, Integer> getEnclosed(int start, String string) {
        boolean curved = string.charAt(start) == '('; // ()s or {}s
        int depth = 1, i = start + 1;
        while (depth > 0) { // Exits when there are no pairs of open brackets
            if (i == string.length()) // Avoid index out of bound errors
                return new Pair<>(null, -1);
            char c = string.charAt(i++);
            if (c == (curved ? ')' : '}')) // We've closed off a pair
                -- depth;
            else if (c == (curved ? '(' : '{')) // We've started a pair
                ++ depth;
        }
        // Return the stuff inside the brackets, and the index of the char after the last bracket
        return new Pair<>(string.substring(start + 1, i - 1), i);
    }

    public static ChatColor color(double value, double[] coloring) {
        for (int i = 0;i < coloring.length;++ i) {
            if (value <= coloring[i])
                return COLORING[i];
        }
        return COLORING[COLORING.length - 1];
    }

    public static <T> T selectRandom(List<T> list) {
        return list.isEmpty() ? null : list.get(RNG.nextInt(list.size()));
    }

    public static int[] hsv2rgb(double h, double s, double v) {
        double c = v * s;
        double x = c * (1 - Math.abs(((h / 60.0) % 2.0) - 1));
        double m = v - c;
        double[] prgb;
        if (h < 60)
            prgb = new double[] {c, x, 0};
        else if (h < 120)
            prgb = new double[] {x, c, 0};
        else if (h < 180)
            prgb = new double[] {0, c, x};
        else if (h < 240)
            prgb = new double[] {0, x, c};
        else if (h < 300)
            prgb = new double[] {x, 0, c};
        else
            prgb = new double[] {c, 0, x};
        return new int[] {(int)((prgb[0] + m) * 255), (int)((prgb[1] + m) * 255), (int)((prgb[2] + m) * 255)};
    }

    public static boolean randomChance(double chance) {
        return RNG.nextDouble() < chance;
    }

    public static void changeBlocksAsync(Player player, Map<Block, WrappedBlockData> changes) {
        if (changes.isEmpty())
            return;

        Map<Chunk, Map<Block, WrappedBlockData>> byChunk = new HashMap<>();
        for (Block block : changes.keySet()) // We have to split it up by chunk because that's how the packet works.
            byChunk.computeIfAbsent(block.getChunk(), k -> new HashMap<>()).put(block, changes.get(block));

        int delay = 1;
        final Chunk[] chunks = byChunk.keySet().toArray(new Chunk[0]);
        for (int i = 0;i < chunks.length;i += 4) { // Send packets for the blocks modified in each chunk.
            final int i0 = i;
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                for (int j = i0;j < Math.min(chunks.length, i0 + 4);++ j) {
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
        if (changes.isEmpty())
            return;

        Map<Chunk, Map<Block, WrappedBlockData>> byChunk = new HashMap<>();
        for (Block block : changes.keySet()) // We have to split it up by chunk because that's how the packet works.
            byChunk.computeIfAbsent(block.getChunk(), k -> new HashMap<>()).put(block, changes.get(block));

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

    public static NBTTagCompound getTag(ItemStack stack) {
        return stack == null ? null : CraftItemStack.asNMSCopy(stack).s();
    }

    public static ItemStack applyTag(NBTTagCompound nbt, ItemStack stack) {
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        nmsStack.b(nbt);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    public static <K, V> V getAndPutIfAbsent(Map<K, V> map, K key, V value) {
        V val = map.get(key);
        if( val == null) {
            map.put(key, value);
            return value;
        } else
            return val;
    }

    public static int biasedRandom(int bound, double bias) {
        double k0 = -(bound * bound) / bias, k1 = -(bias * bias + 2 * bias + 1) / (4 * bias), k2 = (bound * (bias + 1)) / (2 * bias);
        return (int)(-Math.sqrt(k0 * (RNG.nextDouble() + k1)) + k2);
    }

    public static int randomInt(int min, int max) {
        return RNG.nextInt(max - min) + min;
    }

    public static double randomDouble(double min, double max) {
        return (max - min) * RNG.nextDouble() + min;
    }

    public static String dateToString(long date, String format) {
        return (new SimpleDateFormat(format)).format(new Date(date));
    }

    public static void serializeUuid(UUID uuid, byte[] dest, int index) {
        System.arraycopy(FLUtils.serializeLong(uuid.getMostSignificantBits()), 0, dest, index, 8);
        System.arraycopy(FLUtils.serializeLong(uuid.getLeastSignificantBits()), 0, dest, index + 8, 8);
    }

    public static byte[] serializeUuid(UUID uuid) {
        byte[] serUuid = new byte[16];
        System.arraycopy(FLUtils.serializeLong(uuid.getMostSignificantBits()), 0, serUuid, 0, 8);
        System.arraycopy(FLUtils.serializeLong(uuid.getLeastSignificantBits()), 0, serUuid, 8, 8);
        return serUuid;
    }

    public static UUID getUuid(byte[] bytes, int index) {
        return new UUID(getLong(bytes, index), getLong(bytes, index + 8));
    }

    public static byte[] serializeLong(long num) {
        return new byte[] {
                (byte)((num >> 56) & 0xFF),
                (byte)((num >> 48) & 0xFF),
                (byte)((num >> 40) & 0xFF),
                (byte)((num >> 32) & 0xFF),
                (byte)((num >> 24) & 0xFF),
                (byte)((num >> 16) & 0xFF),
                (byte)((num >> 8) & 0xFF),
                (byte)(num & 0xFF)
        };
    }

    public static long getLong(byte[] bytes, int index) {
        return (((long)bytes[index]) & 0xFF) << 56 |
                (((long)bytes[index + 1]) & 0xFF) << 48 |
                (((long)bytes[index + 2]) & 0xFF) << 40 |
                (((long)bytes[index + 3]) & 0xFF) << 32 |
                (((long)bytes[index + 4]) & 0xFF) << 24 |
                (((long)bytes[index + 5]) & 0xFF) << 16 |
                (((long)bytes[index + 6]) & 0xFF) << 8 |
                (((long)bytes[index + 7]) & 0xFF);
    }

    public static String itemName(ItemStack stack) {
        String custom = stack.getItemMeta().getDisplayName();
        return custom.isEmpty() ? stack.getAmount() + " * " + capitalize(stack.getType().toString().replaceAll("_", " ")) : custom;
    }

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

    public static CompoundBinaryTag locationToNBT(Location location) {
        CompoundBinaryTag nbt = CompoundBinaryTag.builder()
            .putString("world", location.getWorld().getUID().toString())
            .putDouble("x", location.getX())
            .putDouble("y", location.getY())
            .putDouble("z", location.getZ())
            .putFloat("yaw", location.getYaw())
            .putFloat("pitch", location.getPitch())
            .build();
        return nbt;
    }

    public static ItemStack itemStackFromNBT(byte[] bytes) {
        return bytes == null ? null : ItemStack.deserializeBytes(bytes);
    }

    public static byte[] itemStackToNBT(ItemStack stack) {
        return stack.serializeAsBytes();
    }

    public static boolean isWithin(Location loc, Pair<Location, Location> region) {
        if (loc == null || region == null || region.getFirst() == null || region.getSecond() == null ||
                !loc.getWorld().equals(region.getFirst().getWorld()))
            return false;
        Location f = region.getFirst(), s = region.getSecond();
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return f.getX() < x && x < s.getX() && f.getY() < y && y < s.getY() && f.getZ() < z && z < s.getZ();
    }

    public static int getMonthInYear() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        return cal.get(Calendar.MONTH);
    }

    public static void giveItem(Player player, ItemStack stack, boolean sendMessage) {
        giveItem(player, player.getInventory(), player.getLocation(), stack, sendMessage);
    }

    public static void giveItem(CommandSender recipient, Inventory inv, Location location, ItemStack stack, boolean sendMessage) {
        if (inv.firstEmpty() > -1)
            inv.addItem(stack.clone());
        else {
            location.getWorld().dropItem(location, stack);
            if (sendMessage)
                recipient.sendMessage(ChatColor.RED + "Your inventory was full, so you dropped the item.");
        }
    }

    public static boolean checkNearby(Location location, Material... materials) {
        List<Material> types = Arrays.asList(materials);
        return Arrays.stream(BlockFace.values()).map(bf -> location.getBlock().getRelative(bf).getType()).anyMatch(types::contains);
    }

    public static void tpPlayer(final Player player, final Location location) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 140, 7));
        player.teleport(location);
        player.setFallDistance(0);
        player.setVelocity(new Vector(0, 0.3, 0));
    }

    public static double constrain(double d, double min, double max) {
        return d < min ? min : (Math.min(d, max));
    }

    public static String toStringTruncated(double d) {
        return toStringTruncated(d, 3);
    }

    public static String toStringTruncated(double d, int precision) {
        String fp = Double.toString(d);
        return fp.contains(".") ? fp.substring(0, Math.min(fp.lastIndexOf('.') + precision + 1, fp.length())) +
                (fp.contains("E") ? fp.substring(fp.lastIndexOf('E')) : "") : fp;
    }

    public static <K, V> K getKey(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue()))
                return entry.getKey();
        }
        return null;
    }

    public static boolean isInSpawn(Location loc, double radius) {
        LocationWrapper spawn = FarLands.getDataHandler().getPluginData().spawn;
        return spawn != null && "world".equals(loc.getWorld().getName()) && loc.distance(spawn.asLocation()) < radius;
    }

    public static boolean isInSpawn(Location loc) {
        return isInSpawn(loc, 600);
    }

    public static String matchCase(String original, String other) {
        String[] ogwords = original.split(" "), owords = other.split(" ");
        for (int i = 0;i < Math.min(ogwords.length, owords.length);++ i) {
            char[] ogwc = ogwords[i].toCharArray(), owc = owords[i].toCharArray();
            for (int j = 0;j < Math.min(ogwc.length, owc.length);++ j) {
                if (Character.isUpperCase(ogwc[j]))
                    owc[j] = Character.toUpperCase(owc[j]);
                else
                    owc[j] = Character.toLowerCase(owc[j]);
            }
            owords[i] = new String(owc);
        }
        return String.join(" ", owords);
    }

    public static <T> T safeValueOf(Function<String, T> valueOf, String name) {
        try {
            return valueOf.apply(name);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String capitalize(String x) {
        if (x == null || x.isEmpty())
            return x;
        String[] split = x.split(" ");
        for (int i = 0;i < split.length;++ i) {
            if (!split[i].isEmpty())
                split[i] = Character.toUpperCase(split[i].charAt(0)) + split[i].substring(1).toLowerCase();
        }
        return String.join(" ", split);
    }

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

    public static int indexOfDefault(int index, int def) {
        return index < 0 ? def : index;
    }

    @SafeVarargs
    public static <K, V> Map<K, V> asMap(Pair<K, V>... entries) {
        if (entries.length == 0)
            return Collections.emptyMap();
        Map<K, V> map = new HashMap<>(entries.length);
        for (Pair<K, V> entry : entries)
            map.put(entry.getFirst(), entry.getSecond());
        return map;
    }

    public static byte[] hash(byte[] data) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new InternalError(ex);
        }
        return md.digest(data);
    }

    public static String removeColorCodes(String message) {
        // RegEx ftw
        return message.replaceAll(
            "(?i)([&" + ChatColor.COLOR_CHAR + "][0-9a-fk-orx])|" + // Match &0 ... &f, &k ... &o, &r, and &x, Ignore Case
                "(&#[0-9a-f]{3,6})", // Match &#rrggbb and &#rgb, Ignore Case
            ""
        );
    }

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

    public static String colorize(String string) {
        // Do 6 char first since the 3 char pattern will also match 6 char occurrences
        StringBuffer sb6 = new StringBuffer();
        Matcher matcher6 = HEX_COLOR_PATTERN_SIX.matcher(string);
        while (matcher6.find()) {
            StringBuilder replacement = new StringBuilder(14).append("&x");
            for (char character : matcher6.group(1).toCharArray())
                replacement.append('&').append(character);
            if (getLuma(replacement.toString(), false) > 16)
                matcher6.appendReplacement(sb6, replacement.toString());
            else
                matcher6.appendReplacement(sb6, "");
        }
        matcher6.appendTail(sb6);
        string = sb6.toString();

        // Now convert 3 char to the same format ex. &#363 -> &x&3&3&6&6&3&3
        StringBuffer sb3 = new StringBuffer();
        Matcher matcher3 = HEX_COLOR_PATTERN_THREE.matcher(string);
        while (matcher3.find()) {
            StringBuilder replacement = new StringBuilder(14).append("&x");
            for (char character : matcher3.group(1).toCharArray())
                replacement.append('&').append(character).append('&').append(character);
            if (getLuma(replacement.toString(), false) > 16)
                matcher3.appendReplacement(sb3, replacement.toString());
            else
                matcher3.appendReplacement(sb3, "");
        }
        matcher3.appendTail(sb3);

        // Translate '&' to '§'
        return ChatColor.translateAlternateColorCodes('&', sb3.toString());
    }

    // Get luminescence passing through Bungee's hex format - &x&r&r&g&g&b&b
    public static double getLuma(String color, boolean hex) {
        int r, g, b;
        color = color.replaceAll("[&x#]", "");

        r = Integer.valueOf(color.substring(0, 2), 16);
        g = Integer.valueOf(color.substring(2, 4), 16);
        b = Integer.valueOf(color.substring(4, 6), 16);
        return (0.2126 * r) + (0.7152 * g) + (0.0722 * b);
    }

    public static String applyEmotes(String message) {
        for (CommandShrug.TextEmote emote : CommandShrug.TextEmote.values) {
            message = message.replaceAll("(?i)(?<!\\\\)(:" + Pattern.quote(emote.name()) + ":)", TextUtils2.escapeExpression(emote.getValue()));
        }
        return message;
    }
}
