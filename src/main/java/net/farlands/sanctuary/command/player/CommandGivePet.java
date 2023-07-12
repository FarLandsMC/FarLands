package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.util.ComponentUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandGivePet extends PlayerCommand {

    public CommandGivePet() {
        super(
            CommandData.simple(
                    "givepet",
                    "Transfer one of your pets to another player, or cancel a transfer.",
                    "/givepet <player|cancel>"
                )
                .category(Category.MISCELLANEOUS)
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length <= 0) {
            return false;
        }

        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);

        // Cancel the transfer
        if (args[0].equalsIgnoreCase("cancel")) {
            session.givePetRecipient.discard();
            return info(sender, "Cancelled pet transfer mode.");
        }

        // Get the recipient
        Player player = getPlayer(args[0], sender);
        if (player == null) {
            return error(sender, "Player not found.");
        }

        // Make sure they're not transferring to themselves
        if (sender.getUniqueId().equals(player.getUniqueId())) {
            return error(sender, "You already own your own pets.");
        }

        session.givePetRecipient.setValue(player, 1200L, () -> sender.sendMessage("Cancelled pet transfer mode"));

        return info(
            sender,
            "Are you sure you want to give your pet to {}? Click the pet you wish to transfer to confirm, or type {} to cancel.",
            ComponentUtils.command("/givepet cancel")
        );
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args[0], sender) : Collections.emptyList();
    }
}
