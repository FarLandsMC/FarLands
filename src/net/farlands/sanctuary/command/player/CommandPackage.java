package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.escapeExpression;
import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.util.Materials;
import com.kicas.rp.util.TextUtils;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Package;
import net.farlands.sanctuary.data.struct.PackageToggle;
import net.farlands.sanctuary.mechanic.Chat;
import net.farlands.sanctuary.util.TimeInterval;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class CommandPackage extends PlayerCommand {
    public CommandPackage() {
        super(Rank.KNIGHT, Category.MISCELLANEOUS, "Send held item to other players.", "/package <player> [message]", "package");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 0)
            return false;

        // Get the recipient and make sure they exist
        OfflineFLPlayer recipientFlp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if (recipientFlp == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        // Don't let people send packages to themselves
        if (sender.getUniqueId().equals(recipientFlp.uuid)) {
            sendFormatted(sender, "&(red)You cannot send a package to yourself.");
            return true;
        }

        // Make sure the sender has exhausted the command cooldown
        FLPlayerSession senderSession = FarLands.getDataHandler().getSession(sender);
        long timeRemaining = senderSession.commandCooldownTimeRemaining(this);
        if (timeRemaining > 0) {
            sendFormatted(sender, "&(red)You can send this person another package in %0",
                    TimeInterval.formatTime(50L * timeRemaining, false));
            return true;
        }

        // Get the item to send and make sure it's not empty (air or nonexistent)
        ItemStack item = sender.getInventory().getItemInMainHand().clone();
        if (Materials.stackType(item) == Material.AIR) {
            sender.sendMessage("&(red)You must hold the item you wish to send in your hand.");
            return true;
        }

        // If the package has a message then grab it and apply color codes if the sender has chat colors
        final String message = Chat.applyColorCodes(Rank.getRank(sender), joinArgsBeyond(0, " ", args)),
              escapedMessage = escapeExpression(message);
        final boolean useEscaped = !senderSession.handle.rank.isStaff();

        if (recipientFlp.packageToggle == PackageToggle.DECLINE || recipientFlp.getIgnoreStatus(sender).includesPackages()) {
            TextUtils.sendFormatted(sender, "&(red)This player is not accepting packages.");
            return true;
        }

        // Players can only queue one item at a time, so make sure this operation actually succeeds
        if (FarLands.getDataHandler().addPackage(recipientFlp.uuid,
                new Package(sender.getUniqueId(), escapeExpression(Chat.removeColorCodes(senderSession.handle.getDisplayName())),
                item, useEscaped ? escapedMessage : message, false))
        ) {
            sender.getInventory().setItemInMainHand(null);
            senderSession.setCommandCooldown(this, senderSession.handle.rank.getPackageCooldown() * 60L * 20L);
            if (recipientFlp.isOnline())
                recipientFlp.getSession().givePackages();

            sendFormatted(sender, "&(green)Package sent.");
        }
        // The sender already has a package queued for this person so the transfer failed
        else
            sendFormatted(sender, "&(red)You cannot send %0 a package right now.", recipientFlp.username);

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender)
                : Collections.emptyList();
    }
}
