package net.farlands.sanctuary.command.player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandDonate extends Command {

    public static final Map<Rank, List<Component>> RANKS_INFO = new HashMap<>();

    public static final String BRIEF_DESCRIPTION = "Donations are processed through Tebex. They are not required but truly appreciated!";
    public static final String FULL_DESCRIPTION  = "Donations are processed through Tebex. Donations are not required but truly appreciated! " +
                                                   "\nAll donations go towards paying for server related bills. " +
                                                   "\nAll donations are final- no refunds will be issued." +
                                                   "\nAll ranks are permanent- you will not lose them at any time." +
                                                   "\nAll donations are cumulative. This means that if you donate $" +
                                                   Rank.DONOR_RANK_COSTS[0] +
                                                   " now for the " + Rank.DONOR.getName() + " rank and donate $" +
                                                   (Rank.DONOR_RANK_COSTS[1] - Rank.DONOR_RANK_COSTS[0]) +
                                                   " later you'll be ranked to " + Rank.PATRON.getName() + " rank($" +
                                                   Rank.DONOR_RANK_COSTS[1] +
                                                   ").";

    /**
     * Update the RANKS_INFO hashmap with the correct data
     */
    private static void updateRanksInfo() {
        Rank[] prevRanks = { Rank.SCHOLAR, Rank.DONOR, Rank.PATRON };
        for (int i = 0; i < Rank.DONOR_RANKS.length; i++) {
            Rank rank = Rank.DONOR_RANKS[i];
            List<Component> value = new ArrayList<>();

            value.add(Component.text("Cost: " + Rank.DONOR_RANK_COSTS[i] + " USD"));
            value.add(Component.text(rank.getHomes() + " homes"));
            value.add(Component.text(rank.getClaimBlockBonus() / 1000 + "k bonus claim blocks"));

            switch (rank) {
                case DONOR: // In-case extra perks are added later
                    break;
                case PATRON:
                    value.add(Component.text("30 minute AFK cooldown"));
                    break;
                case SPONSOR: // In-case extra perks are added later
                    break;
                default:
                    break;
            }
            value.add(Component.text("Special collectible"));

            value.add(ComponentUtils.format(
                "Commands: All {} commands, {}",
                prevRanks[i],
                FarLands.getCommandHandler().getCommands().stream()
                    .filter(cmd -> rank.equals(cmd.getMinRankRequirement()))
                    .map(cmd -> "/" + cmd.getName().toLowerCase())
                    .collect(Collectors.joining(", "))
            ));

            RANKS_INFO.put(rank, value);
        }
    }

    public CommandDonate() {
        super(CommandData.simple("donate", "List donation costs and perks.", "/donate").category(Category.INFORMATIONAL));
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        updateRanksInfo();

        List<Component> outlist = new ArrayList<>();

        for (Rank rank : Rank.DONOR_RANKS) {
            outlist.add(ComponentColor.blue(
                "{}: {}",
                rank.asComponent(),
                Component.join(JoinConfiguration.separator(Component.text("\n -")), RANKS_INFO.get(rank))
            ));
        }

        // Provide the link (formatting depends on where the command was run from)
        if (sender instanceof Player) {
            info(sender,
                 "{} {}",
                 BRIEF_DESCRIPTION,
                 ComponentUtils.hover(ComponentColor.aqua("[Hover for more info]"), FULL_DESCRIPTION)
            );
            outlist.forEach(sender::sendMessage);
            info(sender, "Donate here: {}", ComponentUtils.link(FarLands.getFLConfig().donationLink));
        } else if (sender instanceof DiscordSender) {
            EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Donate", FarLands.getFLConfig().donationLink)
                .setDescription(FULL_DESCRIPTION + "\nDonate here: <" + FarLands.getFLConfig().donationLink + ">")
                .setColor(NamedTextColor.BLUE.value());

            for (Rank rank : Rank.DONOR_RANKS) {
                eb.addField(
                    rank.getName(),
                    String.join("\n - ", RANKS_INFO.get(rank).stream().map(ComponentUtils::toText).toList()),
                    false
                );
            }

            ((DiscordSender) sender).sendMessageEmbeds(eb.build());

        } else {
            sender.sendMessage(FULL_DESCRIPTION);
            outlist.forEach(sender::sendMessage);
            info(sender, "Donate here: {}", ComponentUtils.link(FarLands.getFLConfig().donationLink));
        }

        return true;
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        return this.defaultCommand(false);
    }
}
