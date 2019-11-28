package net.farlands.odyssey.data;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.struct.*;
import net.farlands.odyssey.util.FileSystem;
import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
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

public class PlayerDataHandler {
    private final Map<String, String> queries;
    private final Map<UUID, FLPlayer> cache;
    private Connection connection;
    private boolean active;

    private void initQueries() {
        List<String> flpFields = new ArrayList<>();
        Stream.of(FLPlayer.class.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers())).forEach(field -> {
            if(FLPlayer.SQL_SER_INFO.get("ignored").contains(field.getName()) || FLPlayer.SQL_SER_INFO.get("constants").contains(field.getName()))
                return;
            if(FLPlayer.SQL_SER_INFO.get("objects").contains(field.getName()))
                Stream.of(field.getType().getDeclaredFields()).map(Field::getName).forEach(f -> flpFields.add(field.getName() + "_" + f));
            else if(boolean.class.equals(field.getType())) {
                if(!flpFields.contains("flags"))
                    flpFields.add("flags");
            }else
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
        }catch(Exception ex) {
            System.out.println("Failed to connect to player data database.");
            ex.printStackTrace(System.out);
            active = false;
        }
    }

    public PlayerDataHandler(File dbFile, DataHandler dh) {
        this.queries = new HashMap<>();
        this.cache = new ConcurrentHashMap<>();
        initQueries();
        initDatabase(dbFile, dh);
    }

