package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommandRealName extends Command {
    public CommandRealName() {
        super(Rank.INITIATE, "Get the real name of a player.", "/realname <nickname>", "realname", "rn");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length == 0)
            return false;
        args[0] = args[0].toLowerCase();
        List<String> matches = new ArrayList<>();
        ResultSet rs = FarLands.getPDH().query("SELECT username,nickname FROM playerdata");
        if(rs == null)
            return true;
        try {
            while(rs.next()) {
                String nick = Chat.removeColorCodes(rs.getString("nickname")).toLowerCase(), user = rs.getString("username");
                if(nick.equalsIgnoreCase(args[0])) {
                    sender.sendMessage(ChatColor.GREEN + "Matches: " + user);
                    rs.close();
                    return true;
                }else if(nick.contains(args[0]) || user.toLowerCase().contains(args[0]))
                    matches.add(user);
            }
            rs.close();
        }catch(SQLException ex) {
            ex.printStackTrace();
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + "Matches: " + (matches.isEmpty() ? ChatColor.RED + "None" : ChatColor.GOLD + String.join(", ", matches)));
        return true;
    }
}
