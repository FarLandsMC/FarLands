package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.staff.CommandEntityCount;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandWhyLag extends Command {
    private static final double[] TPS_COLORING = {0.0, 10.0, 25.0, 50.0};
    private static final double[] PING_COLORING = {50, 125, 250, 350};
    private static final double[] ELYTRA_COLORING = {0, 0, 1, 2};
    private static final double[] CLUSTER_COLORING = {0, 2, 4, 6};

    public CommandWhyLag() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "View any reasons for server or personal lag.", "/whylag", true,
                "whylag", "lag", "tps", "ping");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if ("ping".equals(args[0])) {
            if (!(sender instanceof Player) && args.length <= 1) {
                sender.sendMessage(ChatColor.RED + "You must be in-game to use this command.");
                return true;
            }
            CraftPlayer craftPlayer = args.length <= 1 ? (CraftPlayer) sender : (CraftPlayer) getPlayer(args[1], sender);
            if (craftPlayer == null) {
                sendFormatted(sender, "&(red)Could not find player {&(gray)%0} in game", args[1]);
                return true;
            }
            int ping = (craftPlayer).getHandle().ping;
            sender.sendMessage(ChatColor.GOLD + (args.length > 1 ? craftPlayer.getName() + "'s " : "Your ") + "ping: " +
                    FLUtils.color(ping, PING_COLORING) + ping + "ms");
            return true;
        }
        double mspt = FLUtils.serverMspt();
        double tps = Math.min(20.0, 1000.0 / mspt), percentLag = 100.0 * (1.0 - (tps / 20.0));
        sender.sendMessage(ChatColor.GOLD + "Server TPS: " + FLUtils.color(percentLag, TPS_COLORING) + FLUtils.toStringTruncated(tps) +
                " (" + (int) (100.0 - percentLag) + "%), " + FLUtils.toStringTruncated(mspt) + "mspt");
        if ("tps".equals(args[0]))
            return true;
        if (sender instanceof Player) {
            int ping = ((CraftPlayer) sender).getHandle().ping;
            sender.sendMessage(ChatColor.GOLD + "Your ping: " + FLUtils.color(ping, PING_COLORING) + ping + "ms");
        }
        int flying = (int) Bukkit.getOnlinePlayers().stream().filter(Player::isGliding).count();
        sender.sendMessage(ChatColor.GOLD + "Flying players: " + FLUtils.color(flying, ELYTRA_COLORING) + flying);
        List<Entity> entities = Bukkit.getWorld("world").getEntities();
        int clusters = CommandEntityCount.findClusters(entities.stream().filter(entity -> entity instanceof LivingEntity &&
                !FLUtils.isInSpawn(entity.getLocation()) && entity.isValid()).collect(Collectors.toList())).size();
        sender.sendMessage(ChatColor.GOLD + "Entity clusters: " + FLUtils.color(clusters, CLUSTER_COLORING) + clusters);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 && "ping".equals(alias) ?
                getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}
