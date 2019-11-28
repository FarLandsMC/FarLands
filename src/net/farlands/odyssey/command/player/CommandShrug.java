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

public class CommandShrug extends PlayerCommand {
    private static final String SHRUG = "\u00AF\\_(\u30C4)_/\u00AF";

    public CommandShrug() {
        super(Rank.INITIATE, "Append " + SHRUG + " to the end of your message.", "/shrug [action]", "shrug");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        Chat.chat(FarLands.getPDH().getFLPlayer(sender), sender,
                args.length == 0 ? SHRUG : String.join(" ", args).trim() + " " + SHRUG);
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
