package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

public class CommandMe extends PlayerCommand {
    public CommandMe() {
        super(Rank.INITIATE, Category.CHAT, "Broadcast an action.", "/me <action>", "me", "emote", "action");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length <= 0)
            return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        flp.chat(" * " + PlainTextComponentSerializer.plainText().serialize(flp.getDisplayName()) + " ", String.join(" ", args));
        return true;
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
