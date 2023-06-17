package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.mechanic.PocketMechanics;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandPocketReset extends Command {

    private final Set<OfflineFLPlayer> needsConfirm;

    public CommandPocketReset() {
        super(Rank.ADMIN, Category.STAFF, "Remove all set homes from the pocket world and set logout " +
                                          "locations from that world to spawn.", "/resetpocket [confirm]", "resetpocket"); // Not /pocketreset to prevent accidental tab-complete when doing /pocket
        needsConfirm = new HashSet<>();
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (
            this.needsConfirm.contains(FarLands.getDataHandler().getOfflineFLPlayer(sender)) &&
            args.length > 0 &&
            args[0].equalsIgnoreCase("confirm")
        ) {
            this.needsConfirm.remove(FarLands.getDataHandler().getOfflineFLPlayer(sender));
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
                PocketMechanics.resetPocket(sender);
            });
        } else {
            sender.sendMessage(
                ComponentColor.red("Are you sure that you want to run this command? It will delete all homes in the pocket world and teleport all players in the pocket world to spawn.  It will also ")
                    .append(Component.text("delete", Style.style(NamedTextColor.RED, TextDecoration.BOLD)))
                    .append(ComponentColor.red(" the pocket world and generate a new one. If you are certain that you want to do this, run "))
                    .append(ComponentColor.darkRed("/resetpocket confirm"))
                    .append(ComponentColor.red("."))
            );
            this.needsConfirm.add(FarLands.getDataHandler().getOfflineFLPlayer(sender));
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                this.needsConfirm.remove(FarLands.getDataHandler().getOfflineFLPlayer(sender));
            }, 30 * 20);
        }


        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (args.length != 0) return Collections.emptyList();
        return Collections.singletonList("confirm");
    }
}