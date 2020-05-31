package net.farlands.odyssey.command.player;

import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;

import org.bukkit.entity.Player;

import java.util.List;

public class CommandIgnoredList extends PlayerCommand {
    public CommandIgnoredList() {
        super(Rank.INITIATE, "List the players you are ignoring.", "/ignoredlist", "ignoredlist", "ignorelist");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        List<String> ignoreList = FarLands.getDataHandler().getOfflineFLPlayer(sender).getIgnoreList();
        TextUtils.sendFormatted(
                sender,
                "&(green)%0", ignoreList.isEmpty()
                        ? "You are not ignoring any players."
                        : String.join(", ", ignoreList)
        );
        return true;
    }
}
