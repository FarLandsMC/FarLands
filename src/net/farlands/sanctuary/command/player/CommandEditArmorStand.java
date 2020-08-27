package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.Region;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandEditArmorStand extends PlayerCommand {
    private final ItemStack editorBook;

    public CommandEditArmorStand() {
        super(Rank.SPONSOR, Category.UTILITY, "Open the vanilla tweaks armor stand editor book.", "/editarmorstand", "editarmorstand", "aditarmourstand");
        this.editorBook = FarLands.getFLConfig().armorStandBook.getStack();
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(sender.getLocation());
        if (flags != null && !flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(sender, TrustLevel.BUILD, flags)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to edit armor stands here.");
            return true;
        }

        sender.openBook(editorBook);
        return true;
    }
}
