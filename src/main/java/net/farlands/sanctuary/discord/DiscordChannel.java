package net.farlands.sanctuary.discord;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.farlands.sanctuary.FarLands;

/**
 * Discord channels that the bot can interact with
 */
public enum DiscordChannel {
    NOTEBOOK,
    REPORTS,
    ARCHIVES,
    IN_GAME,
    ANNOUNCEMENTS,
    WARP_PROPOSALS,
    DEBUG,
    ALERTS,
    DEV_REPORTS,
    SUGGESTIONS,
    BUG_REPORTS,
    STAFF_COMMANDS,
    COMMAND_LOG;

    public static final DiscordChannel[] VALUES = values();

    public long id() {
        return this.getChannel().getIdLong();
    }

    /**
     * Get the appropriate {@link TextChannel} for the channel
     */
    public TextChannel getChannel() {
        return FarLands.getDiscordHandler().getChannel(this);
    }

    public String toString() {
        return "<#%s>".formatted(id());
    }
}