    public synchronized ResultSet query(String sql) {
        saveCache(true);
        try {
            return connection.createStatement().executeQuery(sql);
        }catch(SQLException ex) {
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
        FLPlayer flp = cache.get(uuid);
        if(flp != null && flp.isOnline())
            flp.updateOnline(flp.getOnlinePlayer());
    }

    public synchronized void setFlag(UUID uuid, int index) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("setFlag"));
            ps.setInt(1, 1 << index);
            ps.setBytes(2, Utils.serializeUuid(uuid));
            ps.executeUpdate();
            ps.close();
            connection.commit();
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> updateOnline(uuid));
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void removeFlag(UUID uuid, int index) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("removeFlag"));
            ps.setInt(1, 255 ^ (1 << index));
            ps.setBytes(2, Utils.serializeUuid(uuid));
            ps.executeUpdate();
            ps.close();
            connection.commit();
            Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> updateOnline(uuid));
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized FLPlayer getCached(UUID uuid) {
        return uuid == null ? null : cache.get(uuid);
    }

    public synchronized List<FLPlayer> getCached() {
        return new ArrayList<>(cache.values());
    }

    public synchronized void saveCache(boolean update) {
        if(!cache.isEmpty()) {
            if(update)
                cache.values().forEach(flp -> flp.updateOnline(flp.getOnlinePlayer(), false));
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
        return getFLPlayer(player).getSecondsPlayed() < 30;
    }

    public synchronized FLPlayer getFLPlayer(Player player) {
        if(cache.containsKey(player.getUniqueId()))
            return cache.get(player.getUniqueId());
        FLPlayer flp = getFLPlayer(player.getUniqueId(), player.getName());
        cache.put(player.getUniqueId(), flp);
        return flp;
    }

    public synchronized FLPlayer getFLPlayerMatching(String name) {
        Pair<UUID, String> ids = getPlayerIds(name);
        if(ids == null)
            return null;
        if(cache.containsKey(ids.getFirst()))
            return cache.get(ids.getFirst());
        return loadFLPlayer(ids.getFirst(), null);
    }

    public synchronized FLPlayer getFLPlayer(UUID uuid) {
        if(cache.containsKey(uuid))
            return cache.get(uuid);
        return loadFLPlayer(uuid, null);
    }

    public synchronized FLPlayer getFLPlayer(UUID uuid, String username) { // Creates new player data (if needed)
        if(cache.containsKey(uuid))
            return cache.get(uuid);
        return loadFLPlayer(uuid, username);
    }

    public synchronized FLPlayer getFLPlayer(String name) { // Not used often, so a search is okay. Note: does not create new player data
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getUuid"));
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if(!rs.next())
                return null;
            byte[] uuid = rs.getBytes("uuid");
            rs.close();
            ps.close();
            return loadFLPlayer(Utils.getUuid(uuid, 0), null);
        }catch(SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized FLPlayer getFLPlayer(CommandSender sender) {
        if(sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender)
            return null;
        return sender instanceof DiscordSender
                ? ((DiscordSender)sender).getFlp()
                : getFLPlayer((Player)sender);
    }

    public synchronized FLPlayer getFLPlayer(long discordID) { // Note: does not create new player data
        if(discordID == 0)
            return null;
        FLPlayer flp = cache.values().stream().filter(flp0 -> flp0.getDiscordID() == discordID).findAny().orElse(null);
        if(flp != null)
            return flp;
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getUuidByDiscordID"));
            ps.setLong(1, discordID);
            ResultSet rs = ps.executeQuery();
            if(!rs.next())
                return null;
            byte[] uuid = rs.getBytes("uuid");
            rs.close();
            ps.close();
            return loadFLPlayer(Utils.getUuid(uuid, 0), null);
        }catch(SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized List<FLPlayer> getAlts(UUID player) {
        List<FLPlayer> alts = new ArrayList<>();
        try {
            ResultSet rs = FarLands.getPDH().query("SELECT uuid FROM playerdata WHERE lastIP=\"" +
                    getFLPlayer(player).getLastIP() + "\" AND rank<" + Rank.JR_BUILDER.ordinal());
            while(rs.next()) {
                UUID uuid = Utils.getUuid(rs.getBytes("uuid"), 0);
                if(!player.equals(uuid))
                    alts.add(FarLands.getPDH().getFLPlayer(uuid));
            }
            rs.close();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
        return alts;
    }

    public synchronized String getUsername(UUID uuid) {
        if(cache.containsKey(uuid))
            return cache.get(uuid).getUsername();
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getUsername"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            if(!rs.next())
                return null;
            String username = rs.getString("username");
            rs.close();
            ps.close();
            return username;
        }catch(SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized String getEffectiveName(UUID uuid) {
        if(cache.containsKey(uuid))
            return cache.get(uuid).getDisplayName();
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getEffectiveName"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            if(!rs.next())
                return null;
            String nickname = rs.getString("nickname");
            rs.close();
            ps.close();
            return nickname == null || nickname.isEmpty() ? getUsername(uuid) : nickname;
        }catch(SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized Rank getRank(UUID uuid) {
        if(cache.containsKey(uuid))
            return cache.get(uuid).getRank();
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getRankByUuid"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            if(!rs.next())
                return Rank.INITIATE;
            int rank = rs.getInt("rank");
            rs.close();
            ps.close();
            return Rank.VALUES[rank];
        }catch(SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized Rank getRank(long discordId) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getRankByDiscordID"));
            ps.setLong(1, discordId);
            ResultSet rs = ps.executeQuery();
            if(!rs.next())
                return Rank.INITIATE;
            int rank = rs.getInt("rank");
            rs.close();
            ps.close();
            return Rank.VALUES[rank];
        }catch(SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized UUID getUuid(CommandSender sender) {
        if(sender instanceof ConsoleCommandSender || sender instanceof BlockCommandSender)
            return null;
        if(sender instanceof Player)
            return ((Player)sender).getUniqueId();
        if(sender instanceof DiscordSender) {
            try {
                PreparedStatement ps = connection.prepareStatement(queries.get("getUuidByDiscordID"));
                ps.setLong(1, ((DiscordSender)sender).getUserID());
                ResultSet rs = ps.executeQuery();
                if(!rs.next())
                    return null;
                byte[] uuid = rs.getBytes("uuid");
                rs.close();
                ps.close();
                return Utils.getUuid(uuid, 0);
            }catch(SQLException ex) {
                ex.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public synchronized void saveLegacy(FLPlayer flp) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("newFlp"));
            ps.setBytes(1, Utils.serializeUuid(flp.getUuid()));
            ps.setString(2, flp.getUsername());
            ps.executeUpdate();
            ps.close();
            connection.commit();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
        saveFLPlayerComplete(flp);
    }

    public synchronized void saveFLPlayerComplete(FLPlayer flp) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("saveFlp"));
            saveFLPlayer(flp, ps, false);
            ps.executeUpdate();
            ps.close();
            byte[] uuid = Utils.serializeUuid(flp.getUuid());
            ps = connection.prepareStatement(queries.get("delAllHomes"));
            ps.setBytes(1, uuid);
            ps.executeUpdate();
            ps.close();
            flp.getHomes().forEach(h -> addHome(flp.getUuid(), h.getName(), h.getLocation()));
            ps = connection.prepareStatement(queries.get("delAllPunishments"));
            ps.setBytes(1, uuid);
            ps.executeUpdate();
            ps.close();
            flp.getPunishments().forEach(p -> punish(flp.getUuid(), p));
            ps = connection.prepareStatement(queries.get("clearMail"));
            ps.setBytes(1, uuid);
            ps.executeUpdate();
            ps.close();
            flp.getMail().forEach(m -> addMail(flp.getUuid(), m.getSender(), m.getMessage()));
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void saveFLPlayer(FLPlayer flp) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("saveFlp"));
            saveFLPlayer(flp, ps, false);
            ps.executeUpdate();
            ps.close();
            connection.commit();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void saveFLPlayer(FLPlayer flp, PreparedStatement saveFlp, boolean batch) {
        try {
            saveFlp.setString(1, flp.getUsername());
            saveFlp.setLong(2, flp.getDiscordID());
            saveFlp.setLong(3, flp.getLastLogin());
            saveFlp.setString(4, flp.getNickname());
            saveFlp.setString(5, flp.getLastIP());
            saveFlp.setInt(6, flp.getSecondsPlayed());
            saveFlp.setInt(7, flp.getTotalVotes());
            saveFlp.setInt(8, flp.getMonthVotes());
            saveFlp.setInt(9, flp.getVoteRewards());
            saveFlp.setInt(10, flp.getAmountDonated());
            saveFlp.setInt(11, flp.getShops());
            int flags = (flp.isFlying() ? 1 : 0) << 7 | (flp.isGod() ? 1 : 0) << 6 | (flp.isVanished() ? 1 : 0) << 5 |
                    (flp.isCensoring() ? 1 : 0) << 4 | (flp.isPvPing() ? 1 : 0) << 3 | (flp.isTopVoter() ? 1 : 0) << 2 |
                    (flp.viewedPatchnotes() ? 1 : 0) << 1 | (flp.isDebugging() ? 1 : 0);
            saveFlp.setInt(12, (byte)flags);
            if(flp.getParticles() == null) {
                saveFlp.setInt(13, -1);
                saveFlp.setInt(14, -1);
            }else{
                saveFlp.setInt(13, flp.getParticles().getType().ordinal());
                saveFlp.setInt(14, flp.getParticles().getLocation().ordinal());
            }
            saveFlp.setInt(15, flp.getRank().ordinal());
            saveFlp.setInt(16, DataHandler.WORLDS.indexOf(flp.getLastLocation().getWorld().getName()));
            saveFlp.setDouble(17, flp.getLastLocation().getX());
            saveFlp.setDouble(18, flp.getLastLocation().getY());
            saveFlp.setDouble(19, flp.getLastLocation().getZ());
            saveFlp.setFloat(20, flp.getLastLocation().getYaw());
            saveFlp.setFloat(21, flp.getLastLocation().getPitch());
            if(flp.getCurrentMute() == null) {
                saveFlp.setLong(22, 0);
                saveFlp.setString(23, "");
            }else{
                saveFlp.setLong(22, flp.getCurrentMute().getDateEnds());
                saveFlp.setString(23, flp.getCurrentMute().getReason());
            }
            byte[] ignored = new byte[flp.getRawIgnoreList().size() * 16];
            int i = 0;
            for(UUID uid : flp.getRawIgnoreList()) {
                Utils.serializeUuid(uid, ignored, i);
                i += 16;
            }
            if(ignored.length == 0)
                saveFlp.setNull(24, Types.BLOB);
            else
                saveFlp.setBytes(24, ignored);

            saveFlp.setBytes(25, Utils.serializeUuid(flp.getUuid()));

            if(batch)
                saveFlp.addBatch();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    private FLPlayer loadFLPlayer(UUID uuid, String username) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getFlpByUuid"));
            byte[] serUuid = Utils.serializeUuid(uuid);

            ps.setBytes(1, serUuid);
            ResultSet rs = ps.executeQuery();

            FLPlayer flp = new FLPlayer(uuid, username);
            if(!rs.next()) {
                if(username == null)
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
            if(username == null)
                flp.setUsername(rs.getString("username"));
            flp.setDiscordIDSilent(rs.getLong("discordID"));
            flp.setLastLogin(rs.getLong("lastLogin"));
            flp.setNicknameSilent(rs.getString("nickname"));
            flp.setLastIP(rs.getString("lastIP"));
            flp.setSecondsPlayed(rs.getInt("secondsPlayed"));
            flp.setTotalVotes(rs.getInt("totalVotes"));
            flp.setMonthVotes(rs.getInt("monthVotes"));
            flp.setVoteRewards(rs.getInt("voteRewards"));
            flp.setAmountDonated(rs.getInt("amountDonated"));
            flp.setShops(rs.getInt("shops"));
            int flags = rs.getInt("flags");
            flp.setFlyingSilent((flags & 0x80) != 0);
            flp.setGod((flags & 0x40) != 0);
            flp.setVanishedSilent((flags & 0x20) != 0);
            flp.setCensoring((flags & 0x10) != 0);
            flp.setPvPing((flags & 0x8) != 0);
            flp.setTopVoter((flags & 0x4) != 0);
            flp.setViewedPatchnotes((flags & 0x2) != 0);
            flp.setDebugging((flags & 0x1) != 0);
            int particleType = rs.getInt("particles_type"), loc = rs.getInt("particles_location");
            if(particleType >= 0 && loc >= 0)
                flp.setParticles(Particle.values()[particleType], Particles.ParticleLocation.VALUES[loc]);
            flp.setRankSilent(Rank.VALUES[rs.getInt("rank")]);
            flp.setLastLocation(DataHandler.WORLDS.get(rs.getInt("lastLocation_world")), rs.getDouble("lastLocation_x"),
                    rs.getDouble("lastLocation_y"), rs.getDouble("lastLocation_z"), rs.getFloat("lastLocation_yaw"),
                    rs.getFloat("lastLocation_pitch"));
            long muteDateEnds = rs.getLong("currentMute_dateEnds");
            if(muteDateEnds > 0)
                flp.setCurrentMute(new Mute(muteDateEnds, rs.getString("currentMute_reason")));
            byte[] ignoredPlayers = rs.getBytes("ignoredPlayers");
            if(ignoredPlayers != null) {
                for (int i = 0; i + 15 < ignoredPlayers.length; i += 16)
                    flp.setIgnoring(Utils.getUuid(ignoredPlayers, i), true);
            }

            flp.setPunishments(getPunishments(uuid));
            flp.setHomes(getHomes(uuid));
            flp.setMail(getMail(uuid));

            rs.close();
            ps.close();

            return flp;
        }catch(SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized void addHome(UUID uuid, String name, Location loc) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("addHome"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ps.setString(2, name);
            ps.setDouble(3, loc.getX());
            ps.setDouble(4, loc.getY());
            ps.setDouble(5, loc.getZ());
            ps.setFloat(6, loc.getYaw());
            ps.setFloat(7, loc.getPitch());
            ps.executeUpdate();
            ps.close();
            connection.commit();
        }catch(SQLException ex) {
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
            ps.setBytes(6, Utils.serializeUuid(uuid));
            ps.setString(7, name);
            ps.executeUpdate();
            ps.close();
            connection.commit();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void delHome(UUID uuid, String name) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("delHome"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ps.setString(2, name);
            ps.executeUpdate();
            ps.close();
            connection.commit();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized List<Home> getHomes(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getHomes"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            List<Home> homes = new ArrayList<>();
            while(rs.next()) {
                homes.add(new Home(rs.getString("name"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch")));
            }
            rs.close();
            ps.close();
            return homes;
        }catch(SQLException ex) {
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

    public synchronized void punish(UUID uuid, Punishment punishment) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("punish"));
            byte[] serUuid = Utils.serializeUuid(uuid);
            ps.setBytes(1, serUuid);
            ps.setInt(2, punishment.getType().ordinal());
            ps.setLong(3, punishment.getDateIssued());
            ps.setString(4, punishment.getRawMessage());
            ps.executeUpdate();
            ps.close();
            connection.commit();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void pardon(UUID uuid, Punishment punishment) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("pardon"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ps.setLong(2, punishment.getDateIssued());
            ps.executeUpdate();
            ps.close();
            connection.commit();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized List<Punishment> getPunishments(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getPunishments"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            List<Punishment> ret = new ArrayList<>();
            while(rs.next()) {
                ret.add(new Punishment(Punishment.PunishmentType.VALUES[rs.getInt("punishmentType")], rs.getLong("dateIssued"),
                        rs.getString("message")));
            }
            rs.close();
            ps.close();
            return ret;
        }catch(SQLException ex) {
            return null;
        }
    }

    public synchronized void addMail(UUID uuid, String sender, String message) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("addMail"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ps.setString(2, sender);
            ps.setString(3, message);
            ps.executeUpdate();
            ps.close();
            connection.commit();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void clearMail(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("clearMail"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ps.executeUpdate();
            ps.close();
            connection.commit();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized List<MailMessage> getMail(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getMail"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            List<MailMessage> ret = new ArrayList<>();
            while(rs.next())
                ret.add(new MailMessage(rs.getString("sender"), rs.getString("message")));
            rs.close();
            ps.close();
            return ret;
        }catch(SQLException ex) {
            return null;
        }
    }

    public synchronized void addNote(UUID uuid, long dateIssued, String sender, String note) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("addNote"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ps.setLong(2, dateIssued);
            ps.setString(3, sender);
            ps.setString(4, note);
            ps.executeUpdate();
            ps.close();
            connection.commit();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void clearNotes(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("clearNotes"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ps.executeUpdate();
            ps.close();
            connection.commit();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized List<String> getNotes(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(queries.get("getNotes"));
            ps.setBytes(1, Utils.serializeUuid(uuid));
            ResultSet rs = ps.executeQuery();
            List<String> ret = new ArrayList<>();
            while(rs.next())
                ret.add(Utils.dateToString(rs.getLong("dateTaken"), "MM/dd/yyyy") + " " + rs.getString("sender") + ": " + rs.getString("note"));
            rs.close();
            ps.close();
            return ret;
        }catch(SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized Pair<UUID, String> getPlayerIds(Object id) {
        try {
            if(id instanceof String) {
                String username = (String)id;
                PreparedStatement ps = connection.prepareStatement(queries.get("getFlpIds"));
                ResultSet rs = ps.executeQuery();
                byte[] match = null;
                String matchName = null;
                String name;
                while(rs.next()) {
                    name = rs.getString("username");
                    if(username.equalsIgnoreCase(name))
                        return new Pair<>(Utils.getUuid(rs.getBytes("uuid"), 0), name);
                    else if(name.toLowerCase().contains(username.toLowerCase())) {
                        match = rs.getBytes("uuid");
                        matchName = name;
                    }
                }
                rs.close();
                ps.close();
                return match == null ? null : new Pair<>(Utils.getUuid(match, 0), matchName);
            }else if(id instanceof UUID) {
                PreparedStatement ps = connection.prepareStatement(queries.get("getUsername"));
                ps.setBytes(1, Utils.serializeUuid((UUID)id));
                ResultSet rs = ps.executeQuery();
                if(!rs.next())
                    return null;
                rs.close();
                ps.close();
                return new Pair<>((UUID)id, rs.getString("username"));
            }else
                return null;
        }catch(SQLException ex) {
            return null;
        }
    }

    public synchronized void onShutdown() {
        if(!active)
            return;

        saveCache(false);

        try {
            connection.close();
        }catch(SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static String valueSpaces(int amount) {
        StringBuilder sb = new StringBuilder("(?");
        for(int i = 1;i < amount;++ i)
            sb.append(",?");
        return sb.append(')').toString();
    }
}
