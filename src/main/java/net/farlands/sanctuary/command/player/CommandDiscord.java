package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;

import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class CommandDiscord extends PlayerCommand {

    public CommandDiscord() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "Get the invite link to our discord server.", "/discord", "discord");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        sender.sendMessage(ComponentColor.gold("Click ")
            .append(ComponentUtils.link("here", FarLands.getFLConfig().discordInvite))
            .append(Component.text(" and follow the link to join our discord server.")));
        return true;
    }
}
