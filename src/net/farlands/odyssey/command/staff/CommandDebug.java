package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.anticheat.AntiCheat;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandDebug extends PlayerCommand {
    public CommandDebug() {
        super(Rank.BUILDER, "Turn on debugging mode.", "/debug [value] [args...]", "debug");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length == 0) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
            boolean debugging = !flp.debugging;
            flp.debugging = debugging;
            sender.sendMessage(ChatColor.AQUA + "Debugging: " + debugging);
            if (debugging)
                FarLands.getMechanicHandler().getMechanic(AntiCheat.class).put(sender);
            else
                FarLands.getMechanicHandler().getMechanic(AntiCheat.class).remove(sender);
        }else{
            String[] newArgs = new String[args.length - 1];
            if(newArgs.length > 0)
                System.arraycopy(args, 1, newArgs, 0, newArgs.length);
            String post = FarLands.getDebugger().getPost(args[0], newArgs);
            if(post == null) {
                sender.sendMessage(ChatColor.RED + args[0] + " has not been posted.");
                return true;
            }
            sender.sendMessage(ChatColor.AQUA + args[0] + ": " + ChatColor.GREEN + post);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? new ArrayList<>(FarLands.getDebugger().getPosts())
                : Collections.emptyList();
    }
}
