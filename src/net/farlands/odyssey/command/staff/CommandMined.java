package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.command.PlayerCommand;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.kicas.rp.util.TextUtils.sendFormatted;

public class CommandMined extends PlayerCommand {
    public CommandMined() {
        super(Rank.BUILDER, "Show what a player has mined within a 100 block radius.", "/mined <player> [rollback|preview]", "mined");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 0)
            return false;
        if (args.length == 1 || "preview".equals(args[1])) {
            sender.chat("/co rollback u:" + args[0] +
                    " r:50 b:glowing_redstone_ore,redstone_ore,diamond_ore,iron_ore,emerald_ore,gold_ore,lapis_ore,coal_ore t:14d #preview");
            sendFormatted(sender, "&(green)Type $(hovercmd,/co cancel,{&(gray)Cancel Preview},&(aqua)/co cancel) when done.");
        } else if ("rollback".equals(args[1]))
            sender.chat("/co rollback u:" + args[0] + " r:50 t:14d");
        else
            return false;
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : (args.length == 2 ?
                Arrays.asList("rollback", "preview") : Collections.emptyList());
    }
}
