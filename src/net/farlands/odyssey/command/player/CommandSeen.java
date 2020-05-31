package net.farlands.odyssey.command.player;

import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.TimeInterval;

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
            TextUtils.sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        Rank rank = Rank.getRank(sender);

        StringBuilder sb = new StringBuilder();
        sb.append("&(gold)Last Seen: &(aqua)").append(TimeInterval.formatTime(System.currentTimeMillis() - flp.getLastLogin(), false));

        // Test to see if this command isn't in #in-game essentially; make sure punishment info is private
        if (sender instanceof DiscordSender && ((DiscordSender) sender).getChannel().getIdLong() ==
                FarLands.getFLConfig().discordBotConfig.channels.get("staffcommands") ||
                sender instanceof Player && rank.isStaff() || sender instanceof ConsoleCommandSender) {
            sb.append("\n&(gold)Muted: &(aqua)").append(flp.isMuted());

            if (!flp.punishments.isEmpty()) {
                sb.append("\n&(gold)Punishments:");
                flp.punishments.forEach(p -> sb.append("\n - ").append(p));
            }
            sb.append("\n&(gold)Last IP: &(aqua)").append(flp.lastIP);
        }

        TextUtils.sendFormatted(sender, sb.toString());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args[0], sender) : Collections.emptyList();
    }
}
