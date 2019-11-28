package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TextUtils;
import net.farlands.odyssey.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CommandEntityCount extends Command {
    private static final List<String> ENTITY_TYPES = Arrays.stream(EntityType.values()).filter(et -> !EntityType.PLAYER.equals(et) &&
            !et.getClass().isAnnotationPresent(Deprecated.class)).map(et -> et.toString().replaceAll("_", "-")
            .toLowerCase()).collect(Collectors.toList());
    private static final List<String> RADIUS_SUGGESTIONS = Arrays.asList("25", "50", "100");
    private static final double[] TOTAL_EC_COLORING = {60, 80, 100, 120};
    private static final double[] DENSITY_COLORING = {0.05, 0.25, 0.5, 0.7};
    private static final double[] CLUSTER_SIZE_COLORING = {0, 40, 55, 70};
    private static final double[] CLUSTER_DENSITY_COLORING = {0.4, 0.7, 1.3, 3.4};

    public CommandEntityCount() {
        super(Rank.JR_BUILDER, "Enable or disable flight.", "/entitycount [player] [type] [radius]", "entitycount", "ec");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            World world = sender instanceof Player ? ((Player)sender).getWorld() : Bukkit.getWorld("world");
            List<Entity> entities = world.getEntities().stream().filter(entity -> entity instanceof LivingEntity &&
                    !EntityType.PLAYER.equals(entity.getType())).collect(Collectors.toList());
            sender.sendMessage(ChatColor.GOLD + "Total entities in your world: " +
                    Utils.color((0.2 * entities.size()) / world.getPlayers().size(),
                            TOTAL_EC_COLORING) + Integer.toString(entities.size()));
            reportClusters(sender, entities, true);
        }else{
            ArgumentBundle bundle = bundleArguments(args, sender, false);
            if(bundle == null)
                return true;
            if(bundle.radius == -1)
                bundle.radius = 50;
            final int radiusSq = bundle.radius * bundle.radius;
            Location center;
            if(bundle.player == null) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "You must be in-game to use this command without a player specified.");
                    return true;
                }
                center = ((Player)sender).getLocation();
            }else
                center = bundle.player.getLocation();
            List<Entity> entities = center.getWorld().getEntities().stream().filter(entity -> entity instanceof LivingEntity &&
                    !EntityType.PLAYER.equals(entity.getType()) && (bundle.type == null || bundle.type.equals(entity.getType())))
                    .filter(entity -> entity.getLocation().distanceSquared(center) < radiusSq).collect(Collectors.toList());

            sender.sendMessage(ChatColor.GOLD + "Showing entity count" + (bundle.type == null ? "" : " of " +
                    bundle.type.toString().replaceAll("_", "-").toLowerCase()) + (bundle.player == null ?
                    "" : " near " + bundle.player.getName()) + " with radius " + bundle.radius + ":");
            sender.sendMessage(ChatColor.GOLD + "Total entities: " + Utils.color((double)entities.size(), TOTAL_EC_COLORING) +
                    Integer.toString(entities.size()));
            // density = cross section area approximation / # of entities
            // Added multiplicative correction term for increasing the density artificially for large radii
            double density = entities.isEmpty() ? 1000 : (((1 + Math.PI / 2) * radiusSq) / entities.size()) *
                    (1 - (bundle.radius / 400.0));
            sender.sendMessage(ChatColor.GOLD + "Average entity density: " + Utils.color(1 / density, DENSITY_COLORING) +
                    "~" + Utils.toStringTruncated(density) + "bk/e");
            reportClusters(sender, entities, false);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        ArgumentBundle bundle = bundleArguments(args, sender, true);
        if(bundle == null)
            return Collections.emptyList();
        List<String> completions = new ArrayList<>();
        if(bundle.type == null)
            completions.addAll(ENTITY_TYPES);
        if(bundle.player == null)
            completions.addAll(getOnlinePlayers(args[args.length - 1]));
        if(bundle.radius == -1)
            completions.addAll(RADIUS_SUGGESTIONS);
        return completions.stream().filter(completion -> completion.startsWith(args[args.length - 1])).collect(Collectors.toList());
    }

    // More thorough than the /whylag estimate
    private static void reportClusters(CommandSender sender, List<Entity> entities, boolean findNearbyPlayer) {
        List<List<Entity>> clusters = findClusters(entities.stream().filter(entity -> entity instanceof LivingEntity &&
                !Utils.isInSpawn(entity.getLocation()) && entity.isValid()).collect(Collectors.toList()));
        if(clusters.isEmpty())
            return;
        sender.sendMessage(ChatColor.GOLD + "Entity Clusters:");
        clusters.forEach(cluster -> {
            double x = 0, y = 0, z = 0;
            for(Entity entity : cluster) {
                x += entity.getLocation().getX();
                y += entity.getLocation().getY();
                z += entity.getLocation().getZ();
            }
            Location center = new Location(cluster.get(0).getWorld(), x / cluster.size(), y / cluster.size(), z / cluster.size());
            Player loader = findNearbyPlayer ? Bukkit.getOnlinePlayers().stream().min(Comparator.comparingDouble(a ->
                    center.getWorld().equals(a.getWorld()) ? center.distanceSquared(a.getLocation()) : Double.MAX_VALUE))
                    .orElse(null) : null;
            double totalDistSq = 0, minDistSq = Double.MAX_VALUE, curDistSq;
            for(Entity anchor : cluster) {
                for(Entity target : cluster) {
                    if(anchor.equals(target))
                        continue;
                    curDistSq = anchor.getLocation().distanceSquared(target.getLocation());
                    if(curDistSq < minDistSq)
                        minDistSq = curDistSq;
                }
                totalDistSq += minDistSq;
            }
            double density = Math.PI * (totalDistSq / cluster.size());
            TextUtils.sendFormatted(sender, "&(gold) - Size: {%0%1}, Density: {%2\u2265%3bk/e}, At: $(hovercmd," +
                    "/chain {gm3} {tl %4 %5 %6},{&(aqua)Click to teleport to this location},{&(aqua)%4x %5y %6z})%7",
                    Utils.color((double)cluster.size(), CLUSTER_SIZE_COLORING), cluster.size(), Utils.color(1 / density,
                    CLUSTER_DENSITY_COLORING), Utils.toStringTruncated(density), center.getBlockX(), center.getBlockY(),
                    center.getBlockZ(), loader == null ? "" : " (near $(hovercmd,/spec " + loader.getName() +
                    ",{&(aqua)Click to spectate " + loader.getName() + "},{&(aqua)" + loader.getName() + "}))");
        });
    }

    public static List<List<Entity>> findClusters(List<Entity> source) {
        if(source.isEmpty())
            return Collections.emptyList();
        List<List<Entity>> clusters = new LinkedList<>();
        while(!source.isEmpty())
            clusters.add(cluster(new LinkedList<>(), source, source.remove(0)));
        clusters.removeIf(cluster -> cluster.size() < 30);
        return clusters;
    }

    private static List<Entity> cluster(List<Entity> base, List<Entity> source, Entity origin) {
        base.add(origin);
        List<Entity> nearby = new LinkedList<>();
        source.removeIf(ent -> {
            if(ent.getLocation().distanceSquared(origin.getLocation()) < 16) {
                nearby.add(ent);
                return true;
            }
            return false;
        });
        nearby.forEach(ent -> cluster(base, source, ent));
        return base;
    }

    private static Object getArgument(String arg, boolean strict) {
        if(ENTITY_TYPES.contains(arg))
            return Utils.safeValueOf(EntityType::valueOf, arg.replaceAll("-", "_").toUpperCase());
        else if(arg.matches("\\d+"))
            return Integer.parseInt(arg);
        return strict ? Bukkit.getOnlinePlayers().stream().filter(player -> player.getName().equals(arg)).findAny().orElse(null) : getPlayer(arg);
    }

    static ArgumentBundle bundleArguments(String[] args, CommandSender sender, boolean strict) {
        ArgumentBundle bundle = new ArgumentBundle();
        Object argument;
        for(String arg : args) {
            argument = getArgument(arg, strict);
            if(argument instanceof EntityType) {
                if(bundle.type == null)
                    bundle.type = (EntityType)argument;
                else if(!strict) {
                    sender.sendMessage(ChatColor.RED + "You cannot specify two argument types.");
                    return null;
                }
            }else if(argument instanceof Integer) {
                if(bundle.radius == -1) {
                    int radius = (int)argument;
                    if((radius < 1 || radius > 200) && !strict) {
                        sender.sendMessage(ChatColor.RED + "The radius must be between 1 and 200 inclusive.");
                        return null;
                    }
                    bundle.radius = radius;
                }else if(!strict) {
                    sender.sendMessage(ChatColor.RED + "You cannot specify two radii.");
                    return null;
                }
            }else if(argument instanceof Player) {
                if(bundle.player == null)
                    bundle.player = (Player)argument;
                else if(!strict) {
                    sender.sendMessage(ChatColor.RED + "You cannot specify two players.");
                    return null;
                }
            }else if(!strict) {
                sender.sendMessage(ChatColor.RED + "Invalid argument: " + arg);
                return null;
            }
        }
        return bundle;
    }

    private static class ArgumentBundle {
        Player player;
        EntityType type;
        int radius;

        ArgumentBundle() {
            this.player = null;
            this.type = null;
            this.radius = -1;
        }
    }
}
