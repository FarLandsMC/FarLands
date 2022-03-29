package net.farlands.sanctuary.command;

import net.dv8tion.jda.api.entities.Message;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.Rank;
import org.jetbrains.annotations.NotNull;

/**
 * A command that can only be run from Discord.
 */
public abstract class DiscordCommand extends Command {

    protected DiscordCommand(CommandData data) {
        super(data);
    }

    @Deprecated
    protected DiscordCommand(Rank minRank, Category category, String description, String usage, boolean requiresAlias,
                             String name, String... aliases) {
        super(minRank, category, description, usage, requiresAlias, name, aliases);
    }

    @Deprecated
    protected DiscordCommand(Rank minRank, Category category, String description, String usage, String name, String... aliases) {
        this(minRank, category, description, usage, false, name, aliases);
    }

    @Deprecated
    protected DiscordCommand(Rank minRank, String description, String usage, boolean requiresAlias,
                             String name, String... aliases) {
        this(minRank, Category.STAFF, description, usage, requiresAlias, name, aliases);
    }

    @Deprecated
    protected DiscordCommand(Rank minRank, String description, String usage, String name, String... aliases) {
        this(minRank, description, usage, false, name, aliases);
    }

    /**
     * Get the discord message from the provided args[0] item.
     * <p>
     * Note: This method will only work if {@link #requiresMessageID()} returns true.
     *
     * @param args0 The first argument provided to the command when {@link #requiresMessageID()} returns true.
     * @return The message associated with the provided command
     */
    public static @NotNull Message getMessage(String args0) {
        String[] discordInfo = args0.split(":");
        String channelId = discordInfo[0];
        String messageId = discordInfo[1];

        return FarLands.getDiscordHandler()
            .getNativeBot()
            .getTextChannelById(channelId)
            .retrieveMessageById(messageId)
            .complete();
    }

    public boolean deleteOnUse() {
        return false;
    }

    public boolean requiresMessageID() {
        return false;
    }
}
