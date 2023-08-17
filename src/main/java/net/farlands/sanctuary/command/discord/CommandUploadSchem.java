package net.farlands.sanctuary.command.discord;

import com.kicas.rp.util.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.SlashCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.FileSystem;
import net.farlands.sanctuary.util.Logging;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class CommandUploadSchem extends SlashCommand {

    public CommandUploadSchem() {
        super(Commands
                  .slash(
                      "uploadschem",
                      "Upload a schematic to WorldEdit"
                  )
                  .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VIEW_AUDIT_LOGS))
                  .addOption(OptionType.ATTACHMENT, "schematic", "Schematic to upload", true, false)
        );
        this.minRank = Rank.BUILDER;
    }

    @Override
    public void execute(OfflineFLPlayer sender, @NotNull SlashCommandInteraction interaction) {
        Message.Attachment attachment = interaction.getOption("schematic").getAsAttachment();
        String fileName = attachment.getFileName();

        if (fileName.endsWith("zip")) { // Zip file was uploaded
            File attachmentDest = FarLands.getDataHandler().getTempFile(fileName);

            if (attachmentDest.exists()) attachmentDest.delete();

            try {
                attachmentDest = attachment.getProxy().downloadToFile(attachmentDest).get();
            } catch (InterruptedException | ExecutionException ex) {
                Logging.error(ex);
                ex.printStackTrace();
                throw new CommandException("Failed to upload schematics.");
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
                ex.printStackTrace();
                throw new CommandException("Failed to upload schematics.");
            }

            attachmentDest.delete();
            interaction.reply("Schematics uploaded.").queue();
        } else {
            File dest = FileSystem.getFile(new File(System.getProperty("user.dir")), "plugins", "WorldEdit",
                                           "schematics", fileName);

            if (dest.exists()) {
                dest.delete();
            }

            attachment.getProxy().downloadToFile(dest);
            interaction.reply("Schematic uploaded.").queue();
        }
    }
}
