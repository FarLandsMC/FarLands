package net.farlands.odyssey.data.struct;

import com.google.common.collect.ImmutableMap;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.discord.DiscordHandler;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.LocationWrapper;
import net.farlands.odyssey.util.Utils;
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
            .put("ignored", Arrays.asList("punishments", "homes", "mail"))
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
        this.lastLocation = Utils.LOC_ZERO;
        this.currentMute = null;
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

    public UUID getUuid() {
        return uuid;
    }

    public long getDiscordID() {
        return discordID;
    }

    public void setDiscordID(long discordID) {
        this.discordID = discordID;
        FarLands.getDataHandler().updateDiscordMap(discordID, this);
    }

    public void setDiscordIDSilent(long discordID) {
        this.discordID = discordID;
    }

    public boolean isDiscordVerified() {
        return discordID != 0;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public long getLastLogin() {
        return isOnline() && !isVanished() ? System.currentTimeMillis() : lastLogin;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setNicknameSilent(String nickname) {
        this.nickname = nickname;
    }

    public String getDisplayName() {
        return nickname == null || nickname.isEmpty() ? username : nickname;
    }

    public String getLastIP() {
        return lastIP;
    }

    public void setLastIP(String lastIP) {
        this.lastIP = lastIP;
    }

    public int getSecondsPlayed() {
        return secondsPlayed;
    }

    public void setSecondsPlayed(int secondsPlayed) {
        this.secondsPlayed = secondsPlayed;
    }

    public void addVote() {
        ++ totalVotes;
        ++ monthVotes;
        FLPlayerSession session = FarLands.getDataHandler().getSession(uuid);
        if(session != null) {
            session.giveVoteRewards(1);
            session.player.sendMessage(ChatColor.GOLD + "Receiving 1 vote reward!");
        }else
            ++ voteRewards;
    }

    public int getVoteRewards() {
        return voteRewards;
    }

    public void setVoteRewards(int voteRewards) {
        this.voteRewards = voteRewards;
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(int totalVotes) {
        this.totalVotes = totalVotes;
    }

    public int getMonthVotes() {
        return monthVotes;
    }

    public void setMonthVotes(int monthVotes) {
        this.monthVotes = monthVotes;
    }

    public void clearMonthVotes() {
        monthVotes = 0;
    }

    public int getAmountDonated() {
        return amountDonated;
    }

    public void addDonation(int amount) {
        amountDonated += amount;
    }

    public void setAmountDonated(int amountDonated) {
        this.amountDonated = amountDonated;
    }

    public int getShops() {
        return shops;
    }

    public void setShops(int shops) {
        this.shops = shops;
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

    public void setFlightPreference(boolean value) {
        this.flightPreference = value;
    }

    public void setFlightPreferenceSilent(boolean value) {
        this.flightPreference = value;
    }

    public boolean getFlightPreference() {
        return flightPreference;
    }

    public void setGod(boolean value) {
        god = value;
    }

    public boolean isGod() {
        return god;
    }

    public void setVanished(boolean value) {
        vanished = value;
    }

    public void setVanishedSilent(boolean value) {
        this.vanished = value;
    }

    public boolean isVanished() {
        return vanished;
    }

    public void setCensoring(boolean value) {
        censoring = value;
    }

    public boolean isCensoring() {
        return censoring;
    }

    public void setPvPing(boolean value) {
        pvp = value;
    }

    public boolean isPvPing() {
        return pvp;
    }

    public void setTopVoter(boolean value) {
        topVoter = value;
    }

    public boolean isTopVoter() {
        return topVoter;
    }

    public boolean viewedPatchnotes() {
        return viewedPatchnotes;
    }

    public void setViewedPatchnotes(boolean value) {
        viewedPatchnotes = value;
    }

    public Particles getParticles() {
        return particles;
    }

    public boolean hasParticles() {
        return particles != null && rank.specialCompareTo(Rank.DONOR) >= 0;
    }

    public void setDebugging(boolean value) {
        this.debugging = value;
    }

    public boolean isDebugging() {
        return debugging;
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

    public Rank getRank() {
        return rank;
    }

    public void setRankSilent(Rank rank) {
        this.rank = rank;
    }

    public void setRank(Rank rank) {
        Player player = getOnlinePlayer();
        boolean online = player != null;
        if(rank.specialCompareTo(this.rank) > 0) {
            Chat.broadcast(ChatColor.GOLD + " ** " + ChatColor.GREEN + getUsername() + ChatColor.GOLD +
                    " has ranked up to " + rank.getColor() + rank.getSymbol() + ChatColor.GOLD + " ** ", true);
            if(online)
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 5.0F, 1.0F);
        }
        this.rank = rank;
        updateDiscord();
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

    public Set<UUID> getRawIgnoreList() {
        return ignoredPlayers;
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

    public void setCurrentMute(Mute mute) {
        currentMute = mute;
    }

    public Mute getCurrentMute() {
        return currentMute;
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
        if(!Utils.deltaEquals(spawn, Utils.LOC_ZERO.asLocation(), 1e-8)) { // Make sure spawn is set
            if(player != null)
                Utils.tpPlayer(player, spawn);
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

    public List<Punishment> getPunishments() {
        return punishments;
    }

    public void setPunishments(List<Punishment> punishments) {
        this.punishments = punishments;
    }

    public boolean isBanned() {
        for(int i = 0;i < punishments.size();++ i) {
            if(punishments.get(i).isActive(i))
                return true;
        }
        return false;
    }

    public List<Home> getHomes() {
        return homes;
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

    public void setHomes(List<Home> homes) {
        this.homes = homes;
    }

    public void addMail(String sender, String message) {
        mail.add(new MailMessage(sender, message));
    }

    public List<MailMessage> getMail() {
        return mail;
    }

    public void clearMail() {
        mail.clear();
    }

    public void setMail(List<MailMessage> mail) {
        this.mail = mail;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof OfflineFLPlayer && uuid.equals(((OfflineFLPlayer)other).getUuid());
    }

    @Override
    public String toString() {
        return username;
    }
}
