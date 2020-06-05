package net.farlands.odyssey.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.PlayerDeath;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandRestoreDeath extends Command {
    
    public CommandRestoreDeath() {
        super(Rank.BUILDER, "Restore the players previous deaths.", "/restoredeath <player> [death]", "restoredeath");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = args.length < 1 ? null : getPlayer(args[0], sender);
        if (player == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }
        List<PlayerDeath> deaths = FarLands.getDataHandler().getDeaths(player.getUniqueId());
        if(deaths.isEmpty()) {
            sendFormatted(sender, "&(red)This player has no deaths on record.");
            return true;
        }
        // newest deaths at tail of list
        int death;
        if (args.length < 2) {
            death = deaths.size() - 1;
        } else {
            try {
                death = deaths.size() - Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sendFormatted(sender, "&(red)Death number must be a number between 1 and " + deaths.size());
                return true;
            }
            if (deaths.size() - 1 < death || death < 0) {
                sendFormatted(sender, "&(red)Death number must be between 1 and " + deaths.size());
                return true;
            }
        }
        player.setLevel(deaths.get(death).getXpLevels());
        player.setExp(deaths.get(death).getXpPoints());
        List<ItemStack> deathInv = deaths.get(death).getInventory();
        for (int i = deathInv.size(); --i >= 0;)
            player.getInventory().setItem(i, deathInv.get(i));
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) :
                args.length <= 2 ? Arrays.asList("1", "2", "3") : Collections.emptyList();
    }
}
