package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.data.RandomAccessDataHandler;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CommandAFK extends PlayerCommand {
    private final RandomAccessDataHandler radh;

    public CommandAFK() {
        super(Rank.INITIATE, "Place yourself in AFK mode.", "/afk", "afk");
        this.radh = FarLands.getDataHandler().getRADH();
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(!radh.isCooldownComplete("afkCmdCooldown", sender.getUniqueId().toString())) { // Check the command cooldown
            sender.sendMessage(ChatColor.RED + "You can use this command again in " +
                    TimeInterval.formatTime(radh.cooldownTimeRemaining("afkCmdCooldown", sender.getUniqueId().toString()) * 50L, false));
            return true;
        }
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> radh.store(true, "afkCmd", sender.getUniqueId().toString()), 50L);
        radh.setCooldown(10L * 60L * 20L, "afkCmdCooldown", sender.getUniqueId().toString()); // Command cooldown

        // There's a chance the cooldown could be unintentionally absent, but this is only during an AFK check,
        // so the cooldown will be set anyway.
        radh.resetCooldown("afk", sender.getUniqueId().toString());

        FLPlayer senderFlp = FarLands.getPDH().getFLPlayer(sender);
        FarLands.broadcast(flp -> !flp.isIgnoring(senderFlp.getUuid()), " * " + sender.getName() + " is now AFK.", false);
        return true;
    }
}
