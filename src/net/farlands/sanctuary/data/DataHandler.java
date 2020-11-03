package net.farlands.sanctuary.data;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.struct.*;
import net.farlands.sanctuary.data.struct.Package;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.Chat;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.farlands.sanctuary.util.FileSystem;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.FLUtils;

import net.minecraft.server.v1_16_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagList;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * This class serves as and API to interact with FarLands' data.
 */
public class DataHandler extends Mechanic {
    private final PlayerDataHandlerOld pdh;
    private final File rootDirectory;
    private final Map<UUID, OfflineFLPlayer> flPlayerMap;
    private final Map<Long, OfflineFLPlayer> discordMap;
    private final Map<UUID, FLPlayerSession> sessionMap;
    private final Map<UUID, FLPlayerSession> cachedSessions;
    private boolean allowNewPlayers;
    private byte[] currentPatchnotesMD5;
    private Config config;
    private PluginData pluginData;
    private final Map<UUID, EvidenceLocker> evidenceLockers;
    private final Map<UUID, List<PlayerDeath>> deathDatabase;
    private final List<UUID> openEvidenceLockers;
    //                recipient, packages
    private final Map<UUID, List<Package>> packages;
    private Map<String, ItemCollection> itemData;

    public static final List<String> WORLDS = Arrays.asList("world", "world_nether", "world_the_end", "farlands");
    private static final List<String> SCRIPTS = Arrays.asList("artifact.sh", "server.sh", "backup.sh", "restart.sh");
    private static final String MAIN_CONFIG_FILE      = "mainConfig.json";
    private static final String PLUGIN_DATA_FILE      = Directory.DATA        + File.separator + "private.json";
    private static final String PLAYER_DATA_FILE      = Directory.PLAYER_DATA + File.separator + "playerdata.json";
    private static final String EVIDENCE_LOCKERS_FILE = Directory.DATA        + File.separator + "evidenceLockers.nbt";
    private static final String DEATH_DATABASE        = Directory.DATA        + File.separator + "deaths.nbt";
    private static final String PACKAGES_FILE         = Directory.DATA        + File.separator + "packages.nbt";
    private static final String ITEMS_FILE            = Directory.DATA        + File.separator + "items.json";

    private void init() {
        SCRIPTS.forEach(script -> {
            byte[] data;
            try {
                data = getResource("scripts/" + script);
            } catch (IOException ex) {
                System.out.println("Failed to load script: " + script);
                ex.printStackTrace();
                return;
            }
            File file = new File(System.getProperty("user.dir") + "/" + script);
            try {
                FileSystem.createFile(file);
                FileOutputStream fos = new FileOutputStream(file);
                for (byte b : data)
                    fos.write(b);
                fos.flush();
                fos.close();
            } catch (IOException ex) {
                System.out.println("Failed to update script: " + script);
                ex.printStackTrace();
            }
        });

        for (Directory directory : Directory.values()) {
            File dir = FileSystem.getFile(rootDirectory, directory.toString());
            if (!dir.exists() && !dir.mkdirs()) {
                throw new RuntimeException("Failed to create directory during the initialization of the data handler. " +
                        "Did you give the process access to the FS?");
            }
        }

        File playerdataFile = new File(PLAYER_DATA_FILE);
        if (!playerdataFile.exists()) {
            try {
                if (!FileSystem.createFile(playerdataFile))
                    throw new RuntimeException("Failed to create player data file.");
            } catch (IOException ex) {
                throw new RuntimeException("Failed to create player data file.", ex);
            }
        }

        initNbt(EVIDENCE_LOCKERS_FILE);
        initNbt(DEATH_DATABASE);
        initNbt(PACKAGES_FILE);

        Logging.log("Initialized data handler.");
    }

