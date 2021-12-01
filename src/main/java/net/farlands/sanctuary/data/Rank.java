package net.farlands.sanctuary.data;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
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

/**
 * FarLands ranks.
 */
public enum Rank {
    /* Player Ranks */

    // symbol color playTimeRequired homes tpDelay shops wildCooldown
    INITIATE("Initiate", NamedTextColor.GRAY,                                        0,  1, 7,  0,  3),

    // symbol color advancement playTimeRequired totalVotesRequired homes tpDelay shops wildCooldown
    BARD    ("Bard",     NamedTextColor.YELLOW,     "story/mine_diamond",            3,  3, 6,  2, 18),
    ESQUIRE ("Esquire",  NamedTextColor.DARK_GREEN, "story/enchant_item",           12,  5, 6,  5, 15),
    KNIGHT  ("Knight",   NamedTextColor.GOLD,       "nether/get_wither_skull",      24,  8, 5, 10, 12),
    SAGE    ("Sage",     NamedTextColor.AQUA,       "end/find_end_city",            72, 10, 5, 15,  9),
    ADEPT   ("Adept",    NamedTextColor.GREEN,      "adventure/totem_of_undying",  144, 12, 4, 20,  8),
    SCHOLAR ("Scholar",  NamedTextColor.BLUE,       "adventure/adventuring_time",  240, 16, 3, 30,  7),

    // symbol color [teamColor=color] playTimeRequired homes tpDelay shops wildCooldown
    VOTER   ("Voter",    NamedTextColor.LIGHT_PURPLE,                               -1, 16, 3, 30,  7), // Same as Scholar
    BIRTHDAY("B-Day",    TextColor.color(0xde3193),             NamedTextColor.RED, -1, 16, 3, 30, 7),
    DONOR   ("Donor",    NamedTextColor.LIGHT_PURPLE,                               -1, 24, 2, 40,  6),
    PATRON  ("Patron",   NamedTextColor.DARK_PURPLE,                                -1, 32, 0, 50,  3),
    SPONSOR ("Sponsor",  TextColor.color(0x32a4ea),          NamedTextColor.BLUE,   -1, 40, 0, 50,  1),
    MEDIA   ("Media",    NamedTextColor.YELLOW,                                     -1, 40, 0, 50,  1), // Same as Sponsor

    /* Staff Ranks */

    // permissionLevel symbol color teamColor
    JR_BUILDER(1, "Jr. Builder", TextColor.color(0xbf6bff), NamedTextColor.AQUA),
    JR_MOD    (1, "Jr. Mod",     TextColor.color(0xd7493d), NamedTextColor.RED),
    JR_DEV    (1, "Jr. Dev",     TextColor.color(0x0bbd9e), NamedTextColor.DARK_AQUA),
    BUILDER   (2, "Builder",     TextColor.color(0x9000ff), NamedTextColor.BLUE),
    MOD       (2, "Mod",         TextColor.color(0xdb1100), NamedTextColor.DARK_RED),
    ADMIN     (3, "Admin",       NamedTextColor.DARK_GREEN),
    DEV       (3, "Dev",         TextColor.color(0x09816b), NamedTextColor.DARK_AQUA),
    OWNER     (4, "Owner",       NamedTextColor.GOLD);

    private final int permissionLevel; // 0: players, 1+: staff
    private final String name;
    private final TextColor color;
    private final NamedTextColor teamColor;
    private final String advancement;
    private final int playTimeRequired; // Hours
    private final int homes;
    private final int tpDelay; // Seconds
    private final int shops;
    private final int wildCooldown; // Minutes

    public static final Rank[] VALUES = values();
    public static final List<Rank> PURCHASED_RANKS = Arrays.asList(DONOR, PATRON);
    public static final int[] DONOR_RANK_COSTS = {10, 30, 60};
    public static final Rank[] DONOR_RANKS = {DONOR, PATRON, SPONSOR};

    Rank(int permissionLevel, String name, TextColor color, NamedTextColor teamColor, String advancement,
         int playTimeRequired, int homes, int tpDelay, int shops, int wildCooldown) {
        this.permissionLevel = permissionLevel;
        this.name = name;
        this.color = color;
        this.teamColor = teamColor;
        this.advancement = advancement;
        this.playTimeRequired = playTimeRequired;
        this.homes = homes;
        this.tpDelay = tpDelay;
        this.shops = shops;
        this.wildCooldown = wildCooldown;
    }

