package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandVote extends Command {
    public CommandVote() {
        super(Rank.INITIATE, Category.MISCELLANEOUS, "Get the link to vote for FarLands.", "/vote", "vote");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(sender instanceof Player)
            sender.sendMessage(
                ComponentColor.gold("Click ")
                    .append(ComponentUtils.link("here", FarLands.getFLConfig().voteConfig.voteLink))
                    .append(ComponentColor.gold(" to vote."))
            );
        else if(sender instanceof DiscordSender) {
            ((DiscordSender) sender)
                    .sendMessage(
                        "Follow this link to vote for the server: <" + FarLands.getFLConfig().voteConfig.voteLink + ">",
                        false
                    );
        } else {
            sender.sendMessage(
                ComponentColor.gold("Follow this link to vote for the server: ")
                    .append(ComponentUtils.link(FarLands.getFLConfig().voteConfig.voteLink))
            );
        }
        return true;
    }
}
