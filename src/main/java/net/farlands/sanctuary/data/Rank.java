package net.farlands.sanctuary.data;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

/**
 * FarLands ranks.
 */
public enum Rank {

    /* Player Ranks */

    // symbol color playTimeRequired homes tpDelay shops wildCooldown [advancement]
    INITIATE ("Initiate", NamedTextColor.GRAY,         0,   1,  7, 0,  3                                            ),
    BARD     ("Bard",     NamedTextColor.YELLOW,       3,   3,  6, 2,  18, "story/mine_diamond"         ),
    ESQUIRE  ("Esquire",  NamedTextColor.DARK_GREEN,   12,  5,  6, 5,  15, "story/enchant_item"         ),
    KNIGHT   ("Knight",   NamedTextColor.GOLD,         24,  8,  5, 10, 12, "nether/get_wither_skull"    ),
    SAGE     ("Sage",     NamedTextColor.AQUA,         72,  10, 5, 15, 9,  "end/find_end_city"          ),
    ADEPT    ("Adept",    NamedTextColor.GREEN,        144, 12, 4, 20, 8,  "adventure/totem_of_undying" ),
    SCHOLAR  ("Scholar",  NamedTextColor.BLUE,         240, 16, 3, 30, 7,  "adventure/adventuring_time" ),

    VOTER    ("Voter",    TextColor.color(0xff6213),   -1,  16, 3, 30, 7 ), // Same as Scholar
    BIRTHDAY ("B-Day",    TextColor.color(0xde3193),   -1,  16, 3, 30, 7 ),
    DONOR    ("Donor",    NamedTextColor.LIGHT_PURPLE, -1,  24, 2, 40, 6 ),
    PATRON   ("Patron",   NamedTextColor.DARK_PURPLE,  -1,  32, 0, 50, 3 ),
    SPONSOR  ("Sponsor",  TextColor.color(0x32a4ea),   -1,  40, 0, 50, 1 ),
    MEDIA    ("Media",    NamedTextColor.YELLOW,       -1,  40, 0, 50, 1 ), // Same as Sponsor

    /* Staff Ranks */

    // permissionLevel symbol color
    JR_BUILDER (1, "Jr. Builder", TextColor.color(0xbf6bff) ),
    JR_MOD     (1, "Jr. Mod",     TextColor.color(0xd7493d) ),
    JR_DEV     (1, "Jr. Dev",     TextColor.color(0x0bbd9e) ),
    BUILDER    (2, "Builder",     TextColor.color(0x9000ff) ),
    MOD        (2, "Mod",         TextColor.color(0xdb1100) ),
    ADMIN      (3, "Admin",       NamedTextColor.DARK_GREEN ),
    DEV        (3, "Dev",         TextColor.color(0x09816b) ),
    OWNER      (4, "Owner",       NamedTextColor.GOLD       );

    private final int permissionLevel; // 0: players, 1+: staff
    private final String name;
    private final TextColor color;
    private final NamedTextColor teamColor; // Determined from color
    private final String advancement;
    private final int playTimeRequired; // Hours
    private final int homes;
    private final int tpDelay; // Seconds
    private final int shops;
    private final int wildCooldown; // Minutes

    private final Component label;

    public static final Rank[] VALUES = values();
    public static final int[] DONOR_RANK_COSTS = { 10, 30, 60 };
    public static final Rank[] DONOR_RANKS = { DONOR, PATRON, SPONSOR };

    Rank(int permissionLevel, String name, TextColor color, String advancement, int playTimeRequired, int homes, int tpDelay, int shops, int wildCooldown) {
        this.permissionLevel = permissionLevel;
        this.name = name;
        this.color = color;
        this.teamColor = NamedTextColor.nearestTo(color);
        this.advancement = advancement;
        this.playTimeRequired = playTimeRequired;
        this.homes = homes;
        this.tpDelay = tpDelay;
        this.shops = shops;
        this.wildCooldown = wildCooldown;

        Component c = Component.text(this.name).color(this.color);
        this.label = isStaff() ? c.decorate(TextDecoration.BOLD) : c;
    }

    Rank(String name, TextColor color, int playTimeRequired, int homes, int tpDelay, int shops, int wildCooldown, String advancement) {
        this(0, name, color, advancement, playTimeRequired, homes, tpDelay, shops, wildCooldown);
    }

    Rank(String name, TextColor color, int playTimeRequired, int homes, int tpDelay, int shops, int wildCooldown) {
        this(0, name, color, null, playTimeRequired, homes, tpDelay, shops, wildCooldown);
    }

    Rank(int permissionLevel, String name, TextColor color) {
        this(permissionLevel, name, color, null, -1, Integer.MAX_VALUE, 0, 60, 0);
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
        // 30 minutes for patron+, otherwise 15
        return this.specialCompareTo(Rank.PATRON) >= 0 ? 30 : 15;
    }

    public int getClaimBlockBonus() {
        return switch (this) {
            case DONOR -> 15000;
            case PATRON -> 60000;
            case SPONSOR -> 100000;
            default -> 0;
        };
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
        if (sender instanceof ConsoleCommandSender) {
            return VALUES[VALUES.length - 1]; // Highest possible perms
        } else if (sender instanceof BlockCommandSender) {
            return MOD; // Command Blocks ~ Moderators
        }
        return FarLands.getDataHandler().getOfflineFLPlayer(sender).rank;
    }

    private String getTeamName() {
        return specialCompareTo(VOTER) >= 0 ? (char) ('a' + ordinal()) + getName() : "aDefault"; // Prefixes to order teams alphabetically
    }

    public Team getTeam() {
        return Bukkit.getScoreboardManager().getMainScoreboard().getTeam(getTeamName());
    }

    public Component getLabel() {
        return this.label;
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
