package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;


public class CommandNightSkip extends Command {

    public CommandNightSkip() {
        super(
            CommandData.simple(
                    "nightskip",
                    "View the number of players required to sleep.",
                    "/nightskip"
                )
                .category(Category.INFORMATIONAL)
                .aliases(false, "sleeping", "sleepcount")
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        List<Player> online = Bukkit.getOnlinePlayers().stream()
                .filter(player -> "world".equals(player.getWorld().getName())).map(player -> (Player) player)
                .filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).vanished)
                .toList();
        int sleeping = (int) online.stream().filter(Player::isSleeping).count();
        int required = Math.max(1, (online.size() + 1) / 2);

        return info(sender,
             "{:aqua} player{} currently sleeping\n{:aqua} player{} required to sleep in order to skip the night.",
             sleeping, sleeping == 1 ? " is" : "s are",
             required, required == 1 ? " is" : "s are"
        );
    }
}
