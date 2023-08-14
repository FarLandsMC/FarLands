package net.farlands.sanctuary.command.player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class CommandGithub extends Command {

    private static final String GH_URL        = "https://github.com/FarLandsMC";
    public static final  String GH_AVATAR_URL = "https://avatars.githubusercontent.com/u/93167637?s=280&v=4"; // Literally just a right click -> copy image link

    public CommandGithub() {
        super(
            CommandData.simple(
                    "github",
                    "View the GitHub of the server's plugins",
                    "/github"
                )
                .aliases(false, "gh")
                .category(Category.INFORMATIONAL)
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof DiscordSender ds) {
            ds.sendMessageEmbeds(
                new EmbedBuilder()
                    .setTitle("FarLandsMC", GH_URL)
                    .setThumbnail(GH_AVATAR_URL)
                    .setDescription("FarLands custom plugins can be found at " + GH_URL + ".")
                    .setColor(Rank.DEV.color().value()) // fun colours
                    .build()
            );
        } else {
            info(sender, "FarLands custom plugins can be found at {}.", ComponentUtils.link(GH_URL));
        }
        return true;
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        return super.defaultCommand(false);
    }
}
