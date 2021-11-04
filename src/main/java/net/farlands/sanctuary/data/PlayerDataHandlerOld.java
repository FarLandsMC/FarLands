package net.farlands.sanctuary.data;

import com.kicas.rp.util.Pair;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.struct.*;
import net.farlands.sanctuary.util.FileSystem;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Old SQL data handler.
 */
public class PlayerDataHandlerOld {
    private final Map<String, String> queries;
    private final Map<UUID, OfflineFLPlayer> cache;
    private Connection connection;
    private boolean active;

    private void initQueries() {
        List<String> flpFields = new ArrayList<>();
        Stream.of(OfflineFLPlayer.class.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers())).forEach(field -> {
            if (OfflineFLPlayer.SQL_SER_INFO.get("ignored").contains(field.getName())
                    || OfflineFLPlayer.SQL_SER_INFO.get("constants").contains(field.getName()))
                return;
            if (OfflineFLPlayer.SQL_SER_INFO.get("objects").contains(field.getName()))
                Stream.of(field.getType().getDeclaredFields()).map(Field::getName).forEach(f -> flpFields.add(field.getName() + "_" + f));
            else if (boolean.class.equals(field.getType())) {
                if (!flpFields.contains("flags"))
                    flpFields.add("flags");
            } else
                flpFields.add(field.getName());
        });
        queries.put("newFlp", "INSERT INTO playerdata (uuid,username," + String.join(",", flpFields) + ") " +
                "VALUES (?,?,0," + System.currentTimeMillis() + ",\"\",\"\",0,0,0,0,0,0,0,-1,-1,0,0,0.0,0.0,0.0,0.0,0.0,0,\"\",0)");
        queries.put("saveFlp", "UPDATE playerdata SET username=?," + flpFields.stream()
                .map(f -> f + "=?").collect(Collectors.joining(",")) + " WHERE uuid=?");
        queries.put("getFlpByUuid", "SELECT username," + String.join(",", flpFields) + " FROM playerdata WHERE uuid=?");
        queries.put("getFlpIds", "SELECT uuid,username FROM playerdata");
        queries.put("getUsername", "SELECT username FROM playerdata WHERE uuid=?");
        queries.put("getEffectiveName", "SELECT nickname FROM playerdata WHERE uuid=?");
        queries.put("getUuid", "SELECT uuid FROM playerdata WHERE username=?");
        queries.put("getUuidByDiscordID", "SELECT uuid FROM playerdata WHERE discordID=?");
        queries.put("setFlag", "UPDATE playerdata SET flags=flags|? WHERE uuid=?");
        queries.put("removeFlag", "UPDATE playerdata SET flags=flags&? WHERE uuid=?");
        queries.put("getRankByUuid", "SELECT rank FROM playerdata WHERE uuid=?");
        queries.put("getRankByDiscordID", "SELECT rank FROM playerdata WHERE discordID=?");

