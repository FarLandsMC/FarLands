package net.farlands.sanctuary.data.struct;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.ChatHandler;
import net.farlands.sanctuary.command.player.CommandHomes;
import net.farlands.sanctuary.command.player.CommandStats;
import net.farlands.sanctuary.command.player.CommandTimeZone;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.DiscordHandler;
import net.farlands.sanctuary.discord.MarkdownProcessor;
import net.farlands.sanctuary.mechanic.GeneralMechanics;
import net.farlands.sanctuary.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * All data related to a FarLands player.
 */
public class OfflineFLPlayer implements ComponentLike {
    public UUID uuid;

    public String lastIP;
    public Component nickname;
    public String username;
    public String timezone;
    public List<String> formerUsernames;

    public long discordID;
    public long lastLogin;

    public long ptime;
    public boolean pweather;

    public int bonusClaimBlocksReceived;
    public int totalVotes;
    public int totalSeasonVotes;
    public int monthVotes;
    public int votesToday;
    public int voteRewards;
    public int secondsPlayed;
    public int deaths;
    public int elytrasObtained;

    public double amountDonated;

    public boolean acceptVoteRewards;
    public boolean censoring;
    public boolean debugging;
    public boolean flightPreference;
    public boolean fireworkLaunch;
    public boolean god;
    public boolean pvp;
    public boolean topVoter;
    public boolean vanished;
    public boolean viewedPatchnotes;

    public Birthday birthday;
    public NamedTextColor  staffChatColor;
    public LocationWrapper lastLocation;
    public Mute            currentMute;
    public PackageToggle packageToggle;
    public Particles particles;
    public Pronouns pronouns;
    public Rank rank;
    public CommandHomes.SortType homesSort;

    public Map<UUID, IgnoreStatus> ignoreStatusMap;
    public Map<String, ShareHome> pendingSharehomes;
    public List<Home> homes;
    public List<MailMessage> mail;
    public List<Punishment> punishments;
    public List<String> notes;

    transient public int shops;

    public OfflineFLPlayer(UUID uuid, String username) {
        this.uuid = uuid;

        this.lastIP = "";
        this.nickname = null;
        this.username = username;
        this.formerUsernames = new ArrayList<>();

        this.timezone = "";

        this.discordID = 0;
        this.lastLogin = System.currentTimeMillis();

        this.ptime = -1;
        this.pweather = false;

        this.bonusClaimBlocksReceived = 0;
        this.totalVotes = 0;
        this.totalSeasonVotes = 0;
        this.monthVotes = 0;
        this.votesToday = 0;
        this.voteRewards = 0;
        this.secondsPlayed = 0;
        this.shops = -1;

        this.amountDonated = 0;

        this.acceptVoteRewards = true;
        this.censoring = false;
        this.debugging = false;
        this.flightPreference = false;
        this.fireworkLaunch = true;
        this.god = false;
        this.pvp = false;
        this.topVoter = false;
        this.vanished = false;
        this.viewedPatchnotes = true;

        this.birthday = null;
        this.staffChatColor = NamedTextColor.RED;
        this.lastLocation = null;
        this.currentMute = null;
        this.packageToggle = PackageToggle.ACCEPT;
        this.particles = null;
        this.pronouns = null;
        this.rank = Rank.INITIATE;
        this.homesSort = CommandHomes.SortType.ALPHABET;

        this.ignoreStatusMap = new HashMap<>();
        this.notes = new ArrayList<>();
        this.punishments = new ArrayList<>();
        this.homes = new ArrayList<>();
        this.pendingSharehomes = new HashMap<>();
        this.mail = new ArrayList<>();

    }

    @SuppressWarnings("unused")
    OfflineFLPlayer() { // No-Args constructor for Moshi
        this(null, null);
    }

    /*
     * WARNING: DO NOT PUT ANY CALLS TO THE FARLANDS SCHEDULER IN THESE UPDATE METHODS OR IT WILL CAUSE SERVER CRASHES
     */

