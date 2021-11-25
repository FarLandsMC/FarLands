package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.chat.ChatHandler;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CommandShrug extends PlayerCommand {

    public CommandShrug() {
        super(Rank.INITIATE, Category.CHAT, "Append text emojis to the end of your message.",
                "/" + Arrays
                        .stream(TextEmote.values)
                        .map(emote -> emote.name().toLowerCase())
                        .collect(Collectors.joining("|")) + " [action]",
                true, "shrug",
                Arrays.stream(TextEmote.values)
                        .map(emote -> emote.name().toLowerCase())
                        .toArray(String[]::new));
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        String emote;
        try {
            emote = TextEmote.valueOf(args[0].toUpperCase()).getValue();
        } catch (IllegalArgumentException e) {
            return false;
        } // Invalid emote
        ChatHandler.chat(flp, args.length == 1 ?
                emote :
                joinArgsBeyond(0, " ", args).trim() + " " + emote);
        return true;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if (!(sender instanceof BlockCommandSender || sender instanceof ConsoleCommandSender ||
                !FarLands.getDataHandler().getOfflineFLPlayer(sender).isMuted())) {
            sender.sendMessage(ComponentColor.red("You cannot use this command while muted."));
            return false;
        }
        return super.canUse(sender);
    }

    public enum TextEmote {
        TABLEFLIP("(╯°□°）╯︵ ┻━┻"),
        UNFLIP("┬─┬ ノ( ゜-゜ノ)"),
        DAB("ㄥ(⸝ ، ⸍ )‾‾‾‾‾"),
        SHRUG("\u00AF\\_(\u30C4)_/\u00AF"); // ¯\_(ツ)_/¯

        public static TextEmote[] values = values();

        private final String value;

        TextEmote(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }
}
