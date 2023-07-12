package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.util.ComponentUtils;
import org.bukkit.entity.Player;

public class CommandDiscord extends PlayerCommand {

    public CommandDiscord() {
        super(
            CommandData.simple(
                    "discord",
                    "Get the invite link to our discord server.",
                    "/discord"
                )
                .category(Category.INFORMATIONAL)
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        return info(
            sender,
            "Click {} and to join the discord server.",
            ComponentUtils.link("here", FarLands.getFLConfig().discordInvite)
        );
    }
}
