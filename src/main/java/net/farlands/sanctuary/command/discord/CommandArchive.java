package net.farlands.sanctuary.command.discord;

import com.kicas.rp.util.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.interactions.commands.*;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.SlashCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CommandArchive extends SlashCommand {

    private static final SimpleDateFormat  DATE_FORMAT    = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy-HH:mm:ss");

    public CommandArchive() {
//        super(Rank.ADMIN, "Archive a channel.", "/archive <channel> [action=none]", "archive");
        super(Commands.slash("archive", "Archive a channel")
                  .addOption(OptionType.CHANNEL, "channel", "Channel to archive", true, false)
                  .addOption(OptionType.INTEGER, "action", "Action", false, true)
                  .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
        );
        this.minRank = Rank.ADMIN;
    }

    @Override
    public @Nullable List<Command.Choice> autoComplete(@NotNull CommandAutoCompleteInteraction query) {
        if (query.getFocusedOption().getName().equals("action")) {
            return Arrays.stream(Action.values())
                .map(e -> new Command.Choice(Utils.formattedName(e), e.ordinal()))
                .toList();
        }
        return null;
    }

    @Override
    public void execute(@Nullable OfflineFLPlayer sender, @NotNull SlashCommandInteraction interaction) {


        Action action;
        try {
            action = interaction.getOption("action") == null
                ? Action.NONE
                : Action.values()[interaction.getOption("action").getAsInt()];
        } catch (IndexOutOfBoundsException ex) {
            throw new CommandException("Invalid action.");
        }

        // Allow this potentially long process to run on another thread
        (new Archiver(interaction, interaction.getOption("channel").getAsChannel(), action)).start();
        return;
    }

    private static final class Archiver extends Thread {

        private final SlashCommandInteraction interaction;
        private final GuildChannelUnion       channel;
        private final Action                  action;

        Archiver(@NotNull SlashCommandInteraction interaction, GuildChannelUnion channel, Action action) {
            this.interaction = interaction;
            this.channel = channel;
            this.action = action;
        }

        @Override
        public void run() {
            // Capture any extraneous exceptions
            try {
                run0();
            } catch (Throwable t) {
                t.printStackTrace();
                throw new CommandException("Failed to archive channel due to an unexpected error.");
            }
        }

        void run0() throws IOException {
            // Find the channel
            TextChannel channel;
            try {
                channel = this.channel.asTextChannel();
            } catch (Exception unused) {
                throw new CommandException("Channel not found.");
            }

            // Load the whole message history
            MessageHistory history = channel.getHistory();
            while (!history.retrievePast(100).complete().isEmpty()) ; // Load all the messages

            // Create the file and print the formatted messages to it
            File archiveFile = FarLands.getDataHandler().getTempFile(channel.getName() + ".txt");
            List<Message> messages = history.getRetrievedHistory();
            try {
                PrintStream ofstream = new PrintStream(new FileOutputStream(archiveFile), false, StandardCharsets.UTF_8);

                // Add a header
                ofstream.println("## Archive of #" + channel.getName() + " taken at " + DATE_FORMAT.format(new Date()) + " UTC. All times are in UTC.");

                // Add the messages
                for (int i = messages.size() - 1; i >= 0; --i) {
                    Message m = messages.get(i);

                    // Ignore special message types
                    if ((m.getContentDisplay().trim().isEmpty() && m.getAttachments().isEmpty()) || m.getAuthor() == null) {
                        continue;
                    }

                    ofstream.print(m.getTimeCreated().atZoneSameInstant(ZoneOffset.UTC).format(DATE_FORMATTER) + ' ');
                    if (m.getContentDisplay().trim().isEmpty()) {
                        ofstream.println("Attachment(s) from: " + m.getAuthor().getName());
                    } else {
                        ofstream.println(m.getAuthor().getName() + ": " + m.getContentDisplay());
                    }
                    m.getAttachments().forEach(attachment -> ofstream.println("    " + attachment.getUrl()));
                }

                ofstream.close();
            } catch (IOException ex) {
                archiveFile.delete();
                throw ex;
            }

            // Tell the user if the process finished successfully or not
            if (FarLands.getDiscordHandler().getChannel(DiscordChannel.ARCHIVES).sendFiles(FileUpload.fromData(archiveFile)).complete() != null) {
                if (action == Action.CLEAR) {
                    channel.purgeMessages(messages);
                } else if (action == Action.DELETE) {
                    channel.delete().complete();
                }

                interaction.reply("Archive Complete").queue();
            } else {
                throw new CommandException("Failed to send archive to discord.");
            }

            archiveFile.delete();
        }
    }

    private enum Action {
        NONE, CLEAR, DELETE;

        static final Action[] VALUES = values();
    }
}
