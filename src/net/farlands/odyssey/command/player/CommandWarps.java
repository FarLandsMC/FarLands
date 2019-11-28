package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TextUtils;
import org.bukkit.entity.Player;

public class CommandWarps extends PlayerCommand {
    public CommandWarps() {
        super(Rank.INITIATE, "View the list of server warps.", "/warps", "warps", "warplist", "warpslist");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("&(gold)Warps: ");
        FarLands.getDataHandler().getPluginData().getWarpNames().stream()
                .filter(name -> name.startsWith(args.length == 0 ? "" : args[0]))
                .forEach(warp -> sb.append("$(hovercmd,/warp ").append(warp).append(",{&(white)Warp to ")
                .append(warp).append("},").append(warp).append(" )"));
        sender.spigot().sendMessage(TextUtils.format(sb.toString()));
        return true;
    }
}
