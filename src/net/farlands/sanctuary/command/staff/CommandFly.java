package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.discord.DiscordChannel;
import org.bukkit.entity.Player;

public class CommandFly extends PlayerCommand {
    public CommandFly() {
        super(Rank.MEDIA, "Enable or disable flight.", "/fly", "fly");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        flp.flightPreference = !flp.flightPreference;
        flp.updateSessionIfOnline(false);
        sendFormatted(sender, "&(gold)Flying %0.", flp.flightPreference ? "enabled" : "disabled");
        FarLands.getDiscordHandler().sendMessage(DiscordChannel.COMMAND_LOG, flp.username + " toggled flight " +
                (flp.flightPreference ? "on" : "off"));
        return true;
    }
}
