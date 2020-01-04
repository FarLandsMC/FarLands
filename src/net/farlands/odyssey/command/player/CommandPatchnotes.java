package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CommandPatchnotes extends Command {
    public CommandPatchnotes() {
        super(Rank.INITIATE, "View the current patchnotes.", "/patchnotes", "patchnotes");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        try {
            sender.sendMessage(ChatColor.GOLD + "Showing notes for patch " + ChatColor.AQUA + "#" + FarLands.getDataHandler().getCurrentPatch() +
                    ":\n" + Chat.applyColorCodes(new String(FarLands.getDataHandler().getResource("patchnotes.txt"), StandardCharsets.UTF_8)));
            FarLands.getDataHandler().getOfflineFLPlayer(sender).viewedPatchnotes = true;
        } catch (IOException ex) {
            sender.sendMessage(ChatColor.RED + "Failed to retrieve patch notes. Please report this error to a staff member.");
        }
        return true;
    }
}
