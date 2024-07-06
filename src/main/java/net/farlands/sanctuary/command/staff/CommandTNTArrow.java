package net.farlands.sanctuary.command.staff;

import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.ItemUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
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
    private static final List<Component> NAMES = Arrays.asList(
        ComponentColor.yellow("Nerfed TNT Arrow"),
        ComponentColor.red("Real TNT Arrow"),
        ComponentColor.green("Firework TNT Arrow"),
        ComponentColor.blue("Animal TNT Arrow"),
        ComponentColor.darkGray("Mob TNT Arrow"),
        ComponentColor.darkRed("Compact TNT Arrow"),
        ComponentColor.darkRed("Rain TNT Arrow")
    );

    public CommandTNTArrow() {
        super(Rank.ADMIN, "Give yourself some tnt arrows.", "/tntarrow [nerfed|standard|amount] [power] [amount] [fakeExplosionDuration]", "tntarrow");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        CompoundTag tntArrow = new CompoundTag();
        tntArrow.putFloat("strength", 1f); // CompoundTag#setFloat
        int amount = 1;
        if(args.length == 1) {
            amount = parseNumber(args[0], Integer::parseInt, 0);
            if(amount == 0) {
                if(!TYPES.contains(args[0]))
                    return false;
                tntArrow.putInt("type", TYPES.indexOf(args[0])); // CompoundTag#setInt
                amount = 1;
            }
        }else if(args.length > 1) {
            if(!TYPES.contains(args[0]))
                return false;
            tntArrow.putInt("type", TYPES.indexOf(args[0])); // CompoundTag#setInt
            float power = parseNumber(args[1], Float::parseFloat, 0.0F);
            if(power == 0.0) {
                sendFormatted(sender, "&(red)The arrow power must be 1 or larger.");
                return true;
            }
            tntArrow.putFloat("strength", power); // CompoundTag#setFloat
            if(args.length >= 3)
                amount = parseNumber(args[2], Integer::parseInt, 0);
            if(args.length >= 4) {
                int duration = parseNumber(args[3], Integer::parseInt, 0);
                if(duration < 5 || duration > 300) {
                    sendFormatted(sender, "&(red)The duration must be between 5 and 300 inclusive.");
                    return true;
                }
                tntArrow.putInt("duration", duration); // CompoundTag#setInt
            }
        }
        if(amount < 1 || amount > 64) {
            sendFormatted(sender, "&(red)The amount must be between 1 and 64 inclusive.");
            return true;
        }

        ItemStack is = new ItemStack(Material.ARROW, amount);
        is.editMeta(im -> {
            im.displayName(NAMES.get(tntArrow.contains("type") ? tntArrow.getInt("type") : 0));
            im.lore(List.of(ComponentUtils.format("Strength: {}", tntArrow.getFloat("strength"))));
        });

        ItemUtils.updateCustomData(is, d -> d.put("tntArrow", tntArrow));

        ItemUtils.giveItem(sender, is, false);
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
