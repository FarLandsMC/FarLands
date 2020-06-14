package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.Utils;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.DataHandler;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandMoveSchems extends Command {
    public CommandMoveSchems() {
        super(Rank.BUILDER, "Move one or more schematic files between the main server and the dev server.",
                "/moveschems from <main|dev> to <main|dev> <schematic...>", "moveschems");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Make sure the config has been setup correctly
        if (Arrays.stream(DataHandler.Server.VALUES).anyMatch(server -> !FarLands.getFLConfig().serverRoots.containsKey(server))) {
            sendFormatted(sender, "&(red)The config has not been setup to support this command, " +
                    "please contact a developer about this.");
            return true;
        }

        // Enforce the string constants
        if (args.length < 4 || !("from".equalsIgnoreCase(args[0]) && "to".equalsIgnoreCase(args[2])))
            return false;

        // Check to make sure at least one schematic is specified
        if (args.length == 4) {
            sendFormatted(sender, "&(red)Please specify one or more schematic files to move.");
            return true;
        }

        // Parse the "from" server and get the directory
        DataHandler.Server fromServer = Utils.valueOfFormattedName(args[1], DataHandler.Server.class);
        if (fromServer == null) {
            sendFormatted(sender, "&(red)Invalid source server: %0", args[1]);
            return true;
        }
        String fromDirectory = FarLands.getFLConfig().serverRoots.get(fromServer);

        // Parse the "to" server and get the directory
        DataHandler.Server toServer = Utils.valueOfFormattedName(args[3], DataHandler.Server.class);
        if (toServer == null) {
            sendFormatted(sender, "&(red)Invalid destination server: %0", args[3]);
            return true;
        }
        String toDirectory = FarLands.getFLConfig().serverRoots.get(toServer);

        // Ensure the servers aren't the same
        if (fromServer == toServer) {
            sendFormatted(sender, "&(red)The two servers specified must be different.");
            return true;
        }

        // Perform the copy
        for (int i = 4;i < args.length; i++) {
            Path dest = Paths.get(toDirectory, "plugins", "WorldEdit", "schematics", args[i]);

            try {
                Files.deleteIfExists(dest);
                Files.copy(Paths.get(fromDirectory, "plugins", "WorldEdit", "schematics", args[i]), dest);
            } catch (IOException ex) {
                sendFormatted(sender, "&(red)Failed to copy schematics.");
                return true;
            }
        }

        sendFormatted(sender, "&(green)Successfully copied schematics.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 1:
                return Collections.singletonList("from");

            case 2:
                return TabCompleterBase.filterStartingWith(args[1], Arrays.stream(DataHandler.Server.VALUES)
                        .map(Utils::formattedName));

            case 3:
                return Collections.singletonList("to");

            case 4:
                return TabCompleterBase.filterStartingWith(args[3], Arrays.stream(DataHandler.Server.VALUES)
                        .filter(server -> server != Utils.valueOfFormattedName(args[1], DataHandler.Server.class))
                        .map(Utils::formattedName));

            // List the schematics in the source server
            default: {
                DataHandler.Server fromServer = Utils.valueOfFormattedName(args[1], DataHandler.Server.class);
                if (fromServer == null)
                    return Collections.emptyList();
                String fromDirectory = FarLands.getFLConfig().serverRoots.get(fromServer);

                try {
                    return TabCompleterBase.filterStartingWith(
                            args[args.length - 1],
                            Files.list(Paths.get(fromDirectory, "plugins", "WorldEdit", "schematics"))
                                    .map(path -> path.toFile().getName())
                    );
                } catch (IOException ex) {
                    return Collections.emptyList();
                }
            }
        }
    }
}
