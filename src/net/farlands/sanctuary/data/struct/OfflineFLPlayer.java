package net.farlands.sanctuary.data.struct;

import com.google.common.collect.ImmutableMap;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.discord.DiscordHandler;
import net.farlands.sanctuary.util.LocationWrapper;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class OfflineFLPlayer {
    public UUID uuid;

    public String lastIP;
    public String nickname;
    public String username;

    public long discordID;
    public long lastLogin;

    public long ptime;
    public boolean pweather;

    public int bonusClaimBlocksReceived;
    public int totalVotes;
    public int totalSeasonVotes;
    public int monthVotes;
    public int voteRewards;
    public int secondsPlayed;
    public int shops;

    public double amountDonated;

    public boolean acceptVoteRewards;
    public boolean censoring;
    public boolean debugging;
    public boolean flightPreference;
    public boolean god;
    public boolean pvp;
    public boolean topVoter;
    public boolean vanished;
    public boolean viewedPatchnotes;

    public Birthday        birthday;
    public ChatColor       staffChatColor;
    public LocationWrapper lastLocation;
    public Mute            currentMute;
    public PackageToggle   packageToggle;
    public Particles       particles;
    public Rank            rank;

    public Set<UUID>         ignoredPlayers;
    public List<Home>        homes;
    public List<MailMessage> mail;
    public List<Punishment>  punishments;
    public List<String>      notes;

    public static final Map<String, List<String>> SQL_SER_INFO = (new ImmutableMap.Builder<String, List<String>>())
            .put("constants", Arrays.asList("uuid", "username"))
            .put("objects", Arrays.asList("particles", "lastLocation", "currentMute"))
            .put("ignored", Arrays.asList("punishments", "homes", "mail", "notes", "staffChatColor", "totalSeasonVotes"))
            .build();

    public OfflineFLPlayer(UUID uuid, String username) {
        this.uuid = uuid;

        this.lastIP = "";
        this.nickname = "";
        this.username = username;

        this.discordID = 0;
        this.lastLogin = System.currentTimeMillis();

        this.ptime = -1;
        this.pweather = false;

        this.bonusClaimBlocksReceived = 0;
        this.totalVotes = 0;
        this.totalSeasonVotes = 0;
        this.monthVotes = 0;
        this.voteRewards = 0;
        this.secondsPlayed = 0;
        this.shops = 0;

        this.amountDonated = 0;

        this.acceptVoteRewards = true;
        this.censoring = false;
        this.debugging = false;
        this.flightPreference = false;
        this.god = false;
        this.pvp = false;
        this.topVoter = false;
        this.vanished = false;
        this.viewedPatchnotes = true;

        this.birthday = null;
        this.staffChatColor = ChatColor.RED;
        this.lastLocation = null;
        this.currentMute = null;
        this.packageToggle = PackageToggle.ACCEPT;
        this.particles = null;
        this.rank = Rank.INITIATE;

        this.ignoredPlayers = new HashSet<>();
        this.notes = new ArrayList<>();
        this.punishments = new ArrayList<>();
        this.homes = new ArrayList<>();
        this.mail = new ArrayList<>();
    }

    OfflineFLPlayer() { // No-Args ctr for GSON
        this(null, null);
    }

    /*
    * WARNING: DO NOT PUT ANY CALLS TO THE FARLANDS SCHEDULER IN THESE UPDATE METHODS OR IT WILL CAUSE SERVER CRASHES
    */

    public synchronized void updateAll(boolean sendMessages) {
        updateDiscord();

        FLPlayerSession session = getSession();
        if (session == null)
            update();
        else
            session.update(sendMessages);
    }

    public synchronized void update() {
        if (currentMute != null && currentMute.hasExpired()) {
            currentMute = null;
            if (isOnline())
                getOnlinePlayer().sendMessage(ChatColor.GREEN + "Your mute has expired.");
        }
    }

    public synchronized void updateDiscord() {
        if (!isDiscordVerified() || !FarLands.getDiscordHandler().isActive())
            return;

        final DiscordHandler dh = FarLands.getDiscordHandler();
        final Guild guild = dh.getGuild();
        guild.retrieveMemberById(discordID).queue(member -> {
            if (member == null || member.isOwner())
                return;

            if (!username.equals(member.getNickname()))
                member.modifyNickname(username).queue();

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

    public String getDisplayName() {
        return nickname == null || nickname.isEmpty() ? username : nickname;
    }

    public void addVote() {
        ++totalVotes;
        ++totalSeasonVotes;
        ++monthVotes;

        if (!acceptVoteRewards) {
            voteRewards = 0;
            return;
        }

        FLPlayerSession session = getSession();
        if (session != null) {
            session.giveVoteRewards(1);
            session.player.sendMessage(ChatColor.GOLD + "Receiving 1 vote reward!");
        } else
            ++voteRewards;
    }

    public void clearMonthVotes() {
        monthVotes = 0;
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
            Logging.broadcast(ChatColor.GOLD + " ** " + ChatColor.GREEN + username + ChatColor.GOLD +
                    " has ranked up to " + rank.getColor() + rank.getName() + ChatColor.GOLD + " ** ", true);
            if (online)
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5.0F, 1.0F);
        }
        this.rank = rank;
        if (!updateSessionIfOnline(false))
            updateDiscord();
    }

    public Location getLastLocation() {
        return lastLocation.asLocation();
    }

    public void setLastLocation(Location location) {
        lastLocation = new LocationWrapper(location);
    }

    public void setLastLocation(UUID world, double x, double y, double z, float yaw, float pitch) {
        lastLocation = new LocationWrapper(world, x, y, z, yaw, pitch);
    }

    public List<String> getIgnoreList() {
        return ignoredPlayers.stream().map(uuid -> FarLands.getDataHandler().getOfflineFLPlayer(uuid).username)
                .collect(Collectors.toList());
    }

    public boolean isIgnoring(CommandSender sender) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);
        return flp != null && ignoredPlayers.contains(flp.uuid);
    }

    public boolean isIgnoring(OfflineFLPlayer flp) {
        return ignoredPlayers.contains(flp.uuid);
    }

    public boolean setIgnoring(UUID player, boolean value) {
        if(value)
            return ignoredPlayers.add(player);
        else
            return ignoredPlayers.remove(player);
    }

    public boolean isMuted() {
        return currentMute != null && !currentMute.hasExpired();
    }

    public long punish(Punishment.PunishmentType type, String message) { // Returns time remaining
        Punishment p = new Punishment(type, message);
        if (punishments.size() >= 4 || punishments.stream().map(Punishment::getType).anyMatch(Punishment.PunishmentType::isPermanent))
            return -1L; // No use for further punishing, this player is permanently banned
        punishments.add(p);
        Player player = getOnlinePlayer();
        if (player != null)
            player.kickPlayer(p.generateBanMessage(punishments.size() - 1, true));
        LocationWrapper spawn = FarLands.getDataHandler().getPluginData().spawn;
        if (spawn != null) { // Make sure spawn is set
            if (player != null)
                FLUtils.tpPlayer(player, spawn.asLocation());
            else
                lastLocation = spawn;
        }
        return p.totalTime(punishments.size() - 1);
    }

    public boolean pardon(Punishment.PunishmentType type) {
        Punishment punishment = null;
        for (int i = punishments.size() - 1; i >= 0; --i) {
            if (type.equals(punishments.get(i).getType()))
                punishment = punishments.get(i);
        }
        return punishments.remove(punishment);
    }

    public Punishment getCurrentPunishment() {
        for (int i = punishments.size() - 1; i >= 0; --i) {
            if (punishments.get(i).isActive(i))
                return punishments.get(i);
        }
        return null;
    }

    public String getCurrentPunishmentMessage() {
        Punishment cp = getCurrentPunishment();
        return cp == null ? null : cp.generateBanMessage(punishments.indexOf(cp), false);
    }

    public Punishment getMostRecentPunishment() {
        return punishments.isEmpty() ? null : punishments.get(punishments.size() - 1);
    }

    public Punishment getTopPunishment() {
        return isBanned() ? getCurrentPunishment() : getMostRecentPunishment();
    }

    public boolean isBanned() {
        for (int i = 0; i < punishments.size(); ++i) {
            if (punishments.get(i).isActive(i))
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
            home = homes.stream().filter(h -> name.toLowerCase().equals(h.getName().toLowerCase())).findAny().orElse(null);
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

    public void addMail(String sender, String message) {
        mail.add(new MailMessage(sender, message));

        Player player = getOnlinePlayer();
        if (player != null) // Notify the player if online
            sendFormatted(player, "&(gold)You have mail. Read it with $(hovercmd,/mail read," +
                    "{&(gray)Click to Run},&(yellow)/mail read)");
    }

    public void clearMail() {
        mail.clear();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof OfflineFLPlayer && uuid.equals(((OfflineFLPlayer)other).uuid);
    }

    @Override
    public String toString() {
        return username;
    }
}
