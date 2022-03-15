package net.farlands.sanctuary.command;

import com.kicas.rp.util.Pair;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Functional interface for defining a custom requirement to use a command.
 */
@FunctionalInterface
public interface CustomRequirement {
    /**
     * Check if the custom requirement has been completed.
     *
     * @param sender the sender executing the command
     * @return a new pair, where the first value is whether the requirement has been met,
     * and the second value is the custom message
     */
    @NotNull Pair<@Nullable Boolean, @Nullable Component> complete(final @Nullable CommandSender sender);
}
