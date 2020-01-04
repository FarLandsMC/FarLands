package net.farlands.odyssey.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;

import org.bukkit.ChatColor;
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
    public boolean execute(Player player, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
        if (flp.censoring) {
            if (ranCommandOnce.containsKey(player.getUniqueId())) {
                flp.censoring = false;
                FarLands.getScheduler().completeTask(ranCommandOnce.get(player.getUniqueId()));
                sendFormatted(player, "&(gold)Censor disabled. You can re-enable it with " +
                        "$(hovercmd,/censor,{&(gray)Click to Run},&(aqua)/censor).");
            } else {
                ranCommandOnce.put(player.getUniqueId(), FarLands.getScheduler()
                        .scheduleSyncDelayedTask(() -> ranCommandOnce.remove(player.getUniqueId()), 30L * 20L));
                sendFormatted(player, "&(red)Are you sure you want to disable the chat censor? Confirm with " +
                        "$(hovercmd,/censor,{&(gray)Click to Run},&(dark_red)/censor).");
            }
        } else {
            flp.censoring = true;
            player.sendMessage(ChatColor.GOLD + "Chat censor enabled.");
        }
        return true;
    }
}
