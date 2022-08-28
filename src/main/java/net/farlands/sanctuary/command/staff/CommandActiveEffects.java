package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.TimeInterval;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandActiveEffects extends Command {
    public CommandActiveEffects() {
        super(Rank.JR_BUILDER, "View active effects on player", "/activeeffects <playername>", "activeeffects", "ae");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if (flp == null) {
            return error(sender, "Player not found.");
        }
        Player player = flp.getOnlinePlayer();
        if (player == null) {
            return error(sender, "Player is not online.");
        }

        List<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());
        if (effects.size() == 0) {
            return info(sender, "%s has no active potion effects.", flp.username);
        }

        sender.sendMessage(
            ComponentColor.gold("%s currently has the following potion effects: ")
                .append(Component.join(
                    JoinConfiguration.commas(true),
                    effects
                        .stream()
                        .map(pe ->
                                 Component.translatable(pe.getType().translationKey())
                                     .append(ComponentColor.green("(%s)", TimeInterval.formatTime(pe.getDuration() * 50L, true))
                                     )
                        )
                        .toList()
                ))
        );

        return true;
    }

    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args[0], sender) : Collections.emptyList();
    }
}
