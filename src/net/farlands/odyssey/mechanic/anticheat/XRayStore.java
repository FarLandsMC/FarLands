package net.farlands.odyssey.mechanic.anticheat;

import static java.lang.Math.abs;

import net.farlands.odyssey.FarLands;

import net.farlands.odyssey.mechanic.Chat;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;


public class XRayStore {
    private final String playerName;
    private final boolean sendAlerts;
    private int strikes;
    private Location last;
    private int mined;

    public XRayStore(String playerName, boolean sendAlerts) {
        this.playerName = playerName;
        this.sendAlerts = sendAlerts;
        this.strikes = 0;
        this.last = null;
        this.mined = 0;
    }
    
    public void onBlockBreak(final Block block) {
        if (!"world".equals(block.getWorld().getName()))
            return;
        if (!((block.getLocation().getY() <= 16 && Material.DIAMOND_ORE.equals(block.getType())) ||
                (block.getLocation().getY() <= 32 && Material.EMERALD_ORE.equals(block.getType())))) {
            ++mined;
            return;
        }
        final String oreType = (Material.DIAMOND_ORE.equals(block.getType())
                ? ChatColor.AQUA + "diamond" : ChatColor.GREEN + "emerald");
        if (last == null) {
            last = block.getLocation();
            FarLands.getDebugger().echo(playerName + " : " + Chat.removeColorCodes(oreType) + " : " +
                    last.getBlockX() + " " + last.getBlockY() + " " + last.getBlockZ() + " @ First Node");
            if (sendAlerts)
                AntiCheat.broadcast(playerName + " has found a vein of " + oreType + " ore @ " +
                        last.getBlockX() + " " + last.getBlockY() + " " + last.getBlockZ(), false);
            return;
        }
    
        final int dx = block.getLocation().getBlockX() - last.getBlockX(),
                dy = block.getLocation().getBlockY() - last.getBlockY(),
                dz = block.getLocation().getBlockZ() - last.getBlockZ();
        if (abs(dy) <= 2 && abs(dx) <= 2 && abs(dz) <= 2)
            return;
        
        last = block.getLocation();
        FarLands.getDebugger().echo(playerName + " : " + Chat.removeColorCodes(oreType) + " : " +
                last.getBlockX() + " " + last.getBlockY() + " " + last.getBlockZ()
                + " @ " + dx + " " + dy + " " + dz);
        if (sendAlerts)
            AntiCheat.broadcast(playerName + " has found a vein of " + oreType + " ore @ " +
                    last.getBlockX() + " " + last.getBlockY() + " " + last.getBlockZ(), false);
        // minimal path is |dy| + 2(|dx|+|dz|) but we +4 to account for an imperfect path
        // we also ignore anything below half the minimal path to account for caving
        int a = abs(dx) + abs(dy) + abs(dz), b = abs(dy) + 2 * (4 + abs(dx) + abs(dz));
        FarLands.getDebugger().echo("mined : " + a + " " + mined + " " + b);
        if ((abs(dy) <= 3 && (abs(dx) <= 2 || abs(dz) <= 2)) || (a >= mined || mined >= b)){
            mined = 0;
            return;
        }
        mined = 0;
        FarLands.getDebugger().echo(playerName + " : XRay : Strikes : " + ++strikes);
        if (sendAlerts)
            AntiCheat.broadcast(playerName, "might be using X-Ray");
    }
}
