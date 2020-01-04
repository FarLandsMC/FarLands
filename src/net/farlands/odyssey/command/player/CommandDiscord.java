package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;

import net.farlands.odyssey.data.Rank;
import org.bukkit.entity.Player;

public class CommandDiscord extends PlayerCommand {
    public CommandDiscord() {
        super(Rank.INITIATE, "Get the invite link to our discord server.", "/discord", "discord");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        sendFormatted(sender, "&(gold)Click $(link,%0,{&(aqua,underline)here}) and follow the link to join our discord server.",
                FarLands.getFLConfig().getDiscordInvite());
        return true;
    }
}
