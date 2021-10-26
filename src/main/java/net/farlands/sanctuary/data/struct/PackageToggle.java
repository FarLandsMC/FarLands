package net.farlands.sanctuary.data.struct;

public enum PackageToggle {
    ACCEPT,
    ASK,
    DECLINE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
