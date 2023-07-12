package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;

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
            return error(sender, "Player not found.");
        }
        String reason = args.length > 1 ? joinArgsBeyond(0, " ", args) : "Kicked by an operator.";
        player.kick(Component.text(reason), PlayerKickEvent.Cause.KICK_COMMAND);
        sender.sendMessage(ComponentColor.gold("Kicked {:aqua} for reason: \"{:aqua}\"", player.getName(), reason));
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.NOTEBOOK, MarkdownProcessor.escapeMarkdown(sender.getName()) + " kicked " +
                MarkdownProcessor.escapeMarkdown(player.getName()) + " for reason: `" + reason + "`");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        return args.length <= 1
                ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender)
                : Collections.emptyList();
    }
}
