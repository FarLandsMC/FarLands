package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.Chat;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CommandNick extends PlayerCommand {
    public CommandNick() {
        super(Rank.ADEPT, Category.COSMETIC, "Set your nickname or remove your current nickname.", "/nick|/nonick [name]",
                true, "nick", "nonick");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        // Don't allow empty nicknames
        if ("nick".equals(args[0]) && args.length == 1)
            return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        // Set the nickname
        if ("nick".equals(args[0])) {
            // Get rid of colors for length checking
            String rawNick = Chat.removeColorCodes(args[1]);
            // Prevent whitespace and profanity
            if (args[1].isEmpty() || args[1].matches("\\s+") || Chat.getMessageFilter().isProfane(rawNick)) {
                sendFormatted(sender, "&(red)You cannot set your nickname to this.");
                return true;
            }

            // Check length
            int rawLen = rawNick.length();
            if (rawLen > 16) {
                sendFormatted(sender, "&(red)That username is too long.");
                return true;
            } else if (rawLen < 3) {
                sendFormatted(sender, "&(red)Your nickname must be at least three characters long.");
                return true;
            }

            // Make sure there are three word characters in a row
            if (!rawNick.matches("(.+)?(\\w\\w\\w)(.+)?")) {
                sendFormatted(sender, "&(red)Your nickname must have at least three word characters in a row in it.");
                return true;
            }

            // Count the number of non-ascii characters for a percentage calculation
            double nonAscii = 0.0;
            for (char c : rawNick.toCharArray()) {
                if (33 > c || c > '~') {
                    ++nonAscii;
                }
            }

            // Enforce 60% ASCII
            if (nonAscii / rawLen > 0.4) {
                sendFormatted(sender, "&(red)Your nickname must be at least 60% ASCII characters.");
                return true;
            }

            // Disallow duplicate names
            for ( // Ignore the sender, they can use their own name
                    OfflineFLPlayer flpl : FarLands.getDataHandler().getOfflineFLPlayers().stream()
                    .filter(flpl -> !flpl.uuid.equals(sender.getUniqueId())).collect(Collectors.toList())
            ) {
                if (rawNick.equalsIgnoreCase(flpl.username) || rawNick.equalsIgnoreCase(flpl.nickname)) {
                    sendFormatted(sender, "&(red)Another player already has this name.");
                    return true;
                }
            }

            flp.nickname = Chat.applyColorCodes(Rank.getRank(sender), args[1]);
            sendFormatted(sender, "&(green)Nickname set.");
        }
        // Remove nickname
        else {
            // Allow staff members to remove other people's nicknames
            if (args.length > 1 && Rank.getRank(sender).isStaff()) {
                flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);

                // Make sure the player exists
                if (flp == null) {
                    sendFormatted(sender, "&(red)Player not found.");
                    return true;
                }

                // Make sure the player actually has a nickname to remove
                if (flp.nickname == null || flp.nickname.isEmpty()) {
                    sendFormatted(sender, "&(red)This person has no nickname to remove.");
                    return true;
                }

            }
            // The sender removes their own nickname
            else {
                // Make sure the player actually has a nickname to remove
                if (flp.nickname == null || flp.nickname.isEmpty()) {
                    sendFormatted(sender, "&(red)You have no nickname to remove.");
                    return true;
                }
            }

            flp.nickname = null;
            sendFormatted(sender, "&(green)Removed nickname.");
        }

        // Update their player's display name
        flp.updateSessionIfOnline(false);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!(Rank.getRank(sender).isStaff() && "nonick".equalsIgnoreCase(alias)))
            return Collections.emptyList();
        switch (args.length) {
            case 0:
                return getOnlinePlayers("", sender);
            case 1:
                return getOnlinePlayers(args[0], sender);
            default:
                return Collections.emptyList();
        }
    }
}