    private void initNbt(String file) {
        File f = FileSystem.getFile(rootDirectory, file);
        if (!f.exists()) {
            try {
                if (!f.createNewFile())
                    throw new RuntimeException("Failed to create " + file + ". Did you give the process access to the FS?");
                NBTCompressedStreamTools.a(new NBTTagCompound(), new FileOutputStream(f));
            } catch (IOException ex) {
                throw new RuntimeException("Failed to create " + file + ". Did you give the process access to the FS?", ex);
            }
        }
    }

    public DataHandler(File rootDirectory) {
        this.pdh = new PlayerDataHandlerOld(FileSystem.getFile(rootDirectory, Directory.DATA.toString(), "playerdata.db"), this);
        this.rootDirectory = rootDirectory;
        this.flPlayerMap = new HashMap<>();
        this.discordMap = new HashMap<>();
        this.sessionMap = new HashMap<>();
        this.cachedSessions = new HashMap<>();
        this.allowNewPlayers = true;
        this.currentPatchnotesMD5 = null;
        this.evidenceLockers = new HashMap<>();
        this.deathDatabase = new HashMap<>();
        this.openEvidenceLockers = new ArrayList<>();
        this.packages = new HashMap<>();
        init();
        loadCriticalData();
        saveCriticalData();
    }

    public void preStartup() {
        loadData();
        saveData();
    }

