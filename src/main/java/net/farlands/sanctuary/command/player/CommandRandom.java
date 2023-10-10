package net.farlands.sanctuary.command.player;

import com.kicas.rp.command.TabCompleterBase;
import com.kicasmads.cs.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.Range;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandRandom extends Command {

    public CommandRandom() {
        super(
            CommandData.simple(
                    "random",
                    "Generate a random number and broadcast it to other players on the server",
                    "/random <roll|value> <range>"
                )
                .category(Category.CHAT)
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return false;
        }

        SubCommand subcommand = Utils.valueOfFormattedName(args[0], SubCommand.class);
        if (subcommand == null) {
            return false;
        }
        Range<Integer> range;
        try {
            range = Range.from(args[1]);
        } catch (IllegalArgumentException ex) {
            error(sender, ex.getMessage());
            return false;
        }

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        int low = range.low() == null ? 0 : range.low();
        int high = range.high() == null ? Integer.MAX_VALUE : range.high();

        if (high < low) {
            return error(sender, "Invalid Range: high must be greater than low.");
        }

        int result = FLUtils.randomInt(low, high);

        switch(subcommand) {
            case ROLL -> {
                TranslatableComponent comp = Component.translatable("commands.random.roll")
                    .args(
                        flp,
                        Component.text(result),
                        Component.text(low),
                        Component.text(high)
                    );
                Logging.broadcastIngame(session -> !session.handle.getIgnoreStatus(sender).includesChat(), comp, true);
            }
            case VALUE -> {
                TranslatableComponent comp = Component.translatable("commands.random.sample.success").args(Component.text(result));
                sender.sendMessage(comp);
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return switch (args.length) {
            case 1 -> TabCompleterBase.filterStartingWith(args[0], SubCommand.FORMATTED);
            case 2 -> {
                String arg = args[1];
                if (arg.length() == 0 || arg.contains("..")) {
                    yield List.of(arg);
                }
                if (arg.endsWith(".")) {
                    yield List.of(arg + ".");
                }
                yield List.of(arg + "..");
            }
            default -> Collections.emptyList();
        };
    }

    public enum SubCommand {
        ROLL,
        VALUE,
        ;

        public static List<String> FORMATTED = Arrays.stream(SubCommand.values()).map(Utils::formattedName).toList();
    }
}
