package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandVote extends Command {
    public CommandVote() {
        super(Rank.INITIATE, "Get the link to vote for FarLands.", "/vote", "vote");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(sender instanceof Player)
            sendFormatted(sender, "&(gold)Click $(link,%0,{&(aqua,underline)here}) to vote.", FarLands.getFLConfig().getVoteConfig().getVoteLink());
        else{
            sender.sendMessage(ChatColor.GOLD + "Follow this link to vote for the server: " + ChatColor.AQUA + ChatColor.UNDERLINE +
                    FarLands.getFLConfig().getVoteConfig().getVoteLink());
        }
        return true;
    }
}
