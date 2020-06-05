package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.struct.Home;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.Command;

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
            sendFormatted(sender, "&(red)You must be in-game to use this command.");
            return true;
        }

        // Someone else's home (staff)
        if(Rank.getRank(sender).isStaff() && args.length > 0) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
            if (flp == null) {
                sendFormatted(sender, "&(red)Player not found.");
                return true;
            }

            if (flp.homes.isEmpty()) {
                sendFormatted(sender, "&(green)This player does not have any homes.");
                return true;
            }

            flp.homes.forEach(home -> {
                Location location = home.getLocation();
                sendFormatted(sender, "&(gold)$(hovercmd,/home %0 %1,Go to home {&(aqua)%0},%0: {&(aqua)%2 %3 %4})",
                        home.getName(), flp.username, location.getBlockX(), location.getBlockY(), location.getBlockZ());
            });
        }
        // The sender's homes
        else {
            List<Home> homes = FarLands.getDataHandler().getOfflineFLPlayer(sender).homes;
            if (homes.isEmpty()) {
                sendFormatted(sender, "&(green)You don\'t have any homes! Set one with &(aqua)/sethome");
                return true;
            }
        }

        return true;
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage("Usage: " + (Rank.getRank(sender).isStaff() ? "/homes [player]" : getUsage()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 && Rank.getRank(sender).isStaff()
                ? getOnlinePlayers(args[0], sender)
                : Collections.emptyList();
    }
}
