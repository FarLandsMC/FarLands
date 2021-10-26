package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandPetblock extends Command {
    public CommandPetblock() {
        super(Rank.SPONSOR, Category.COSMETIC, "Command to open the PetBlocks GUI",
                "/petblock", false, "petblock");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        Bukkit.dispatchCommand(sender, "petblock:petblock " + String.join(" ", args));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return Collections.emptyList();
    }
}
