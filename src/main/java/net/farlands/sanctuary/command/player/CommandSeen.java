package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.Punishment;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.TimeInterval;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandSeen extends Command {
    public CommandSeen() {
        super(Rank.INITIATE, Category.PLAYER_SETTINGS_AND_INFO, "Check when a player was last online.", "/seen [player]", "seen");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        OfflineFLPlayer flp = args.length <= 0
                ? FarLands.getDataHandler().getOfflineFLPlayer(sender)
                : FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);

        if (flp == null) {
            sender.sendMessage(ComponentColor.red("Player not found."));
            return true;
        }

        Rank rank = Rank.getRank(sender);

        StringBuilder sb = new StringBuilder();
        TextComponent.Builder cb = Component.text()
            .color(NamedTextColor.GOLD)
            .content("Last Seen: ")
            .append(ComponentColor.aqua(TimeInterval.formatTime(System.currentTimeMillis() - flp.getLastLogin(), false)));
        sb.append("&(gold)Last Seen: &(aqua)").append(TimeInterval.formatTime(System.currentTimeMillis() - flp.getLastLogin(), false));

        // Test to see if this command isn't in #in-game essentially; make sure punishment info is private
        if (sender instanceof DiscordSender && ((DiscordSender) sender).getChannel().getIdLong() ==
                FarLands.getFLConfig().discordBotConfig.channels.get(DiscordChannel.STAFF_COMMANDS) ||
                sender instanceof Player && rank.isStaff() || sender instanceof ConsoleCommandSender) {
            cb.append(Component.text("\nMuted: "))
                .append(ComponentColor.aqua(flp.isMuted() + ""));

            if (!flp.punishments.isEmpty()) {
                List<Punishment> validPunishments = flp.punishments.stream().filter(Punishment::isNotPardoned).collect(Collectors.toList());
                int hourIndex = 0;
                cb.append(Component.text("Punishments: "));
                for (Punishment p : flp.punishments) {
                    cb.append(Component.text("\n - " + p.toFormattedString(hourIndex)));
                    if(validPunishments.contains(p))
                        ++hourIndex;
                }
            }
            cb.append(Component.text("\nLast IP: "))
                .append(ComponentColor.aqua(flp.lastIP));
        }

        sendFormatted(sender, sb.toString());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args[0], sender) : Collections.emptyList();
    }
}
