package net.farlands.sanctuary.data.struct;

/**
 * Player's options for package receiving.
 */
public enum PackageToggle {
    ACCEPT,
    ASK,
    DECLINE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
