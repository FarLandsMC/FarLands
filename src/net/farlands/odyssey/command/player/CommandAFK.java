package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.TimeInterval;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandAFK extends PlayerCommand {
    public CommandAFK() {
        super(Rank.INITIATE, "Place yourself in AFK mode.", "/afk", "afk");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        if (!session.isCommandCooldownComplete(this)) { // Check the command cooldown
            sender.sendMessage(ChatColor.RED + "You can use this command again in " +
                    TimeInterval.formatTime(session.commandCooldownTimeRemaining(this) * 50L, false));
            return true;
        }

        if (!session.afkCheckCooldown.isComplete())
            return true;

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> session.afk = true, 50L);
        session.setCommandCooldown(this, 10L * 60L * 20L);

        if (session.afkCheckInitializerCooldown != null)
            session.afkCheckInitializerCooldown.resetCurrentTask();

        Chat.broadcast(flp -> !flp.handle.isIgnoring(session.handle), " * " + sender.getName() + " is now AFK.", false);
        return true;
    }
}
