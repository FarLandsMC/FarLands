package net.farlands.odyssey.command.player;

import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandGivePet extends PlayerCommand {
    public CommandGivePet() {
        super(Rank.INITIATE, "Transfer one of your pets to another player.", "/givepet <player|cancel>", "givepet");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length <= 0)
            return false;

        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);

        // Cancel the transfer
        if (args[0].equalsIgnoreCase("cancel")) {
            session.givePetRecipient.discard();
            TextUtils.sendFormatted(sender, "&(gold)Cancelled pet transfer mode.");
            return true;
        }

        // Get the recipient
        Player player = getPlayer(args[0], sender);
        if (player == null) {
            TextUtils.sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        // Make sure they're not transferring to themself
        if (sender.getUniqueId().equals(player.getUniqueId())) {
            TextUtils.sendFormatted(sender, "&(red)You already own your own pets.");
            return true;
        }

        session.givePetRecipient.setValue(player, 1200L, () -> sender.sendMessage("Cancelled pet transfer mode"));
        TextUtils.sendFormatted(sender, "&(gold)Are you sure you want to give your pet to %0? " +
                "Click the pet you wish to transfer to confirm, or type " +
                "$(hovercmd,/givepet cancel,{&(gray)Click to Run},&(aqua)/givepet cancel) to cancel.", player.getName());

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers( args[0], sender) : Collections.emptyList();
    }
}
