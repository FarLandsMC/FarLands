package net.farlands.odyssey.command.player;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.ReflectionHelper;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPig;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CommandSit extends PlayerCommand {
    
    public CommandSit() {
        super(Rank.KNIGHT, "Have a seat.", "/sit", "sit");
    }

    @Override
    public boolean canUse(Player sender) {
        if (sender.getVehicle() != null && FarLands.getDataHandler().getRADH()
                .retrieve("seat_exit", sender.getUniqueId().toString()) == null) {
            sender.sendMessage("You are already sitting");
            return false;
        }
        return super.canUse(sender);
    }
    
    @Override
    public boolean execute(Player sender, String[] args) {
        Location exit = (Location)FarLands.getDataHandler().getRADH()
                .retrieve("seat_exit", sender.getUniqueId().toString());
        if (exit != null) {
            Entity chair = sender.getVehicle();
            if (chair != null)
                chair.eject();
            return true;
        }
        if (!sender.isOnGround()) { // can't go in canUse as it prevents /sit exit
            sender.sendMessage(ChatColor.RED + "You must be on the ground to use this command.");
            return true;
        }
        FarLands.getDataHandler().getRADH().store(sender.getLocation(), "seat_exit", sender.getUniqueId().toString());
        Pig seat = (Pig) sender.getWorld().spawnEntity(sender.getLocation().clone().subtract(0, .875, 0), EntityType.PIG);
        seat.setAdult();
        seat.setGravity(false);
        seat.setSilent(true);
        seat.setInvulnerable(true);
        seat.setAI(false);
        seat.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
        ReflectionHelper.setFieldValue("vehicle", net.minecraft.server.v1_14_R1.Entity.class,
                ((CraftPlayer)sender).getHandle(), ((CraftPig)seat).getHandle());
        ((CraftPig)seat).getHandle().passengers.add(((CraftPlayer)sender).getHandle());
        return true;
    }
}
