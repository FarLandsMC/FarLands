package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.data.Shop;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.CommandData;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.pdc.JSONDataType;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CommandEditSign extends PlayerCommand {

    public CommandEditSign() {
        super(
            CommandData.withRank(
                    "editsign",
                    "/editsign <set|clear> <line> <text>",
                    "Edit or clear the side of a sign at which you are looking",
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

        try {
            Side side = sign.getInteractableSideFor(sender);
            SignSide signSide = sign.getSide(side);
            if (args[0].equalsIgnoreCase("set") && args.length > 2) {
                int line = Integer.parseInt(args[1]) - 1;
                String text = TabCompleterBase.joinArgsBeyond(1, " ", args);
                Component comp = ComponentUtils.parse(text, flp);
                if (ComponentUtils.toText(comp).length() > 15) {
                    error(sender, "Sign lines are limited to 15 characters.");
                    return true;
                }
                signSide.line(line, comp);
                saveUnstyled(sign, side, text, line);
                sign.update();
                success(sender, "Line {} set to: {}", line + 1, comp);
            } else if (args[0].equalsIgnoreCase("clear")) {
                if (args.length == 1) {
                    for (int i = 0; i < 4; i++) {
                        signSide.line(i, Component.empty());
                    }
                    clearUnstyled(sign, side);
                    sign.update();
                    success(sender, "Sign text cleared.");
                } else {
                    int line = Integer.parseInt(args[1]) - 1;
                    signSide.line(line, Component.empty());
                    saveUnstyled(sign, side, "", line);
                    sign.update();
                    success(sender, "Line {} text cleared on.", line + 1);
                }
            } else {
                return false;
            }
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            return false;
        }
        return true;
    }

    private void saveUnstyled(Sign sign, Side side, String line, int index) {
        NamespacedKey key = FLUtils.nsKey(side.toString().toLowerCase() + "_raw");
        var type = new JSONDataType<>(String[].class);

        String[] lines = sign.getPersistentDataContainer().get(key, type);
        if (lines == null) { // If it's not been initialised, then read the sign content
            lines = sign.getSide(side)
                .lines()
                .stream()
                .map(ComponentUtils::toText)
                .toArray(String[]::new);
        }
        lines[index] = line;

        // Save the raw style such that we can restore it when editing
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        pdc.set(key, type, lines);
        sign.update();
    }

    private static final String[] CLEARED_SIGN_CONTENT = { "", "", "", "" };
    private void clearUnstyled(Sign sign, Side side) {
        NamespacedKey key = FLUtils.nsKey(side.toString().toLowerCase() + "_unstyled_content");
        var type = new JSONDataType<>(String[].class);

        // Save the raw style such that we can restore it when editing
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        pdc.set(key, type, CLEARED_SIGN_CONTENT);
    }

    @Override
    public @NotNull List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return switch (args.length) {
            case 1 -> List.of("set", "clear");
            case 2 -> List.of("1", "2", "3", "4");
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
