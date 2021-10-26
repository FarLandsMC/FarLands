package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.mechanic.Chat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.kicas.rp.util.TextUtils.sendFormatted;

public class CommandPatchnotes extends PlayerCommand {
    public CommandPatchnotes() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "View the most recent patchnotes.", "/patchnotes", "patchnotes");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        try {
            sender.sendMessage(
                ChatColor.GOLD + "Showing notes for patch " + ChatColor.AQUA + "#" +
                    FarLands.getDataHandler().getCurrentPatch() +
                    ":\n" + ChatColor.GRAY +
                    Chat.colorize(
                        new String(
                            FarLands.getDataHandler().getResource("patchnotes.txt"),
                            StandardCharsets.UTF_8
                        )
                    )

            );

            FarLands.getDataHandler().getOfflineFLPlayer(sender).viewedPatchnotes = true;
        } catch (IOException ex) {
            sendFormatted(sender, "&(red)Failed to retrieve patch notes. Please report this error to a staff member.");
        }

        return true;
    }
}
