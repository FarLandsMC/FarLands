package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommandPurchase extends Command {
    public CommandPurchase() {
        super(Rank.MOD, "Command for Tebex (BuyCraft)", "/purchase <name> <rank> [uuid] [price]", "purchase");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        FarLands.getDebugger().echo("Donation command execution: /purchase " + String.join(" ", args));
        if(args.length < 2)
            return false;
        OfflineFLPlayer flp = getFLPlayer(args[0]);
        if(flp == null) {
            if(args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(args[2]);
            }catch(IllegalArgumentException ex) {
                FarLands.error("Failed to execute purchase command due to an invalid UUID for player " + args[0]);
                return true;
            }
            flp = FarLands.getPDH().getFLPlayer(uuid, args[0]);
        }
        if(!FarLands.getDataHandler().getRADH().isCooldownComplete("purchase", args[2]))
            return true;
        else
            FarLands.getDataHandler().getRADH().setCooldown(100L, "purchase", args[2]);
        Rank rank = Utils.safeValueOf(Rank::valueOf, args[1].toUpperCase());
        int price = args.length >= 4 ? Integer.parseInt(args[3]) : 0;
        flp.addDonation(price);
        if (args[1].equalsIgnoreCase("none")) {
            if (flp.getAmountDonated() >= Rank.PATRON_COST_USD)
                rank = Rank.PATRON;
            else if (flp.getAmountDonated() >= Rank.DONOR_COST_USD)
                rank = Rank.DONOR;
        }
        if(rank != null && rank.specialCompareTo(flp.getRank()) > 0) {
            if(rank == Rank.DONOR)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "claimblocks add " + flp.getUsername() + " 15000");
            else if(rank == Rank.PATRON) {
                if (flp.isOnline()) {
                    Utils.giveItem(flp.getOnlinePlayer(), FarLands.getDataHandler().getPatronCollectable(), false);
                } else
                    FarLands.getDataHandler().addPackage(flp.getUuid(), "FarLands Staff",
                            FarLands.getDataHandler().getPatronCollectable(), "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "claimblocks add " + flp.getUsername() + " " +
                        (flp.getRank() == Rank.DONOR ? "45000" : "60000"));
            }
            flp.setRank(rank);
            Player player = flp.getOnlinePlayer();
            if(player != null) // Notify the player if they're online
                player.sendMessage(ChatColor.GREEN + "Your rank has been updated to " + rank.getColor() + rank.toString());
            FarLands.getPDH().saveFLPlayer(flp);
        }
        if (price > 0)
            FarLands.getDiscordHandler().sendMessage("output", args[0] + " has donated " + price + " USD.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch(args.length) {
            case 0:
            case 1:
                return getOnlineVanishedPlayers(args.length == 0 ? "" : args[0]);
            case 2:
                return Rank.PURCHASED_RANKS.stream().map(Rank::toString).collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }
}
