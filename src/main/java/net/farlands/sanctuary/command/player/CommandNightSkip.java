package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;


public class CommandNightSkip extends Command {
    public CommandNightSkip() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "View the number of players required to sleep.", "/nightskip", "nightskip");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        List<Player> online = Bukkit.getOnlinePlayers().stream()
                .filter(player -> "world".equals(player.getWorld().getName())).map(player -> (Player) player)
                .filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).vanished)
                .collect(Collectors.toList());
        int sleeping = (int) online.stream().filter(Player::isSleeping).count();
        int required = Math.max(1, (online.size() + 1) / 2);

        Component c = Component.text()
            .color(NamedTextColor.GOLD)
            .append(
                ComponentColor.aqua(sleeping + "")
            )
            .append(
                Component.text(" player" + (sleeping == 1 ? " is" : "s are") + " currently sleeping.\n")
            )
            .append(
                ComponentColor.aqua(required + "")
            )
            .append(
                Component.text(" player" + (required == 1 ? " is" : "s are") + " required to sleep in order to skip the night.")
            )
            .build();
        sender.sendMessage(c);
        return true;
    }
}
