package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CommandColors extends PlayerCommand {
    public CommandColors() {
        super(Rank.ADEPT, Category.COSMETIC, "Show available color codes for chat and signs.", "/colors", "colors", "colours");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        sendFormatted(sender, "&(gold)Color codes: %0", Arrays.stream(ChatColor.values())
                .filter(color -> !FLUtils.ILLEGAL_COLORS.contains(color) && !ChatColor.RESET.equals(color))
                .map(color -> color + color.toString().replace("\u00A7", "\\&") + ChatColor.RESET + ChatColor.WHITE)
                .collect(Collectors.joining(" ")));
        sendFormatted(sender, "&(gold)Hexadecimal Colors: \\&#rrggbb or \\&#rgb, like {&(#92b9bd)\\&#92b9bd} or {&(#55ff77)\\&#5f7}.");
        return true;
    }
}
