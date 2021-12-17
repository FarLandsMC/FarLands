package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FLUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.kicas.rp.util.TextUtils.sendFormatted;

public class CommandTNTArrow extends PlayerCommand {
    private static final List<String> TYPES = Arrays.asList("nerfed", "standard", "firework", "animals", "mob", "compact", "rain");
    private static final List<String> NAMES = Arrays.asList(
            "{\"text\":\"Nerfed TNT Arrow\",\"italic\":false,\"color\":\"yellow\"}",
            "{\"text\":\"Real TNT Arrow\",\"italic\":false,\"color\":\"red\"}",
            "{\"text\":\"Firework TNT Arrow\",\"italic\":false,\"color\":\"green\"}",
            "{\"text\":\"Animal TNT Arrow\",\"italic\":false,\"color\":\"blue\"}",
            "{\"text\":\"Mob TNT Arrow\",\"italic\":false,\"color\":\"dark_gray\"}",
            "{\"text\":\"Compact TNT Arrow\",\"italic\":false,\"color\":\"dark_red\"}",
            "{\"text\":\"Rain TNT Arrow\",\"italic\":false,\"color\":\"dark_red\"}"
    );

    public CommandTNTArrow() {
        super(Rank.ADMIN, "Give yourself some tnt arrows.", "/tntarrow [nerfed|standard|amount] [power] [amount] [fakeExplosionDuration]", "tntarrow");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        NBTTagCompound tntArrow = new NBTTagCompound();
        tntArrow.a("strength", 1f); // NBTTagCompound#setFloat
        int amount = 1;
        if(args.length == 1) {
            amount = parseNumber(args[0], Integer::parseInt, 0);
            if(amount == 0) {
                if(!TYPES.contains(args[0]))
                    return false;
                tntArrow.a("type", TYPES.indexOf(args[0])); // NBTTagCompound#setInt
                amount = 1;
            }
        }else if(args.length > 1) {
            if(!TYPES.contains(args[0]))
                return false;
            tntArrow.a("type", TYPES.indexOf(args[0])); // NBTTagCompound#setInt
            float power = parseNumber(args[1], Float::parseFloat, 0.0F);
            if(power == 0.0) {
                sendFormatted(sender, "&(red)The arrow power must be 1 or larger.");
                return true;
            }
            tntArrow.a("strength", power); // NBTTagCompound#setFloat
            if(args.length >= 3)
                amount = parseNumber(args[2], Integer::parseInt, 0);
            if(args.length >= 4) {
                int duration = parseNumber(args[3], Integer::parseInt, 0);
                if(duration < 5 || duration > 300) {
                    sendFormatted(sender, "&(red)The duration must be between 5 and 300 inclusive.");
                    return true;
                }
                tntArrow.a("duration", duration); // NBTTagCompound#setInt
            }
        }
        if(amount < 1 || amount > 64) {
            sendFormatted(sender, "&(red)The amount must be between 1 and 64 inclusive.");
            return true;
        }
        NBTTagCompound display = new NBTTagCompound();
        display.a("Name", NAMES.get(tntArrow.e("type") ? tntArrow.h("type") : 0)); // NBTTagCompound#setString, NBTTagCompound#hasKey, NBTTagCompound#getInt
        NBTTagList lore = new NBTTagList();
        lore.add(NBTTagString.a("\"Strength: " + (int) tntArrow.j("strength") /* NBTTagCompound#getFloat */ + "\""));
        display.a("Lore", lore); // NBTTagCompound#set
        NBTTagCompound tag = new NBTTagCompound();
        tag.a("tntArrow", tntArrow); // NBTTagCompound#set
        tag.a("display", display); // NBTTagCompound#set
        FLUtils.giveItem(sender, FLUtils.applyTag(tag, new ItemStack(Material.ARROW, amount)), false);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (!Rank.getRank(sender).isStaff())
            return Collections.emptyList();
        return args.length == 1
                ? TYPES.stream().filter(o -> o.startsWith(args[0])).collect(Collectors.toList())
                : Collections.emptyList();
    }
}
