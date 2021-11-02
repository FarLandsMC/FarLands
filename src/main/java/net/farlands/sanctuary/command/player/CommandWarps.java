package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class CommandWarps extends PlayerCommand {

    public CommandWarps() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "View the list of server warps.", "/warps", "warps", "warplist", "warpslist");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        TextComponent.Builder builder = Component.text()
            .content("Warps: ")
            .color(NamedTextColor.GOLD);

        FarLands.getDataHandler().getPluginData().getWarpNames().stream()
            .filter(name -> name.startsWith(args.length == 0 ? "" : args[0]))
            .forEach(warp -> builder.append(
                ComponentUtils.command("/warp " + warp)
            ));
        sender.sendMessage(builder.build());

        return true;
    }
}
