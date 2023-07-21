package net.farlands.sanctuary.command.player;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.staff.CommandEntityCount;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandWhyLag extends Command {

    // @formatter:off
    private static final double[] TPS_COLORING     = { 0.0, 10.0, 25.0, 50.0 };
    private static final double[] PING_COLORING    = {  50,  125,  250, 350  };
    private static final double[] ELYTRA_COLORING  = {   0,    0,    1, 2    };
    private static final double[] CLUSTER_COLORING = {   0,    2,    4, 6    };
    // @formatter:on

    public CommandWhyLag() {
        super(
            CommandData.simple(
                    "lag",
                    "/lag|tps|ping [player]",
                    "View any reasons for server or personal lag"
                )
                .category(Category.INFORMATIONAL)
                .aliases(true, "whylag", "tps", "ping")
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if ("ping".equals(args[0])) {
            if (!(sender instanceof Player) && args.length <= 1) {
                return error(sender, "You must be in-game to use this command.");
            }
            Player player = args.length == 1 ? (Player) sender : Bukkit.getPlayer(args[1]);
            if (player == null) {
                return error(sender, "Could not find player {:gray} in game", args[1]);
            }
            int ping = player.getPing();
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
            return info(
                sender,
                "{:aqua}{::?Your:'s} ping: {}",
                args.length == 1 ? "" : flp,
                args.length == 1,
                FLUtils.color(ping, PING_COLORING, ping + "ms")
            );
        }
        double mspt = FLUtils.serverMspt();
        double tps = Math.min(20.0, 1000.0 / mspt);
        double percentLag = 100.0 * (1.0 - (tps / 20.0));

        info(
            sender,
            "Server TPS: {}",
            ComponentColor.color(
                FLUtils.color(percentLag, TPS_COLORING),
                "{} ({::%.0f}%), {::%.3f}mpst",
                tps, 100 - percentLag,
                mspt
            )
        );

        if ("tps".equals(args[0])) {
            return true;
        }

        if (sender instanceof Player player) {
            int ping = player.getPing();
            info(
                sender,
                "Your ping: {}",
                FLUtils.color(ping, PING_COLORING, ping + "ms")
            );
        }

        int flying = (int) Bukkit.getOnlinePlayers().stream().filter(Player::isGliding).count();
        info(
            sender,
            "Flying players: {}",
            FLUtils.color(flying, ELYTRA_COLORING, flying)
        );


        List<Entity> entities = Bukkit.getWorld("world").getEntities();
        int clusters = CommandEntityCount.findClusters(
                entities
                    .stream()
                    .filter(entity -> entity instanceof LivingEntity && !FLUtils.isInSpawn(entity.getLocation()) && entity.isValid())
                    .collect(Collectors.toList())
            )
            .size();
        info(
            sender,
            "Entity clusters: {}",
            FLUtils.color(clusters, CLUSTER_COLORING, clusters)
        );

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 && "ping".equals(alias)
            ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender)
            : Collections.emptyList();
    }

    @Override
    public @NotNull List<SlashCommandData> discordCommands() {
        List<SlashCommandData> cmds = this.defaultCommands(false);
        cmds.stream()
            .filter(cmd -> cmd.getName().equalsIgnoreCase("ping"))
            .findFirst()
            .ifPresent(ping -> ping.addOption(
                           OptionType.STRING,
                           "player-name",
                           "Name of the player to check ping",
                           true,
                           true
                       )
            );
        return cmds;
    }

}
