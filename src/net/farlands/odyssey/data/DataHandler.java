package net.farlands.odyssey.data;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.struct.*;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.mechanic.Mechanic;
import net.farlands.odyssey.util.FileSystem;
import net.farlands.odyssey.util.Logging;
import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.FLUtils;

import net.minecraft.server.v1_15_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NBTTagList;
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
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

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
    private byte[] currentPatchnotesMD5;
    private Config config;
    private PluginData pluginData;
    private Map<UUID, EvidenceLocker> evidenceLockers;
    private Map<UUID, List<PlayerDeath>> deathDatabase;
    private List<UUID> openEvidenceLockers;
    private Map<UUID, Map<String, Pair<org.bukkit.inventory.ItemStack, String>>> packages;

    public static final List<String> WORLDS = Arrays.asList("world", "world_nether", "world_the_end", "farlands");
    private static final List<String> SCRIPTS = Arrays.asList("artifact.sh", "server.sh", "backup.sh", "restart.sh");
    private static final Map<String, String> DIRECTORIES = FLUtils.asMap(
        new Pair<>("playerdata", "playerdata"),
        new Pair<>("data", "data"), // General plugin data
        new Pair<>("tmp", "cache")
    );
    private static final String MAIN_CONFIG_FILE = "mainConfig.json";
    private static final String PLUGIN_DATA_FILE = DIRECTORIES.get("data") + File.separator + "private.json";
    private static final String PLAYER_DATA_FILE = DIRECTORIES.get("playerdata") + File.separator + "playerdata.json";
    private static final String EVIDENCE_LOCKERS_FILE = DIRECTORIES.get("data") + File.separator + "evidenceLockers.nbt";
    private static final String DEATH_DATABASE = DIRECTORIES.get("data") + File.separator + "deaths.nbt";
    private static final String PACKAGES_FILE = DIRECTORIES.get("data") + File.separator + "packages.nbt";

    private void init() {
        SCRIPTS.forEach(script -> {
            byte[] data;
            try {
                data = getResource("scripts/" + script);
            }catch(IOException ex) {
                System.out.println("Failed to load script: " + script);
                ex.printStackTrace();
                return;
            }
            File file = new File(System.getProperty("user.dir") + "/" + script);
            try {
                FileSystem.createFile(file);
                FileOutputStream fos = new FileOutputStream(file);
                for(byte b : data)
                    fos.write(b);
                fos.flush();
                fos.close();
            }catch(IOException ex) {
                System.out.println("Failed to update script: " + script);
                ex.printStackTrace();
            }
        });
        for(String dirName : DIRECTORIES.values()) {
            File dir = FileSystem.getFile(rootDirectory, dirName);
            if(!dir.exists() && !dir.mkdirs())
                throw new RuntimeException("Failed to create directory during the initialization of the data handler. " +
                        "Did you give the process access to the FS?");
        }
        File playerdataFile = new File(PLAYER_DATA_FILE);
        if(!playerdataFile.exists()) {
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
        if(!f.exists()) {
            try {
                if(!f.createNewFile())
                    throw new RuntimeException("Failed to create " + file + ". Did you give the process access to the FS?");
                NBTCompressedStreamTools.a(new NBTTagCompound(), new FileOutputStream(f));
            }catch(IOException ex) {
                throw new RuntimeException("Failed to create " + file + ". Did you give the process access to the FS?", ex);
            }
        }
    }

    public DataHandler(File rootDirectory) {
        this.pdh = new PlayerDataHandlerOld(FileSystem.getFile(rootDirectory, DIRECTORIES.get("data"), "playerdata.db"), this);
        this.rootDirectory = rootDirectory;
        this.flPlayerMap = new HashMap<>();
        this.discordMap = new HashMap<>();
        this.sessionMap = new HashMap<>();
        this.cachedSessions = new HashMap<>();
        this.currentPatchnotesMD5 = null;
        this.evidenceLockers = new HashMap<>();
        this.deathDatabase = new HashMap<>();
        this.openEvidenceLockers = new ArrayList<>();
        this.packages = new HashMap<>();
        init();
        loadData();
        saveData();
    }

    @Override
    public void onStartup() {
        loadNbt();
        saveNbt();

        FarLands.getScheduler().scheduleSyncRepeatingTask(this::update, 50L, 5L * 60L * 20L);

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            DataHandler dh = FarLands.getDataHandler();
            if(dh.arePatchnotesDifferent()) {
                try {
                    FarLands.getDiscordHandler().sendMessageRaw("announcements", "@everyone Patch **#" + dh.getCurrentPatch() +
                            "** has been released!\n```" + Chat.removeColorCodes(new String(dh.getResource("patchnotes.txt"),
                            StandardCharsets.UTF_8)) + "```");
                    flPlayerMap.values().forEach(flp -> flp.setViewedPatchnotes(false));
                }catch(IOException ex) {
                    Logging.error("Failed to post patch notes to #announcements");
                    ex.printStackTrace(System.out);
                }
            }
        }, 100L);

        int gcCycleTime = FarLands.getFLConfig().gcCycleTime;
        if(gcCycleTime > 0)
            Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), System::gc, 20L * 60L * 5L, 20L * 60L * gcCycleTime);
    }

    @Override
    public void onShutdown() {
        update();
        if(arePatchnotesDifferent()) {
            pluginData.lastPatchnotesMD5 = currentPatchnotesMD5;
            ++pluginData.lastPatch;
        }
        pdh.onShutdown();
        saveData();
        saveNbt();
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FLPlayerSession session = getSession(player);

        // Give packages and update
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            Map<String, Pair<ItemStack, String>> packages = getAndRemovePackages(player.getUniqueId());
            if(!packages.isEmpty()) {
                // Notify the player how many packages they're getting
                player.spigot().sendMessage(TextUtils.format("&(gold)Receiving {&(aqua)%0} $(inflect,noun,0,package) from {&(aqua)%1}",
                        packages.size(), packages.keySet().stream().map(sender -> "{" + sender + "}").collect(Collectors.joining(", "))));

                // Give the packages and send the messages
                packages.values().forEach(item -> {
                    final String message = item.getSecond();
                    if (message != null && !message.isEmpty())
                        player.spigot().sendMessage(TextUtils.format("&(gold)Item {&(aqua)%0} was sent with the following message {&(aqua)%1}",
                                FLUtils.itemName(item.getFirst()), message));
                    FLUtils.giveItem(player, item.getFirst(), true);
                });
            }

            session.update(true);
        }, 5L);

        FarLands.getDiscordHandler().updateStats();
    }

    @Override
    public void onPlayerQuit(Player player) {
        FLPlayerSession session = getSession(player);
        session.updatePlaytime();
        session.handle.setLastLocation(player.getLocation());
        if(!session.handle.vanished)
            session.handle.lastLogin = System.currentTimeMillis();

        final UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            FarLands.getDiscordHandler().updateStats();
            FLPlayerSession cached = sessionMap.remove(uuid);
            cached.deactivateAFKChecks();
            cachedSessions.put(uuid, cached);
        }, 5L);

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            if (Bukkit.getPlayer(uuid) == null)
                cachedSessions.remove(uuid).destroy();
        }, 60L * 20L);
    }

    @EventHandler
    public void onVillagerAcquireTrades(VillagerAcquireTradeEvent event) {
        if(pluginData.isSpawnTrader(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onVillagerChangeCareer(VillagerCareerChangeEvent event) {
        if(pluginData.isSpawnTrader(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onVillagerReplenishTrades(VillagerReplenishTradeEvent event) {
        if(pluginData.isSpawnTrader(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled=true)
    public void onTeleport(PlayerTeleportEvent event) {
        FLPlayerSession session = getSession(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> session.update(false), 1L);
        if (!session.ignoreTeleportForBackLocations()) {
            if(session.backLocations.size() >= 5)
                session.backLocations.remove(0);
            if(!session.backLocations.contains(event.getFrom()))
                session.backLocations.add(event.getFrom());
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        FLPlayerSession session = getSession(event.getEntity());
        if(session.backLocations.size() >= 5)
            session.backLocations.remove(0);
        session.backLocations.add(event.getEntity().getLocation());

        List<PlayerDeath> deaths = FLUtils.getAndPutIfAbsent(deathDatabase, event.getEntity().getUniqueId(), new ArrayList<>());
        if(deaths.size() >= 3)
            deaths.remove(0);
        deaths.add(new PlayerDeath(event.getEntity()));
    }

    private void update() {
        sessionMap.values().forEach(session -> session.update(true));
        pluginData.getProposals().removeIf(proposal -> {
            proposal.update();
            return proposal.isResolved();
        });
    }

    public Config getConfig() {
        return config;
    }

    public PluginData getPluginData() {
        return pluginData;
    }

    public boolean arePatchnotesDifferent() {
        if(currentPatchnotesMD5 == null) {
            try {
                byte[] notes = getResource("patchnotes.txt");
                if(notes.length > 0)
                    currentPatchnotesMD5 = FLUtils.hash(notes);
            }catch(IOException ex) {
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
        while((len = istream.read(buffer)) > 0)
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
        }

        return session;
    }

    public OfflineFLPlayer getOfflineFLPlayer(Player player) {
        return getOfflineFLPlayer(player.getUniqueId(), player.getName());
    }

    public OfflineFLPlayer getOfflineFLPlayer(CommandSender sender) {
        if(sender instanceof Player)
            return getOfflineFLPlayer((Player)sender);
        else if(sender instanceof DiscordSender)
            return ((DiscordSender)sender).getFlp();
        return null;
    }

    public OfflineFLPlayer getOfflineFLPlayer(long discordID) {
        return discordMap.get(discordID);
    }

    public OfflineFLPlayer getOfflineFLPlayer(UUID uuid, String username) {
        OfflineFLPlayer flp = flPlayerMap.get(uuid);
        if(flp == null) {
            flp = new OfflineFLPlayer(uuid, username);
            flPlayerMap.put(uuid, flp);
        }
        return flp;
    }

    public OfflineFLPlayer getOfflineFLPlayer(UUID uuid) {
        return flPlayerMap.get(uuid);
    }

    public OfflineFLPlayer getOfflineFLPlayer(String username) {
        return flPlayerMap.values().stream().filter(flp -> username.equalsIgnoreCase(flp.getUsername())).findFirst().orElse(null);
    }

    public OfflineFLPlayer getOfflineFLPlayerMatching(String username) {
        String lowerCaseUsername = username.toLowerCase();
        OfflineFLPlayer match = null;
        int matchLevel = 0;
        for(OfflineFLPlayer flp : flPlayerMap.values()) {
            if(flp.username.equals(username))
                return flp;
            else if(matchLevel == 1 && flp.username.equalsIgnoreCase(username)) {
                match = flp;
                matchLevel = 2;
            }else if(matchLevel == 0 && flp.username.toLowerCase().startsWith(lowerCaseUsername)) {
                match = flp;
                matchLevel = 1;
            }
        }
        return match;
    }

    public List<OfflineFLPlayer> getOfflineFLPlayers() {
        return new ArrayList<>(flPlayerMap.values());
    }

    public void updateDiscordMap(long discordID, OfflineFLPlayer flp) {
        discordMap.put(discordID, flp);
    }

    public String getDataTextFile(String fileName) throws IOException {
        return FileSystem.readUTF8(FileSystem.getFile(rootDirectory, DIRECTORIES.get("data"), fileName));
    }

    public File getDirectory(String name) {
        String dir = DIRECTORIES.get(name);
        if(dir == null)
            return null;
        return FileSystem.getFile(rootDirectory, dir);
    }

    public File getTempFile(String name) {
        return FileSystem.getFile(rootDirectory, DIRECTORIES.get("tmp"), name);
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
        }catch(IOException ex) {
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
        }catch(IOException ex) {
            Logging.error("Failed to save evidence lockers file.");
            ex.printStackTrace(System.out);
        }
    }

    private void loadDeathDatabase() {
        NBTTagCompound nbt;
        try {
            nbt = NBTCompressedStreamTools.a(new FileInputStream(FileSystem.getFile(rootDirectory, DEATH_DATABASE)));
        }catch(IOException ex) {
            Logging.error("Failed to load death database.");
            ex.printStackTrace(System.out);
            return;
        }

        nbt.getKeys().forEach(uuid -> {
            List<PlayerDeath> deaths = new ArrayList<>();
            nbt.getList(uuid, 10).stream().map(base -> new PlayerDeath((NBTTagCompound)base)).forEach(deaths::add);
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
        }catch(IOException ex) {
            Logging.error("Failed to save evidence lockers file.");
            ex.printStackTrace(System.out);
        }
    }

    public void loadPackages() {
        NBTTagCompound nbt;
        try {
            nbt = NBTCompressedStreamTools.a(new FileInputStream(FileSystem.getFile(rootDirectory, PACKAGES_FILE)));
        }catch(IOException ex) {
            Logging.error("Failed to load items file.");
            ex.printStackTrace(System.out);
            return;
        }
        if(nbt.isEmpty())
            return;

        nbt.getKeys().forEach(key -> {
            NBTTagCompound serIndividualPackages = nbt.getCompound(key);
            Map<String, Pair<ItemStack, String>> individualPackages = new HashMap<>();
            serIndividualPackages.getKeys().forEach(packageSender -> {
                NBTTagCompound serPackage = serIndividualPackages.getCompound(packageSender);
                individualPackages.put(packageSender, new Pair<>(FLUtils.itemStackFromNBT(serPackage.getCompound("item")),
                        serPackage.getString("message")));
            });
            packages.put(UUID.fromString(key), individualPackages);
        });
    }

    public void savePackages() {
        NBTTagCompound nbt = new NBTTagCompound();
        packages.forEach((uuid, individualPackages) -> {
            NBTTagCompound serIndividualPackages = new NBTTagCompound();
            individualPackages.forEach((packageSender, pkg) -> {
                NBTTagCompound serPackage = new NBTTagCompound();
                serPackage.set("item", FLUtils.itemStackToNBT(pkg.getFirst()));
                serPackage.setString("message", pkg.getSecond());
                serIndividualPackages.set(packageSender, serPackage);
            });
            nbt.set(uuid.toString(), serIndividualPackages);
        });

        try {
            NBTCompressedStreamTools.a(nbt, new FileOutputStream(FileSystem.getFile(rootDirectory, PACKAGES_FILE)));
        }catch(IOException ex) {
            Logging.error("Failed to save items file.");
            ex.printStackTrace(System.out);
        }
    }

    public boolean addPackage(UUID recipient, String sender, ItemStack stack, String message) {
        packages.putIfAbsent(recipient, new HashMap<>());
        Map<String, Pair<ItemStack, String>> pkgs = packages.get(recipient);
        if(pkgs.containsKey(sender))
            return false;
        else{
            pkgs.put(sender, new Pair<>(stack, message));
            return true;
        }
    }

    public Map<String, Pair<ItemStack, String>> getAndRemovePackages(UUID recipient) {
        if(!packages.containsKey(recipient))
            return Collections.emptyMap();
        return packages.remove(recipient);
    }

    public List<PlayerDeath> getDeaths(UUID uuid) {
        return FLUtils.getAndPutIfAbsent(deathDatabase, uuid, new ArrayList<>());
    }

    public void loadData() {
        try {
            String flPlayerMapData = FileSystem.readUTF8(FileSystem.getFile(rootDirectory, PLAYER_DATA_FILE));
            if (flPlayerMapData.isEmpty())
                flPlayerMapData = "[]";
            Collection<OfflineFLPlayer> flps = FarLands.getGson().fromJson(flPlayerMapData, new TypeToken<Collection<OfflineFLPlayer>>(){}.getType());
            flps.forEach(flp -> {
                flPlayerMap.put(flp.uuid, flp);
                if(flp.isDiscordVerified())
                    discordMap.put(flp.discordID, flp);
            });
        }catch (IOException ex) {
            throw new RuntimeException("Failed to load player data.", ex);
        }

        // Convert SQL things
        ResultSet rs = pdh.query("select uuid from playerdata");
        List<UUID> sqlUuids = new ArrayList<>();
        try {
            while (rs.next()) {
                byte[] uuid = rs.getBytes(1);
                sqlUuids.add(FLUtils.getUuid(uuid, 0));
            }
            rs.close();
        }catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        sqlUuids.stream().filter(uuid -> !flPlayerMap.containsKey(uuid)).forEach(uuid -> {
            OfflineFLPlayer flp = pdh.getFLPlayer(uuid);
            flp.notes.addAll(pdh.getNotes(uuid));
            flPlayerMap.put(uuid, flp);
        });

        config = FileSystem.loadJson(Config.class, FileSystem.getFile(rootDirectory, MAIN_CONFIG_FILE));
        pluginData = FileSystem.loadJson(PluginData.class, FileSystem.getFile(rootDirectory, PLUGIN_DATA_FILE));
    }

    public void saveData() {
        FileSystem.saveJson((new GsonBuilder()).create(), flPlayerMap.values(), FileSystem.getFile(rootDirectory, PLAYER_DATA_FILE));
        FileSystem.saveJson(config, FileSystem.getFile(rootDirectory, MAIN_CONFIG_FILE));
        FileSystem.saveJson(pluginData, FileSystem.getFile(rootDirectory, PLUGIN_DATA_FILE));
    }

    public void loadNbt() {
        loadEvidenceLockers();
        loadDeathDatabase();
        loadPackages();
    }

    public void saveNbt() {
        saveEvidenceLockers();
        saveDeathDatabase();
        savePackages();
    }
}
