package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;

import net.farlands.odyssey.util.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static net.farlands.odyssey.util.Utils.*;

public class CommandWild extends PlayerCommand {
    public CommandWild() {
        super(Rank.INITIATE, "Teleport to a random location on the map.", "/wild", "wild", "rtp");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        RandomAccessDataHandler radh = FarLands.getDataHandler().getRADH();
        long timeRemaining = radh.cooldownTimeRemaining("wildCooldown", sender.getUniqueId().toString()) * 50L;
        if(timeRemaining > 0L) {
            sender.sendMessage(ChatColor.RED + "You can use the command again in " + TimeInterval.formatTime(timeRemaining, false) + ".");
            return true;
        }

        if(!"world".equals(sender.getWorld().getName())) { // rtpFindSafe is optimized for overworld and cannot be used if this changes
            sender.sendMessage(ChatColor.RED + "You can only use this command in the overworld.");
            return true;
        }

        if(Utils.serverMspt() > 80) {
            sender.sendMessage(ChatColor.RED + "The server is too laggy right now to use this command.");
            return true;
        }

        if(!radh.isCooldownComplete("wildCooldown", "global")) {
            sender.sendMessage(ChatColor.RED + "You cannot use this command right now. Try again in a few seconds.");
            return true;
        }

        radh.setCooldown(200L, "wildCooldown", "global");

        int wildCooldown = Rank.getRank(sender).getWildCooldown();
        if(wildCooldown > 0)
            radh.setCooldown(wildCooldown * 60L * 20L, "wildCooldown", sender.getUniqueId().toString());
        rtpPlayer(sender, 15000);
        return true;
    }

    // in case we decide to make a portal so we can copy into utils
    public static void rtpPlayer(final Player player, final int range) {
        Location rtp = new Location(player.getWorld(),
                RNG.nextInt(2 * range) - range, 62, RNG.nextInt(2 * range) - range,
                player.getLocation().getYaw(), player.getLocation().getPitch());
        while (rtp.getBlock().isLiquid() || rtp.getBlock().isPassable() || rtp.getBlock().getType().equals(Material.SEA_PICKLE)) {
            rtp.setX(RNG.nextInt(2 * range) - range);
            if (rtp.getBlock().isLiquid())
                rtp.setZ(RNG.nextInt(2 * range) - range);
        }
        final int x = rtp.getX() > 0 ? -4 : 4,
                  z = rtp.getZ() > 0 ? -4 : 4;
        Location temp = rtpFindSafe(rtp);
        while (temp == null) {
            rtp.setX(rtp.getBlockX() + x);
            rtp.setZ(rtp.getBlockZ() + z);
            temp = rtpFindSafe(rtp);
        }
        tpPlayer(player, temp);
        if (FarLands.getPDH().getFLPlayer(player).getHomes().isEmpty())
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.AQUA +
                    "You have no homes, use /sethome [name] so you can safely return to your location!"));
    }

    private static Location rtpFindSafe(final Location origin) {
        Location safe = origin.clone();
        safe.setX(safe.getBlockX() + .5);
        safe.setZ(safe.getBlockZ() + .5);
        safe.getChunk().load();
        if (canStand(safe.getBlock()) && isSafe(safe.clone()))
            return safe.add(0, .5, 0);
        int s = 62, e = 254;
        do {
            safe.setY((s + e + 1) >> 1);
            if (safe.getBlock().getLightFromSky() <= 8)
                s = safe.getBlockY();
            else
                e = safe.getBlockY();
        } while (e - s > 1);
        safe.setY((s + e - 1) >> 1);
        if (canStand(safe.getBlock()) && isSafe(safe.clone()))
            return safe.add(0, 1.5, 0);
        FarLands.getDebugger().echo("unsafe rtp @ " +
                safe.getBlockX() + " " + safe.getBlockY() + " " + safe.getBlockZ());
        return null;
    }
}
