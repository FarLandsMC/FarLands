package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.command.TabCompleterBase;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.discord.DiscordChannel;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandProposeWarp extends PlayerCommand {
    private static final List<String> WARP_TYPES = Arrays.asList("shop", "showcase", "town", "public-farm", "other");

    public CommandProposeWarp() {
        super(Rank.INITIATE, "Propose a warp to be set by staff.", "/proposewarp <type> <name> <description>", "proposewarp", "warpform");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length < 3)
            return false;

        if(!WARP_TYPES.contains(args[0])) {
            sendFormatted(sender, "&(red)Invalid warp type: %0", args[0]);
            return true;
        }

        if(FarLands.getDataHandler().getPluginData().getWarpNames().contains(args[1])) {
            sendFormatted(sender, "&(red)A warp with that name already exists, please choose another name.");
            return true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("New **").append(args[0]).append("** warp proposal from `").append(sender.getName()).append("`\n");
        sb.append("Name: `").append(args[1]).append("`\n");
        Location l = sender.getLocation();
        sb.append("Location: `/tl ").append(Math.floor(l.getX()) + 0.5).append(' ').append((int)l.getY()).append(' ')
                .append(Math.floor(l.getZ()) + 0.5).append(' ').append((int)l.getYaw()).append(' ').append((int)l.getPitch())
                .append(' ').append(l.getWorld().getName()).append("`\n");
        sb.append("Description:\n```").append(joinArgsBeyond(1, " ", args)).append("```");
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.WARP_PROPOSALS, sb.toString());
        sendFormatted(sender, "&(green)Proposal sent.");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], WARP_TYPES)
                : Collections.emptyList();
    }
}
