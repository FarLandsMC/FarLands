package net.farlands.sanctuary.command.player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

public class CommandVote extends Command {


    public CommandVote() {
        super(
            CommandData.withRank(
                    "vote",
                    "Get the links to vote for FarLands",
                    "/vote",
                    Rank.INITIATE
                )
                .category(Category.MISCELLANEOUS)
        );
    }

    private static final String HEADER = "Use the following links to vote for the server:";

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof DiscordSender ds) {
            ds.sendMessageEmbeds(
                new EmbedBuilder()
                    .setTitle(HEADER)
                    .setDescription(
                        FarLands.getFLConfig().voteConfig.voteLinks
                            .values()
                            .stream()
                            .filter(l -> !l.isBlank())
                            .map(l -> "- <" + l + ">")
                            .collect(Collectors.joining("\n"))
                    )
                    .setColor(NamedTextColor.GOLD.value())
                    .build()
            );
        } else {
            TextComponent.Builder builder = Component.text();

            builder.append(ComponentColor.gold(HEADER));

            FarLands.getFLConfig().voteConfig.voteLinks.forEach((k, vl) -> {
                if (!vl.isBlank()) {
                    builder.append(Component.newline())
                        .append(ComponentColor.gray("- "))
                        .append(ComponentUtils.link(vl));
                }
            });
            sender.sendMessage(builder);
        }
        return true;
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        return this.defaultCommand(false);
    }
}
