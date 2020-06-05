package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandCensor extends PlayerCommand {
    private final Map<UUID, Integer> ranCommandOnce;

    public CommandCensor() {
        super(Rank.INITIATE, "Toggle on or off chat censor.", "/censor", "censor");
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
                sendFormatted(sender, "&(gold)Censor disabled. You can re-enable it with " +
                        "$(hovercmd,/censor,{&(gray)Click to Run},&(aqua)/censor).");
            }
            // Prompt the sender to confirm by rerunning the command
            else {
                ranCommandOnce.put(sender.getUniqueId(), FarLands.getScheduler()
                        .scheduleSyncDelayedTask(() -> ranCommandOnce.remove(sender.getUniqueId()), 30L * 20L));
                sendFormatted(sender, "&(red)Are you sure you want to disable the chat censor? Confirm with " +
                        "$(hovercmd,/censor,{&(gray)Click to Run},&(dark_red)/censor).");
            }
        }
        // Enable the censor
        else {
            flp.censoring = true;
            sendFormatted(sender, "&(gold)Chat censor enabled.");
        }

        return true;
    }
}
