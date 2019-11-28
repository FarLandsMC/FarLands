package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TextUtils;
import net.farlands.odyssey.util.TimeInterval;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandTop extends Command {
    public CommandTop() {
        super(Rank.INITIATE, "View the people with the most votes or play time.", "/top <votes|playtime|donors> votes?[default:month|all]", "top");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length == 0)
            return false;
        if("votes".equals(args[0])) {
            if(args.length == 1 || "month".equals(args[1])) {
                ResultSet rs = FarLands.getPDH().query("SELECT username,monthVotes,totalVotes FROM playerdata ORDER BY (monthVotes*65536+totalVotes) DESC LIMIT 10");
                sender.spigot().sendMessage(TextUtils.format("&(gold)Showing the top voters for this month:"));
                try {
                    int count = 0;
                    while(rs.next()) {
                        ++ count;
                        sender.spigot().sendMessage(TextUtils.format("&(gold)%0: {&(aqua)%1} - %2 $(inflect,noun,2,vote) " +
                                        "this month, %3 total $(inflect,noun,3,vote)", count, rs.getString("username"),
                                rs.getInt("monthVotes"), rs.getInt("totalVotes")));
                    }
                    rs.close();
                }catch(SQLException ex) {
                    ex.printStackTrace();
                    sender.sendMessage(ChatColor.RED + "There was an error executing this command.");
                }
            }else if("all".equals(args[1])) {
                ResultSet rs = FarLands.getPDH().query("SELECT username,totalVotes FROM playerdata ORDER BY totalVotes DESC LIMIT 10");
                sender.spigot().sendMessage(TextUtils.format("&(gold)Showing the top voters of all time:"));
                try {
                    int count = 0;
                    while(rs.next()) {
                        ++ count;
                        sender.spigot().sendMessage(TextUtils.format("&(gold)%0: {&(aqua)%1} - %2 $(inflect,noun,2,vote)",
                                count, rs.getString("username"), rs.getInt("totalVotes")));
                    }
                    rs.close();
                }catch(SQLException ex) {
                    ex.printStackTrace();
                    sender.sendMessage(ChatColor.RED + "There was an error executing this command.");
                }
            }else
                return false;
        }else if("playtime".equals(args[0])) {
            ResultSet rs = FarLands.getPDH().query("SELECT username,secondsPlayed FROM playerdata ORDER BY secondsPlayed DESC LIMIT 10");
            sender.spigot().sendMessage(TextUtils.format("&(gold)Showing the top players with the longest play time:"));
            try {
                int count = 0;
                while(rs.next()) {
                    ++ count;
                    sender.spigot().sendMessage(TextUtils.format("&(gold)%0: {&(aqua)%1} - %2", count, rs.getString("username"),
                            TimeInterval.formatTime(1000L * rs.getInt("secondsPlayed"), true)));
                }
                rs.close();
            }catch(SQLException ex) {
                ex.printStackTrace();
                sender.sendMessage(ChatColor.RED + "There was an error executing this command.");
            }
        }else if("donors".equals(args[0])) {
            ResultSet rs = FarLands.getPDH().query("SELECT username,amountDonated FROM playerdata WHERE amountDonated > 0 " +
                    "ORDER BY (amountDonated*65536+totalVotes) DESC LIMIT 10");
            sender.spigot().sendMessage(TextUtils.format("&(gold)Showing the top server donors:"));
            try {
                int count = 0;
                while(rs.next()) {
                    ++ count;
                    sender.spigot().sendMessage(TextUtils.format("&(gold)%0: &(aqua)%1", count, rs.getString("username")));
                }
                rs.close();
            }catch(SQLException ex) {
                ex.printStackTrace();
                sender.sendMessage(ChatColor.RED + "There was an error executing this command.");
            }
        }else
            return false;
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? Stream.of("votes", "playtime", "donors").filter(o -> o.startsWith(args.length == 0 ? "" : args[0])).collect(Collectors.toList()) :
                ("votes".equals(args[0]) ? Stream.of("month", "all").filter(o -> o.startsWith(args[1])).collect(Collectors.toList()) : Collections.emptyList());
    }
}
