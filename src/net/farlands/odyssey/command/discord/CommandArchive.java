package net.farlands.odyssey.command.discord;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.util.Utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.discord.DiscordChannel;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommandArchive extends DiscordCommand {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy-HH:mm:ss");

    public CommandArchive() {
        super(Rank.ADMIN, "Archive a channel.", "/archive <channel> [action=none]", "archive");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;

        // Delete the # from the start of the channel name
        if (args[0].charAt(0) == '#')
            args[0] = args[0].substring(1);

        Action action = Utils.valueOfFormattedName(args[1], Action.class);
        if (action == null) {
            sendFormatted(sender, "&(red)Invalid action: %0. Valid actions: %1.", args[1],
                    Arrays.stream(Action.VALUES).map(Utils::formattedName).collect(Collectors.joining(", ")));
            return true;
        }

        // Allow this potentially long process to run on another thread
        (new Archiver(sender, args[0], action)).start();
        return true;
    }

    private static final class Archiver extends Thread {
        private final CommandSender sender;
        private final String channelName;
        private final Action action;

        Archiver(CommandSender sender, String channelName, Action action) {
            this.sender = sender;
            this.channelName = channelName;
            this.action = action;
        }

        @Override
        public void run() {
            // Capture any extraneous exceptions
            try {
                run0();
            } catch (Throwable t) {
                sender.sendMessage(ChatColor.RED + "Failed to archive channel due to an unexpected error.");
                t.printStackTrace(System.out);
            }
        }

        void run0() throws IOException {
            // Find the channel
            TextChannel channel = FarLands.getDiscordHandler().getGuild().getTextChannels().stream()
                    .filter(ch -> ch.getName().equals(channelName)).findAny().orElse(null);
            if (channel == null) {
                sender.sendMessage(ChatColor.RED + "Channel not found.");
                return;
            }

            // Load the whole message history
            MessageHistory history = channel.getHistory();
            while (!history.retrievePast(100).complete().isEmpty()) ; // Load all the messages

            // Create the file and print the formatted messages to it
            File archiveFile = FarLands.getDataHandler().getTempFile(channelName + ".txt");
            List<Message> messages = history.getRetrievedHistory();
            try {
                PrintStream ofstream = new PrintStream(new FileOutputStream(archiveFile), false, "UTF-8");

                // Add a header
                ofstream.println("## Archive of #" + channelName + " taken at " + DATE_FORMAT.format(new Date()) + " UTC. All times are in UTC.");

                // Add the messages
                for (int i = messages.size() - 1; i >= 0; --i) {
                    Message m = messages.get(i);

                    // Ignore special message types
                    if ((m.getContentDisplay().trim().isEmpty() && m.getAttachments().isEmpty()) || m.getAuthor() == null)
                        continue;

                    ofstream.print(m.getTimeCreated().atZoneSameInstant(ZoneOffset.UTC).format(DATE_FORMATTER) + ' ');
                    if (m.getContentDisplay().trim().isEmpty())
                        ofstream.println("Attachment(s) from: " + m.getAuthor().getName());
                    else
                        ofstream.println(m.getAuthor().getName() + ": " + m.getContentDisplay());
                    m.getAttachments().forEach(attachment -> ofstream.println("    " + attachment.getUrl()));
                }

                ofstream.close();
            } catch (IOException ex) {
                archiveFile.delete();
                throw ex;
            }

            // Tell the user if the process finished successfully or not
            if (FarLands.getDiscordHandler().getChannel(DiscordChannel.ARCHIVES).sendFile(archiveFile).complete() != null) {
                if (action == Action.CLEAR)
                    messages.stream().map(Message::delete).forEach(AuditableRestAction::queue);
                else if (action == Action.DELETE)
                    channel.delete().complete();

                sendFormatted(sender, "&(green)Archive complete.");
            } else
                sendFormatted(sender, "&(red)Failed to send archive to discord.");

            archiveFile.delete();
        }
    }

    private enum Action {
        NONE, CLEAR, DELETE;

        static final Action[] VALUES = values();
    }
}
