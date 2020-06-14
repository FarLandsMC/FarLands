package net.farlands.sanctuary.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;

import net.farlands.sanctuary.FarLands;

import net.minecraft.server.v1_15_R1.MerchantRecipe;
import net.minecraft.server.v1_15_R1.MerchantRecipeList;
import net.minecraft.server.v1_15_R1.NBTTagCompound;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public final class FLUtils {
    public static final Random RNG = new Random();
    public static final Runnable NO_ACTION = () -> { };
    private static final ChatColor[] COLORING = {ChatColor.DARK_GREEN, ChatColor.GREEN, ChatColor.YELLOW, ChatColor.RED, ChatColor.DARK_RED};

    private FLUtils() { }

    public static MerchantRecipeList copyRecipeList(MerchantRecipeList list) {
        MerchantRecipeList copy = new MerchantRecipeList();
        Field[] fields = MerchantRecipe.class.getFields();
        list.forEach(recipe -> {
            MerchantRecipe r = new MerchantRecipe(recipe.getBuyItem1(), recipe.getBuyItem2(), 1, 1, 1F);
            for(Field f : fields)
                ReflectionHelper.setFieldValue(f, r, ReflectionHelper.getFieldValue(f, recipe));
            copy.add(r);
        });
        return copy;
    }

    public static double serverMspt() {
        long totalMspt = 0;
        long[] mspts = ((CraftServer)Bukkit.getServer()).getServer().f;
        for(long v : mspts)
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
        for(Pair<Location, Location> subregion : region) {
            if(isWithin(loc, subregion))
                return true;
        }
        return false;
    }

    public static boolean passedThrough(Location from, Location to, List<Pair<Location, Location>> region) {
        if(isWithin(from, region))
            return false;
        if(isWithin(to, region))
            return true;
        else{ // Take small steps and check each one, account for high velocities
            double xs = to.getX() - from.getX(), ys = to.getY() - from.getY(), zs = to.getZ() - from.getZ();
            double div = Stream.of(xs, ys, zs).map(Math::abs).max(Double::compare).get() * 1.1;
            xs /= div;
            ys /= div;
            zs /= div;
            Location loc = from.clone();
            while(loc.distanceSquared(to) > 0.95) {
                loc = loc.add(xs, ys, zs);
                if(isWithin(loc, region))
                    return true;
            }
            return false;
        }
    }

    // Get text enclosed in brackets
    public static Pair<String, Integer> getEnclosed(int start, String string) {
        boolean curved = string.charAt(start) == '('; // ()s or {}s
        int depth = 1, i = start + 1;
        while(depth > 0) { // Exits when there are no pairs of open brackets
            if(i == string.length()) // Avoid index out of bound errors
                return new Pair<>(null, -1);
            char c = string.charAt(i++);
            if(c == (curved ? ')' : '}')) // We've closed off a pair
                -- depth;
            else if(c == (curved ? '(' : '{')) // We've started a pair
                ++ depth;
        }
        // Return the stuff inside the brackets, and the index of the char after the last bracket
        return new Pair<>(string.substring(start + 1, i - 1), i);
    }

    public static ChatColor color(double value, double[] coloring) {
        for(int i = 0;i < coloring.length;++ i) {
            if(value <= coloring[i])
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
        if(h < 60)
            prgb = new double[] {c, x, 0};
        else if(h < 120)
            prgb = new double[] {x, c, 0};
        else if(h < 180)
            prgb = new double[] {0, c, x};
        else if(h < 240)
            prgb = new double[] {0, x, c};
        else if(h < 300)
            prgb = new double[] {x, 0, c};
        else
            prgb = new double[] {c, 0, x};
        return new int[] {(int)((prgb[0] + m) * 255), (int)((prgb[1] + m) * 255), (int)((prgb[2] + m) * 255)};
    }

    public static boolean randomChance(double chance) {
        return RNG.nextDouble() < chance;
    }

    public static void changeBlocksAsync(Player player, Map<Block, WrappedBlockData> changes) {
        if(changes.isEmpty())
            return;

        Map<Chunk, Map<Block, WrappedBlockData>> byChunk = new HashMap<>();
        for (Block block : changes.keySet()) // We have to split it up by chunk because that's how the packet works.
            byChunk.computeIfAbsent(block.getChunk(), k -> new HashMap<>()).put(block, changes.get(block));

        int delay = 1;
        final Chunk[] chunks = byChunk.keySet().toArray(new Chunk[0]);
        for(int i = 0;i < chunks.length;i += 4) { // Send packets for the blocks modified in each chunk.
            final int i0 = i;
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                for(int j = i0;j < Math.min(chunks.length, i0 + 4);++ j) {
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
        if(changes.isEmpty())
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
        }catch(InvocationTargetException ex) {
            ex.printStackTrace();
        }
    }

    public static NBTTagCompound getTag(ItemStack stack) {
        return stack == null ? null : CraftItemStack.asNMSCopy(stack).getTag();
    }

    public static ItemStack applyTag(NBTTagCompound nbt, ItemStack stack) {
        net.minecraft.server.v1_15_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        nmsStack.setTag(nbt);
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    public static <K, V> V getAndPutIfAbsent(Map<K, V> map, K key, V value) {
        V val = map.get(key);
        if(val == null) {
            map.put(key, value);
            return value;
        }else
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

    public static Location locationFromNBT(NBTTagCompound nbt) {
        return new Location(Bukkit.getWorld(UUID.fromString(nbt.getString("world"))), nbt.getDouble("x"), nbt.getDouble("y"),
                nbt.getDouble("z"), nbt.getFloat("yaw"), nbt.getFloat("pitch"));
    }

    public static NBTTagCompound locationToNBT(Location location) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("world", location.getWorld().getUID().toString());
        nbt.setDouble("x", location.getX());
        nbt.setDouble("y", location.getY());
        nbt.setDouble("z", location.getZ());
        nbt.setFloat("yaw", location.getYaw());
        nbt.setFloat("pitch", location.getPitch());
        return nbt;
    }

    public static ItemStack itemStackFromNBT(NBTTagCompound nbt) {
        return nbt == null || nbt.isEmpty() ? null: CraftItemStack.asBukkitCopy(ReflectionHelper
                .instantiate(net.minecraft.server.v1_15_R1.ItemStack.class, nbt));
    }

    public static NBTTagCompound itemStackToNBT(ItemStack stack) {
        NBTTagCompound nbt = new NBTTagCompound();
        if(stack != null)
            CraftItemStack.asNMSCopy(stack).save(nbt);
        return nbt;
    }

    public static boolean isWithin(Location loc, Pair<Location, Location> region) {
        if(loc == null || region == null || region.getFirst() == null || region.getSecond() == null ||
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
        if(player.getInventory().firstEmpty() > -1)
            player.getInventory().addItem(stack.clone());
        else{
            player.getWorld().dropItem(player.getLocation(), stack);
            if(sendMessage)
                player.sendMessage(ChatColor.RED + "Your inventory was full, so you dropped the item.");
        }
    }

    public static boolean checkNearby(Location location, Material... materials) {
        List<Material> types = Arrays.asList(materials);
        return Arrays.stream(BlockFace.values()).map(bf -> location.getBlock().getRelative(bf).getType()).anyMatch(types::contains);
    }

    public static void tpPlayer(final Player player, final Location location) {
        player.addPotionEffect((new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 140, 7)));
        player.teleport(location);
        player.setFallDistance(0);
        player.setVelocity(new Vector(0, 0.3, 0));
    }

    public static double constrain(double d, double min, double max) {
        return d < min ? min : (d > max ? max : d);
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
        for(Map.Entry<K, V> entry : map.entrySet()) {
            if(Objects.equals(value, entry.getValue()))
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
        for(int i = 0;i < Math.min(ogwords.length, owords.length);++ i) {
            char[] ogwc = ogwords[i].toCharArray(), owc = owords[i].toCharArray();
            for(int j = 0;j < Math.min(ogwc.length, owc.length);++ j) {
                if(Character.isUpperCase(ogwc[j]))
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
        }catch(Throwable t) {
            return null;
        }
    }

    public static String capitalize(String x) {
        if(x == null || x.isEmpty())
            return x;
        String[] split = x.split(" ");
        for(int i = 0;i < split.length;++ i) {
            if(!split[i].isEmpty())
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
        if(entries.length == 0)
            return Collections.emptyMap();
        Map<K, V> map = new HashMap<>(entries.length);
        for(Pair<K, V> entry : entries)
            map.put(entry.getFirst(), entry.getSecond());
        return map;
    }

    public static byte[] hash(byte[] data) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        }catch(NoSuchAlgorithmException ex) {
            throw new InternalError(ex);
        }
        return md.digest(data);
    }
}
