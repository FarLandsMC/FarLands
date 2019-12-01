package net.farlands.odyssey.command.discord;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import org.bukkit.command.CommandSender;

public class CommandPropose extends DiscordCommand {
    public CommandPropose() {
        super(Rank.BUILDER, "Issue a proposal to be voted on.", "/propose <message>", "propose");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        FarLands.getDataHandler().getPluginData().addProposal(flp.getUsername(), String.join(" ", args));
        return true;
    }

    @Override
    public boolean deleteOnUse() {
        return true;
    }
}
