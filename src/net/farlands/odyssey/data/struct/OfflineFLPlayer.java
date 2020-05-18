package net.farlands.odyssey.data.struct;

import com.google.common.collect.ImmutableMap;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.discord.DiscordHandler;
import net.farlands.odyssey.util.LocationWrapper;
import net.farlands.odyssey.util.Logging;
import net.farlands.odyssey.util.FLUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class OfflineFLPlayer {
    public UUID uuid;
    public String username;
    public long discordID;
    public long lastLogin;
    public String nickname;
    public String lastIP;
    public int secondsPlayed;
    public int totalVotes;
    public int monthVotes;
    public int voteRewards;
    public int amountDonated;
    public int shops;
    public boolean flightPreference;
    public boolean god;
    public boolean vanished;
    public boolean censoring;
    public boolean pvp;
    public boolean topVoter;
    public boolean viewedPatchnotes;
    public boolean debugging;
    public Particles particles;
    public Rank rank;
    public LocationWrapper lastLocation;
    public Mute currentMute;
    public List<String> notes;
    public List<Punishment> punishments;
    public Set<UUID> ignoredPlayers;
    public List<Home> homes;
    public List<MailMessage> mail;

    public static final Map<String, List<String>> SQL_SER_INFO = (new ImmutableMap.Builder<String, List<String>>())
            .put("constants", Arrays.asList("uuid", "username"))
            .put("objects", Arrays.asList("particles", "lastLocation", "currentMute"))
            .put("ignored", Arrays.asList("punishments", "homes", "mail", "notes"))
            .build();

    public OfflineFLPlayer(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.discordID = 0;
        this.lastLogin = System.currentTimeMillis();
        this.nickname = "";
        this.lastIP = "";
        this.secondsPlayed = 0;
        this.totalVotes = 0;
        this.monthVotes = 0;
        this.voteRewards = 0;
        this.amountDonated = 0;
        this.shops = 0;
        this.flightPreference = false;
        this.god = false;
        this.vanished = false;
        this.censoring = false;
        this.pvp = false;
        this.topVoter = false;
        this.viewedPatchnotes = true;
        this.debugging = false;
        this.particles = null;
        this.rank = Rank.INITIATE;
        this.lastLocation = FLUtils.LOC_ZERO;
        this.currentMute = null;
        this.notes = new ArrayList<>();
        this.punishments = new ArrayList<>();
        this.ignoredPlayers = new HashSet<>();
        this.homes = new ArrayList<>();
        this.mail = new ArrayList<>();
    }

    OfflineFLPlayer() { // No-Args ctr for GSON
        this(null, null);
    }

    public synchronized void update() {
        if(currentMute != null && currentMute.hasExpired()) {
            currentMute = null;
            if(isOnline())
                getOnlinePlayer().sendMessage(ChatColor.GREEN + "Your mute has expired.");
        }
        updateDiscord();
    }

    public synchronized void updateDiscord() {
        if(!isDiscordVerified() || !FarLands.getDiscordHandler().isActive())
            return;
        DiscordHandler dh = FarLands.getDiscordHandler();
        Guild guild = dh.getGuild();
        Member member;
        try {
            member = guild.getMember(dh.getNativeBot().getUserById(discordID));
        }catch(NullPointerException ex) {
            discordID = 0;
            return;
        }
        if(member.isOwner()) // For Koneko :P
            return;
        if(!username.equals(member.getNickname()))
            guild.getController().setNickname(member, username).queue();
        List<Role> roles = new ArrayList<>();
        roles.add(rank.isStaff() ? dh.getRole(DiscordHandler.STAFF_ROLE) : dh.getRole(DiscordHandler.VERIFIED_ROLE));
        if(rank.specialCompareTo(Rank.DONOR) >= 0)
            roles.add(dh.getRole(rank.getSymbol()));
        if(!member.getRoles().containsAll(roles)) {
            List<Role> add = new ArrayList<>(), remove = new ArrayList<>();
            roles.stream().filter(role -> !member.getRoles().contains(role)).forEach(add::add);
            member.getRoles().stream().filter(role -> !roles.contains(role) && DiscordHandler.isManagedRole(role)).forEach(remove::add);
            guild.getController().modifyMemberRoles(member, add, remove).queue();
        }
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

    public void updateSessionIfOnline(boolean sendMessages) {
        Player player = getOnlinePlayer();
        if(player != null)
            FarLands.getDataHandler().getSession(player).update(sendMessages);
    }

    public void setDiscordID(long discordID) {
        this.discordID = discordID;
        FarLands.getDataHandler().updateDiscordMap(discordID, this);
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
        ++ totalVotes;
        ++ monthVotes;
        FLPlayerSession session = getSession();
        if(session != null) {
            session.giveVoteRewards(1);
            session.player.sendMessage(ChatColor.GOLD + "Receiving 1 vote reward!");
        }else
            ++ voteRewards;
    }

    public void clearMonthVotes() {
        monthVotes = 0;
    }

    public void addDonation(int amount) {
        amountDonated += amount;
    }

    public void addShop() {
        ++ shops;
    }

    public boolean canAddShop() {
        return shops < rank.getShops();
    }

    public void removeShop() {
        if(shops > 0)
            -- shops;
    }

    public boolean hasParticles() {
        return particles != null && rank.specialCompareTo(Rank.DONOR) >= 0;
    }

    public void setParticles(Particle type, Particles.ParticleLocation location) {
        if(type == null || location == null)
            particles = null;
        else{
            if(particles == null)
                particles = new Particles(type, location);
            else
                particles.setTypeAndLocation(type, location);
        }
    }

    public void setRank(Rank rank) {
        Player player = getOnlinePlayer();
        boolean online = player != null;
        if(rank.specialCompareTo(this.rank) > 0) {
            Logging.broadcast(ChatColor.GOLD + " ** " + ChatColor.GREEN + username + ChatColor.GOLD +
                    " has ranked up to " + rank.getColor() + rank.getSymbol() + ChatColor.GOLD + " ** ", true);
            if(online)
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5.0F, 1.0F);
        }
        this.rank = rank;
        updateSessionIfOnline(false);
    }

    public Location getLastLocation() {
        return lastLocation.asLocation();
    }

    public void setLastLocation(Location location) {
        lastLocation = new LocationWrapper(location);
    }

    public void setLastLocation(String world, double x, double y, double z, float yaw, float pitch) {
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
        if(punishments.size() >= 4 || punishments.stream().map(Punishment::getType).anyMatch(Punishment.PunishmentType::isPermanent))
            return -1L; // No use for further punishing, this player is permanently banned
        punishments.add(p);
        Player player = getOnlinePlayer();
        if(player != null)
            player.kickPlayer(p.generateBanMessage(punishments.size() - 1, true));
        Location spawn = FarLands.getDataHandler().getPluginData().getSpawn();
        if(!FLUtils.deltaEquals(spawn, FLUtils.LOC_ZERO.asLocation(), 1e-8)) { // Make sure spawn is set
            if(player != null)
                FLUtils.tpPlayer(player, spawn);
            else
                setLastLocation(spawn);
        }
        return p.totalTime(punishments.size() - 1);
    }

    public boolean pardon(Punishment.PunishmentType type) {
        Punishment punishment = null;
        for(int i = punishments.size() - 1;i >= 0;-- i) {
            if(type.equals(punishments.get(i).getType()))
                punishment = punishments.get(i);
        }
        return punishments.remove(punishment);
    }

    public Punishment getCurrentPunishment() {
        for(int i = punishments.size() - 1;i >= 0;-- i) {
            if(punishments.get(i).isActive(i))
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
        for(int i = 0;i < punishments.size();++ i) {
            if(punishments.get(i).isActive(i))
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

    public void addMail(String sender, String message) {
        mail.add(new MailMessage(sender, message));
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
