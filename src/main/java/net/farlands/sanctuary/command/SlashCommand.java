package net.farlands.sanctuary.command;

import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class SlashCommand {

    private final SlashCommandData commandData;
    protected     Rank             minRank;

    protected SlashCommand(SlashCommandData commandData) {
        this.commandData = commandData;
        this.minRank = Rank.INITIATE;
    }

    /**
     * Check that the command is valid to execute
     *
     * @param sender      flp to check
     * @param interaction interaction to check
     * @throws IllegalPermissionException if the sender's rank is lower than {@code this.minRank}
     */
    public void check(@Nullable OfflineFLPlayer sender, @NotNull SlashCommandInteraction interaction) {
        if (sender != null && sender.rank.specialCompareTo(this.minRank) < 0) {
            throw new IllegalPermissionException(this.minRank);
        }
    }

    public abstract void execute(@Nullable OfflineFLPlayer sender, @NotNull SlashCommandInteraction interaction);

    public @Nullable List<Choice> autoComplete(@NotNull CommandAutoCompleteInteraction query) {
        return Collections.emptyList();
    }

    public SlashCommandData commandData() {
        return commandData;
    }

    protected static List<Choice> stringChoices(@NotNull List<String> strings) {
        return strings.stream().map(s -> new Choice(s, s)).toList();
    }

    protected static List<Choice> longChoices(@NotNull List<Long> strings) {
        return strings.stream().map(s -> new Choice(s + "", s)).toList();
    }

    protected static List<Choice> doubleChoices(@NotNull List<Double> strings) {
        return strings.stream().map(s -> new Choice(s + "", s)).toList();
    }

    public static class CommandException extends RuntimeException {

        public CommandException(String message) {
            super(message);
        }

    }

    public static class IllegalPermissionException extends CommandException {

        public IllegalPermissionException(Rank requiredRank) {
            super(
                requiredRank == null
                    ? "Insufficient Permissions"
                    : "You must be at least rank " + requiredRank + " to use this command."
            );
        }

    }
}
