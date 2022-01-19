package net.farlands.sanctuary.discord;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.farlands.sanctuary.FarLands;

/**
 * All discord channels.
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

    public MessageChannel getChannel() {
        return FarLands.getDiscordHandler().getChannel(this);
    }
}
