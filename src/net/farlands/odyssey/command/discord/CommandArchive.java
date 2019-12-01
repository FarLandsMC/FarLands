package net.farlands.odyssey.command.discord;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.data.Rank;
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

public class CommandArchive extends DiscordCommand {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy-HH:mm:ss");
    private static final List<String> ACTIONS = Arrays.asList("none", "clear", "delete");

    public CommandArchive() {
        super(Rank.ADMIN, "Archive a channel.", "/archive <channel> [action=none]", "archive");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;
        if (args[0].charAt(0) == '#') // Delete the # from the start of the channel name
            args[0] = args[0].substring(1);
        // Allow this potentially long process to run separately
        int action = args.length > 1 ? ACTIONS.indexOf(args[1].toLowerCase()) : 0;
        if (action < 0) {
            sender.sendMessage(ChatColor.RED + "Invalid action: " + args[1] + ". Valid actions: " + String.join(", ", ACTIONS) + ".");
            return true;
        }
        (new Archiver(sender, args[0], action)).start();
        return true;
    }

    private static final class Archiver extends Thread {
        private final CommandSender sender;
        private final String channelName;
        private final int action; // 0: none, 1: clear, 2: delete

        Archiver(CommandSender sender, String channelName, int action) {
            this.sender = sender;
            this.channelName = channelName;
            this.action = action;
        }

        @Override
        public void run() {
            try {
                run0();
            } catch (Throwable t) {
                sender.sendMessage(ChatColor.RED + "Failed to archive channel due to an unexpected error.");
                t.printStackTrace(System.out);
            }
        }

        void run0() throws IOException {
            TextChannel channel = FarLands.getDiscordHandler().getGuild().getTextChannels().stream()
                    .filter(ch -> ch.getName().equals(channelName)).findAny().orElse(null);
            if (channel == null) {
                sender.sendMessage(ChatColor.RED + "Channel not found.");
                return;
            }
            MessageHistory history = channel.getHistory();
            while (!history.retrievePast(100).complete().isEmpty()) ; // Load all the messages

            // Create the file and print the formatted messages to it
            File file = FarLands.getDataHandler().getTempFile(channelName + ".txt");
            List<Message> messages = history.getRetrievedHistory();
            try {
                PrintStream ofstream = new PrintStream(new FileOutputStream(file), false, "UTF-8");
                ofstream.println("## Archive of #" + channelName + " taken at " + DATE_FORMAT.format(new Date()) + " UTC. All times are in UTC.");
                for (int i = messages.size() - 1; i >= 0; --i) {
                    Message m = messages.get(i);
                    if ((m.getContentDisplay().trim().isEmpty() && m.getAttachments().isEmpty()) || m.getAuthor() == null)
                        continue;
                    ofstream.print(m.getCreationTime().atZoneSameInstant(ZoneOffset.UTC).format(DATE_FORMATTER) + ' ');
                    if (m.getContentDisplay().trim().isEmpty())
                        ofstream.println("Attachment(s) from: " + m.getAuthor().getName());
                    else
                        ofstream.println(m.getAuthor().getName() + ": " + m.getContentDisplay());
                    m.getAttachments().forEach(attachment -> ofstream.println("    " + attachment.getUrl()));
                }
                ofstream.flush();
                ofstream.close();
            } catch (IOException ex) {
                file.delete();
                throw ex;
            }

            // Tell the user if the process finished successfully or not
            if (FarLands.getDiscordHandler().getChannel("archives").sendFile(file).complete() != null) {
                if (action == 1)
                    messages.stream().map(Message::delete).forEach(AuditableRestAction::queue);
                else if (action == 2)
                    channel.delete().complete();
                sender.sendMessage(ChatColor.GREEN + "Archive complete.");
            } else
                sender.sendMessage(ChatColor.RED + "Failed to send archive to discord.");
            file.delete();
        }
    }
}
