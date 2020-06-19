package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.entity.Player;

public class CommandWarps extends PlayerCommand {
    public CommandWarps() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "View the list of server warps.", "/warps", "warps", "warplist", "warpslist");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("&(gold)Warps: ");
        FarLands.getDataHandler().getPluginData().getWarpNames().stream()
                .filter(name -> name.startsWith(args.length == 0 ? "" : args[0]))
                .forEach(warp -> sb.append("$(hovercmd,/warp ").append(warp).append(",{&(gray)Warp to {&(white)")
                .append(warp).append("}},").append(warp).append(" )"));
        sendFormatted(sender, sb.toString());
        return true;
    }
}
