package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ReflectionHelper;

import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPig;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CommandSit extends PlayerCommand {

    public CommandSit() {
        super(Rank.KNIGHT, Category.MISCELLANEOUS, "Have a seat.", "/sit", "sit");
    }

    @Override
    public boolean canUse(Player sender) {
        if (sender.getVehicle() != null && FarLands.getDataHandler().getSession(sender).seatExit == null) {
            sendFormatted(sender, "&(red)You are already sitting");
            return false;
        }
        return super.canUse(sender);
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        if (session.seatExit != null) {
            Entity chair = sender.getVehicle();
            if (chair != null)
                chair.eject();
            return true;
        }
        if (!sender.isOnGround()) { // can't go in canUse as it prevents /sit exit
            sendFormatted(sender, "&(red)You must be on the ground to use this command.");
            return true;
        }
        session.seatExit = sender.getLocation().clone();
        Pig seat = (Pig) sender.getWorld().spawnEntity(sender.getLocation().clone().subtract(0, .875, 0), EntityType.PIG);
        seat.setAdult();
        seat.setGravity(false);
        seat.setSilent(true);
        seat.setInvulnerable(true);
        seat.setAI(false);
        seat.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
        seat.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,         Integer.MAX_VALUE, 1));
        ReflectionHelper.setFieldValue("vehicle", net.minecraft.server.v1_16_R1.Entity.class,
                ((CraftPlayer) sender).getHandle(), ((CraftPig) seat).getHandle());
        ((CraftPig) seat).getHandle().passengers.add(((CraftPlayer) sender).getHandle());
        return true;
    }
}
