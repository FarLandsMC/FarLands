package net.farlands.odyssey.command.player;

import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandRules extends Command {
    public CommandRules() {
        super(Rank.INITIATE, "Look at the server rules.", "/rules", "rules");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage(
            ChatColor.GOLD + "Server Rules:\n" +
            "1. No hacking, cheating, or exploits of any kind.\n" +
            "2. Respect players and staff at all times. This includes claims and staff decisions.\n" +
            "3. No slurs, adult-only or revolting content.\n" +
            "4. No spam or advertisement of other servers.\n" +
            "5. No lag inducing mechanisms or actions (this includes chunk loaders and bypassing the AFK system).\n" +
            "6. Griefing, trapping, player killing, or bypassing the PVP toggle is not allowed.\n" +
            "7. Ban evasion with an alternate account will lead to a permanent ban for both accounts.\n" +
            "8. The top of the nether is off limits (you will die trying).\n" +
            "9. Keep inventory is off, travel with care.\n" +
            "10. All rules apply to discord where applicable."
        );
        return true;
    }
}
