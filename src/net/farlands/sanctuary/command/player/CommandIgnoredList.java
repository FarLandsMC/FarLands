package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.command.PlayerCommand;

import org.bukkit.entity.Player;

import java.util.List;

public class CommandIgnoredList extends PlayerCommand {
    public CommandIgnoredList() {
        super(Rank.INITIATE, Category.CHAT, "List ignored players.", "/ignoredlist", "ignoredlist", "ignorelist");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        List<String> ignoreList = FarLands.getDataHandler().getOfflineFLPlayer(sender).getIgnoreList();
        sendFormatted(
                sender,
                "&(green)%0", ignoreList.isEmpty()
                        ? "You are not ignoring any players."
                        : String.join(", ", ignoreList)
        );
        return true;
    }
}
