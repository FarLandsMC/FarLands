package net.farlands.sanctuary.command.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.DiscordCommand;
import net.farlands.sanctuary.data.Cooldown;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.TimeInterval;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CommandDevReport extends DiscordCommand {
    private final Cooldown globalCooldown;

    public CommandDevReport() {
        super(
            CommandData.simple(
                    "suggest",
                    "Issue a suggestion or bug report for staff to review.",
                    "/suggest|bugreport <message>"
                )
                .category(Category.REPORTS)
                .aliases(true, "bugreport", "glitch")
        );
        this.globalCooldown = new Cooldown(10L * 60L * 20L);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;

        long t = globalCooldown.timeRemaining();
        if (t > 0) {
            return error(sender, "You can use this command again in {}.", TimeInterval.formatTime(50L * t, false));
        }

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        String body = joinArgsBeyond(0, " ", args).replaceAll("`", "`\u200B");
        String title;
        String description = "```" + body + "```";
        EmbedBuilder embedBuilder = new EmbedBuilder().setDescription(description);

        boolean isSuggestion = "suggest".equalsIgnoreCase(args[0]);
        if (isSuggestion) {
            title = "Suggestion from `" + flp.username + "`";
            embedBuilder.setTitle(title).setColor(NamedTextColor.DARK_GREEN.value());
        } else {
            title = "Bug Report from `" + flp.username + "`";
            embedBuilder.setTitle("⚠ " + title).setColor(NamedTextColor.RED.value());
        }


        FarLands.getDiscordHandler().sendMessageEmbed(
            isSuggestion ? DiscordChannel.SUGGESTIONS : DiscordChannel.BUG_REPORTS,
            embedBuilder,
            (message) -> {
                FarLands.getDiscordHandler().sendMessageEmbed(
                    DiscordChannel.DEV_REPORTS,
                    embedBuilder
                        .setTitle(title, message.getJumpUrl())
                        .setDescription("[Public Message](" + message.getJumpUrl() + ")\n" + description)
                );
                success(sender, "{::?Suggestion:Bug report} submitted.", isSuggestion);
            }
        );

        globalCooldown.reset();
        return true;
    }

    public boolean deleteOnUse() {
        return globalCooldown.timeRemaining() <= 0;
    }

    @Override
    public @NotNull List<SlashCommandData> discordCommands() {
        List<SlashCommandData> cmds = super.defaultCommands(false);
        cmds.forEach(c -> c.addOption(
            OptionType.STRING,
            "description",
            "Description of " + c.getName(),
            true, true
        ));
        return cmds;
    }
}
