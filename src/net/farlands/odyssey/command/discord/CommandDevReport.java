package net.farlands.odyssey.command.discord;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandDevReport extends DiscordCommand {
    public CommandDevReport() {
        super(Rank.INITIATE, "Issue a suggestion or bug report for staff to review.", "/suggest|bugreport <message>", true,
                "suggest", "bugreport", "glitch");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length < 2)
            return false;
        if(!(sender instanceof DiscordSender)) {
            sender.sendMessage(ChatColor.RED + "Please only use this command in our discord; use /report instead.");
            return true;
        }
        String uid = Long.toString(((DiscordSender)sender).getUserID());
        long t = FarLands.getDataHandler().getRADH().cooldownTimeRemaining("devReportCooldown", uid);
        if(t > 0) {
            sender.sendMessage("You can use this command again in " + TimeInterval.formatTime(50L * t, false) + ".");
            return true;
        }
        if("suggest".equalsIgnoreCase(args[0])) {
            String message = "Suggestion from `" + sender.getName() + "`:```" + joinArgsBeyond(0, " ", args) + "```";
            FarLands.getDiscordHandler().sendMessageRaw("suggestions", message);
            FarLands.getDiscordHandler().sendMessageRaw("devreports", message);
        }else{
            String message = "Glitch/bug report from `" + sender.getName() + "`:```" + joinArgsBeyond(0, " ", args) + "```";
            FarLands.getDiscordHandler().sendMessageRaw("bugreports", message);
            FarLands.getDiscordHandler().sendMessageRaw("devreports", message);
        }
        FarLands.getDataHandler().getRADH().resetOrSetCooldown(10L * 60L * 20L, "devReportCooldown", uid, null);
        return true;
    }

    public boolean requiresVerifiedDiscordSenders() {
        return false;
    }

    public boolean deleteOnUse() {
        return true;
    }
}
