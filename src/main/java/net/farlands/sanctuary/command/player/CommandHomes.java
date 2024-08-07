package net.farlands.sanctuary.command.player;

import com.kicasmads.cs.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.Pagination;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.Home;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public class CommandHomes extends Command {

    public CommandHomes() {
        super(
            CommandData.simple("homes",
                               "List your homes.",
                               "/homes [page|sort] [sort-method]"
            ).category(Category.HOMES)
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] argsArr) {

        if ((sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender) && argsArr.length == 0) {
            return error(sender, "You must be in-game to use this command.");
        }

        List<String> args = new ArrayList<>(Arrays.asList(argsArr)); // Lists are easier to work with :P

        if (!args.isEmpty() && args.getFirst().equalsIgnoreCase("sort")) {
            updateSort(sender, args);
            return true;
        }

        int page = 1;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        boolean self = true;

        if (!args.isEmpty() && args.getFirst().matches("(?i)^.+[a-z].+$") && flp.rank.isStaff()) {
            flp = FarLands.getDataHandler().getOfflineFLPlayer(args.removeFirst());
            self = false;
            if (flp == null) {
                return error(sender, "Player not found.");
            }
        }

        if(flp.homes.isEmpty()) {
            return error(sender, "{} {} no homes.", self ? "You" : flp, self ? "have" : " has");
        }

        if (!args.isEmpty()) {
            if (!args.getFirst().matches("\\d+")) {
                error(sender, "Invalid page number.");
                return true;
            }
            page = Integer.parseInt(args.removeFirst());
        }

        boolean finalSelf = self;
        String username = flp.username;
        List<Component> homes = flp.homes
            .stream()
            .sorted(self
                        ? flp.homesSort.getComparator(flp.getOnlinePlayer())
                        : FarLands.getDataHandler().getOfflineFLPlayer(sender).homesSort.getComparator(sender instanceof Player plr ? plr : null)
            )
            .map(h -> ComponentUtils.command(
                "/home " + h.getName() + " " + (finalSelf ? "" : username),
                h.asComponent(true, false)
            ))
            .toList();

        Component header = ComponentColor.gold(
            "{} {} Home{1::s}",
            self ? "Your" : flp.username + "'s",
            homes.size()
        );

        String pageCommand = "/homes " + (self ? "" : flp.username);

        Pagination pagination = new Pagination(header, pageCommand);
        pagination.addLines(homes);

        try {
            pagination.sendPage(page, sender);
        } catch (Pagination.InvalidPageException ex) {
            return error(sender, "Invalid page number");
        }

        return true;
    }

    private void updateSort(CommandSender sender, List<String> args) {

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        if (args.size() == 1) {
            info(sender, "Currently sorting using: {:aqua}", flp.homesSort);
            return;
        }

        SortType type = Utils.valueOfFormattedName(args.get(1), SortType.class);
        if (type == null) {
            error(sender, "Invalid sort type. Options: {}", (Object) SortType.values());
            return;
        }
        flp.homesSort = type;
        success(sender, "Now sorting using: {:aqua}", flp.homesSort);
    }

    @Override
    protected void showUsage(CommandSender sender) {
        error(sender, "Usage: {}", Rank.getRank(sender).isStaff() ? "/homes [player] [page]" : getUsage());
    }

    @Override
    public @NotNull List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 1 -> {
                List<String> fill = new ArrayList<>();
                if (Rank.getRank(sender).isStaff()) {
                    fill.addAll(getOnlinePlayers(args[0], sender));
                }
                fill.add("sort");
                return fill;
            }
            case 2 -> {
                if (args[0].equalsIgnoreCase("sort")) {
                    return Utils.filterStartingWith(args[1], SortType.NAMES);
                }
            }
        }
        return Collections.emptyList();
    }

    public enum SortType {
        ALPHABET(p -> (a, b) -> a.getName().compareToIgnoreCase(b.getName())),
        DISTANCE(p -> {
            Location pLoc = p == null ? FarLands.getDataHandler().getPluginData().spawn.asLocation() : p.getLocation();
            return Comparator.comparingDouble(h -> {
                Location hLoc = h.asLocation().clone();
                hLoc.setWorld(pLoc.getWorld()); // Prevent errors
                return hLoc.distanceSquared(pLoc);
            });
        }),
        WORLD(p -> Comparator.comparingInt(
            h ->
                switch (h.asLocation().getWorld().getName()) { // sort as overworld, nether, end, party
                    case "world" -> 1;
                    case "world_nether" -> 2;
                    case "world_the_end" -> 3;
                    default -> 4;
                }
        ));

        public static final List<String> NAMES = Arrays.stream(values()).map(Utils::formattedName).toList();

        public final Function<Player, Comparator<Home>> comparatorGenerator;

        SortType(Function<Player, Comparator<Home>> comparator) {
            this.comparatorGenerator = comparator;
        }

        public Comparator<Home> getComparator(Player player) {
            return this.comparatorGenerator.apply(player);
        }


    }
}