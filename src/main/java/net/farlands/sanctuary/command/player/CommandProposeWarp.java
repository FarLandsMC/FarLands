package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandProposeWarp extends PlayerCommand {

    private static final List<String> WARP_TYPES = Arrays.asList("shop", "showcase", "town", "public-farm", "other");

    public CommandProposeWarp() {
        super(Rank.INITIATE, Category.REPORTS, "Propose a warp to be set by staff.", "/proposewarp <type> <name> <description>", "proposewarp", "warpform");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length < 3) {
            return false;
        }

        if (!WARP_TYPES.contains(args[0])) {
            sender.sendMessage(ComponentColor.red("Invalid warp type: \"{}\"", args[0]));
            return true;
        }

        if (FarLands.getDataHandler().getPluginData().getWarpNames().contains(args[1])) {
            sender.sendMessage(ComponentColor.red("A warp with that name already exists, please choose another name."));
            return true;
        }

        Location l = sender.getLocation();
        String coords = Math.floor(l.getX()) + 0.5 + " " +
            (int) l.getY() + " " +
            Math.floor(l.getZ()) + 0.5 + " " +
            (int) l.getYaw() + " " +
            (int) l.getPitch() + " " +
            l.getWorld().getName();

        String s = "New **" + args[0] + "** warp proposal from `" + sender.getName() + "`\n" +
            "Name: `" + args[1] + "`\n" +
            "Location: `/tl " + coords + "`\n" +
            "Description:\n```" + joinArgsBeyond(1, " ", args) + "```";

        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.WARP_PROPOSALS, s);
        sender.sendMessage(ComponentColor.green("Proposal sent."));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
            ? TabCompleterBase.filterStartingWith(args[0], WARP_TYPES)
            : Collections.emptyList();
    }
}