        List<String> punishFields = Stream.of(Punishment.class.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(Field::getName).collect(Collectors.toList());
        queries.put("punish", "INSERT INTO punishments (uuid," + String.join(",", punishFields) + ") " +
                "VALUES " + valueSpaces(punishFields.size() + 1));
        queries.put("punishmentCount", "SELECT Count(*) FROM punishments WHERE uuid=?");
        queries.put("pardonSelect", "SELECT dateIssued FROM punishments WHERE uuid=? AND punishmentType=?");
        queries.put("pardon", "DELETE FROM punishments WHERE uuid=? AND dateIssued=?");
        queries.put("getPunishments", "SELECT " + String.join(",", punishFields) + " FROM punishments " +
                "WHERE uuid=?");
        queries.put("delAllPunishments", "DELETE FROM punishments WHERE uuid=?");

        List<String> homeFields = Stream.of(Home.class.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(Field::getName).collect(Collectors.toList());
        queries.put("addHome", "INSERT INTO homes (uuid," + String.join(",", homeFields) + ") " +
                "VALUES " + valueSpaces(homeFields.size() + 1));
        queries.put("moveHome", "UPDATE homes SET " + homeFields.stream()
                .filter(f -> !"name".equals(f)).map(f -> f + "=?").collect(Collectors.joining(",")) + " WHERE uuid=? AND name=?");
        queries.put("delHome", "DELETE FROM homes WHERE uuid=? AND name=?");
        queries.put("getHomes", "SELECT " + String.join(",", homeFields) + " FROM homes WHERE uuid=?");
        queries.put("delAllHomes", "DELETE FROM homes WHERE uuid=?");

        List<String> mailFields = Stream.of(MailMessage.class.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(Field::getName).collect(Collectors.toList());
        queries.put("addMail", "INSERT INTO mail (uuid," + String.join(",", mailFields) + ") VALUES " +
                valueSpaces(mailFields.size() + 1));
        queries.put("clearMail", "DELETE FROM mail WHERE uuid=?");
        queries.put("getMail", "SELECT " + String.join(",", mailFields) + " FROM mail WHERE uuid=?");

        queries.put("addNote", "INSERT INTO notes (uuid,dateTaken,sender,note) VALUES (?,?,?,?)");
        queries.put("clearNotes", "DELETE FROM notes WHERE uuid=?");
        queries.put("getNotes", "SELECT dateTaken,sender,note FROM notes WHERE uuid=?");
    }

    private void initDatabase(File dbFile, DataHandler dh) {
        try {
            FileSystem.createFile(dbFile);
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            statement.executeUpdate(new String(dh.getResource("playerdata.sql")));
            statement.close();
            connection.commit();
            active = true;
        } catch (Exception ex) {
            System.out.println("Failed to connect to player data database.");
            ex.printStackTrace(System.out);
            active = false;
        }
    }

    public PlayerDataHandlerOld(File dbFile, DataHandler dh) {
        this.queries = new HashMap<>();
        this.cache = new ConcurrentHashMap<>();
        initQueries();
        initDatabase(dbFile, dh);
    }

    public synchronized ResultSet query(String sql) {
        saveCache(true);
        try {
            return connection.createStatement().executeQuery(sql);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized boolean update(String sql) {
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(sql);
            statement.close();
            connection.commit();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public synchronized void updateOnline(UUID uuid) {
        OfflineFLPlayer flp = cache.get(uuid);
        if (flp != null && flp.isOnline())
            flp.updateSessionIfOnline(true);
    }

    public synchronized void setFlag(UUID uuid, int index) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("setFlag"));
            ps.setInt(1, 1 << index);
            ps.setBytes(2, FLUtils.serializeUuid(uuid));
            ps.executeUpdate();
            ps.close();
            connection.commit();
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> updateOnline(uuid));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void removeFlag(UUID uuid, int index) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("removeFlag"));
            ps.setInt(1, 255 ^ (1 << index));
            ps.setBytes(2, FLUtils.serializeUuid(uuid));
            ps.executeUpdate();
            ps.close();
            connection.commit();
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> updateOnline(uuid));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized OfflineFLPlayer getCached(UUID uuid) {
        return uuid == null ? null : cache.get(uuid);
    }

    public synchronized List<OfflineFLPlayer> getCached() {
        return new ArrayList<>(cache.values());
    }

    public synchronized void saveCache(boolean update) {
        if (!cache.isEmpty()) {
            if (update)
                cache.values().forEach(flp -> flp.updateSessionIfOnline(false));
            try {
                PreparedStatement ps = connection.prepareStatement(queries.get("saveFlp"));
                cache.values().forEach(flp -> saveFLPlayer(flp, ps, true));
                ps.executeBatch();
                ps.close();
                connection.commit();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public synchronized void uncachePlayer(UUID uuid) {
        saveFLPlayer(cache.get(uuid));
        cache.remove(uuid);
    }

    public synchronized boolean isNew(Player player) {
        return getFLPlayer(player).secondsPlayed < 30;
    }

    public synchronized OfflineFLPlayer getFLPlayer(Player player) {
        if (cache.containsKey(player.getUniqueId()))
            return cache.get(player.getUniqueId());
        OfflineFLPlayer flp = getFLPlayer(player.getUniqueId(), player.getName());
        cache.put(player.getUniqueId(), flp);
        return flp;
    }

    public synchronized OfflineFLPlayer getFLPlayerMatching(String name) {
        Pair<UUID, String> ids = getPlayerIds(name);
        if (ids == null)
            return null;
        if (cache.containsKey(ids.getFirst()))
            return cache.get(ids.getFirst());
        return loadFLPlayer(ids.getFirst(), null);
    }

    public synchronized OfflineFLPlayer getFLPlayer(UUID uuid) {
        if (cache.containsKey(uuid))
            return cache.get(uuid);
        return loadFLPlayer(uuid, null);
    }

    public synchronized OfflineFLPlayer getFLPlayer(UUID uuid, String username) { // Creates new player data (if needed)
        if (cache.containsKey(uuid))
            return cache.get(uuid);
        return loadFLPlayer(uuid, username);
    }

    public synchronized OfflineFLPlayer getFLPlayer(String name) { // Not used often, so a search is okay. Note: does not create new player data
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getUuid"));
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (!rs.next())
                return null;
            byte[] uuid = rs.getBytes("uuid");
            rs.close();
            ps.close();
            return loadFLPlayer(FLUtils.getUuid(uuid, 0), null);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized OfflineFLPlayer getFLPlayer(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender)
            return null;
        return sender instanceof DiscordSender
                ? ((DiscordSender) sender).getFlp()
                : getFLPlayer((Player) sender);
    }

    public synchronized OfflineFLPlayer getFLPlayer(long discordID) { // Note: does not create new player data
        if (discordID == 0)
            return null;
        OfflineFLPlayer flp = cache.values().stream().filter(flp0 -> flp0.discordID == discordID).findAny().orElse(null);
        if (flp != null)
            return flp;
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getUuidByDiscordID"));
            ps.setLong(1, discordID);
            ResultSet rs = ps.executeQuery();
            if (!rs.next())
                return null;
            byte[] uuid = rs.getBytes("uuid");
            rs.close();
            ps.close();
            return loadFLPlayer(FLUtils.getUuid(uuid, 0), null);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized List<OfflineFLPlayer> getAlts(UUID player) {
        List<OfflineFLPlayer> alts = new ArrayList<>();
        try {
            ResultSet rs = query("SELECT uuid FROM playerdata WHERE lastIP=\"" +
                    getFLPlayer(player).lastIP + "\" AND rank<" + Rank.JR_BUILDER.ordinal());
            while (rs.next()) {
                UUID uuid = FLUtils.getUuid(rs.getBytes("uuid"), 0);
                if (!player.equals(uuid))
                    alts.add(getFLPlayer(uuid));
            }
            rs.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return alts;
    }

    public synchronized String getUsername(UUID uuid) {
        if (cache.containsKey(uuid))
            return cache.get(uuid).username;
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getUsername"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            if (!rs.next())
                return null;
            String username = rs.getString("username");
            rs.close();
            ps.close();
            return username;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized String getEffectiveName(UUID uuid) {
        if (cache.containsKey(uuid))
            return cache.get(uuid).getDisplayName();
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getEffectiveName"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            if (!rs.next())
                return null;
            String nickname = rs.getString("nickname");
            rs.close();
            ps.close();
            return nickname == null || nickname.isEmpty() ? getUsername(uuid) : nickname;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized Rank getRank(UUID uuid) {
        if (cache.containsKey(uuid))
            return cache.get(uuid).rank;
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getRankByUuid"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            if (!rs.next())
                return Rank.INITIATE;
            int rank = rs.getInt("rank");
            rs.close();
            ps.close();
            return Rank.VALUES[rank];
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized Rank getRank(long discordId) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getRankByDiscordID"));
            ps.setLong(1, discordId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next())
                return Rank.INITIATE;
            int rank = rs.getInt("rank");
            rs.close();
            ps.close();
            return Rank.VALUES[rank];
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized UUID getUuid(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender)
            return null;
        if (sender instanceof Player)
            return ((Player) sender).getUniqueId();
        if (sender instanceof DiscordSender) {
            try {
                PreparedStatement ps = connection.prepareStatement(queries.get("getUuidByDiscordID"));
                ps.setLong(1, ((DiscordSender) sender).getUserID());
                ResultSet rs = ps.executeQuery();
                if (!rs.next())
                    return null;
                byte[] uuid = rs.getBytes("uuid");
                rs.close();
                ps.close();
                return FLUtils.getUuid(uuid, 0);
            } catch (SQLException ex) {
                ex.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public synchronized void saveLegacy(OfflineFLPlayer flp) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("newFlp"));
            ps.setBytes(1, FLUtils.serializeUuid(flp.uuid));
            ps.setString(2, flp.username);
            ps.executeUpdate();
            ps.close();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        saveFLPlayerComplete(flp);
    }

    public synchronized void saveFLPlayerComplete(OfflineFLPlayer flp) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("saveFlp"));
            saveFLPlayer(flp, ps, false);
            ps.executeUpdate();
            ps.close();
            byte[] uuid = FLUtils.serializeUuid(flp.uuid);
            ps = connection.prepareStatement(queries.get("delAllHomes"));
            ps.setBytes(1, uuid);
            ps.executeUpdate();
            ps.close();
            flp.homes.forEach(h -> addHome(flp.uuid, h.getName(), h.getLocation()));
            ps = connection.prepareStatement(queries.get("delAllPunishments"));
            ps.setBytes(1, uuid);
            ps.executeUpdate();
            ps.close();
            flp.punishments.forEach(p -> punish(flp.uuid, p));
            ps = connection.prepareStatement(queries.get("clearMail"));
            ps.setBytes(1, uuid);
            ps.executeUpdate();
            ps.close();
            flp.mail.forEach(m -> addMail(flp.uuid, m.sender(), m.message()));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void saveFLPlayer(OfflineFLPlayer flp) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("saveFlp"));
            saveFLPlayer(flp, ps, false);
            ps.executeUpdate();
            ps.close();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void saveFLPlayer(OfflineFLPlayer flp, PreparedStatement saveFlp, boolean batch) {
        try {
            saveFlp.setString(1, flp.username);
            saveFlp.setLong(2, flp.discordID);
            saveFlp.setLong(3, flp.getLastLogin());
            saveFlp.setString(4, flp.nickname);
            saveFlp.setString(5, flp.lastIP);
            saveFlp.setInt(6, flp.secondsPlayed);
            saveFlp.setInt(7, flp.totalVotes);
            saveFlp.setInt(8, flp.monthVotes);
            saveFlp.setInt(9, flp.voteRewards);
            saveFlp.setDouble(10, flp.amountDonated);
            saveFlp.setInt(11, flp.shops);
            int flags = (flp.flightPreference ? 1 : 0) << 7 | (flp.god ? 1 : 0) << 6 | (flp.vanished ? 1 : 0) << 5 |
                    (flp.censoring ? 1 : 0) << 4 | (flp.pvp ? 1 : 0) << 3 | (flp.topVoter ? 1 : 0) << 2 |
                    (flp.viewedPatchnotes ? 1 : 0) << 1 | (flp.debugging ? 1 : 0);
            saveFlp.setInt(12, (byte) flags);
            if (flp.particles == null) {
                saveFlp.setInt(13, -1);
                saveFlp.setInt(14, -1);
            } else {
                saveFlp.setInt(13, flp.particles.getType().ordinal());
                saveFlp.setInt(14, flp.particles.getLocation().ordinal());
            }
            saveFlp.setInt(15, flp.rank.ordinal());
            saveFlp.setInt(16, DataHandler.WORLDS.indexOf(flp.getLastLocation().getWorld().getName()));
            saveFlp.setDouble(17, flp.getLastLocation().getX());
            saveFlp.setDouble(18, flp.getLastLocation().getY());
            saveFlp.setDouble(19, flp.getLastLocation().getZ());
            saveFlp.setFloat(20, flp.getLastLocation().getYaw());
            saveFlp.setFloat(21, flp.getLastLocation().getPitch());
            if (flp.currentMute == null) {
                saveFlp.setLong(22, 0);
                saveFlp.setString(23, "");
            } else {
                saveFlp.setLong(22, flp.currentMute.dateEnds());
                saveFlp.setString(23, flp.currentMute.reason());
            }
            byte[] ignored = new byte[flp.ignoreStatusMap.size() * 16];
            int i = 0;
            for (UUID uid : flp.ignoreStatusMap.keySet()) {
                FLUtils.serializeUuid(uid, ignored, i);
                i += 16;
            }
            if (ignored.length == 0)
                saveFlp.setNull(24, Types.BLOB);
            else
                saveFlp.setBytes(24, ignored);

            saveFlp.setBytes(25, FLUtils.serializeUuid(flp.uuid));

            if (batch)
                saveFlp.addBatch();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public OfflineFLPlayer loadFLPlayer(UUID uuid, String username) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getFlpByUuid"));
            byte[] serUuid = FLUtils.serializeUuid(uuid);

            ps.setBytes(1, serUuid);
            ResultSet rs = ps.executeQuery();

            OfflineFLPlayer flp = new OfflineFLPlayer(uuid, username);
            if (!rs.next()) {
                if (username == null)
                    return null;
                rs.close();
                ps.close();
                ps = connection.prepareStatement(queries.get("newFlp"));
                ps.setBytes(1, serUuid);
                ps.setString(2, username);
                ps.executeUpdate();
                ps.close();
                connection.commit();
                return flp;
            }

            int rankOrdinal = rs.getInt("rank");

            if (username == null)
                flp.username = rs.getString("username");
            flp.discordID = rs.getLong("discordID");
            flp.lastLogin = rs.getLong("lastLogin");
            flp.nickname = rankOrdinal < 8 ? "" : rs.getString("nickname");
            flp.lastIP = rs.getString("lastIP");
            flp.secondsPlayed = 0;
            flp.totalVotes = rs.getInt("totalVotes");
            flp.monthVotes = 0;
            flp.voteRewards = 0;
            flp.amountDonated = rs.getDouble("amountDonated");
            flp.shops = 0;
            int flags = rs.getInt("flags");
            flp.flightPreference = false;
            flp.god = false;
            flp.vanished = false;
            flp.censoring = (flags & 0x10) != 0;
            flp.pvp = (flags & 0x8) != 0;
            flp.topVoter = false;
            flp.viewedPatchnotes = true;
            flp.debugging = (flags & 0x1) != 0;
            flp.rank = rankOrdinal >= 8 ? Rank.VALUES[rankOrdinal] : Rank.INITIATE;
            flp.setLastLocation(Bukkit.getWorld("world").getUID(), 0, 0, 0, 0, 0);
            byte[] ignoredPlayers = rs.getBytes("ignoredPlayers");
            if (ignoredPlayers != null) {
                for (int i = 0; i + 15 < ignoredPlayers.length; i += 16)
                    flp.updateIgnoreStatus(FLUtils.getUuid(ignoredPlayers, i), IgnoreStatus.IgnoreType.ALL, true);
            }

            flp.punishments = getPunishments(uuid);

            rs.close();
            ps.close();

            return flp;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized void addHome(UUID uuid, String name, Location loc) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("addHome"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ps.setString(2, name);
            ps.setDouble(3, loc.getX());
            ps.setDouble(4, loc.getY());
            ps.setDouble(5, loc.getZ());
            ps.setFloat(6, loc.getYaw());
            ps.setFloat(7, loc.getPitch());
            ps.executeUpdate();
            ps.close();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void moveHome(UUID uuid, String name, Location loc) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("moveHome"));
            ps.setDouble(1, loc.getX());
            ps.setDouble(2, loc.getY());
            ps.setDouble(3, loc.getZ());
            ps.setFloat(4, loc.getYaw());
            ps.setFloat(5, loc.getPitch());
            ps.setBytes(6, FLUtils.serializeUuid(uuid));
            ps.setString(7, name);
            ps.executeUpdate();
            ps.close();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void delHome(UUID uuid, String name) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("delHome"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ps.setString(2, name);
            ps.executeUpdate();
            ps.close();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized List<Home> getHomes(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getHomes"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            List<Home> homes = new ArrayList<>();
            while (rs.next()) {
                homes.add(new Home(rs.getString("name"), Bukkit.getWorld("world").getUID(), rs.getDouble("x"),
                        rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch")));
            }
            rs.close();
            ps.close();
            return homes;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

    public synchronized void punish(UUID uuid, Punishment punishment) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("punish"));
            byte[] serUuid = FLUtils.serializeUuid(uuid);
            ps.setBytes(1, serUuid);
            ps.setInt(2, punishment.getType().ordinal());
            ps.setLong(3, punishment.getDateIssued());
            ps.setString(4, punishment.getRawMessage());
            ps.executeUpdate();
            ps.close();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void pardon(UUID uuid, Punishment punishment) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("pardon"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ps.setLong(2, punishment.getDateIssued());
            ps.executeUpdate();
            ps.close();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized List<Punishment> getPunishments(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getPunishments"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            List<Punishment> ret = new ArrayList<>();
            while (rs.next()) {
                ret.add(new Punishment(Punishment.PunishmentType.VALUES[rs.getInt("punishmentType")], rs.getLong("dateIssued"),
                        rs.getString("message")));
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            return null;
        }
    }

    public synchronized void addMail(UUID uuid, String sender, String message) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("addMail"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ps.setString(2, sender);
            ps.setString(3, message);
            ps.executeUpdate();
            ps.close();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void clearMail(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("clearMail"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ps.executeUpdate();
            ps.close();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized List<MailMessage> getMail(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getMail"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            List<MailMessage> ret = new ArrayList<>();
            while (rs.next())
                ret.add(new MailMessage(rs.getString("sender"), rs.getString("message")));
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            return null;
        }
    }

    public synchronized void addNote(UUID uuid, long dateIssued, String sender, String note) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("addNote"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ps.setLong(2, dateIssued);
            ps.setString(3, sender);
            ps.setString(4, note);
            ps.executeUpdate();
            ps.close();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void clearNotes(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("clearNotes"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ps.executeUpdate();
            ps.close();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized List<String> getNotes(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getNotes"));
            ps.setBytes(1, FLUtils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            List<String> ret = new ArrayList<>();
            while (rs.next())
                ret.add(FLUtils.dateToString(rs.getLong("dateTaken"), "MM/dd/yyyy") + " " + rs.getString("sender") + ": " + rs.getString("note"));
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized Pair<UUID, String> getPlayerIds(Object id) {
        try {
            if (id instanceof String) {
                String username = (String) id;
                PreparedStatement ps = connection.prepareStatement(queries.get("getFlpIds"));
                ResultSet rs = ps.executeQuery();
                byte[] match = null;
                String matchName = null;
                String name;
                while (rs.next()) {
                    name = rs.getString("username");
                    if (username.equalsIgnoreCase(name))
                        return new Pair<>(FLUtils.getUuid(rs.getBytes("uuid"), 0), name);
                    else if (name.toLowerCase().contains(username.toLowerCase())) {
                        match = rs.getBytes("uuid");
                        matchName = name;
                    }
                }
                rs.close();
                ps.close();
                return match == null ? null : new Pair<>(FLUtils.getUuid(match, 0), matchName);
            } else if (id instanceof UUID) {
                PreparedStatement ps = connection.prepareStatement(queries.get("getUsername"));
                ps.setBytes(1, FLUtils.serializeUuid((UUID) id));
                ResultSet rs = ps.executeQuery();
                if (!rs.next())
                    return null;
                rs.close();
                ps.close();
                return new Pair<>((UUID) id, rs.getString("username"));
            } else
                return null;
        } catch (SQLException ex) {
            return null;
        }
    }

    public synchronized void onShutdown() {
        if (!active)
            return;

        saveCache(false);

        try {
            connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static String valueSpaces(int amount) {
        return "(?" + ",?".repeat(Math.max(0, amount - 1)) + ')';
    }
}
