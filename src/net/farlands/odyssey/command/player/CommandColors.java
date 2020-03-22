package net.farlands.odyssey.command.player;

import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandColors extends PlayerCommand {
    public CommandColors() {
        super(Rank.ADEPT, "Show available color codes.", "/colors", "colors", "colours");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        List<String> colors = Arrays.stream(ChatColor.values()).filter(color -> !Chat.ILLEGAL_COLORS.contains(color) && !ChatColor.RESET.equals(color))
                .map(color -> color + color.toString().replace("\u00A7", "&") + ChatColor.RESET).collect(Collectors.toList());
        player.sendMessage(ChatColor.GOLD + "Color codes: " + String.join("  ", colors));
        return true;
    }
}
