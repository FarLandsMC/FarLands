package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandWarps extends PlayerCommand {

    public CommandWarps() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "View the list of server warps.", "/warps", "warps", "warplist", "warpslist");
    }

    @Override
    public boolean execute(Player sender, String[] args) {

        List<Component> warps = FarLands.getDataHandler().getPluginData().getWarpNames().stream()
            .filter(name -> name.startsWith(args.length == 0 ? "" : args[0]))
            .map(warp -> ComponentUtils.command(
                "/warp " + warp,
                ComponentColor.aqua(warp),
                ComponentColor.gray("Teleport to warp"))
            )
            .toList();

        sender.sendMessage(
            ComponentColor.gold("Warps: ")
                .append(ComponentUtils.join(warps))
        );

        return true;
    }
}