    @Override
    public void onStartup() {
        FarLands.getScheduler().scheduleSyncRepeatingTask(this::update, 50L, 5L * 60L * 20L);

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            DataHandler dh = FarLands.getDataHandler();
            if (dh.arePatchnotesDifferent()) {
                try {
                    FarLands.getDiscordHandler().sendMessageRaw(DiscordChannel.ANNOUNCEMENTS, "Patch **#" + dh.getCurrentPatch() +
                            "** has been released!\n```" + Chat.removeColorCodes(new String(dh.getResource("patchnotes.txt"),
                            StandardCharsets.UTF_8)) + "```");
                    flPlayerMap.values().forEach(flp -> flp.viewedPatchnotes = false);
                } catch (IOException ex) {
                    Logging.error("Failed to post patch notes to #announcements");
                    ex.printStackTrace(System.out);
                }
            }
        }, 100L);

        int gcCycleTime = FarLands.getFLConfig().gcCycleTime;
        if (gcCycleTime > 0)
            Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), System::gc, 20L * 60L * 5L, 20L * 60L * gcCycleTime);

        System.out.println("Offline FLP Count: " + getOfflineFLPlayers().size());
    }

    @Override
    public void onShutdown() {
        update();
        if (arePatchnotesDifferent()) {
            pluginData.lastPatchnotesMD5 = currentPatchnotesMD5;
            ++pluginData.lastPatch;
        }
        pdh.onShutdown();
        saveCriticalData();
        saveData();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        FLPlayerSession session = getSession(event.getPlayer());

        // Give packages and update
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            session.givePackages();
            session.update(true);
        }, 5L);

        FarLands.getDiscordHandler().updateStats();
    }

    @Override
    public void onPlayerQuit(Player player) {
        FLPlayerSession session = getSession(player);
        session.updatePlaytime();
        session.handle.setLastLocation(player.getLocation());
        if (!session.handle.vanished)
            session.handle.lastLogin = System.currentTimeMillis();

        final UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            FarLands.getDiscordHandler().updateStats();
            FLPlayerSession cached = sessionMap.remove(uuid);
            cached.deactivateAFKChecks();
            cachedSessions.put(uuid, cached);
        }, 5L);

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            if (Bukkit.getPlayer(uuid) == null && cachedSessions.containsKey(uuid))
                cachedSessions.remove(uuid).destroy();
        }, 60L * 20L);
    }

    @EventHandler
    public void onVillagerAcquireTrades(VillagerAcquireTradeEvent event) {
        if (pluginData.isSpawnTrader(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onVillagerChangeCareer(VillagerCareerChangeEvent event) {
        if (pluginData.isSpawnTrader(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onVillagerReplenishTrades(VillagerReplenishTradeEvent event) {
        if (pluginData.isSpawnTrader(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        FLPlayerSession session = getSession(event.getPlayer());
        session.addBackLocation(event.getFrom());
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> session.update(false), 1L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        getSession(event.getEntity()).addBackLocation(event.getEntity().getLocation());

        List<PlayerDeath> deaths = FLUtils.getAndPutIfAbsent(deathDatabase, event.getEntity().getUniqueId(), new ArrayList<>());
        if (deaths.size() >= 3)
            deaths.remove(0);
        deaths.add(new PlayerDeath(event.getEntity()));
    }

    private void update() {
        sessionMap.values().forEach(session -> session.update(true));
        pluginData.getProposals().removeIf(proposal -> {
            proposal.update();
            return proposal.isResolved();
        });
        long currentTime = System.currentTimeMillis();
        pluginData.playerTrades.entrySet().removeIf(entry -> currentTime > entry.getValue().expirationDate);
    }

    public Config getConfig() {
        return config;
    }

    public PluginData getPluginData() {
        return pluginData;
    }

    public boolean allowNewPlayers() {
        return allowNewPlayers;
    }

    public void setAllowNewPlayers(boolean allowNewPlayers) {
        this.allowNewPlayers = allowNewPlayers;
    }

    public boolean arePatchnotesDifferent() {
        if (currentPatchnotesMD5 == null) {
            try {
                byte[] notes = getResource("patchnotes.txt");
                if (notes.length > 0)
                    currentPatchnotesMD5 = FLUtils.hash(notes);
            } catch (IOException ex) {
                Logging.error("Failed to compare patch notes.");
                ex.printStackTrace(System.out);
            }
        }
        return currentPatchnotesMD5 != null && !Arrays.equals(currentPatchnotesMD5, pluginData.lastPatchnotesMD5);
    }

    public int getCurrentPatch() {
        return arePatchnotesDifferent() ? pluginData.lastPatch + 1 : pluginData.lastPatch;
    }

    public byte[] getResource(String filename) throws IOException { // Loads a resource in the default package
        InputStream istream = FarLands.class.getResourceAsStream("/" + filename);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[65535];
        int len;
        while ((len = istream.read(buffer)) > 0)
            baos.write(buffer, 0, len);
        return baos.toByteArray();
    }

    public FLPlayerSession getSession(Player player) {
        FLPlayerSession session = cachedSessions.remove(player.getUniqueId());
        if (session != null) {
            session = new FLPlayerSession(player, session);
            sessionMap.put(player.getUniqueId(), session);
            return session;
        }

        session = sessionMap.get(player.getUniqueId());
        if (session == null) {
            session = new FLPlayerSession(player, getOfflineFLPlayer(player));
            sessionMap.put(player.getUniqueId(), session);
        } else {
            session.update(false);
        }

        return session;
    }

    public Collection<FLPlayerSession> getSessions() {
        return sessionMap.values();
    }

    public OfflineFLPlayer getOfflineFLPlayer(Player player) {
        return getOfflineFLPlayer(player.getUniqueId(), player.getName());
    }

    public OfflineFLPlayer getOfflineFLPlayer(CommandSender sender) {
        if (sender instanceof Player)
            return getOfflineFLPlayer((Player) sender);
        else if (sender instanceof DiscordSender)
            return ((DiscordSender) sender).getFlp();
        return null;
    }

    public OfflineFLPlayer getOfflineFLPlayer(long discordID) {
        return discordMap.get(discordID);
    }

    public OfflineFLPlayer getOfflineFLPlayer(UUID uuid, String username) {
        OfflineFLPlayer flp = flPlayerMap.get(uuid);
        if (flp == null) {
            flp = new OfflineFLPlayer(uuid, username);
            flPlayerMap.put(uuid, flp);
        }
        return flp;
    }

    public OfflineFLPlayer getOfflineFLPlayer(UUID uuid) {
        return flPlayerMap.get(uuid);
    }

    public OfflineFLPlayer getOfflineFLPlayer(String username) {
        return flPlayerMap.values().stream().filter(flp -> username.equalsIgnoreCase(flp.username)).findFirst().orElse(null);
    }

    public OfflineFLPlayer getOfflineFLPlayerMatching(String username) {
        String lowerCaseUsername = username.toLowerCase();
        List<OfflineFLPlayer> matches = new ArrayList<>();
        int matchLevel = 0;

        for (OfflineFLPlayer flp : flPlayerMap.values()) {
            if (flp.username.equals(username))
                return flp;
            else if (matchLevel <= 2 && flp.username.equalsIgnoreCase(username)) {
                if (matchLevel < 2)
                    matches.clear();
                matches.add(flp);
                matchLevel = 2;
            } else if (matchLevel <= 1 && flp.username.toLowerCase().startsWith(lowerCaseUsername)) {
                matches.add(flp);
                matchLevel = 1;
            }
        }

        // Return match with the highest play time
        return matches.stream()
                .max(Comparator.comparingInt(flp -> flp.secondsPlayed))
                .orElse(null);
    }

    public List<OfflineFLPlayer> getOfflineFLPlayers() {
        return new ArrayList<>(flPlayerMap.values());
    }

    public void updateDiscordMap(long oldID, long newID, OfflineFLPlayer flp) {
        discordMap.remove(oldID);
        if (newID != 0)
            discordMap.put(newID, flp);
    }

    public String getDataTextFile(String fileName) throws IOException {
        return FileSystem.readUTF8(FileSystem.getFile(rootDirectory, Directory.DATA.toString(), fileName));
    }

    public File getTempFile(String name) {
        return FileSystem.getFile(rootDirectory, Directory.CACHE.toString(), name);
    }

    public EvidenceLocker getEvidenceLocker(OfflineFLPlayer flp) {
        return FLUtils.getAndPutIfAbsent(evidenceLockers, flp.uuid, new EvidenceLocker(flp)).update(flp);
    }

    public synchronized void openEvidenceLocker(UUID uuid) {
        openEvidenceLockers.add(uuid);
    }

    public synchronized boolean isLockerOpen(UUID uuid) {
        return openEvidenceLockers.contains(uuid);
    }

    public synchronized void closeEvidenceLocker(UUID uuid) {
        openEvidenceLockers.remove(uuid);
    }

    private void loadEvidenceLockers() {
        NBTTagCompound nbt;
        try {
            nbt = NBTCompressedStreamTools.a(new FileInputStream(FileSystem.getFile(rootDirectory, EVIDENCE_LOCKERS_FILE)));
        } catch (IOException ex) {
            Logging.error("Failed to load evidence locker data.");
            ex.printStackTrace(System.out);
            return;
        }

        nbt.getKeys().forEach(key -> evidenceLockers.put(UUID.fromString(key), new EvidenceLocker(nbt.getCompound(key))));
    }

    public void saveEvidenceLockers() {
        NBTTagCompound nbt = new NBTTagCompound();
        evidenceLockers.forEach((key, locker) -> nbt.set(key.toString(), locker.serialize()));

        try {
            NBTCompressedStreamTools.a(nbt, new FileOutputStream(FileSystem.getFile(rootDirectory, EVIDENCE_LOCKERS_FILE)));
        } catch (IOException ex) {
            Logging.error("Failed to save evidence lockers file.");
            ex.printStackTrace(System.out);
        }
    }

    private void loadDeathDatabase() {
        NBTTagCompound nbt;
        try {
            nbt = NBTCompressedStreamTools.a(new FileInputStream(FileSystem.getFile(rootDirectory, DEATH_DATABASE)));
        } catch (IOException ex) {
            Logging.error("Failed to load death database.");
            ex.printStackTrace(System.out);
            return;
        }

        nbt.getKeys().forEach(uuid -> {
            List<PlayerDeath> deaths = new ArrayList<>();
            nbt.getList(uuid, 10).stream().map(base -> new PlayerDeath((NBTTagCompound) base)).forEach(deaths::add);
            deathDatabase.put(UUID.fromString(uuid), deaths);
        });
    }

    public void saveDeathDatabase() {
        NBTTagCompound nbt = new NBTTagCompound();
        deathDatabase.forEach((uuid, deathList) -> {
            NBTTagList serDeathList = new NBTTagList();
            deathList.stream().map(PlayerDeath::serialize).forEach(serDeathList::add);
            nbt.set(uuid.toString(), serDeathList);
        });

        try {
            NBTCompressedStreamTools.a(nbt, new FileOutputStream(FileSystem.getFile(rootDirectory, DEATH_DATABASE)));
        } catch (IOException ex) {
            Logging.error("Failed to save evidence lockers file.");
            ex.printStackTrace(System.out);
        }
    }

    public void loadPackages() {
        NBTTagCompound nbt;
        try {
            nbt = NBTCompressedStreamTools.a(new FileInputStream(FileSystem.getFile(rootDirectory, PACKAGES_FILE)));
        } catch (IOException ex) {
            Logging.error("Failed to load items file.");
            ex.printStackTrace(System.out);
            return;
        }
        if (nbt.isEmpty())
            return;

        List<Package> rtsPackages = new ArrayList<>();
        nbt.getKeys().forEach(key -> {
            NBTTagCompound serIndividualPackages = nbt.getCompound(key);
            List<Package> individualPackages = new ArrayList<>();
            serIndividualPackages.getKeys().forEach(packageSender -> {
                NBTTagCompound packageNBT = serIndividualPackages.getCompound(packageSender);
                String uuid = packageNBT.getString("sender");
                Package lPackage = new Package(
                        uuid.isEmpty() ? null : UUID.fromString(uuid), packageSender,
                        FLUtils.itemStackFromNBT(packageNBT.getCompound("item")),
                        packageNBT.getString("message"), packageNBT.getLong("sentTime"),
                        packageNBT.getBoolean("forceSend"));
                if (lPackage.hasExpired())
                    rtsPackages.add(lPackage);
                else
                    individualPackages.add(lPackage);
            });
            packages.put(UUID.fromString(key), individualPackages);
        });
        rtsPackages.forEach(lPackage -> addPackage(lPackage.senderUuid,
                new Package(null, "FarLands Packaging Service",
                lPackage.item, "Return To Sender", true)
        ));
    }

    public void savePackages() {
        NBTTagCompound nbt = new NBTTagCompound();
        packages.forEach((uuid, individualPackages) -> {
            if (uuid != null) {
                NBTTagCompound serIndividualPackages = new NBTTagCompound();
                individualPackages.forEach(lPackage -> {
                    NBTTagCompound packageNBT = new NBTTagCompound();
                    packageNBT.setString("sender", lPackage.senderUuid == null ? "" : lPackage.senderUuid.toString());
                    packageNBT.set("item", FLUtils.itemStackToNBT(lPackage.item));
                    packageNBT.setString("message", lPackage.message);
                    packageNBT.setLong("sentTime", lPackage.sentTime);
                    packageNBT.setBoolean("forceSend", lPackage.forceSend);
                    serIndividualPackages.set(lPackage.senderName, packageNBT);
                });
                nbt.set(uuid.toString(), serIndividualPackages);
            }
        });

        try {
            NBTCompressedStreamTools.a(nbt, new FileOutputStream(FileSystem.getFile(rootDirectory, PACKAGES_FILE)));
        } catch (IOException ex) {
            Logging.error("Failed to save items file.");
            ex.printStackTrace(System.out);
        }
    }

    public boolean addPackage(UUID recipient, Package pack) {
        List<Package> localPackages;
        if (!packages.containsKey(recipient))
            packages.put(recipient, new ArrayList<>());
        localPackages = packages.get(recipient);

        for (Package lPackage : localPackages) {
            if (pack.senderUuid != null && pack.senderUuid.equals(lPackage.senderUuid))
                return false;
        }
        localPackages.add(pack);
        return true;
    }

    public List<Package> getPackages(UUID recipient) {
        List<Package> lPackages = packages.get(recipient);
        return lPackages == null ? Collections.emptyList() : lPackages;
    }

    public List<PlayerDeath> getDeaths(UUID uuid) {
        return FLUtils.getAndPutIfAbsent(deathDatabase, uuid, new ArrayList<>());
    }

    public ItemCollection getItemCollection(String name) {
        return itemData.get(name);
    }

    public void loadCriticalData() {
        config =     FileSystem.loadJson(Config.class,     FileSystem.getFile(rootDirectory, MAIN_CONFIG_FILE));
        pluginData = FileSystem.loadJson(PluginData.class, FileSystem.getFile(rootDirectory, PLUGIN_DATA_FILE));
    }

    public void saveCriticalData() {
        FileSystem.saveJson(config,     FileSystem.getFile(rootDirectory, MAIN_CONFIG_FILE));
        FileSystem.saveJson(pluginData, FileSystem.getFile(rootDirectory, PLUGIN_DATA_FILE));
    }

    public void loadData() {
        FileSystem.loadJson(new TypeToken<Collection<OfflineFLPlayer>>() { }, Collections.emptyList(),
                FileSystem.getFile(rootDirectory, PLAYER_DATA_FILE)).forEach(flp -> {
            flPlayerMap.put(flp.uuid, flp);
            if (flp.isDiscordVerified())
                discordMap.put(flp.discordID, flp);
        });

        try {
            // Convert SQL things
            ResultSet rs = pdh.query("select uuid from playerdata");
            List<UUID> sqlUuids = new ArrayList<>();
            try {
                while (rs.next()) {
                    byte[] uuid = rs.getBytes(1);
                    sqlUuids.add(FLUtils.getUuid(uuid, 0));
                }
                rs.close();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            //sqlUuids.forEach(uuid -> {
            //    OfflineFLPlayer flp = pdh.loadFLPlayer(uuid, null);
            //    if (flPlayerMap.containsKey(uuid))
            //        flPlayerMap.get(uuid).discordID = flp.discordID;
            //});
            sqlUuids.stream().filter(uuid -> !flPlayerMap.containsKey(uuid)).forEach(uuid -> {
                OfflineFLPlayer flp = pdh.loadFLPlayer(uuid, null);
                flp.notes.addAll(pdh.getNotes(uuid));
                flPlayerMap.put(uuid, flp);
            });
        } catch (Throwable t) {
            t.printStackTrace(System.out);
        }

        itemData = FileSystem.loadJson(new TypeToken<HashMap<String, ItemCollection>>() { }, new HashMap<>(),
                FileSystem.getFile(rootDirectory, ITEMS_FILE));
        loadEvidenceLockers();
        loadDeathDatabase();
        loadPackages();
    }

    public void saveData() {
        // Disable pretty-printing
        FileSystem.saveJson((new GsonBuilder()).create(), flPlayerMap.values(), FileSystem.getFile(rootDirectory, PLAYER_DATA_FILE));
        FileSystem.saveJson(itemData, FileSystem.getFile(rootDirectory, ITEMS_FILE));
        saveEvidenceLockers();
        saveDeathDatabase();
        savePackages();
    }

    public enum Directory {
        PLAYER_DATA("playerdata"),
        DATA("data"),
        CACHE("cache");

        public static final Directory[] VALUES = values();

        private final String directory;

        Directory(String directory) {
            this.directory = directory;
        }

        @Override
        public String toString() {
            return directory;
        }
    }

    public enum Server {
        MAIN, DEV;

        public static final Server[] VALUES = values();
    }
}
