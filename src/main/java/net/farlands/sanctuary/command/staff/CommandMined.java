package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.anticheat.XRayStore;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandMined extends PlayerCommand {
    public CommandMined() {
        super(Rank.JR_BUILDER, "Show what a player has mined within a 100 block radius.", "/mined <player> [rollback|preview]", "mined");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 0)
            return false;

        if (args.length == 1 || "preview".equals(args[1])) {
            FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
            session.allowCoRollback();

            // Includes stone variants, deepslate variants, nether gold ore, and ancient debris
            sender.chat(
                "/co rollback u:" + args[0] + " r:50 b:" +
                    XRayStore.ORES.stream()
                        .map(Enum::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.joining(","))
                + " t:14d #preview"
            );
            session.resetCoRollback();
            sender.sendMessage(
                ComponentColor.green("Type ")
                    .append(ComponentUtils.command("/co cancel"))
                    .append(ComponentColor.green(" when done."))
            );
        } else if ("rollback".equals(args[1])) {
            if (Rank.getRank(sender).specialCompareTo(Rank.BUILDER) >= 0) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to rollback areas yet.");
                return true;
            }

            sender.chat("/co rollback u:" + args[0] + " r:50 t:14d");
        } else
            return false;
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        switch (args.length) {
            case 0:
                return getOnlinePlayers("", sender);
            case 1:
                return getOnlinePlayers(args[0], sender);
            case 2:
                return Arrays.asList("rollback", "preview");
            default:
                return Collections.emptyList();
        }
    }
}
