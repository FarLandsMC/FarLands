package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandNick extends PlayerCommand {
    public CommandNick() {
        super(Rank.ADEPT, "Set your nickname. Use /nonick to remove your nickname.", "/nick <name>", true, "nick", "nonick");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if("nick".equals(args[0]) && args.length == 1)
            return false;
        FLPlayer flp = FarLands.getPDH().getFLPlayer(sender);
        if("nick".equals(args[0])) {
            if(args[1].isEmpty() || args[1].matches("\\s+") || Chat.getMessageFilter().isProfane(Chat.removeColorCodes(args[1]))) {
                sender.sendMessage(ChatColor.RED + "You cannot set your nickname to this.");
                return true;
            }
            String rawNick = Chat.removeColorCodes(args[1]);
            int rawLen = rawNick.length();
            if(rawLen > 16) {
                sender.sendMessage(ChatColor.RED + "That username is too long.");
                return true;
            }else if(rawLen < 3) {
                sender.sendMessage(ChatColor.RED + "Your nickname must be at least three characters long.");
                return true;
            }
            double nonAscii = 0.0;
            for(char c : rawNick.toCharArray()) {
                if(c < 33 || c > '~') {
                    ++ nonAscii;
                }
            }
            if(!rawNick.matches("(.+)?(\\w\\w\\w)(.+)?")) {
                sender.sendMessage(ChatColor.RED + "Your nickname must have at least three word characters in a row in it.");
                return true;
            }
            if(nonAscii / rawLen > 0.4) {
                sender.sendMessage(ChatColor.RED + "Your nickname must be at least 60% ASCII characters.");
                return true;
            }
            flp.setNickname(Chat.applyColorCodes(args[1]));
            sender.sendMessage(ChatColor.GREEN + "Nickname set.");
        }else{ // Remove nickname
            if(args.length > 1 && Rank.getRank(sender).isStaff()) {
                flp = getFLPlayer(args[1]);
                if(flp.getNickname().isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "This person has no nickname to remove.");
                    return true;
                }
                flp.setNickname("");
                FarLands.getPDH().saveFLPlayer(flp);
            }else{
                if(flp.getNickname().isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "You have no nickname to remove.");
                    return true;
                }
                flp.setNickname("");
            }
            sender.sendMessage(ChatColor.GREEN + "Removed nickname.");
        }
        return true;
    }
}
