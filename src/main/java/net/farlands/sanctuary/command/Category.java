package net.farlands.sanctuary.command;

import com.kicasmads.cs.Utils;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Command categories.
 */
public enum Category implements ComponentLike {
    HOMES("Our /home commands"),
    CLAIMS("Commands related to creating and modifying claims"),
    TELEPORTING("Commands related to teleporting around the world"),
    CHAT("Commands to modify or enhance in-game chat"),
    PLAYER_SETTINGS_AND_INFO("Player Settings and Info", "Commands for modifying your settings or viewing other player's stats"),
    UTILITY("Commands to assist with gameplay"),
    REPORTS("Proposal and report commands"),
    COSMETIC("Commands related to decoration, design, and appearance"),
    INFORMATIONAL("Ways to get information about our server through commands"),
    MISCELLANEOUS("Commands that don't fit into a particular category"),
    STAFF(null);

    private final String alias;
    private final String description;

    public static final Category[] VALUES = values();

    Category(String alias, String description) {
        this.alias = alias;
        this.description = description;
    }

    Category(String description) {
        this(null, description);
    }

    public static List<Category> player() {
        return Arrays.stream(values()).filter(category -> category != STAFF).toList();
    }

    public String getAlias() {
        return alias == null ? FLUtils.capitalize(name()) : alias;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public @NotNull Component asComponent() {
        return ComponentUtils.command(
            "/help " + Utils.formattedName(this),
            ComponentColor.gold("{} - {:white}", getAlias(), ComponentColor.white(getDescription())),
            ComponentColor.gray("View commands in category")
        );
    }
}
