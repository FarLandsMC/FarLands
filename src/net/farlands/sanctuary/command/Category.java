package net.farlands.sanctuary.command;

import com.kicasmads.cs.Utils;
import net.farlands.sanctuary.util.FLUtils;

public enum Category {
    UTILITY("commands to assist with gameplay"),
    CHAT("commands to modify or enhance in-game chat"),
    COSMETIC("commands related to decoration, design, and appearance"),
    HOMES("our /home commands"),
    INFORMATIONAL("ways to get information about our server through commands"),
    REPORTS("proposal and report commands"),
    TELEPORTING("commands related to teleporting around the world"),
    MISCELLANEOUS("commands that don't fit into a particular category"),
    PLAYER_SETTINGS_AND_INFO("Player Settings and Info", "commands for modifying your settings, or viewing other player's stats"),
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

    public String getAlias() {
        return alias == null ? FLUtils.capitalize(name()) : alias;
    }

    public String getDescription() {
        return description;
    }
}