    Rank(String name, TextColor color, String advancement, int playTimeRequired, int homes, int tpDelay, int shops, int wildCooldown) {
        this(0, name, color, NamedTextColor.nearestTo(color), advancement, playTimeRequired,
                homes, tpDelay, shops, wildCooldown);
    }

    Rank(String name, TextColor color, int playTimeRequired, int homes, int tpDelay, int shops, int wildCooldown) {
        this(0, name, color, NamedTextColor.nearestTo(color), null, playTimeRequired, homes,
                tpDelay, shops, wildCooldown);
    }

    Rank(String name, TextColor color, NamedTextColor teamColor, int playTimeRequired, int homes, int tpDelay,
         int shops, int wildCooldown) {
        this(0, name, color, teamColor, null, playTimeRequired, homes, tpDelay, shops, wildCooldown);
    }

    Rank(int permissionLevel, String name, TextColor color, NamedTextColor teamColor) {
        this(permissionLevel, name, color, teamColor, null, -1, Integer.MAX_VALUE, 0, 60, 0);
    }

    Rank(int permissionLevel, String name, TextColor color) {
        this(permissionLevel, name, color, NamedTextColor.nearestTo(color), null, -1,
                Integer.MAX_VALUE, 0, 60, 0);
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
        return playTimeRequired >= 0 && flp.secondsPlayed >= (playTimeRequired - flp.totalSeasonVotes) * 3600;
    }

    public boolean hasOP() {
        return permissionLevel > 1;
    }

    public int getAfkCheckInterval() {
        if (isStaff())
            return 20;
        else if (this == PATRON || this == SPONSOR || this == MEDIA)
            return 30;
        else
            return 15;
    }

    public int getClaimBlockBonus() {
        switch (this) {
            case DONOR:
                return 15000;
            case PATRON:
                return 60000;
            case SPONSOR:
                return 100000;
            default:
                return 0;
        }
    }

    public int getPackageCooldown() {
        return this.specialCompareTo(Rank.SPONSOR) >= 0 ? 5 : 10;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public String getName() {
        return name;
    }

    /**
     * @Depricated Move to {@link Rank#color()}
     */
    @Deprecated
    public ChatColor getColor() {
        return ChatColor.of(color.asHexString());
    }

    public TextColor color() { return color; }

    public ChatColor getNameColor() {
        return specialCompareTo(Rank.VOTER) >= 0 ? getColor() : ChatColor.WHITE;
    }

    public TextColor nameColor() {
        return specialCompareTo(Rank.VOTER) >= 0 ? color() : NamedTextColor.WHITE;
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

    public static Rank getRank(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender)
            return VALUES[VALUES.length - 1];
        else if (sender instanceof BlockCommandSender)
            return MOD;
        else if (sender instanceof DiscordSender) {
            OfflineFLPlayer flp = ((DiscordSender) sender).getFlp();
            return flp == null ? Rank.INITIATE : flp.rank;
        } else
            return FarLands.getDataHandler().getOfflineFLPlayer((Player) sender).rank;
    }

    private String getTeamName() {
        return specialCompareTo(VOTER) >= 0 ? (char) ('a' + ordinal()) + getName() : "aDefault"; // Prefixes to order teams alphabetically
    }

    public Team getTeam() {
        return Bukkit.getScoreboardManager().getMainScoreboard().getTeam(getTeamName());
    }

    public Component getLabel() {
        TextComponent.Builder c = Component.text(this.name).toBuilder();
        if(isStaff()) {
            c.style(Style.style(TextDecoration.BOLD));
        }
        c.color(color());
        return c.build();
    }

    public Component colorName(String name) {
        return Component.text(name).color(nameColor());
    }

    public static void createTeams() {
        final Scoreboard sc = Bukkit.getScoreboardManager().getMainScoreboard();
        sc.getTeams().forEach(Team::unregister); // Remove old teams
        Arrays.stream(VALUES).filter(rank -> rank.getTeam() == null).forEach(rank -> { // Add teams
            Team team = sc.registerNewTeam(rank.getTeamName());
            team.color(rank.teamColor);
            team.prefix(Component.text("").color(rank.nameColor()));
        });
    }
}
