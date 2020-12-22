package net.farlands.sanctuary.command.discord;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.DiscordCommand;
import net.farlands.sanctuary.data.Cooldown;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.TimeInterval;

import org.bukkit.command.CommandSender;

public class CommandDevReport extends DiscordCommand {
    private final Cooldown globalCooldown;

    public CommandDevReport() {
        super(Rank.INITIATE, Category.REPORTS, "Issue a suggestion or bug report for staff to review.",
                "/suggest|bugreport <message>", true, "suggest", "bugreport", "glitch");
        this.globalCooldown = new Cooldown(10L * 60L * 20L);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;

        long t = globalCooldown.timeRemaining();
        if (t > 0) {
            sendFormatted(sender, "&(red)You can use this command again in %0.", TimeInterval.formatTime(50L * t, false));
            return true;
        }

        String body = joinArgsBeyond(0, " ", args).replaceAll("`", "\\`");
        String message;
        if ("suggest".equalsIgnoreCase(args[0])) {
            message = "Suggestion from `" + sender.getName() + "`:```" + body + "```";
            FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.SUGGESTIONS, message);
        } else {
            message = "Glitch/bug report from `" + sender.getName() + "`:```" + body + "```";
            FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.BUG_REPORTS, message);
        }
        FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.DEV_REPORTS, message);

        globalCooldown.reset();
        return true;
    }

    public boolean deleteOnUse() {
        return globalCooldown.timeRemaining() <= 0;
    }
}
