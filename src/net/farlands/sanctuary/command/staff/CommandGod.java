package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.discord.DiscordChannel;
import org.bukkit.entity.Player;

public class CommandGod extends PlayerCommand {
    public CommandGod() {
        super(Rank.JR_BUILDER, "Enable or disable god mode.", "/god", "god");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        sendFormatted(sender, "&(gold)God mode %0.", (flp.god = !flp.god) ? "enabled" : "disabled");
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.COMMAND_LOG, flp.username + " toggled god " +
                (flp.god ? "on" : "off"));
        return true;
    }
}
