package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CommandColors extends PlayerCommand {
    public CommandColors() {
        super(Rank.ADEPT, "Show available color codes.", "/colors", "colors", "colours");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        sendFormatted(sender, "&(gold)Color codes: %0", Arrays.stream(ChatColor.values())
                .filter(color -> !Chat.ILLEGAL_COLORS.contains(color) && !ChatColor.RESET.equals(color))
                .map(color -> color + color.toString().replace("\u00A7", "&") + ChatColor.RESET)
                .collect(Collectors.joining(" ")));
        return true;
    }
}
