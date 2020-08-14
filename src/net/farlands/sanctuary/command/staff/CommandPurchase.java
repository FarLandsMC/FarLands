package net.farlands.sanctuary.command.staff;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CommandPurchase extends Command {
    private final Map<UUID, Long> commandCooldowns;
    private static final long PURCHASE_COMMAND_COOLDOWN = 5000L;

    public CommandPurchase() {
        super(Rank.BUILDER, "Command for Tebex (BuyCraft)", "/purchase <name> <rank> [uuid] [price]", "purchase");
        this.commandCooldowns = new HashMap<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        FarLands.getDebugger().echo("Donation command execution: /purchase " + String.join(" ", args));
        if (args.length < 2)
            return false;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if (flp == null) {
            if (args.length <3) { // love donates
                sendFormatted(sender, "&(red)Player not found.");
                return true;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(args[2]);
            } catch (IllegalArgumentException ex) {
                Logging.error("Failed to execute purchase command due to an invalid UUID for player " + args[0]);
                return true;
            }
            flp = FarLands.getDataHandler().getOfflineFLPlayer(uuid, args[0]);
        }

        if (commandCooldowns.containsKey(flp.uuid) && System.currentTimeMillis() - commandCooldowns.get(flp.uuid) < PURCHASE_COMMAND_COOLDOWN)
            return true;
        else
            commandCooldowns.put(flp.uuid, System.currentTimeMillis());

        Logging.broadcastFormatted("&(gold){&(aqua)%0} just donated to the server! Consider donating $(link,%1,{&(aqua,underline)here}).",
                true, flp.username, FarLands.getFLConfig().donationLink);

        Rank rank = FLUtils.safeValueOf(Rank::valueOf, args[1].toUpperCase());
        double price = args.length >= 4 ? Double.parseDouble(args[3]) : 0;
        flp.amountDonated += price;

        for (int i = Rank.DONOR_RANK_COSTS.length; --i >= 0;) {
            if (flp.amountDonated >= Rank.DONOR_RANK_COSTS[i]) {
                rank = Rank.DONOR_RANKS[i];
                break;
            }
        }

        if (rank != null && rank.specialCompareTo(flp.rank) > 0) {
            if (rank == Rank.PATRON || (flp.rank != Rank.PATRON && rank == Rank.SPONSOR)) {
                if (flp.isOnline()) {
                    FLUtils.giveItem(flp.getOnlinePlayer(), FarLands.getFLConfig().patronCollectable.getStack(), false);
                } else {
                    FarLands.getDataHandler().addPackage(flp.uuid, "FarLands Staff",
                            FarLands.getFLConfig().patronCollectable.getStack(), "");
                }
            }

            flp.setRank(rank);

            Player player = flp.getOnlinePlayer();
            if (player != null) // Notify the player if they're online
                sendFormatted(player, "&(green)Your rank has been updated to " + rank.getColor() + rank.toString());
        }

        if (price > 0)
            FarLands.getDiscordHandler().sendMessage(DiscordChannel.NOTEBOOK, args[0] + " has donated " + price + " USD.");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        switch (args.length) {
            case 0:
                return getOnlinePlayers("", sender);
            case 1:
                return getOnlinePlayers(args[0], sender);
            case 2:
                return Rank.PURCHASED_RANKS.stream().map(Rank::toString).collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }
}
