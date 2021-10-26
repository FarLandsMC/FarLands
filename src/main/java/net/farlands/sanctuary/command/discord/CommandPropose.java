package net.farlands.sanctuary.command.discord;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.DiscordCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;

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
        FarLands.getDataHandler().getPluginData().addProposal(flp.username, String.join(" ", args));
        return true;
    }

    @Override
    public boolean deleteOnUse() {
        return true;
    }
}
