package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.mechanic.anticheat.AntiCheat;
import net.farlands.sanctuary.util.ComponentColor;
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
        if (args.length == 0) {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
            boolean debugging = !flp.debugging;
            flp.debugging = debugging;
            sender.sendMessage(ComponentColor.aqua("Debugging: %s", debugging));
            if (debugging)
                FarLands.getMechanicHandler().getMechanic(AntiCheat.class).put(sender);
            else
                FarLands.getMechanicHandler().getMechanic(AntiCheat.class).remove(sender);
        } else {
            String[] newArgs = new String[args.length - 1];
            if (newArgs.length > 0)
                System.arraycopy(args, 1, newArgs, 0, newArgs.length);
            String post = FarLands.getDebugger().getPost(args[0], newArgs);
            if (post == null) {
                return error(sender, "%s has not been posted.", args[0]);
            }
            sender.sendMessage(ComponentColor.aqua("%s: ", args[0]).append(ComponentColor.green(post)));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        return args.length <= 1
                ? new ArrayList<>(FarLands.getDebugger().getPosts())
                : Collections.emptyList();
    }
}
