package net.farlands.sanctuary.data.struct;

public final class TabMenuConfig {
    private boolean worlds;
    private boolean lag;
    private boolean mobcaps;

    public TabMenuConfig(
        boolean worlds,
        boolean lag,
        boolean mobcaps // Staff only
    ) {
        this.worlds = worlds;
        this.lag = lag;
        this.mobcaps = mobcaps;
    }

    public TabMenuConfig() {
        this(true, false, false);
    }

    public boolean worlds() {
        return worlds;
    }

    public boolean lag() {
        return lag;
    }

    public boolean mobcaps() {
        return mobcaps;
    }

    public void worlds(boolean worlds) {
        this.worlds = worlds;
    }

    public void lag(boolean lag) {
        this.lag = lag;
    }

    public void mobcaps(boolean mobcaps) {
        this.mobcaps = mobcaps;
    }

    @Override
    public String toString() {
        return "TabMenuConfig{online=%s, lag=%s, mobcaps=%s}".formatted(worlds, lag, mobcaps);
    }
}
