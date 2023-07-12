package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class CommandEmote extends PlayerCommand {

    public CommandEmote() {
        super(
            CommandData.simple(
                    "shrug",
                    "Append text emotes to the end of your message.",
                    "/" + String.join("|", TextEmote.commands()) + " [action]"
                )
                .category(Category.CHAT)
                .aliases(true, TextEmote.commands())
        );
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        String emote;
        try {
            emote = TextEmote.valueOf(args[0].toUpperCase()).getValue();
        } catch (IllegalArgumentException e) { // Shouldn't happen ever, but just to be safe
            return false;
        } // Invalid emote
        flp.chat(args.length == 1 ? emote : joinArgsBeyond(0, " ", args).trim() + " " + emote);
        return true;
    }

    @Override
    public boolean canUse(CommandSender sender) {
        if (!(sender instanceof BlockCommandSender || sender instanceof ConsoleCommandSender ||
                !FarLands.getDataHandler().getOfflineFLPlayer(sender).isMuted())) {
            error(sender, "You cannot use this command while muted.");
            return false;
        }
        return super.canUse(sender);
    }

    public enum TextEmote {
        TABLEFLIP("(╯°□°）╯︵ ┻━┻"),
        UNFLIP("┬─┬ ノ( ゜-゜ノ)"),
        DAB("ㄥ(⸝ ، ⸍ )‾‾‾‾‾"),
        SHRUG("¯\\_(ツ)_/¯"), // ¯\_(ツ)_/¯
        TM("™", false), // :tm: -> ™
        SKULL("☠", false),
        ;

        public static TextEmote[] values = values();

        private final String value;
        private final boolean isCommand;

        TextEmote(String value) {
            this(value, true);
        }

        TextEmote(String value, boolean isCommand) {
            this.value = value;
            this.isCommand = isCommand;
        }

        public String getValue() {
            return this.value;
        }

        public boolean isCommand() {
            return this.isCommand;
        }

        public static String[] commands() {
            return Arrays.stream(values())
                .filter(TextEmote::isCommand)
                .map(Enum::name)
                .map(String::toLowerCase)
                .toArray(String[]::new);
        }
    }
}
