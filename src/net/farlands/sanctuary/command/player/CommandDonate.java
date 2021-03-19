package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.Rank;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandDonate extends Command {
    public CommandDonate() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "List donation costs and perks.", "/donate", "donate");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        Map<String, String> info = new HashMap<>();

        info.put("description", "Donations are processed through Tebex. Donations are not required but truly appreciated! " +
                "All donations go towards paying for server related bills. All donations are final, no refunds will be issued.");


        info.put(Rank.DONOR.getColor() + Rank.DONOR.getName() + ":", // Donor
                ChatColor.BLUE + " - Cost: " + Rank.DONOR_RANK_COSTS[0] + " USD\n" +
                " - " + Rank.DONOR.getHomes() + " homes\n" +
                " - Commands: " + FarLands.getCommandHandler().getCommands().stream()
                    .filter(cmd -> Rank.DONOR.equals(cmd.getMinRankRequirement()))
                    .map(cmd -> "/" + cmd.getName().toLowerCase())
                    .collect(Collectors.joining(", ")));

        info.put(Rank.PATRON.getColor() + Rank.PATRON.getName() + ":", // Patron
                ChatColor.BLUE + " - Cost: " + Rank.DONOR_RANK_COSTS[1] + " USD\n" +
                " - " + Rank.PATRON.getHomes() + " homes\n" +
                " - 30 minute AFK cooldown\n" +
                " - Special collectible\n" +
                " - Commands: " + FarLands.getCommandHandler().getCommands().stream()
                    .filter(cmd -> Rank.PATRON.equals(cmd.getMinRankRequirement()))
                    .map(cmd -> "/" + cmd.getName().toLowerCase())
                    .collect(Collectors.joining(", ")));

        info.put(Rank.SPONSOR.getColor() + Rank.SPONSOR.getName() + ":", // Sponsor
                ChatColor.BLUE + " - Cost: " + Rank.DONOR_RANK_COSTS[2] + " USD\n" +
                " - " + Rank.SPONSOR.getHomes() + " homes\n" +
                " - Commands: " + FarLands.getCommandHandler().getCommands().stream()
                    .filter(cmd -> Rank.SPONSOR.equals(cmd.getMinRankRequirement()))
                    .map(cmd -> "/" + cmd.getName().toLowerCase())
                    .collect(Collectors.joining(", ")));

        List<String> outlist = new ArrayList<>();

        info.forEach((k, v) -> {
            outlist.add(k.equals("description") ? "" : "\n" + k + "\n" + v);
        });

        // Provide the link (formatting depends on where the command was run from)
        if (sender instanceof Player) {
            sender.sendMessage(String.join("\n", info.values()));
            sendFormatted(sender, "&(gold)Donate here: $(hoverlink,%0,{&(gray)Click to Follow},&(aqua,underline)%0)",
                    FarLands.getFLConfig().donationLink);
        }
        else if (sender instanceof DiscordSender) {
            EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Donate")
                .setDescription(info.get("description") + "\nDonate here: <" + FarLands.getFLConfig().donationLink + ">")
                .setColor(0x5555FF); // BLUE
            info.forEach((k, v) -> {
                if (k.equals("description")) { return; }
               eb.addField(ChatColor.stripColor(k), ChatColor.stripColor(v), false);
            });
            ((DiscordSender) sender).getChannel().sendMessage(eb.build()).queue();
        } else {
            sender.sendMessage(String.join("\n", info.values()));
            sendFormatted(sender, "&(gold)Donate here: &(aqua)%0", FarLands.getFLConfig().donationLink);
        }

        return true;
    }
}
