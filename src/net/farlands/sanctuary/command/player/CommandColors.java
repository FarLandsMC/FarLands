package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.Chat;

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
                .filter(color -> !Chat.ILLEGAL_COLORS.contains(color) && !ChatColor.RESET.equals(color))
                .map(color -> color + color.toString().replace("\u00A7", "\\&") + ChatColor.RESET)
                .collect(Collectors.joining(" ")));
        return true;
    }
}
