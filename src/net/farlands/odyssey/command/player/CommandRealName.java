package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class CommandRealName extends Command {
    public CommandRealName() {
        super(Rank.INITIATE, "Get the real name of a player.", "/realname <nickname>", "realname", "rn");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;
        args[0] = args[0].toLowerCase();
        List<String> matches = new ArrayList<>();
        for (OfflineFLPlayer flp : FarLands.getDataHandler().getOfflineFLPlayers()) {
            String nickname = flp.nickname.toLowerCase();
            if (args[0].equals(nickname)) {
                sender.sendMessage(ChatColor.GREEN + "Matches: " + flp.username);
                return true;
            } else if (nickname.contains(args[0]) || flp.username.toLowerCase().contains(args[0]))
                matches.add(flp.username);
        }
        sender.sendMessage(ChatColor.GREEN + "Matches: " + (matches.isEmpty() ? ChatColor.RED + "None" : ChatColor.GOLD + String.join(", ", matches)));
        return true;
    }
}
