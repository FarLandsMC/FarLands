package net.farlands.sanctuary.command;

import net.farlands.sanctuary.util.FLUtils;

public enum Category {
    HOMES("our /home commands"),
    CLAIMS("commands related to creating and modifying claims"),
    TELEPORTING("commands related to teleporting around the world"),
    CHAT("commands to modify or enhance in-game chat"),
    PLAYER_SETTINGS_AND_INFO("Player Settings and Info", "commands for modifying your settings or viewing other player's stats"),
    UTILITY("commands to assist with gameplay"),
    REPORTS("proposal and report commands"),
    COSMETIC("commands related to decoration, design, and appearance"),
    INFORMATIONAL("ways to get information about our server through commands"),
    MISCELLANEOUS("commands that don't fit into a particular category"),
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
