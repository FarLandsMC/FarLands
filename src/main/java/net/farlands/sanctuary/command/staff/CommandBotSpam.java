package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.command.CommandSender;

public class CommandBotSpam extends Command {

    public CommandBotSpam() {
        super(Rank.BUILDER, "Toggle on or off bot spam mode.", "/botspam", "botspam");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        FarLands.getDataHandler().setAllowNewPlayers(!FarLands.getDataHandler().allowNewPlayers());
        return info(
            sender, "Bot spam mode %s.",
            FarLands.getDataHandler().allowNewPlayers() ? "disabled" : "enabled"
        );
    }
}
