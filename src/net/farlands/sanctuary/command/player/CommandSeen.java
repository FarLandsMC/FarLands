package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.TimeInterval;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandSeen extends Command {
    public CommandSeen() {
        super(Rank.INITIATE, "Check when a player was last online.", "/seen [player]", "seen");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflineFLPlayer flp = args.length <= 0
                ? FarLands.getDataHandler().getOfflineFLPlayer(sender)
                : FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);

        if (flp == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        Rank rank = Rank.getRank(sender);

        StringBuilder sb = new StringBuilder();
        sb.append("&(gold)Last Seen: &(aqua)").append(TimeInterval.formatTime(System.currentTimeMillis() - flp.getLastLogin(), false));

        // Test to see if this command isn't in #in-game essentially; make sure punishment info is private
        if (sender instanceof DiscordSender && ((DiscordSender) sender).getChannel().getIdLong() ==
                FarLands.getFLConfig().discordBotConfig.channels.get(DiscordChannel.STAFF_COMMANDS) ||
                sender instanceof Player && rank.isStaff() || sender instanceof ConsoleCommandSender) {
            sb.append("\n&(gold)Muted: &(aqua)").append(flp.isMuted());

            if (!flp.punishments.isEmpty()) {
                sb.append("\n&(gold)Punishments:");
                flp.punishments.forEach(p -> sb.append("\n - ").append(p));
            }
            sb.append("\n&(gold)Last IP: &(aqua)").append(flp.lastIP);
        }

        sendFormatted(sender, sb.toString());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args[0], sender) : Collections.emptyList();
    }
}
