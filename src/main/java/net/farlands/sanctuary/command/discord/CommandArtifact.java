package net.farlands.sanctuary.command.discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.DiscordCommand;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.command.staff.CommandShutdown;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.StringPrintStream;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class CommandArtifact extends DiscordCommand {

    /**
     * Should the server update paper on next artifact restart?
     * <p>
     * TODO: Make this work for all server restarts, not just those caused by artifacting.
     */
    public static boolean updatePaper = false;

    public CommandArtifact() {
        super(CommandData.withRank(
            "artifact",
            "Set the current plugin artifact on the server.",
            "/artifact [args]",
            Rank.ADMIN));
    }

    @Override
    public boolean execute(CommandSender sender, String[] argsArr) {
        if (!canUse(sender)) return true; // Extra security (Paranoia)
        if (!(sender instanceof DiscordSender)) return error(sender, "This command must be used from Discord.");
        if (FarLands.getFLConfig().isScreenSessionNotSet()) {
            return error(sender, "The screen session for this server instance is not specified.  This command requires that field to run.");
        }

        Arguments args = new Arguments();
        Message message = getMessage(argsArr[0]);

        try {
            if(argsArr.length > 1) {
                new CommandLine(args).parse(joinArgsBeyond(0, " ", argsArr));
            }
        } catch (Exception e) {
            args.help = true; // Show the usage
        }

        if (args.help) {
            StringPrintStream ps = new StringPrintStream();
            CommandLine.usage(args, ps, CommandLine.Help.Ansi.OFF);
            message.reply("Usage: ```\n%s```".formatted(ps.toString())).queue();
            return true;
        }

        if (args.updatePaper) {
            File dest = FarLands.getDataHandler().getTempFile("paper.jar");
            dest.delete();

            String paperDownload = FLUtils.getLatestReleaseUrl();
            if (paperDownload == null) {
                return error(sender, "Failed to get latest paper download url.");
            }

            try (InputStream in = new URL(paperDownload).openStream()) {
                long b = Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                success(sender, "Successfully downloaded %,d bytes from paper", b);
            } catch (IOException e) {
                dest.delete();
                e.printStackTrace();
                return error(sender, "Failed to download latest paper version.");
            }
        }

        // Get the attachments
        List<Attachment> attachments = message.getAttachments().stream().filter(attachment -> "jar".equalsIgnoreCase(attachment.getFileExtension())).toList();
        if (attachments.isEmpty()) success(sender, "No attached jars found.");

        List<String> failures = new ArrayList<>();
        attachments.forEach(attachment -> {

            File dest = FarLands.getDataHandler().getTempFile(attachment.getFileName());
            if (dest.exists()) dest.delete();

            attachment.getProxy()
                .download()
                .thenAccept(is -> {
                    try {
                        long b = Files.copy(is, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        success(sender, "Downloaded %,d bytes from %s", b, attachment.getFileName());
                    } catch (Exception ex) {
                        failures.add(attachment.getFileName());
                        Logging.error(ex.getMessage());
                        ex.printStackTrace();
                    }
                });
        });

        if (!failures.isEmpty()) {
            return error(
                sender,
                "Unable to download file(s): %s%s",
                String.join(", ", failures),
                args.restartServer ? "\nAborting Restart..." : ""
            );
        }


        if (args.restartServer) CommandShutdown.restart();

        return true;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            return true;
        } else if (sender instanceof BlockCommandSender) {
            return false; // Prevent people circumventing permissions by using a command block
        }

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp == null || !FarLands.getFLConfig().jsUsers.contains(flp.uuid.toString())) {
            error(sender, "You cannot use this command.");
            return false;
        }

        return super.canUse(sender);
    }

    @Override
    public boolean requiresMessageID() {
        return true;
    }

    @CommandLine.Command(name = "/artifact", header = "Artifact a jar or make changes to the server.")
    private static class Arguments {

        @CommandLine.Option(names = { "-u", "--update-paper" }, description = "Update Paper to the latest version on next restart")
        private boolean updatePaper;

        @CommandLine.Option(names = { "-r", "--restart" }, description = "Artifact and restart")
        private boolean restartServer;

        @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display this help and exit")
        private boolean help;

    }
}
