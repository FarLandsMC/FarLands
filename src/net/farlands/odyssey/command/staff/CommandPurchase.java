package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.Logging;
import net.farlands.odyssey.util.FLUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CommandPurchase extends Command {
    private final Map<UUID, Long> commandCooldowns;
    private static final long PURCHASE_COMMAND_COOLDOWN = 5000L;

    public CommandPurchase() {
        super(Rank.MOD, "Command for Tebex (BuyCraft)", "/purchase <name> <rank> [uuid] [price]", "purchase");
        this.commandCooldowns = new HashMap<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        FarLands.getDebugger().echo("Donation command execution: /purchase " + String.join(" ", args));
        if (args.length < 2)
            return false;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[0]);
        if (flp == null) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
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
        Rank rank = FLUtils.safeValueOf(Rank::valueOf, args[1].toUpperCase());
        int price = args.length >= 4 ? Integer.parseInt(args[3]) : 0;
        flp.addDonation(price);
        if (args[1].equalsIgnoreCase("none")) {
            if (flp.amountDonated >= Rank.PATRON_COST_USD)
                rank = Rank.PATRON;
            else if (flp.amountDonated >= Rank.DONOR_COST_USD)
                rank = Rank.DONOR;
        }
        if (rank != null && rank.specialCompareTo(flp.rank) > 0) {
            if (rank == Rank.DONOR)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "claimblocks add " + flp.username + " 15000");
            else if (rank == Rank.PATRON) {
                if (flp.isOnline()) {
                    FLUtils.giveItem(flp.getOnlinePlayer(), FarLands.getFLConfig().patronCollectable.getStack(), false);
                } else
                    FarLands.getDataHandler().addPackage(flp.uuid, "FarLands Staff",
                            FarLands.getFLConfig().patronCollectable.getStack(), "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "claimblocks add " + flp.username + " " +
                        (flp.rank == Rank.DONOR ? "45000" : "60000"));
            }
            flp.setRank(rank);
            Player player = flp.getOnlinePlayer();
            if (player != null) // Notify the player if they're online
                player.sendMessage(ChatColor.GREEN + "Your rank has been updated to " + rank.getColor() + rank.toString());
        }
        if (price > 0)
            FarLands.getDiscordHandler().sendMessage("output", args[0] + " has donated " + price + " USD.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 0:
            case 1:
                return getOnlinePlayers(args.length == 0 ? "" : args[0], sender);
            case 2:
                return Rank.PURCHASED_RANKS.stream().map(Rank::toString).collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }
}
