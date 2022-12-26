package net.farlands.sanctuary.command.player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kicas.rp.util.TextUtils.sendFormatted;

public class CommandDonate extends Command {

    public static final Map<Rank, List<String>> RANKS_INFO = new HashMap<>();
    public static final String BRIEF_DESCRIPTION = "Donations are processed through Tebex. They are not required but truly appreciated!";
    public static final String FULL_DESCRIPTION = "Donations are processed through Tebex. Donations are not required but truly appreciated! " +
            "\nAll donations go towards paying for server related bills. " +
            "\nAll donations are final- no refunds will be issued." +
            "\nAll ranks are permanent- you will not lose them at any time." +
            "\nAll donations are cumulative. This means that if you donate \\$" +
            Rank.DONOR_RANK_COSTS[0] +
            " now for the " + Rank.DONOR.getName() + " rank and donate \\$" +
            (Rank.DONOR_RANK_COSTS[1] - Rank.DONOR_RANK_COSTS[0]) +
            " later you'll be ranked to " + Rank.PATRON.getName() + " rank(\\$" +
            Rank.DONOR_RANK_COSTS[1] +
            ").";

    private static void updateRanksInfo() { // Set all of the ranks info
        String[] prevRanks = {"Scholar", "Donor", "Patron"};
        for (int i = 0; i < Rank.DONOR_RANKS.length; i++) {
            Rank rank = Rank.DONOR_RANKS[i];
            List<String> value = new ArrayList<>();

            value.add("Cost: " + Rank.DONOR_RANK_COSTS[i] + " USD");
            value.add(rank.getHomes() + " homes");
            value.add(rank.getClaimBlockBonus() / 1000 + "k bonus claim blocks");

            switch (rank) {
                case DONOR: // In-case extra perks are added later
                    break;
                case PATRON:
                    value.add("30 minute AFK cooldown");
                    value.add("Special collectible");
                    break;
                case SPONSOR: // In-case extra perks are added later
                    break;
                default:
                    break;
            }

            value.add("Commands: All " + prevRanks[i] + " commands, " +
                    FarLands.getCommandHandler().getCommands().stream()
                            .filter(cmd -> rank.equals(cmd.getMinRankRequirement()))
                            .map(cmd -> "/" + cmd.getName().toLowerCase())
                            .collect(Collectors.joining(", "))
            );

            RANKS_INFO.put(rank, value);
        }
    }

    public CommandDonate() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "List donation costs and perks.", "/donate", "donate");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        updateRanksInfo();

        List<String> outlist = new ArrayList<>();

        for(Rank rank : Rank.DONOR_RANKS) {
            outlist.add(rank.getColor() + rank.getName() + ": " + ChatColor.BLUE + String.join("\n - ", RANKS_INFO.get(rank)));
        }

        // Provide the link (formatting depends on where the command was run from)
        if (sender instanceof Player) {
            sendFormatted(sender, "$(hover,&(blue)%1,&(gold)%0 &(aqua)(Hover For More Info))", BRIEF_DESCRIPTION, FULL_DESCRIPTION);
            sendFormatted(sender, String.join("\n", outlist));
            sendFormatted(sender, "&(gold)Donate here: $(hoverlink,%0,{&(gray)Click to Follow},&(aqua,underline)%0)",
                    FarLands.getFLConfig().donationLink);
        } else if (sender instanceof DiscordSender) {
            EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Donate", FarLands.getFLConfig().donationLink)
                .setDescription(FULL_DESCRIPTION + "\nDonate here: <" + FarLands.getFLConfig().donationLink + ">")
                .setColor(0x5555FF); // BLUE

            for(Rank rank : Rank.DONOR_RANKS) {
                eb.addField(rank.getName(), ChatColor.stripColor(String.join("\n - ", RANKS_INFO.get(rank))), false);
            }

            ((DiscordSender) sender).sendMessageEmbeds(eb.build());

        } else {
            sender.sendMessage(FULL_DESCRIPTION);
            sender.sendMessage(String.join("\n", outlist));
            sendFormatted(sender, "&(gold)Donate here: &(aqua)%0", FarLands.getFLConfig().donationLink);
        }

        return true;
    }

    @Override
    public @Nullable SlashCommandData discordCommand() {
        return this.defaultCommand(false);
    }
}
