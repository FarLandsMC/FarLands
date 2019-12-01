package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.TextUtils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandMessage extends PlayerCommand {
    public CommandMessage() {
        super(Rank.INITIATE, "Send a private message to another player.", "/msg <player> <message>", true, "msg",
                "w", "m", "r", "tell", "whisper");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(Player sender, String[] args) {
        if("r".equals(args[0])) { // Reply to the last message sent
            if (args.length < 2)
                return true;
            CommandSender recipient = (CommandSender)FarLands.getDataHandler().getRADH().retrieve("msg", sender.getName());
            if(recipient == null) {
                sender.sendMessage(ChatColor.RED + "You have no recent messages to reply to.");
                return true;
            }
            // Keep the name stored
            sendMessage(recipient, sender, Chat.applyColorCodes(Rank.getRank(sender), joinArgsBeyond(0, " ", args)));
        }else{
            RandomAccessDataHandler radh = FarLands.getDataHandler().getRADH();
            if(args.length == 1) {
                Player toggled = (Player)radh.retrieve("replytoggle", sender.getName());
                CommandSender recipient = (CommandSender)FarLands.getDataHandler().getRADH().retrieve("msg", sender.getName());
                if(toggled == null) {
                    if(recipient == null)
                        sender.sendMessage(ChatColor.RED + "You do not have an active reply toggle currently.");
                    else {
                        radh.store(recipient, "replytoggle", sender.getName());
                        sendFormatted(sender, "&(gold)You are now messaging {&(aqua)%0}. Type " +
                                "$(hovercmd,/m,{&(gray)Click to Run},&(aqua)/m) to toggle off, " +
                                "or start your message with {&(aqua)!} to send it to public chat.", recipient.getName());
                    }
                }else{
                    radh.delete("replytoggle", sender.getName());
                    sender.sendMessage(ChatColor.GOLD + "You are no longer messaging " + toggled.getName() + ".");
                }
                return true;
            }else if(args.length == 2) {
                Player newToggled = Rank.getRank(sender).isStaff() ? getVanishedPlayer(args[1]) : getPlayer(args[1]);
                if (newToggled == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found");
                    return true;
                }
                Player toggled = (Player)radh.retrieve("replytoggle", sender.getName());
                if(toggled != null && newToggled.getUniqueId().equals(toggled.getUniqueId())) {
                    radh.delete("replytoggle", sender.getName());
                    sender.sendMessage(ChatColor.GOLD + "You are no longer messaging " + toggled.getName() + ".");
                }else{
                    radh.store(newToggled, "replytoggle", sender.getName());
                    sendFormatted(sender, "&(gold)You are now messaging {&(aqua)%0}" +
                            (toggled == null ? "" : " and no longer messaging {&(aqua)" + toggled.getName() + "}") +
                            ". Type $(hovercmd,/m,{&(gray)Click to Run},&(aqua)/m) to toggle off, " +
                            "or start your message with {&(aqua)!} to send it to public chat.", newToggled.getName());
                }
                return true;
            }
            Player recipient = Rank.getRank(sender).isStaff() ? getVanishedPlayer(args[1]) : getPlayer(args[1]);
            if(recipient == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            // Try to send the message, and if it succeeds then store the metadata for /r
            sendMessage(recipient, sender, Chat.applyColorCodes(Rank.getRank(sender), joinArgsBeyond(1, " ", args)));
        }
        return true;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if(!(sender instanceof BlockCommandSender || sender instanceof ConsoleCommandSender ||
                !FarLands.getPDH().getFLPlayer(sender).isMuted())) {
            sender.sendMessage(ChatColor.RED + "You cannot use this command while muted.");
            return false;
        }
        return super.canUse(sender);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 && !"r".equals(alias) ? (Rank.getRank(sender).isStaff() ? getOnlineVanishedPlayers(args.length == 0 ? "" : args[0]) :
                getOnlinePlayers(args.length == 0 ? "" : args[0])) : Collections.emptyList();
    }

    // Send the formatted message
    public static void sendMessage(CommandSender recipient, CommandSender sender, String message) {
        OfflineFLPlayer recipientFlp = FarLands.getPDH().getFLPlayer(recipient),
                 senderFlp = FarLands.getPDH().getFLPlayer(sender);
        // Make sure everyone exists, and that the recipient isn't ignoring the sender
        if(recipientFlp != null && senderFlp != null && recipientFlp.isIgnoring(senderFlp.getUuid()))
            return;
        // Censor the message if censoring
        if (recipientFlp.isCensoring())
            message = Chat.getMessageFilter().censor(message);
        // This changes the message for the sender so they can see their message was censored when sent
        
        if(sender instanceof Player)
            sender.sendMessage(format("To", getRank(recipient), FarLands.getPDH().getFLPlayer(recipient).getDisplayName(), message));
        recipient.sendMessage(format("From", getRank(sender), FarLands.getPDH().getFLPlayer(sender).getDisplayName(), message));
        if(recipient instanceof Player) {
            Player player = (Player)recipient;
            // Check for AFK toggle
            if(FarLands.getDataHandler().getRADH().retrieveBoolean("afkCmd", player.getUniqueId().toString()))
                sender.sendMessage(ChatColor.RED + "This player is AFK, so they may not receive your message.");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
        }
        FarLands.getDataHandler().getRADH().store(sender, "msg", recipient.getName());
        FarLands.getDataHandler().getRADH().resetOrSetCooldown(10L * 60L * 20L, "msgCooldown", recipient.getName(),
                () -> FarLands.getDataHandler().getRADH().delete("msg", recipient.getName()));
        FarLands.getDataHandler().getRADH().store(recipient, "msg", sender.getName());
        FarLands.getDataHandler().getRADH().resetOrSetCooldown(10L * 60L * 20L, "msgCooldown", sender.getName(),
                () -> FarLands.getDataHandler().getRADH().delete("msg", sender.getName()));
        String senderName = sender instanceof Player ? ((Player)sender).getPlayerListName() : sender.getName();
        String recipientName = recipient instanceof Player ? ((Player)recipient).getPlayerListName() : recipient.getName();
        FarLands.broadcastStaff(TextUtils.format("&(red)[%0 -> %1]: &(gray)%2", senderName, recipientName, message));
    }

    private static String format(String prefix, Rank rank, String name, String message) {
        return ChatColor.DARK_GRAY + prefix + ' ' + rank.getNameColor() + name + ": " + ChatColor.RESET + message;
    }

    private static Rank getRank(CommandSender sender) {
        return sender instanceof Player || sender instanceof DiscordSender ? Rank.getRank(sender) : Rank.INITIATE;
    }
}
