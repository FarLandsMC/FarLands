package net.farlands.odyssey.command.discord;

import net.dv8tion.jda.core.entities.Message;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.command.FLShutdownEvent;
import net.farlands.odyssey.data.Config;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.File;
import java.util.List;

public class CommandArtifact extends DiscordCommand {
    public CommandArtifact() {
        super(Rank.ADMIN, "Set the current plugin artifact on the server.",
                "/artifact [forcePush=false] [updateSpigot=false] {add jar as attachment}", "artifact");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) // Extra security
            return true;
        if (!(sender instanceof DiscordSender)) {
            sender.sendMessage(ChatColor.RED + "This command must be used from discord.");
            return false;
        }
        if (FarLands.getFLConfig().isScreenSessionNotSet()) {
            sender.sendMessage(ChatColor.RED + "The screen session for this server instance is not specified. " +
                    "This command requires that field to run.");
            return true;
        }

        String cid = args[0].substring(0, args[0].indexOf(':')), mid = args[0].substring(args[0].indexOf(':') + 1);
        Message msg = FarLands.getDiscordHandler().getNativeBot().getTextChannelById(cid).getMessageById(mid).complete();
        List<Message.Attachment> attachments = msg.getAttachments();
        if (attachments.isEmpty()) {
            sender.sendMessage("You must attach the jar to the command message.");
            return true;
        }
        File dest = FarLands.getDataHandler().getTempFile(attachments.get(0).getFileName());
        if (dest.exists())
            dest.delete();
        attachments.get(0).download(dest);
        Config cfg = FarLands.getFLConfig();
        if (args.length > 1 && "true".equals(args[1])) {
            FarLands.executeScript("artifact.sh", cfg.screenSession, cfg.paperDownload, cfg.dedicatedMemory,
                    args.length > 2 ? args[2] : "false");
            FarLands.getInstance().getServer().getPluginManager().callEvent(new FLShutdownEvent());
        }
        return true;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender)
            return true;
        else if (sender instanceof BlockCommandSender) // Prevent people circumventing permissions by using a command block
            return false;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp == null || !FarLands.getFLConfig().jsUsers.contains(flp.getUuid().toString())) {
            sender.sendMessage(ChatColor.RED + "You cannot use this command.");
            return false;
        }
        return super.canUse(sender);
    }

    @Override
    public boolean requiresMessageID() {
        return true;
    }
}
