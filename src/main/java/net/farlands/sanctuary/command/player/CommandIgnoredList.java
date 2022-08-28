package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.PlayerCommand;

import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandIgnoredList extends PlayerCommand {

    public CommandIgnoredList() {
        super(Rank.INITIATE, Category.CHAT, "List ignored players.", "/ignoredlist", "ignoredlist", "ignorelist");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        List<String> ignoreList = FarLands.getDataHandler().getOfflineFLPlayer(sender).getIgnoreList();
        sender.sendMessage(ComponentColor.green(
            ignoreList.isEmpty()
                ? "You are not ignoring any players."
                : String.join(", ", ignoreList))
        );
        return true;
    }
}
