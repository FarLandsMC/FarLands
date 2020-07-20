package net.farlands.sanctuary.command.discord;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.util.Utils;

import net.dv8tion.jda.api.entities.Message;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.DiscordCommand;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FileSystem;
import net.farlands.sanctuary.util.Logging;

import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class CommandUploadSchem extends DiscordCommand {
    public CommandUploadSchem() {
        super(Rank.BUILDER, "Upload a schematic file to the server.", "/uploadschem {add schematic as attachment}",
                "uploadschem");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof DiscordSender)) {
            sendFormatted(sender, "&(red)This command must be used from discord.");
            return false;
        }

        // Locate the attachment
        String channelId = args[0].substring(0, args[0].indexOf(':')), messageId = args[0].substring(args[0].indexOf(':') + 1);
        Message message = FarLands.getDiscordHandler().getNativeBot().getTextChannelById(channelId).retrieveMessageById(messageId).complete();
        List<Message.Attachment> attachments = message.getAttachments();
        if (attachments.isEmpty()) {
            sender.sendMessage("You must attach the schematic to the command message.");
            return true;
        }

        String fileName = attachments.get(0).getFileName();

        // They uploaded a zip file with multiple schematics, so unpack each one
        if (fileName.endsWith("zip")) {
            File attachmentDest = FarLands.getDataHandler().getTempFile(fileName);

            if (attachmentDest.exists())
                attachmentDest.delete();

            try {
                attachmentDest = attachments.get(0).downloadToFile(attachmentDest).get();
            } catch (InterruptedException | ExecutionException ex) {
                Logging.error(ex);
                ex.printStackTrace(System.out);
                sender.sendMessage("Failed to upload schematics.");
                return true;
            }

            // Unpack the zip
            try {
                ZipFile zippedSchems = new ZipFile(attachmentDest);
                Enumeration<? extends ZipEntry> entries = zippedSchems.entries();

                while (entries.hasMoreElements()) {
                    // Get the entry and truncate the name to just the file
                    ZipEntry entry = entries.nextElement();
                    String schemName = entry.getName();
                    schemName = schemName.substring(Utils.indexOfDefault(schemName.lastIndexOf('/'), 0));

                    // Get and clear the destination path
                    Path schemDest = Paths.get(System.getProperty("user.dir"), "plugins", "WorldEdit", "schematics", schemName);
                    Files.deleteIfExists(schemDest);

                    // Copy over the schematic
                    Files.copy(new ZipInputStream(zippedSchems.getInputStream(entry)), schemDest);
                }
            } catch (IOException ex) {
                Logging.error(ex);
                ex.printStackTrace(System.out);
                sender.sendMessage("Failed to upload schematics.");
                return true;
            }

            attachmentDest.delete();
            sender.sendMessage("Schematics uploaded.");
        }
        // Just one schematic was uploaded
        else {
            File dest = FileSystem.getFile(new File(System.getProperty("user.dir")), "plugins", "WorldEdit",
                    "schematics", fileName);

            if (dest.exists())
                dest.delete();

            attachments.get(0).downloadToFile(dest);
            sender.sendMessage("Schematic uploaded.");
        }

        return true;
    }

    @Override
    public boolean requiresMessageID() {
        return true;
    }
}
