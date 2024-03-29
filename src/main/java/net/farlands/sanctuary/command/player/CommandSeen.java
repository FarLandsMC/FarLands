package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Punishment;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.TimeInterval;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

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

        var now = System.currentTimeMillis();
        var last = flp.getLastLogin();
        TextComponent.Builder cb;
        if (now == last) {
            cb = ((TextComponent) ComponentColor.gold("{} is online now.", flp)).toBuilder();
        } else {
            cb = ((TextComponent) ComponentColor.gold(
                "{} was last seen {:aqua} ago.",
                flp,
                TimeInterval.formatTime(now - last, false))
            ).toBuilder();
        }

        // Test to see if this command isn't in #in-game essentially; make sure punishment info is private
        if (
            sender instanceof DiscordSender && ((DiscordSender) sender).getChannel().getIdLong() == FarLands.getFLConfig().discordBotConfig.channels.get(DiscordChannel.STAFF_COMMANDS)
            || sender instanceof Player && rank.isStaff()
            || sender instanceof ConsoleCommandSender
        ) {
            cb.append(ComponentUtils.format("\nMuted: {:aqua}", flp.isMuted()));

            if (!flp.punishments.isEmpty()) {
                List<Punishment> validPunishments = flp.punishments
                    .stream()
                    .filter(Punishment::isNotPardoned)
                    .toList();

                int hourIndex = 0;
                cb.append(Component.text("Punishments: "));
                for (Punishment p : flp.punishments) {
                    cb.append(ComponentUtils.format("\n- {:aqua}", p.asComponent(hourIndex)));
                    if(validPunishments.contains(p))
                        ++hourIndex;
                }
            }
            cb.append(ComponentUtils.format("\nLast IP: {:aqua}", flp.lastIP));
        }

        sender.sendMessage(cb.build());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args[0], sender) : Collections.emptyList();
    }
}
