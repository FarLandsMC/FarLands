package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.discord.DiscordChannel;
import net.farlands.odyssey.mechanic.Chat;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        String reason = args.length > 1 ? joinArgsBeyond(0, " ", args) : "Kicked by an operator.";
        player.kickPlayer(reason);
        sender.sendMessage(ChatColor.GOLD + "Kicked " + ChatColor.AQUA + player.getName() + ChatColor.GOLD + " for reason: \"" + reason + "\"");
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.NOTEBOOK, Chat.applyDiscordFilters(sender.getName()) + " kicked " +
                Chat.applyDiscordFilters(player.getName()) + " for reason: `" + reason + "`");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}
