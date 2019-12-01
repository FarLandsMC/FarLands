package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.TextUtils;
import net.farlands.odyssey.util.TimeInterval;
import net.farlands.odyssey.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class CommandPackage extends PlayerCommand {
    public CommandPackage() {
        super(Rank.KNIGHT, "Send held item to other players.", "/package <player> [message]", "package");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length == 0)
            return false;
        OfflineFLPlayer flp = getFLPlayer(args[0]);
        if(flp == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        if(sender.getUniqueId().equals(flp.getUuid())) {
            sender.sendMessage(ChatColor.RED + "You cannot send a package to yourself.");
            return true;
        }
        long timeRemaining = FarLands.getDataHandler().getRADH().cooldownTimeRemaining("package", sender.getUniqueId().toString() + flp.getUsername());
        if(timeRemaining > 0) {
            sender.sendMessage(ChatColor.RED + "You can send this person another package in " + TimeInterval.formatTime(50L * timeRemaining, false));
            return true;
        }
        ItemStack item = sender.getInventory().getItemInMainHand();
        if(item == null || Material.AIR.equals(item.getType())) {
            sender.sendMessage(ChatColor.RED + "You must hold the item you wish to send in your hand.");
            return true;
        }
        item = item.clone();
        final String message = Chat.applyColorCodes(Rank.getRank(sender), joinArgsBeyond(0, " ", args));
        if(flp.isOnline()) {
            Player player = flp.getOnlinePlayer();
            player.spigot().sendMessage(TextUtils.format("&(gold){%0} has sent you &(aqua)%1" +
                    (message.equals("") ? "" : "&(gold) with the following message &(aqua)" + message), FarLands.getPDH()
                    .getEffectiveName(sender.getUniqueId()), Utils.itemName(item)));
            Utils.giveItem(player, item, true);
            sender.sendMessage(ChatColor.GOLD + "Package sent.");
            FarLands.getDataHandler().getRADH().setCooldown(10L * 60L * 20L, "package", sender.getUniqueId().toString() + flp.getUsername());
            sender.getInventory().setItemInMainHand(null);
        } else {
            if (FarLands.getDataHandler().addPackage(flp.getUuid(), FarLands.getPDH().getEffectiveName(sender.getUniqueId()), item, message)) {
                sender.sendMessage(ChatColor.GOLD + "Package sent.");
                sender.getInventory().setItemInMainHand(null);
                FarLands.getDataHandler().getRADH().setCooldown(10L * 60L * 20L, "package", sender.getUniqueId().toString() + flp.getUsername());
            } else
                sender.sendMessage(ChatColor.RED + "You cannot send " + flp.getUsername() + " a package right now.");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? (Rank.getRank(sender).isStaff() ? getOnlineVanishedPlayers(args.length == 0 ? "" : args[0]) :
                getOnlinePlayers(args.length == 0 ? "" : args[0])) : Collections.emptyList();
    }
}
