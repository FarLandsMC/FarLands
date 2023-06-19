package net.farlands.sanctuary.command.discord;

import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.DiscordCommand;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CommandKickme extends DiscordCommand {

    public CommandKickme() {
        super(CommandData.withRank(
            "kickme",
            "Kick yourself from the server",
            "/kickme",
            Rank.INITIATE));
    }

    @Override
    public boolean execute(CommandSender sender, String[] argsArr) {
        if (!(sender instanceof DiscordSender ds)) return error(sender, "This command must be used from Discord.");

        OfflineFLPlayer flp = ds.getFlp();
        if (flp == null) {
            return error(sender, "You must be verified to run this command.");
        }

        Player pl = flp.getOnlinePlayer();

        if (pl == null) {
            return error(sender, "Your account must be logged in to run this command.");
        }

        pl.kick(ComponentColor.gold("Kicked from Discord."));
        success(sender, "Kicked %s from the game.", pl.getName());

        return true;
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        return defaultCommand(false);
    }
}
