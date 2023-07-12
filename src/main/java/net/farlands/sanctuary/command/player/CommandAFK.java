package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.TabListMechanics;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.TimeInterval;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CommandAFK extends PlayerCommand {

    public CommandAFK() {
        super(Rank.INITIATE, Category.MISCELLANEOUS, "Notify players that you are AFK.", "/afk", "afk");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);

        // Check the command cooldown
        if (!session.isCommandCooldownComplete(this)) {
            error(sender,
                "You can use this command again in {}.",
                TimeInterval.formatTime(session.commandCooldownTimeRemaining(this) * 50L, false)
            );
            return true;
        }

        // Don't allow them to run this command while they're actively being AFK checked
        if (!session.afkCheckCooldown.isComplete()) {
            return true;
        }

        // Set them to be AFK
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> session.afk = true, 50L);
        TabListMechanics.update();
        session.setCommandCooldown(this, 3L * 60L * 20L);

        // Reset their AFK check cooldown
        if (session.afkCheckInitializerCooldown != null) {
            session.afkCheckInitializerCooldown.resetCurrentTask();
        }

        // Notify the server
        Logging.broadcast(flp -> !flp.handle.getIgnoreStatus(session.handle).includesChat(), " * %s is now AFK.", session.handle.username);
        return true;
    }
}
