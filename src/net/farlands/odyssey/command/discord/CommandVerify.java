package net.farlands.odyssey.command.discord;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.data.RandomAccessDataHandler;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CommandVerify extends DiscordCommand {
    public CommandVerify() {
        super(Rank.INITIATE, "Verify and link your discord account to your minecraft account.",
                "/verify <inGameName>", "verify");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length >= 3 && Rank.getRank(sender).specialCompareTo(Rank.BUILDER) >= 0) {
            FLPlayer flp = FarLands.getPDH().getFLPlayer(UUID.fromString(args[1]), args[0]);
            flp.setDiscordID(Long.parseLong(args[2]));
            flp.updateDiscord();
            FarLands.getPDH().saveFLPlayer(flp);
            if(flp.isDiscordVerified())
                sender.sendMessage(ChatColor.GREEN + "Successfully manually verified " + args[0] + ".");
            else
                sender.sendMessage(ChatColor.RED + "Manual verification failed. Are you sure you copied the discord ID correctly?");
            return true;
        }
        if(sender instanceof DiscordSender) { // From discord
            if(args.length == 0)
                return false;
            Player player = getPlayer(args[0]);
            if(player == null) {
                sender.sendMessage("Player not found in-game.");
                return true;
            }
            RandomAccessDataHandler radh = FarLands.getDataHandler().getRADH();
            if(radh.retrieve("verify", player.getUniqueId().toString()) != null) { // Check if they have a verification pending
                sender.sendMessage("This player already has a verification pending.");
                return true;
            }
            // Mark that they have a verification pending; set the command cooldown; tell them what to do
            radh.store(sender, "verify", player.getUniqueId().toString());
            radh.setCooldown(60L * 20L, "verifyCooldown", player.getUniqueId().toString(), () -> radh.delete("verify", player.getUniqueId().toString()));
            player.sendMessage(ChatColor.GOLD + "Type " + ChatColor.GREEN + "/verify" + ChatColor.GOLD + " in-game to verify your account.");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5.0F, 1.0F);
        }else if(sender instanceof Player) { // From in-game
            Player player = (Player)sender;
            RandomAccessDataHandler radh = FarLands.getDataHandler().getRADH();
            if(radh.retrieve("verify", player.getUniqueId().toString()) == null) { // Check if they have a verification pending
                sender.sendMessage(ChatColor.RED + "You have no verification pending. Did you type " + ChatColor.GOLD +
                        "/verify " + sender.getName() + ChatColor.RED + " in discord yet?");
                return true;
            }
            DiscordSender discord = (DiscordSender)radh.retrieve("verify", player.getUniqueId().toString());
            // Remove temporary data
            radh.removeCooldown("verifyCooldown", player.getUniqueId().toString());
            radh.delete("verify", player.getUniqueId().toString());
            // Actually do the verification
            FLPlayer flp = FarLands.getPDH().getFLPlayer(player);
            flp.setDiscordID(discord.getUserID());
            flp.updateDiscord();
            // Tell them that they're verified
            player.sendMessage(ChatColor.GREEN + "Account verified!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5.0F, 1.0F);
            discord.sendMessage("Account verified!");
        }else
            sender.sendMessage(ChatColor.RED + "You must use this command from in-game or discord.");
        return true;
    }

    @Override
    public boolean requiresVerifiedDiscordSenders() {
        return false;
    }
}
