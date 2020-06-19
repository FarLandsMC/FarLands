package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.mechanic.Chat;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class CommandShrug extends PlayerCommand {
    private static final String SHRUG = "\u00AF\\_(\u30C4)_/\u00AF";
    private static final String TABLEFLIP = "(╯°□°）╯︵ ┻━┻";
    private static final String UNFLIP = "┬─┬ ノ( ゜-゜ノ)";

    public CommandShrug() {
        super(Rank.INITIATE, Category.CHAT, "Append text emojis to the end of your message.",
                "/shrug|tableflip|unflip [action]", true, "shrug", "tableflip", "unflip");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        switch (args[0]) {
            case "shrug":
                Chat.chat(flp, sender, args.length == 1 ? SHRUG : joinArgsBeyond(0, " ", args).trim() + " " + SHRUG);
                return true;
            case "tableflip":
                Chat.chat(flp, sender, args.length == 1 ? TABLEFLIP : joinArgsBeyond(0, " ", args).trim() + " " + TABLEFLIP);
                return true;
            case "unflip":
                Chat.chat(flp, sender, args.length == 1 ? UNFLIP : joinArgsBeyond(0, " ", args).trim() + " " + UNFLIP);
                return true;
        }
        return false;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if (!(sender instanceof BlockCommandSender || sender instanceof ConsoleCommandSender ||
                !FarLands.getDataHandler().getOfflineFLPlayer(sender).isMuted())) {
            sendFormatted(sender, "&(red)You cannot use this command while muted.");
            return false;
        }
        return super.canUse(sender);
    }
}
