package net.farlands.sanctuary.command;

import net.farlands.sanctuary.data.Rank;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This class does NOT define a command for players in the sense of rank, rather in the sense that the execute method requires
 * the sender being an online player.
 */
public abstract class PlayerCommand extends Command {
    @Deprecated
    protected PlayerCommand(Rank minRank, Category category, String description, String usage, boolean requiresAlias,
                            String name, String... aliases) {
        super(minRank, category, description, usage, requiresAlias, name, aliases);
    }

    @Deprecated
    protected PlayerCommand(Rank minRank, Category category, String description, String usage, String name, String... aliases) {
        this(minRank, category, description, usage, false, name, aliases);
    }

    @Deprecated
    protected PlayerCommand(Rank minRank, String description, String usage, boolean requiresAlias,
                            String name, String... aliases) {
        this(minRank, Category.STAFF, description, usage, requiresAlias, name, aliases);
    }

    @Deprecated
    protected PlayerCommand(Rank minRank, String description, String usage, String name, String... aliases) {
        this(minRank, description, usage, false, name, aliases);
    }

    protected PlayerCommand(CommandData data) {
        super(data);
    }

    protected abstract boolean execute(Player sender, String[] args);

    @Override
    public final boolean execute(CommandSender sender, String[] args) {
        return execute((Player) sender, args);
    }

    public boolean canUse(Player player) {
        return super.canUse(player);
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return error(sender, "You must be in-game to use this command.");
        }
        return canUse((Player) sender);
    }
}
