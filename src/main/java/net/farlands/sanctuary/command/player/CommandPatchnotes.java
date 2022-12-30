package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
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
                ComponentColor.gray("").append(
                    ComponentColor.gold("Showing notes for patch ")
                        .append(ComponentColor.aqua("#%s", FarLands.getDataHandler().getCurrentPatch()))
                        .append(Component.text(":\n"))
                ).append(
                    ComponentUtils.parse(
                        new String(
                            FarLands.getDataHandler().getResource("patchnotes.txt"),
                            StandardCharsets.UTF_8
                        )
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
