package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandBotSpam extends Command {
    public CommandBotSpam() {
        super(Rank.BUILDER, "Toggle on or off bot spam mode.", "/botspam", "botspam");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        FarLands.getDataHandler().setAllowNewPlayers(!FarLands.getDataHandler().allowNewPlayers());
        sender.sendMessage(ChatColor.GOLD + "Bot spam mode " +
                (FarLands.getDataHandler().allowNewPlayers() ? "disabled." : "enabled."));
        return true;
    }
}
