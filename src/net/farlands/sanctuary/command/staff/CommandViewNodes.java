package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.util.Pair;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.anticheat.AntiCheat;
import net.farlands.sanctuary.mechanic.anticheat.Detecting;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandViewNodes extends Command {

    private static final int NODES_PER_PAGE = 10;

    public CommandViewNodes() {
        super(Rank.JR_BUILDER, "Display clickable tps for all alert nodes", "/viewnodes <player> [page]", "viewnodes");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length <= 0)
            return false;

        Player player = getPlayer(args[0], sender);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        List<Pair<Detecting, Location>> nodes = FarLands.getMechanicHandler().getMechanic(AntiCheat.class)
                .getXRayNodes(player.getUniqueId());

        if (nodes == null || nodes.isEmpty()) {
            sendFormatted(sender, "&(red)no nodes available for player " + player.getName());
            return true;
        }

        int page;
        if (args.length < 2)
            page = 0;
        else {
            try {
                page = Integer.parseInt(args[1]) - 1; // 0 indexed
            } catch (NumberFormatException e) {
                sender.sendMessage("2nd argument must be a number between 1 and " + nodes.size() / NODES_PER_PAGE);
                return true;
            }
            if (page < 0)
                page = 0;
            if (page > nodes.size() / NODES_PER_PAGE)
                page = nodes.size() / NODES_PER_PAGE;
        }

        StringBuilder message = new StringBuilder();

        Pair<Detecting, Location> node;
        for (int i = page * NODES_PER_PAGE, e = Math.min(i + NODES_PER_PAGE, nodes.size()); i < e; ++i) {
            node = nodes.get(i);
            message.append("$(hovercmd,/chain {gm3} {tl ")
                   .append(node.getSecond().getBlockX()).append(" ")
                   .append(node.getSecond().getBlockY()).append(" ")
                   .append(node.getSecond().getBlockZ()).append(" ~ ~ ")
                   .append(node.getSecond().getWorld().getName()).append("},{&(gray)Click to tp},&(")
                   .append(node.getFirst().getColor().name().toLowerCase()).append(")")
                   .append(node.getFirst().toString()).append(" @ ")
                   .append(node.getSecond().getBlockX()).append(" ")
                   .append(node.getSecond().getBlockY()).append(" ")
                   .append(node.getSecond().getBlockZ()).append("),\n");
        }
        sendFormatted(sender, message.toString().trim());

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        return args.length <= 1
                ? getOnlinePlayers(args.length <= 0 ? "" : args[0], sender)
                : Collections.emptyList();
    }
}
