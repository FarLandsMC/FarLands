package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;

import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandCensor extends PlayerCommand {
    private final Map<UUID, Integer> ranCommandOnce;

    public CommandCensor() {
        super(Rank.INITIATE, Category.CHAT, "Toggle chat censor (profanity filter).", "/censor", "censor");
        this.ranCommandOnce = new HashMap<>();
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        if (flp.censoring) {
            // They ran it once and confirmed that they want to disable the censor
            if (ranCommandOnce.containsKey(sender.getUniqueId())) {
                flp.censoring = false;
                FarLands.getScheduler().completeTask(ranCommandOnce.get(sender.getUniqueId()));
                Component component = Component.text()
                    .content("Censor disabled.  You can re-enable it with ")
                    .color(NamedTextColor.GOLD)
                    .append(ComponentUtils.command("/censor"))
                    .build();
                sender.sendMessage(component);
            }
            // Prompt the sender to confirm by rerunning the command
            else {
                ranCommandOnce.put(sender.getUniqueId(), FarLands.getScheduler()
                        .scheduleSyncDelayedTask(() -> ranCommandOnce.remove(sender.getUniqueId()), 30L * 20L));
                Component component = Component.text()
                    .content("Are you sure you want to disable the chat censor? Confirm with ")
                    .color(NamedTextColor.RED)
                    .append(ComponentUtils.command("/censor", NamedTextColor.DARK_RED))
                    .build();
                sender.sendMessage(component);
            }
        }
        // Enable the censor
        else {
            flp.censoring = true;
            sender.sendMessage(ComponentColor.gold("Chat censor enabled."));
        }

        return true;
    }
}
