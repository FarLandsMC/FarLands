package net.farlands.odyssey.command.player;

import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;

import org.bukkit.entity.Player;

public class CommandNick extends PlayerCommand {
    public CommandNick() {
        super(Rank.ADEPT, "Set your nickname. Use /nonick to remove your nickname.", "/nick <name>", true, "nick", "nonick");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        // Don't allow empty nicknames
        if ("nick".equals(args[0]) && args.length == 1)
            return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        // Set the nickname
        if ("nick".equals(args[0])) {
            // Prevent whitespace and profanity
            if (args[1].isEmpty() || args[1].matches("\\s+") || Chat.getMessageFilter().isProfane(Chat.removeColorCodes(args[1]))) {
                TextUtils.sendFormatted(sender, "&(red)You cannot set your nickname to this.");
                return true;
            }

            // Get rid of colors for length checking
            String rawNick = Chat.removeColorCodes(args[1]);
            // Check length
            int rawLen = rawNick.length();
            if (rawLen > 16) {
                TextUtils.sendFormatted(sender, "&(red)That username is too long.");
                return true;
            } else if (rawLen < 3) {
                TextUtils.sendFormatted(sender, "&(red)Your nickname must be at least three characters long.");
                return true;
            }

            // Make sure there are three word characters in a row
            if (!rawNick.matches("(.+)?(\\w\\w\\w)(.+)?")) {
                TextUtils.sendFormatted(sender, "&(red)Your nickname must have at least three word characters in a row in it.");
                return true;
            }

            // Count the number of non-ascii characters for a percentage calculation
            double nonAscii = 0.0;
            for (char c : rawNick.toCharArray()) {
                if (c < 33 || c > '~') {
                    ++nonAscii;
                }
            }

            // Enforce 60% ASCII
            if (nonAscii / rawLen > 0.4) {
                TextUtils.sendFormatted(sender, "&(red)Your nickname must be at least 60\\% ASCII characters.");
                return true;
            }

            flp.nickname = Chat.applyColorCodes(Rank.getRank(sender), args[1]);
            TextUtils.sendFormatted(sender, "&(green)Nickname set.");
        }
        // Remove nickname
        else {
            // Allow staff members to remove other people's nicknames
            if (args.length > 1 && Rank.getRank(sender).isStaff()) {
                flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);

                // Make sure the player exists
                if (flp == null) {
                    TextUtils.sendFormatted(sender, "&(red)Player not found.");
                    return true;
                }

                // Make sure the player actually has a nickname to remove
                if (flp.nickname == null || flp.nickname.isEmpty()) {
                    TextUtils.sendFormatted(sender, "&(red)This person has no nickname to remove.");
                    return true;
                }

                flp.nickname = null;
            }
            // The sender removes their own nickname
            else {
                // Make sure the player actually has a nickname to remove
                if (flp.nickname == null || flp.nickname.isEmpty()) {
                    TextUtils.sendFormatted(sender, "&(red)You have no nickname to remove.");
                    return true;
                }

                flp.nickname = null;
            }

            TextUtils.sendFormatted(sender, "&(green)Removed nickname.");
        }

        // Update their player's display name
        flp.updateSessionIfOnline(false);
        return true;
    }
}
