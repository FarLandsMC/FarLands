package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.MailMessage;
import net.farlands.odyssey.mechanic.Chat;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandMail extends Command {
    public CommandMail() {
        super(Rank.INITIATE, "Send a mail message to a player.", "/mail <send|read|clear> [player|pageNumber] [message]", "mail");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length == 0 || ("send".equals(args[0]) && args.length < 3))
            return false;
        if(sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) {
            sender.sendMessage(ChatColor.RED + "You must be in-game to use this command.");
            return true;
        }
        if("send".equals(args[0])) {
            FLPlayer flp = getFLPlayer(args[1]);
            if(flp == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            if(FarLands.getPDH().getMail(flp.getUuid()).stream().filter(msg -> msg.getSender().equals(sender.getName())).count() >= 5) {
                sender.sendMessage(ChatColor.RED + "You cannot send any more mail to this person until the read your current messages and clear them.");
                return true;
            }
            // Apply formatting
            String message = Chat.applyColorCodes(Rank.getRank(sender), joinArgsBeyond(1, " ", args));
            sender.sendMessage(format("To", flp.getRank().getNameColor(), flp.getUsername(), message));
            if(!flp.isIgnoring(FarLands.getPDH().getFLPlayer(sender).getUuid())) { // Check for ignoring
                flp.addMail(sender.getName(), message);
                Player player = flp.getOnlinePlayer();
                if (player != null) // Notify the play if online
                    player.sendMessage(ChatColor.GOLD + "You have mail. Read it with " + ChatColor.RED + "/mail read");
            }
        }else if("read".equals(args[0])) { // Send them their mail
            List<MailMessage> mail = FarLands.getPDH().getMail(FarLands.getPDH().getUuid(sender));
            if(mail.isEmpty()) {
                sender.sendMessage(ChatColor.GOLD + "You have no mail.");
                return true;
            }
            int index;
            try {
                index = args.length == 1 ? 0 : (Integer.parseInt(args[1]) - 1) * 5;
            }catch(NumberFormatException ex) {
                index = -1;
            }
            if(index < 0) {
                sender.sendMessage(ChatColor.RED + "Invalid page number: " + args[1]);
                return true;
            }else if(index >= mail.size()) {
                sender.sendMessage(ChatColor.RED + "You do not have enough mail to fill that page.");
                return true;
            }
            for(int i = index;i < Math.min(index + 5, mail.size());++ i)
                sender.sendMessage(format("From", ChatColor.GOLD, mail.get(i).getSender(), mail.get(i).getMessage()));
            sender.sendMessage(ChatColor.GOLD + "Clear your mail with " + ChatColor.RED + "/mail clear");
        }else if("clear".equals(args[0])) { // Remove their mail
            FarLands.getPDH().getFLPlayer(sender).clearMail();
            sender.sendMessage(ChatColor.GOLD + "Mail cleared.");
        }else
            return false;
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if(args.length <= 1) {
            return Stream.of("send", "read", "clear").filter(action -> action.startsWith(args.length == 0 ? "" : args[0]))
                    .collect(Collectors.toList());
        }else if("send".equals(args[0]) && args.length == 2)
            return getOnlinePlayers(args[1]);
        else
            return Collections.emptyList();
    }

    private static String format(String prefix, ChatColor color, String name, String message) {
        return ChatColor.DARK_GRAY + prefix + ' ' + color + name + ": " + ChatColor.WHITE + message;
    }
}