    public synchronized void updateAll(boolean sendMessages) {
        updateDiscord();

        FLPlayerSession session = getSession();
        if (session != null)
            session.update(sendMessages);
    }

    public synchronized void updateDiscord() {
        if (!isDiscordVerified() || !FarLands.getDiscordHandler().isActive())
            return;

        final DiscordHandler dh = FarLands.getDiscordHandler();
        final Guild guild = dh.getGuild();
        guild.retrieveMemberById(discordID).queue(member -> {
            if (member == null || member.isOwner())
                return;

            boolean nickIsUser = username.equals(member.getNickname());
            boolean pronounsEnabled = pronouns != null && pronouns.showOnDiscord();
            boolean nickIsUserPronouns = pronounsEnabled && (username + " " + pronouns).equals(member.getNickname());

            if ((!nickIsUser && !pronounsEnabled) || (pronounsEnabled && !nickIsUserPronouns))
                member.modifyNickname(username + (pronounsEnabled ? " " + pronouns.toString() : "")).queue();

            List<Role> roles = new ArrayList<>();
            roles.add(rank.isStaff() ? dh.getRole(DiscordHandler.STAFF_ROLE) : dh.getRole(DiscordHandler.VERIFIED_ROLE));
            if (rank.specialCompareTo(Rank.DONOR) >= 0)
                roles.add(dh.getRole(rank.getName()));

            if (roles.stream().anyMatch(Objects::isNull))
                return;

            if (!member.getRoles().containsAll(roles)) {
                guild.modifyMemberRoles(member, roles).queue();
            }
        });
    }

    public Player getOnlinePlayer() {
        return Bukkit.getServer().getPlayer(uuid);
    }

    public FLPlayerSession getSession() {
        Player player = getOnlinePlayer();
        return player == null ? null : FarLands.getDataHandler().getSession(player);
    }

    public boolean isOnline() {
        return getOnlinePlayer() != null;
    }

    public boolean updateSessionIfOnline(boolean sendMessages) {
        Player player = getOnlinePlayer();
        if (player != null) {
            FarLands.getDataHandler().getSession(player).update(sendMessages);
            return true;
        }
        return false;
    }

    public void setDiscordID(long discordID) {
        FarLands.getDataHandler().updateDiscordMap(this.discordID, discordID, this);
        this.discordID = discordID;
    }

    public boolean isDiscordVerified() {
        return discordID != 0;
    }

    public long getLastLogin() {
        return isOnline() && !vanished ? System.currentTimeMillis() : lastLogin;
    }

    public Component getDisplayName() {
        return nickname == null || PlainTextComponentSerializer.plainText().serialize(nickname).isBlank()
            ? Component.text(username).color(rank.nameColor()) : nickname;
    }

    /**
     * @param displayName If it should use {@link OfflineFLPlayer#getDisplayName} or the username
     * @return The player's full display name, including display rank
     */
    public Component getFullDisplayName(boolean displayName) {
        return Component.empty()
            .append(this.getDisplayRank())
            .append(Component.space())
            .append(displayName ? this.getDisplayName() : this.rank.colorName(this.username));
    }

    public void addVote() {
        ++totalVotes;
        ++totalSeasonVotes;
        ++monthVotes;

        ++this.votesToday;

        if (!acceptVoteRewards) {
            voteRewards = 0;
            return;
        }

        FLPlayerSession session = getSession();
        if (session != null) {
            session.giveVoteRewards(1);
            session.player.sendMessage(ComponentColor.gold("Receiving 1 vote reward!"));
        } else
            ++voteRewards;
    }

    public void addShop() {
        ++shops;
    }

    public boolean canAddShop() {
        return shops < rank.getShops();
    }

    public void removeShop() {
        if (shops > 0)
            --shops;
    }

    public boolean hasParticles() {
        return particles != null && rank.specialCompareTo(Rank.DONOR) >= 0;
    }

    public void setParticles(Particle type, Particles.ParticleLocation location) {
        if (type == null || location == null)
            particles = null;
        else {
            if (particles == null)
                particles = new Particles(type, location);
            else
                particles.setTypeAndLocation(type, location);
        }
    }

