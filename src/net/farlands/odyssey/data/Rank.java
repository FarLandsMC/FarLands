package net.farlands.odyssey.data;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Arrays;
import java.util.List;

public enum Rank {
    /* Player Ranks */
    INITIATE("Initiate", ChatColor.GRAY,                           0,    1,  7, 0,  3),
    BARD("Bard", ChatColor.YELLOW, "story/mine_diamond",           3,    3,  6, 2,  18),
    ESQUIRE("Esquire", ChatColor.DARK_GREEN, "story/enchant_item", 24,   5,  6, 5,  15),
    KNIGHT("Knight", ChatColor.GOLD, "end/elytra",                 120,  8,  5, 10, 12),
    SAGE("Sage", ChatColor.AQUA, "adventure/adventuring_time",     240,  10, 5, 15, 9),
    ADEPT("Adept", ChatColor.GREEN, "adventure/totem_of_undying",  720,  12, 4, 20, 8),
    SCHOLAR("Scholar", ChatColor.BLUE, "nether/all_effects",       1440, 16, 3, 30, 7),
    VOTER("Voter", ChatColor.LIGHT_PURPLE,      -1,   16, 3, 30, 7), // Same as Scholar
    DONOR("Donor", ChatColor.LIGHT_PURPLE,      -1,   24, 2, 40, 6),
    PATRON("Patron", ChatColor.DARK_PURPLE,     -1,   32, 0, 50, 3),
    MEDIA("Media", ChatColor.YELLOW,            -1,   32, 0, 50, 3), // Same as Patron

    /* Staff Ranks */
    JR_BUILDER(1, "Jr. Builder", ChatColor.LIGHT_PURPLE),
    JR_MOD(1, "Jr. Mod", ChatColor.RED),
    JR_DEV(1, "Jr. Dev", ChatColor.AQUA),
    BUILDER(2, "Builder", ChatColor.DARK_PURPLE),
    MOD(2, "Mod", ChatColor.DARK_RED),
    ADMIN(3, "Admin", ChatColor.DARK_GREEN),
    DEV(3, "Dev", ChatColor.DARK_AQUA),
    OWNER(4, "Owner", ChatColor.GOLD);

    private final int permissionLevel; // 0: players, 1+: staff
    private final String symbol;
    private final ChatColor color;
    private final String advancement;
    private final int playTimeRequired; // Hours
    private final int homes;
    private final int tpDelay; // Seconds
    private final int shops;
    private final int wildCooldown; // Minutes

    public static final Rank[] VALUES = values();
    public static final List<Rank> PURCHASED_RANKS = Arrays.asList(DONOR, PATRON);
    public static final int DONOR_COST_USD = 10;
    public static final int PATRON_COST_USD = 30;
    public static final String DONOR_COST_STR = DONOR_COST_USD + " USD";
    public static final String PATRON_COST_STR = PATRON_COST_USD + " USD";

    Rank(int permissionLevel, String symbol, ChatColor color, String advancement, int playTimeRequired, int homes,
         int tpDelay, int shops, int wildCooldown) {
        this.permissionLevel = permissionLevel;
        this.symbol = symbol;
        this.color = color;
        this.advancement = advancement;
        this.playTimeRequired = playTimeRequired;
        this.homes = homes;
        this.tpDelay = tpDelay;
        this.shops = shops;
        this.wildCooldown = wildCooldown;
    }

    Rank(String symbol, ChatColor color, String advancement, int playTimeRequired, int homes, int tpDelay, int shops, int wildCooldown) {
        this(0, symbol, color, advancement, playTimeRequired, homes, tpDelay, shops, wildCooldown);
    }

    Rank(String symbol, ChatColor color, int playTimeRequired, int homes, int tpDelay, int shops, int wildCooldown) {
        this(0, symbol, color, null, playTimeRequired, homes, tpDelay, shops, wildCooldown);
    }

    Rank(int permissionLevel, String symbol, ChatColor color) {
        this(permissionLevel, symbol, color, null, -1, Integer.MAX_VALUE, 0, 60, 0);
    }

    public int specialCompareTo(Rank other) {
        // For the players, order in the enum specifies the hierarchy; for staff, only the permission level specifies the hierarchy.
        return permissionLevel == other.permissionLevel
               ? (permissionLevel == 0 ? Integer.compare(ordinal(), other.ordinal()) : 0)
               : Integer.compare(permissionLevel, other.permissionLevel);
    }

    public boolean isStaff() {
        return permissionLevel > 0;
    }

    public boolean isPlaytimeObtainable() {
        return playTimeRequired >= 0;
    }

    public boolean hasPlaytime(OfflineFLPlayer flp) {
        return playTimeRequired >= 0 && flp.getSecondsPlayed() >= playTimeRequired * 3600;
    }

    public boolean hasOP() {
        return permissionLevel > 1;
    }

    public boolean hasAfkChecks() {
        return ordinal() < JR_BUILDER.ordinal();
    }

    public int getAfkCheckInterval() {
        return hasAfkChecks() ? (ordinal() >= PATRON.ordinal() ? 30 : 15) : 0;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public String getSymbol() {
        return symbol;
    }

    public ChatColor getColor() {
        return color;
    }

    public ChatColor getNameColor() {
        return specialCompareTo(Rank.VOTER) >= 0 ? getColor() : ChatColor.WHITE;
    }

    public Advancement getAdvancement() {
        return advancement == null ? null : Bukkit.getServer().getAdvancement(NamespacedKey.minecraft(advancement));
    }

    public boolean completedAdvancement(Player player) {
        Advancement adv = getAdvancement();
        return adv == null || player.getAdvancementProgress(adv).isDone();
    }

    public boolean hasRequirements(Player player, OfflineFLPlayer flp) {
        return hasPlaytime(flp) && completedAdvancement(player);
    }

    public int getPlayTimeRequired() {
        return playTimeRequired;
    }

    public int getHomes() {
        return homes;
    }

    public int getTpDelay() {
        return tpDelay;
    }

    public int getShops() {
        return shops;
    }

    public int getWildCooldown() {
        return wildCooldown;
    }

    public Rank getNextRank() {
        return equals(VALUES[VALUES.length - 1]) ? this : VALUES[ordinal() + 1];
    }

    private String getTeamName() {
        return specialCompareTo(VOTER) >= 0 ? (char)('a' + ordinal()) + getSymbol() : "aDefault"; // Prefixes to order teams alphabetically
    }

    public Team getTeam() {
        return Bukkit.getScoreboardManager().getMainScoreboard().getTeam(getTeamName());
    }

    public static Rank getRank(CommandSender sender) {
        if(sender instanceof ConsoleCommandSender)
            return VALUES[VALUES.length - 1];
        else if(sender instanceof BlockCommandSender)
            return MOD;
        else if(sender instanceof DiscordSender) {
            OfflineFLPlayer flp = ((DiscordSender)sender).getFlp();
            return flp == null ? Rank.INITIATE : flp.getRank();
        } else
            return FarLands.getDataHandler().getOfflineFLPlayer((Player)sender).rank;
    }

    public static void createTeams() {
        final Scoreboard sc = Bukkit.getScoreboardManager().getMainScoreboard();
        sc.getTeams().forEach(Team::unregister); // Remove old teams
        Arrays.stream(VALUES).filter(rank -> rank.getTeam() == null).forEach(rank -> { // Add teams
            Team team = sc.registerNewTeam(rank.getTeamName());
            team.setColor(rank.getNameColor());
            team.setPrefix(rank.getNameColor().toString());
        });
    }
}
