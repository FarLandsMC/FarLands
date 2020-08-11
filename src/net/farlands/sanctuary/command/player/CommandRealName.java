package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;

import net.farlands.sanctuary.mechanic.Chat;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandRealName extends Command {
    public CommandRealName() {
        super(Rank.INITIATE, Category.PLAYER_SETTINGS_AND_INFO, "Get the real name of a player.", "/realname <nickname>", "realname", "rn");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0)
            return false;

        args[0] = args[0].toLowerCase();
        List<String> matches = new ArrayList<>();
        for (OfflineFLPlayer flp : FarLands.getDataHandler().getOfflineFLPlayers()) {
            String nickname = Chat.removeColorCodes(flp.nickname.toLowerCase());

            // Match ignoring case
            if (args[0].equals(nickname)) {
                sendFormatted(sender, "&(green)Matches: &(gold)%0", flp.username);
                return true;
            }
            // Match via containment (ignoring case)
            else if (nickname.contains(args[0]) || flp.username.toLowerCase().contains(args[0]))
                matches.add(flp.username);
        }

        sendFormatted(sender, "&(green)Matches: %0", matches.isEmpty() ? "&(red)None" : "&(gold)" + String.join(", ", matches));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender).stream()
                    .map(p -> FarLands.getDataHandler().getOfflineFLPlayer(p).nickname)
                    .collect(Collectors.toList())
                : Collections.emptyList();
    }
}
