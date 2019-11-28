package net.farlands.odyssey.command.staff;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.DataHandler;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.data.struct.GameRewardSet;
import net.farlands.odyssey.data.struct.ItemReward;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandAddItemReward extends PlayerCommand {
    public CommandAddItemReward() {
        super(Rank.BUILDER, "Add the item currently in your hand to a vote reward pool.", "/itemreward " +
                "<vote|voteParty|patronCollectable|game> <gameName> <item|final|bias> [rarity|bias]", "itemreward",
                "ir");
    }

    @Override
    protected boolean execute(Player sender, String[] args) {
        ItemStack stack = sender.getInventory().getItemInMainHand();
        if((stack == null || Material.AIR.equals(stack.getType())) && !(args.length >= 3 && "bias".equals(args[2]))) {
            sender.sendMessage(ChatColor.RED + "You must have an item in your hand to use this command.");
            return true;
        }
        stack = stack.clone();
        DataHandler dh = FarLands.getDataHandler();
        if("vote".equals(args[0]))
            dh.getVoteRewards().add(stack);
        else if("voteParty".equals(args[0])) {
            if(args.length < 2) {
                sender.sendMessage(ChatColor.RED + "A rarity must be specified for a vote party reward.");
                return true;
            }
            int rarity;
            try {
                rarity = Integer.parseInt(args[1]);
            }catch(NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid rarity, must be a number.");
                return true;
            }
            if(rarity < 0) {
                sender.sendMessage(ChatColor.RED + "The item rarity must be greater than or equal to zero.");
                return true;
            }
            dh.getVotePartyRewards().add(new ItemReward(stack, rarity));
        }else if("patronCollectable".equals(args[0]))
            dh.setPatronCollectable(stack);
        else if("game".equals(args[0])) {
            if(args.length < 3)
                return false;
            String game = args[1];
            GameRewardSet grs = FarLands.getDataHandler().getGameRewardSet(game);
            if(grs == null) {
                sender.sendMessage(ChatColor.RED + "Game not found!");
                return false;
            }
            if("item".equals(args[2])) {
                int rarity;
                try {
                    rarity = Integer.parseInt(args[1]);
                }catch(NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "Invalid rarity, must be a number.");
                    return true;
                }
                if(rarity < 0) {
                    sender.sendMessage(ChatColor.RED + "The item rarity must be greater than or equal to zero.");
                    return true;
                }
                grs.addReward(new ItemReward(stack, rarity));
            }else if("final".equals(args[2])) {
                grs.setFinalReward(stack);
                sender.sendMessage(ChatColor.GREEN + "Final reward set.");
                return true;
            }else if("bias".equals(args[2])) {
                if(args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "A bias must be specified.");
                    return true;
                }
                double bias;
                try {
                    bias = Double.parseDouble(args[3]);
                }catch(NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "Invalid bias.");
                    return true;
                }
                if(bias <= 0.0 || bias > 1.0) {
                    sender.sendMessage(ChatColor.RED + "The bias must be on the interval (0,1]");
                    return true;
                }
                grs.setBias(bias);
                sender.sendMessage(ChatColor.GREEN + "Bias set.");
                return true;
            }
        }else
            return false;
        sender.sendMessage(ChatColor.GREEN + "Item added.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch(args.length) {
            case 0:
            case 1:
                return Stream.of("vote", "voteParty", "patronCollectable", "game")
                        .filter(o -> o.startsWith(args.length == 0 ? "" : args[0])).collect(Collectors.toList());
            case 2:
                if("game".equals(args[0]))
                    return new ArrayList<>(FarLands.getDataHandler().getGames());
                else
                    return Collections.emptyList();
            case 3:
                return Stream.of("item", "final", "bias").filter(o -> o.startsWith(args[2])).collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }
}
