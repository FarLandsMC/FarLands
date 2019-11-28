package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.FLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.gui.GuiEvidenceLocker;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommandEvidenceLocker extends PlayerCommand {
    public CommandEvidenceLocker() {
        super(Rank.JR_BUILDER, "Open a player's evidence locker.", "/evidencelocker <player>", "evidencelocker", "el");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(Player sender, String[] args) {
        if(args.length == 0)
            return false;
        FLPlayer flp = getFLPlayer(args[0]);
        if(flp == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        if(flp.getPunishments().isEmpty()) {
            sender.sendMessage(ChatColor.RED + "This player has no punishments, thus no evidence locker slots.");
            return true;
        }
        List<FLPlayer> editList = (List<FLPlayer>)FarLands.getDataHandler().getRADH().retrieveAndStoreIfAbsent(new CopyOnWriteArrayList<>(), "evidencelocker", "editing");
        if(editList.contains(flp)) {
            sender.sendMessage(ChatColor.RED + "This player\'s evidence locker is already being edited. You must wait for the other editor to finish.");
            return true;
        }
        editList.add(flp);
        (new GuiEvidenceLocker(flp)).openGui(sender);
        return true;
    }
}
