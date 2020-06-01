package net.farlands.odyssey.command.player;

import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;
import org.bukkit.command.CommandSender;

public class CommandReboot extends Command {
    public CommandReboot() {
        super(Rank.INITIATE, "See when the next server reboot will occur.", "/reboot", "reboot", "restart");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        TextUtils.sendFormatted(
                sender,
                "&(gold)Next reboot in %0",
                TimeInterval.formatTime(
                        86459999L - ((System.currentTimeMillis() - FarLands.getFLConfig().restartTime) % 86400000L),
                        false,
                        TimeInterval.MINUTE
                )
        );
        return true;
    }
}
