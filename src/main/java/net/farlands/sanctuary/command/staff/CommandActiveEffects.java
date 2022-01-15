package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.TimeInterval;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.kicas.rp.util.TextUtils.sendFormatted;

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
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }
        Player player = flp.getOnlinePlayer();
        if (player == null) {
            sendFormatted(sender, "&(red)Player is not online.");
            return true;
        }

        List<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());
        if (effects.size() == 0) {
            sendFormatted(sender, "&(gold)%0 has no active potion effects.", flp.username);
            return true;
        }

        sendFormatted(
                sender,
                "&(gold)%0 currently has the following potion effects: {&(green)%1}.",
                flp.username,
                effects
                        .stream()
                        .map(potionEffect -> potionEffect.getType().getName().toLowerCase().replaceAll("_", "-") +
                                "(" + TimeInterval.formatTime(potionEffect.getDuration() * 50L, true) + ")"
                        )
                        .collect(Collectors.joining(", "))
        );
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args[0], sender) : Collections.emptyList();
    }
}