    public Rank getDisplayRank() {
        if (rank.isStaff())
            return rank;

        if (birthday != null && birthday.isToday())
            return Rank.BIRTHDAY;
        else if (topVoter)
            return Rank.VOTER;
        else
            return rank;
    }

    public void setRank(Rank rank) {
        Player player = getOnlinePlayer();
        boolean online = player != null;
        if (rank.specialCompareTo(this.rank) > 0) {
            Logging.broadcastIngame(
                ComponentColor.gold(" ** ") // " ** <username> has ranked up to <rank> ** "
                    .append(ComponentColor.green(username))
                    .append(ComponentColor.gold(" has ranked up to "))
                    .append(rank)
                    .append(ComponentColor.gold(" ** ")),
                false
            );
            FarLands.getDiscordHandler().sendMessageEmbed(
                DiscordChannel.IN_GAME,
                new EmbedBuilder()
                    .setTitle(MarkdownProcessor.escapeMarkdown(" ** " + username + " has ranked up to " + rank.getName() + " ** "))
                    .setColor(rank.color().value())
            );
            if (online)
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5.0F, 1.0F);
        }
        this.rank = rank;
        if (!updateSessionIfOnline(false))
            updateDiscord();
    }

    public Location getLastLocation() {
        return this.lastLocation.asLocation();
    }

    public void setLastLocation(Location location) {
        this.lastLocation = new LocationWrapper(location);
    }

    public void setLastLocation(UUID world, double x, double y, double z, float yaw, float pitch) {
        this.lastLocation = new LocationWrapper(world, x, y, z, yaw, pitch);
    }

    public List<String> getIgnoreList() {
        return ignoreStatusMap.keySet()
                .stream()
                .map(uuid -> FarLands.getDataHandler().getOfflineFLPlayer(uuid).username)
                .collect(Collectors.toList());
    }

    public IgnoreStatus getIgnoreStatus(CommandSender sender) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        return flp == null ? IgnoreStatus.NONE : getIgnoreStatus(flp);
    }

    public IgnoreStatus getIgnoreStatus(OfflineFLPlayer flp) {
        return ignoreStatusMap.getOrDefault(flp.uuid, IgnoreStatus.NONE);
    }

    public void updateIgnoreStatus(UUID player, IgnoreStatus.IgnoreType type, boolean value) {
        IgnoreStatus status = ignoreStatusMap.get(player);

        if (status == null) {
            if (value) {
                status = new IgnoreStatus();
                ignoreStatusMap.put(player, status);
            } else
                return;
        }

        status.set(type, value);
        if (status.includesNone())
            ignoreStatusMap.remove(player);
    }

    public boolean isMuted() {
        return currentMute != null && !currentMute.hasExpired();
    }

    public long punish(Punishment.PunishmentType type, String message) { // Returns time remaining
        Punishment p = new Punishment(type, message);
        List<Punishment> validPunishments = punishments.stream().filter(Punishment::isNotPardoned).collect(Collectors.toList());
        if (validPunishments.size() >= 4 || validPunishments.stream().map(Punishment::getType).anyMatch(Punishment.PunishmentType::isPermanent))
            return -1L; // No use for further punishing, this player is permanently banned
        punishments.add(p);
        validPunishments = punishments.stream().filter(Punishment::isNotPardoned).collect(Collectors.toList());
        Player player = getOnlinePlayer();
        if (player != null)
            player.kickPlayer(p.generateBanMessage(validPunishments.size() - 1, true));
        this.moveToSpawn();
        GeneralMechanics.recentlyPunished.add(this);
        return p.totalTime(validPunishments.size() - 1);
    }

    public boolean pardon(Punishment.PunishmentType type) {
        Punishment punishment = null;
        for (int i = punishments.size() - 1; i >= 0; --i) {
            if (type.equals(punishments.get(i).getType()) && punishments.get(i).isNotPardoned())
                punishment = punishments.get(i);
        }
        return punishment != null && punishment.pardon();
    }

    public boolean removePunishment(Punishment.PunishmentType type) {
        Punishment punishment = null;
        for (int i = punishments.size() - 1; i >= 0; --i) {
            if (type.equals(punishments.get(i).getType()))
                punishment = punishments.get(i);
        }
        return punishments.remove(punishment);
    }

    public Punishment getCurrentPunishment() {
        for (int i = punishments.size() - 1; i >= 0; --i) {
            if (punishments.get(i).isActive(i) && punishments.get(i).isNotPardoned())
                return punishments.get(i);
        }
        return null;
    }

    public String getCurrentPunishmentMessage() {
        Punishment cp = getCurrentPunishment();
        List<Punishment> validPunishments = punishments.stream().filter(Punishment::isNotPardoned).collect(Collectors.toList());
        return cp == null ? null : cp.generateBanMessage(validPunishments.indexOf(cp), false);
    }

    public Punishment getMostRecentPunishment() {
        List<Punishment> validPunishments = punishments.stream().filter(Punishment::isNotPardoned).collect(Collectors.toList());
        return validPunishments.isEmpty() ? null :
                validPunishments.get(validPunishments.size() - 1);
    }

    public Punishment getMostRecentPunishmentAll() {
        return punishments.isEmpty() ? null : punishments.get(punishments.size() - 1);
    }

    public Punishment getTopPunishment() {
        return isBanned() ? getCurrentPunishment() : getMostRecentPunishment();
    }

    public boolean isBanned() {
        for (int i = 0; i < punishments.size(); ++i) {
            if (punishments.get(i).isActive(i) && punishments.get(i).isNotPardoned())
                return true;
        }
        return false;
    }

    public boolean hasHome(String name) {
        return homes.stream().map(Home::getName).anyMatch(name::equals);
    }

    public int numHomes() {
        return homes.size();
    }

    public Location getHome(String name) {
        Home home = homes.stream().filter(h -> name.equals(h.getName())).findAny().orElse(null);
        if (home == null)
            home = homes.stream().filter(h -> name.equalsIgnoreCase(h.getName())).findAny().orElse(null);
        return home == null ? null : home.getLocation();
    }

    public void addHome(String name, Location loc) {
        homes.add(new Home(name, loc));
    }

    public void moveHome(String name, Location loc) {
        removeHome(name);
        addHome(name, loc);
    }

    public void removeHome(String name) {
        homes.removeIf(home -> name.equals(home.getName()));
    }

    public void renameHome(String oldName, String newName) {
        Home home = homes.stream().filter(h -> oldName.equals(h.getName())).findAny().orElse(null);
        home.setName(newName);
    }

    public boolean canAddHome() {
        return homes.size() < rank.getHomes();
    }

    public boolean addSharehome(String sender, ShareHome shareHome) {
        if (pendingSharehomes.containsKey(sender)) {
            return false;
        }

        Player player = getOnlinePlayer();
        if (player != null) { // If the player is online, notify
            player.sendMessage(
                ComponentColor.gold("")
                    .append(ComponentColor.aqua(sender))
                    .append(Component.text(" has sent you a home: "))
                    .append(ComponentColor.aqua(shareHome.home().getName()))
                    .append(shareHome.message() == null ? Component.empty() : Component.text("\nMessage: " + shareHome.message()))
                    .append(Component.text("\nYou can accept it with "))
                    .append(ComponentUtils.command("/sharehome accept " + sender))
                    .append(Component.text(" or decline it with "))
                    .append(ComponentUtils.command("/sharehome decline " + sender))
                    .append(Component.text("."))
            );
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
        }

        pendingSharehomes.put(sender, shareHome);
        return true;
    }

    public boolean removeShareHome(String sender) {
        return pendingSharehomes.remove(sender) != null;
    }

    public void addMail(UUID sender, Component message) {
        mail.add(new MailMessage(sender, message));

        Player player = getOnlinePlayer();
        if (player != null) {
            player.sendMessage(MailMessage.UNREAD_MAIL);
            this.getSession().addAFKMessage(MailMessage.UNREAD_MAIL);
        }
    }

    public void setTimezone(String tz) {
        this.timezone = tz;
    }

    /**
     * Gets the formatted version of the player's current time
     *
     * @return the formatted time, null if no timezone set
     */
    public String currentTime() {
        return this.currentTime(null);
    }

    /**
     * Gets the formatted version of the player's current time
     *
     * @param sender The sender to format for (localisation) -- if not specified, will use system default
     * @return the formatted time, null if no timezone set
     */
    public String currentTime(CommandSender sender) {
        if (this.timezone == null || this.timezone.isEmpty()) return null;
        TimeZone tz = TimeZone.getTimeZone(this.timezone);

        return CommandTimeZone.getTime(tz, sender);
    }

    public void moveToSpawn() {
        LocationWrapper spawn = FarLands.getDataHandler().getPluginData().spawn;
        if(spawn == null) return;
        Player player = getOnlinePlayer();
        if (player != null) // If the player is online, teleport them to spawn
            player.teleport(spawn.asLocation());
        else // Otherwise, move their last location
            lastLocation = spawn;
    }

    public void giveCollectables(Rank fromRank, Rank toRank) {
        String[] collectables = { "donorCollectable", "patronCollectable", "sponsorCollectable" };

        int toRankI;
        if (toRank.specialCompareTo(Rank.SPONSOR) >= 0) {
            toRankI = 2;
        } else {
            toRankI = switch (toRank) {
                case DONOR -> 0;
                case PATRON -> 1;
                case SPONSOR -> 2;
                default -> -1;
            };
        }

        int fromRankI;
        if (fromRank.specialCompareTo(Rank.SPONSOR) >= 0) {
            fromRankI = 2;
        } else {
            fromRankI = switch (fromRank) {
                case DONOR -> 0;
                case PATRON -> 1;
                case SPONSOR -> 2;
                default -> -1;
            };
        }

        Arrays.stream(collectables).toList().subList(fromRankI + 1, toRankI + 1).forEach(item -> {
            if (this.isOnline()) {
                FLUtils.giveItem(this.getOnlinePlayer(), FarLands.getDataHandler().getItem(item), false);
            } else {
                FarLands.getDataHandler().addPackage(
                    this.uuid,
                    new Package(
                        null,
                        "FarLands Staff",
                        FarLands.getDataHandler().getItem(item),
                        null,
                        true
                    ));
            }

        });
    }

    public void updateDeaths() {
        deaths = Bukkit.getOfflinePlayer(uuid).getStatistic(Statistic.DEATHS);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof OfflineFLPlayer && uuid.equals(((OfflineFLPlayer) other).uuid);
    }

    @Override
    public String toString() {
        return username;
    }

    public void chat(String message) {
        ChatHandler.chat(this, ChatHandler.handleReplacements(message, this));
    }

    public void chat(String prefix, String message) {
        ChatHandler.chat(this, Component.text(prefix), ChatHandler.handleReplacements(message, this));
    }

    public UserSnowflake discordUser() {
        return UserSnowflake.fromId(this.discordID);
    }

    public void unverifyDiscord() {
        if (this.isDiscordVerified()) {
            DiscordHandler dh = FarLands.getDiscordHandler();
            dh.getGuild().removeRoleFromMember(this.discordUser(), dh.getRole(DiscordHandler.VERIFIED_ROLE)).queue();
            dh.getGuild().removeRoleFromMember(this.discordUser(), dh.getRole(DiscordHandler.STAFF_ROLE)).queue();
            if (this.rank.specialCompareTo(Rank.DONOR) > 0) {
                dh.getGuild().removeRoleFromMember(this.discordUser(), dh.getRole(this.rank.getName())).queue();
            }
        }
    }

    @Override
    public @NotNull Component asComponent() {
        return Component.text(this.username)
            .color(this.rank.nameColor())
            .hoverEvent(HoverEvent.showText(CommandStats.getFormattedStats(this, null, false)));
    }
}
