package net.farlands.odyssey.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.PluginData;
import net.farlands.odyssey.data.Rank;

import org.bukkit.entity.Player;

public class CommandSetWarp extends PlayerCommand {
    public CommandSetWarp() {
        super(Rank.BUILDER, "Set a public warp at your current location.", "/setwarp <name>", "setwarp");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if(args.length == 0)
            return false;
        PluginData pd = FarLands.getDataHandler().getPluginData();
        if(pd.getWarpNames().stream().anyMatch(args[0]::equalsIgnoreCase)) {
            sendFormatted(sender, "&(red)A warp with that name already exists.");
            return true;
        }
        pd.addWarp(args[0], sender.getLocation());
        sendFormatted(sender, "&(green)Warp set.");
        return true;
    }
}
