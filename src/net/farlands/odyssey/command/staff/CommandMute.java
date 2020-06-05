package net.farlands.odyssey.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.struct.Mute;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.discord.DiscordChannel;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.TimeInterval;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Collections;
import java.util.List;

public class CommandMute extends Command {
    public CommandMute() {
        super(Rank.JR_BUILDER, "Mute a player.", "/mute <player> <time> [reason]", true, "mute", "unmute");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length < ("mute".equals(args[0]) ? 3 : 2))
            return false;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);
        if(flp == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }
        if("mute".equals(args[0])) {
            Rank senderRank = Rank.getRank(sender), mutedRank = flp.rank;
            // Staff can mute players, Non-Jr. staff can mute Jr. staff, Owners can mute all staff except owners.
            if(!(sender instanceof ConsoleCommandSender) && ((senderRank.specialCompareTo(mutedRank) == 0) ||
                    ((senderRank.getPermissionLevel() == 2 || senderRank.getPermissionLevel() == 3) &&
                        (mutedRank.getPermissionLevel() == 2 || mutedRank.getPermissionLevel() == 3)) ||
                    (senderRank.getPermissionLevel() == 1 && mutedRank.getPermissionLevel() > 1))) {
                sendFormatted(sender, "&(red)You do not have permission to mute this person.");
                return true;
            }
            int time = (int)TimeInterval.parseSeconds(args[2]);
            if(time <= 0) {
                sendFormatted(sender, "&(red)Invalid time.");
                return true;
            }
            Mute mute;
            if(args.length == 3 || args[3].isEmpty())
                mute = new Mute(time);
            else{
                String reason = joinArgsBeyond(2, " ", args);
                if(reason.length() > 256) {
                    sendFormatted(sender, "&(red)A mute reason cannot be longer the 256 characters, it will be truncated.");
                    reason = reason.substring(0, 256);
                }
                mute = new Mute(time, reason);
            }
            flp.currentMute = mute; // Update the player's mute
            if(flp.isOnline())
                mute.sendMuteMessage(flp.getOnlinePlayer());
            // Send formatted message to player and discord
            String message = "uted " + flp.username + " with reason `" + mute.getReason() + "`. Expires: " +
                    TimeInterval.formatTime(1000L * time, false);
            sendFormatted(sender, "&(gold)M%0", message.replaceAll("`", "\""));
            FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.NOTEBOOK, Chat.applyDiscordFilters(sender.getName()) + " m" +
                    Chat.removeColorCodes(message));
        }else{ // Un-mute
            if(!flp.isMuted()) {
                sendFormatted(sender, "&(red)This player is not muted.");
                return true;
            }
            flp.currentMute = null;
            if(flp.isOnline())
                flp.getOnlinePlayer().sendMessage(ChatColor.GREEN + "Your mute has expired.");
            sendFormatted(sender, "&(green)Un-muted %0.", flp.username);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}
