package net.farlands.sanctuary.command;

import net.farlands.sanctuary.data.Rank;

public abstract class DiscordCommand extends Command {
    protected DiscordCommand(Rank minRank, Category category, String description, String usage, boolean requiresAlias,
                             String name, String... aliases) {
        super(minRank, category, description, usage, requiresAlias, name, aliases);
    }

    protected DiscordCommand(Rank minRank, Category category, String description, String usage, String name, String... aliases) {
        this(minRank, category, description, usage, false, name, aliases);
    }

    protected DiscordCommand(Rank minRank, String description, String usage, boolean requiresAlias,
                             String name, String... aliases) {
        this(minRank, Category.STAFF, description, usage, requiresAlias, name, aliases);
    }

    protected DiscordCommand(Rank minRank, String description, String usage, String name, String... aliases) {
        this(minRank, description, usage, false, name, aliases);
    }

    public boolean deleteOnUse() {
        return false;
    }

    public boolean requiresMessageID() {
        return false;
    }
}
