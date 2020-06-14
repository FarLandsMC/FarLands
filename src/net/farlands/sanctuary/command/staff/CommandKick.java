package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.Chat;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandKick extends Command {
    public CommandKick() {
        super(Rank.JR_BUILDER, "Kick a player.", "/kick <player> [reason]", "kick");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length == 0)
            return false;
        Player player = getPlayer(args[0], sender);
        if(player == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }
        String reason = args.length > 1 ? joinArgsBeyond(0, " ", args) : "Kicked by an operator.";
        player.kickPlayer(reason);
        sendFormatted(sender, "&(gold)Kicked {&(aqua)%0} for reason: \"%1\"", player.getName(), reason);
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.NOTEBOOK, Chat.applyDiscordFilters(sender.getName()) + " kicked " +
                Chat.applyDiscordFilters(player.getName()) + " for reason: `" + reason + "`");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}
