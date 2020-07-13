package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;

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

        sendFormatted(sender,
                "&(gold){&(aqua)%0} player%2 currently sleeping.\n" +
                "{&(aqua)%1} player%3 required to sleep in order to skip the night.",
                sleeping, required, sleeping == 1 ? " is" : "s are", required == 1 ? " is" : "s are");
        return true;
    }
}
