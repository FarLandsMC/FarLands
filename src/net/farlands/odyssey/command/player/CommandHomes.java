package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.data.struct.Home;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.util.TextUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Collections;
import java.util.List;

public class CommandHomes extends Command {
    public CommandHomes() {
        super(Rank.INITIATE, "List your homes.", "/homes", "homes");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if((sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) && args.length == 0) {
            sender.sendMessage(ChatColor.RED + "You must be in-game to use this command.");
            return true;
        }
        StringBuilder sb = new StringBuilder("&(gold)");
        if(Rank.getRank(sender).isStaff() && args.length > 0) { // Someone else's home (staff)
            FLPlayer flp = getFLPlayer(args[0]);
            if (flp == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            List<Home> homes = flp.getHomes();
            if (homes.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "This player does not have any homes.");
                return true;
            }
            homes.forEach(home -> sb.append("$(hovercmd,/home ").append(home.getName()).append(" ").append(args[0])
                    .append(",{&(white)Go to home ").append(home.getName()).append("},").append(home.getName()).append("), "));
        } else {
            List<Home> homes = FarLands.getPDH().getFLPlayer(sender).getHomes();
            if (homes.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "You don\'t have any homes! Set one with " + ChatColor.AQUA + "/sethome");
                return true;
            }
            homes.forEach(home -> sb.append("$(hovercmd,/home ").append(home.getName()).append(",{&(white)Go to home ")
                    .append(home.getName()).append("},").append(home.getName()).append("), "));
        }
        String msg = sb.toString();
        sender.spigot().sendMessage(TextUtils.format(msg.substring(0, msg.length() - 2)));
        return true;
    }

    @Override
    protected void showUsage(CommandSender sender) {
        if(Rank.getRank(sender).isStaff())
            sender.sendMessage("Usage: /homes [player]");
        else
            sender.sendMessage("Usage: /homes");
    }
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 && Rank.getRank(sender).isStaff() ? getOnlinePlayers(args.length == 0 ? "" : args[0]) : Collections.emptyList();
    }
}
