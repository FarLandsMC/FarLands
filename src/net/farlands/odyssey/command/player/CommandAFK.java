package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.Logging;
import net.farlands.odyssey.util.TimeInterval;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandAFK extends PlayerCommand {
    public CommandAFK() {
        super(Rank.INITIATE, "Place yourself in AFK mode.", "/afk", "afk");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);

        // Check the command cooldown
        if (!session.isCommandCooldownComplete(this)) {
            sendFormatted(sender, "&(red)You can use this command again in %0.",
                    TimeInterval.formatTime(session.commandCooldownTimeRemaining(this) * 50L, false));
            return true;
        }

        // Don't allow them to run this command while they're actively being AFK checked
        if (!session.afkCheckCooldown.isComplete())
            return true;

        // Set them to be AFK
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> session.afk = true, 50L);
        session.setCommandCooldown(this, 10L * 60L * 20L);

        // Reset their AFK check cooldown
        if (session.afkCheckInitializerCooldown != null)
            session.afkCheckInitializerCooldown.resetCurrentTask();

        // Notify the server
        Logging.broadcast(flp -> !flp.handle.isIgnoring(session.handle), " * %0 is now AFK.", session.handle.username);
        return true;
    }
}
