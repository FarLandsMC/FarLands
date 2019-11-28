package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class CommandMe extends PlayerCommand {
    public CommandMe() {
        super(Rank.INITIATE, "Broadcast an action.", "/me <action>", "me", "emote", "action");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length <= 0)
            return false;
        Chat.chat(FarLands.getPDH().getFLPlayer(sender), sender, String.join(" ", args),
                " * " + Chat.removeColorCodes(FarLands.getPDH().getFLPlayer(sender).getDisplayName()) + ' ');
        return true;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if(!(sender instanceof BlockCommandSender || sender instanceof ConsoleCommandSender ||
                !FarLands.getPDH().getFLPlayer(sender).isMuted())) {
            sender.sendMessage(ChatColor.RED + "You cannot use this command while muted.");
            return false;
        }
        return super.canUse(sender);
    }
}
