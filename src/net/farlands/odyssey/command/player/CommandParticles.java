package net.farlands.odyssey.command.player;

import com.kicas.rp.util.Utils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.PlayerCommand;
import net.farlands.odyssey.data.struct.Particles;
import net.farlands.odyssey.data.Rank;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandParticles extends PlayerCommand {
    private static final List<Particle> ILLEGAL_PARTICLES = Arrays.asList(Particle.MOB_APPEARANCE, Particle.BARRIER,
            Particle.REDSTONE, Particle.BLOCK_CRACK);

    public CommandParticles() {
        super(Rank.DONOR, "Choose a particle to spawn around you.", "/particles <type> [location=above-head]", "particles");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 0)
            return false;
        if ("none".equalsIgnoreCase(args[0])) {
            FarLands.getDataHandler().getOfflineFLPlayer(sender).setParticles(null, null); // Removes particles
            sender.sendMessage(ChatColor.GREEN + "Particles removed.");
        } else {
            Particle type = Utils.valueOfFormattedName(args[0], Particle.class);
            if (type == null) {
                sender.sendMessage(ChatColor.RED + "Invalid particle type: " + args[0]);
                return true;
            } else if (ILLEGAL_PARTICLES.contains(type)) {
                sender.sendMessage(ChatColor.RED + "You cannot use that particle type.");
                return true;
            }
            Particles.ParticleLocation location = args.length == 1
                    ? Particles.ParticleLocation.ABOVE_HEAD
                    : Particles.ParticleLocation.specialValueOf(args[1]);
            if (location == null) {
                sender.sendMessage(ChatColor.RED + "Invalid particle location: " + args[1]);
                return true;
            }
            FarLands.getDataHandler().getOfflineFLPlayer(sender).setParticles(type, location);
            sender.sendMessage(ChatColor.GREEN + "Particles set.");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 0:
            case 1: {
                List<String> completions = Arrays.stream(Particle.values()).filter(p -> !ILLEGAL_PARTICLES.contains(p))
                        .map(Utils::formattedName)
                        .filter(p -> p.startsWith(args.length == 0 ? "" : args[0])).collect(Collectors.toCollection(ArrayList::new));
                completions.add("none");
                return completions;
            }
            case 2:
                return Arrays.stream(Particles.ParticleLocation.VALUES).map(Particles.ParticleLocation::getAlias)
                        .filter(l -> l.startsWith(args[1])).collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }
}
