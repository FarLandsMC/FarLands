package net.farlands.odyssey.command.discord;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordCommand;
import net.farlands.odyssey.data.Cooldown;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandDevReport extends DiscordCommand {
    private final Cooldown globalCooldown;

    public CommandDevReport() {
        super(Rank.INITIATE, "Issue a suggestion or bug report for staff to review.", "/suggest|bugreport <message>", true,
                "suggest", "bugreport", "glitch");
        this.globalCooldown = new Cooldown(10L * 60L * 20L);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2)
            return false;

        long t = globalCooldown.timeRemaining();
        System.out.println(t);
        if (t > 0) {
            sender.sendMessage(ChatColor.RED + "You can use this command again in " + TimeInterval.formatTime(50L * t, false) + ".");
            return true;
        }
        if ("suggest".equalsIgnoreCase(args[0])) {
            String message = "Suggestion from `" + sender.getName() + "`:```" + joinArgsBeyond(0, " ", args) + "```";
            FarLands.getDiscordHandler().sendMessageRaw("suggestions", message);
            FarLands.getDiscordHandler().sendMessageRaw("devreports", message);
        } else {
            String message = "Glitch/bug report from `" + sender.getName() + "`:```" + joinArgsBeyond(0, " ", args) + "```";
            FarLands.getDiscordHandler().sendMessageRaw("bugreports", message);
            FarLands.getDiscordHandler().sendMessageRaw("devreports", message);
        }
        globalCooldown.reset();
        return true;
    }

    public boolean deleteOnUse() {
        return true;
    }
}
