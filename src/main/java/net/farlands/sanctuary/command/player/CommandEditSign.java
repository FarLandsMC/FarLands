package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import com.kicas.rp.util.Utils;
import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.data.Shop;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CommandEditSign extends PlayerCommand {

    public CommandEditSign() {
        super(
            CommandData.withRank(
                    "editsign",
                    "/editsign <set|clear> <front|back> <line> <text>",
                    "Edit a specific line of a sign",
                    Rank.SAGE
                )
                .category(Category.UTILITY)
        );
    }

    @Override
    protected boolean execute(Player sender, String[] args) {

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        // Make sure the player has build permission in the region they're in
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(sender.getLocation());
        if (flags != null && !flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(sender, TrustLevel.BUILD, flags)) {
            error(sender, "You do not have permissions to edit signs here.");
            return true;
        }

        Block target = sender.getTargetBlock(null, 5); // 5 seems like a good number

        // Make sure the sign is not a chest shop
        Shop shop = ChestShops.getDataHandler().getShop(target.getLocation());
        if (shop != null) {
            error(sender, "You cannot edit chest shop signs.");
            return true;
        }

        // Make sure the player is looking at a sign
        if (!(target.getState() instanceof Sign)) {
            error(sender, "You must be looking at a sign to use this command.");
            return true;
        }

        Sign sign = (Sign) target.getState();

        if (sign.isWaxed()) {
            error(sender, "This sign is waxed!");
            return true;
        }

        // TODO: Find some way to figure out which side of the sign a player is looking at, so that we don't need the <side> argument
        try {
            if (args[0].equalsIgnoreCase("set") && args.length > 3) {
                int line = Integer.parseInt(args[2]) - 1;
                String text = TabCompleterBase.joinArgsBeyond(2, " ", args);
                Component comp = ComponentUtils.parse(text, flp);
                if (ComponentUtils.toText(comp).length() > 15) {
                    error(sender, "Sign lines are limited to 15 characters.");
                    return true;
                }
                SignSide side = sign.getSide(Utils.valueOfFormattedName(args[1], Side.class));
                side.line(line, comp);
                sign.update();
                sender.sendMessage(ComponentColor.gold("Line %d on %s set to: ", line + 1, args[1]).append(comp));
            } else if (args[0].equalsIgnoreCase("clear")) {
                if (args.length == 1) {
                    for (int i = 0; i < 4; i++) {
                        sign.getSide(Side.FRONT).line(i, Component.empty());
                        sign.getSide(Side.BACK).line(i, Component.empty());
                    }
                    sign.update();
                    success(sender, "Sign text cleared.");
                } else if (args.length == 2) {
                    SignSide side = sign.getSide(Utils.valueOfFormattedName(args[1], Side.class));
                    for (int i = 0; i < 4; i++) {
                        side.line(i, Component.empty());
                    }
                    sign.update();
                    success(sender, "Sign text cleared.");
                } else {
                    SignSide side = sign.getSide(Utils.valueOfFormattedName(args[1], Side.class));
                    int line = Integer.parseInt(args[2]) - 1;
                    side.line(line, Component.empty());
                    sign.update();
                    sender.sendMessage(ComponentColor.gold("Line %d text cleared on %s.", line + 1, args[1]));
                }
            } else {
                return false;
            }
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            return false;
        }
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return switch (args.length) {
            case 1 -> List.of("set", "clear");
            case 2 -> List.of("front", "back");
            case 3 -> List.of("1", "2", "3", "4");
            default -> arg3(args, sender);
        };
    }

    private List<String> arg3(String[] args, CommandSender sender) {
        if (ComponentUtils.toText(
            ComponentUtils.parse(
                TabCompleterBase.joinArgsBeyond(1, " ", args),
                FarLands.getDataHandler().getOfflineFLPlayer(sender)
            )
        ).length() > 15) {
            return Collections.singletonList("Too Long!");
        }
        return Collections.emptyList();
    }
}
