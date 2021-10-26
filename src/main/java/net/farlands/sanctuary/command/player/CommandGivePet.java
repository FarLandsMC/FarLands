package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandGivePet extends PlayerCommand {
    public CommandGivePet() {
        super(Rank.INITIATE, Category.MISCELLANEOUS, "Transfer one of your pets to another player, or cancel a transfer.",
                "/givepet <player|cancel>", "givepet");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length <= 0)
            return false;

        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);

        // Cancel the transfer
        if (args[0].equalsIgnoreCase("cancel")) {
            session.givePetRecipient.discard();
            sendFormatted(sender, "&(gold)Cancelled pet transfer mode.");
            return true;
        }

        // Get the recipient
        Player player = getPlayer(args[0], sender);
        if (player == null) {
            sendFormatted(sender, "&(red)Player not found.");
            return true;
        }

        // Make sure they're not transferring to themself
        if (sender.getUniqueId().equals(player.getUniqueId())) {
            sendFormatted(sender, "&(red)You already own your own pets.");
            return true;
        }

        session.givePetRecipient.setValue(player, 1200L, () -> sender.sendMessage("Cancelled pet transfer mode"));
        sendFormatted(sender, "&(gold)Are you sure you want to give your pet to %0? " +
                "Click the pet you wish to transfer to confirm, or type " +
                "$(hovercmd,/givepet cancel,{&(gray)Click to Run},&(aqua)/givepet cancel) to cancel.", player.getName());

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers( args[0], sender) : Collections.emptyList();
    }
}
