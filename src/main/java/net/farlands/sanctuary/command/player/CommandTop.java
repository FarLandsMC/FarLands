package net.farlands.sanctuary.command.player;

import com.kicas.rp.util.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.Pagination;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.TimeInterval;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.kicasmads.cs.Utils.filterStartingWith;

public class CommandTop extends Command {

    public CommandTop() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "View the people with the most votes or play time.",
              "/top <votes|playtime|donors|deaths> [page|month|all]", "top");
    }

    @Override
    public boolean execute(CommandSender sender, String[] argsArr) {
        if (argsArr.length == 0) {
            return false;
        }

        List<String> args = new ArrayList<>(List.of(argsArr));


        TopCategory category = Utils.valueOfFormattedName(args.remove(0), TopCategory.class);
        if (category == null) {
            return false;
        }

        // `args` are now [subcommand, page], [subcommand] or [page]
        args = new ArrayList<>(args.subList(0, Math.min(2, args.size())));

        String subcommand = args.isEmpty() || args.get(0).matches("\\d+") ? null : args.remove(0);
        int page = args.isEmpty() ? 1 : parseNumber(args.remove(0), Integer::parseInt, -1);

        List<OfflineFLPlayer> flps = FarLands.getDataHandler().getOfflineFLPlayers();
        Function<OfflineFLPlayer, Component> toValueComponent = null;
        Predicate<OfflineFLPlayer> filter = null;
        Comparator<OfflineFLPlayer> comparator = null;
        Component header = Component.empty();
        OfflineFLPlayer senderFlp = FarLands.getDataHandler().getOfflineFLPlayer(sender.getName());

        switch (category) {
            case VOTES -> {
                Comparator<OfflineFLPlayer> compareMonth = Comparator.comparingInt(f -> f.monthVotes);
                Comparator<OfflineFLPlayer> compareTotal = Comparator.comparingInt(f -> f.totalVotes);
                Comparator<OfflineFLPlayer> compareSeason = Comparator.comparingInt(f -> f.totalSeasonVotes);
                comparator = compareMonth.thenComparing(compareTotal).thenComparing(compareSeason);
                filter = f -> f.totalVotes > 0;
                toValueComponent = f -> ComponentColor.gold("%d votes this month, %d votes total", f.monthVotes, f.totalVotes);
                header = ComponentColor.gold("Top Voters (This Month)");

                if (subcommand != null && subcommand.equalsIgnoreCase("all")) {
                    comparator = compareTotal.thenComparing(compareSeason);
                    toValueComponent = f -> ComponentColor.gold("%d vote%s", f.totalVotes, f.totalVotes == 1 ? "" : "s");
                    header = ComponentColor.gold("Top Voters (All Time)");
                }
            }
            case PLAYTIME -> {
                comparator = Comparator.comparingInt(f -> f.secondsPlayed);
                filter = f -> f.secondsPlayed > 0;
                toValueComponent = f -> TimeInterval.formatTimeComponent(1000L * f.secondsPlayed, true);
                header = ComponentColor.gold("Top Playtime");
            }
            case DONORS -> {
                comparator = Comparator.comparingDouble(f -> f.amountDonated);
                filter = f -> f.amountDonated > 0;
                header = ComponentColor.gold("Top Donors");
            }
            case DEATHS -> {
                comparator = Comparator.comparingInt(f -> f.deaths);
                filter = f -> f.deaths > 0;
                toValueComponent = f -> ComponentColor.gold("%d death%s", f.deaths, f.deaths == 1 ? "" : "s");
                header = ComponentColor.gold("Top Deaths");

            }
            default -> error(sender, "Invalid category. Options: ", String.join(", ", TopCategory.NAMES));
        }
        Pagination pagination = new Pagination(header, "/top " + category + " " + (subcommand == null ? "" : subcommand));
        flps = flps
            .stream()
            .filter(filter)
            .sorted(Collections.reverseOrder(comparator))
            .toList();
        pagination.maxChatWidth(70); // Seems to be a decent length

        List<Component> lines = new ArrayList<>();
        int index = -1;
        for (int i = 0; i < flps.size(); i++) {
            OfflineFLPlayer flp = flps.get(i);
            TextComponent.Builder bldr = Component.text().color(NamedTextColor.GOLD);

            if (senderFlp.uuid.equals(flp.uuid)) {
                bldr.append(ComponentColor.green(i + 1 + ": "));
                index = i + 1;
            } else {
                bldr.append(ComponentColor.gold(i + 1 + ": "));
            }

            bldr.append(ComponentColor.aqua(flp.username));
            if (toValueComponent != null) {
                bldr.append(ComponentColor.gold(" - "))
                    .append(toValueComponent.apply(flp));
            }
            lines.add(bldr.build());
        }
        pagination.addLines(lines);
        if (page > pagination.numPages() || page < 1) {
            error(sender, "Invalid page number, must be an integer between 1 and %d.", pagination.numPages());
            return true;
        }
        if (sender instanceof DiscordSender discordSender) {
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle(MarkdownProcessor.fromMinecraft(header))
                .addField(
                    " ~~---~~ ",
                    pagination
                        .render(page)
                        .stream()
                        .map(MarkdownProcessor::fromMinecraft)
                        .filter(s -> !s.startsWith("«")) // Remove the header
                        .collect(Collectors.joining("\n")),
                    false
                )
                .setColor(NamedTextColor.GOLD.value());
            if (index != -1) {
                embed.addField(
                    String.format("You are #%d", index),
                    toValueComponent == null ? " " : MarkdownProcessor.fromMinecraft(toValueComponent.apply(senderFlp)),
                    false
                );
            }
            discordSender.sendMessageEmbeds(embed.build());
        } else {
            pagination.sendPage(page, sender);
            if (index != -1) {
                TextComponent.Builder bldr = Component.text().color(NamedTextColor.GOLD).content("You are ")
                    .append(ComponentColor.aqua("#%d", index));
                if (toValueComponent != null) {
                    bldr.append(ComponentColor.gold(" - "))
                        .append(toValueComponent.apply(senderFlp));
                }
                sender.sendMessage(bldr.build());
            }
        }


        return true;
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        SlashCommandData command = this.defaultCommand(false);

        for (TopCategory value : TopCategory.values()) {
            if (value.subCategories.isEmpty()) {
                command.addSubcommands(
                    new SubcommandData(
                        Utils.formattedName(value),
                        FLUtils.capitalize(value.name())
                    )
                );
            } else {
                SubcommandGroupData group = new SubcommandGroupData(
                    Utils.formattedName(value),
                    FLUtils.capitalize(value.name())
                );
                for (String s : value.subCategories) {
                    group.addSubcommands(new SubcommandData(s, FLUtils.capitalize(s)));
                }
                command.addSubcommandGroups(group);
            }
        }
        return command;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 1:
                return filterStartingWith(args[0], TopCategory.NAMES);
            case 2: {
                TopCategory cat = Utils.valueOfFormattedName(args[0], TopCategory.class);
                if (cat == null) return Collections.emptyList();
                return filterStartingWith(args[1], Utils.valueOfFormattedName(args[0], TopCategory.class).subCategories);
            }
            default:
                return Collections.emptyList();
        }
    }

    private enum TopCategory {
        VOTES("month", "all"),
        PLAYTIME,
        DONORS,
        DEATHS,
        ;

        public static final List<String> NAMES = Arrays.stream(values()).map(Utils::formattedName).toList();
        public static final TopCategory[] VALUES = values();

        public final List<String> subCategories;

        TopCategory(String... subCategories) {
            this.subCategories = List.of(subCategories);
        }
    }
}
