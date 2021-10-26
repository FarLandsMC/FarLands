package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandVote extends Command {
    public CommandVote() {
        super(Rank.INITIATE, Category.MISCELLANEOUS, "Get the link to vote for FarLands.", "/vote", "vote");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(sender instanceof Player)
            sendFormatted(sender, "&(gold)Click $(link,%0,{&(aqua,underline)here}) to vote.", FarLands.getFLConfig().voteConfig.voteLink);
        else if(sender instanceof DiscordSender) {
            ((DiscordSender) sender)
                    .sendMessage(
                        "Follow this link to vote for the server: <" + FarLands.getFLConfig().voteConfig.voteLink + ">",
                        false
                    );
        } else {
            sender.sendMessage(ChatColor.GOLD + "Follow this link to vote for the server: " + ChatColor.AQUA + ChatColor.UNDERLINE +
                    FarLands.getFLConfig().voteConfig.voteLink);
        }
        return true;
    }
}
