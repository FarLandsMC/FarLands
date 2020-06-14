package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.anticheat.AntiCheat;

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
            sendFormatted(sender, "&(aqua)Debugging: %0", debugging);
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
                sendFormatted(sender, "&(red)%0 has not been posted.", args[0]);
                return true;
            }
            sendFormatted(sender, "&(aqua)%0: &(green)%1", args[0], post);
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
