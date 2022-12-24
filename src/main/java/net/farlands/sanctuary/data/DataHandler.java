package net.farlands.sanctuary.data;

import com.kicasmads.cs.ChestShops;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import net.dv8tion.jda.api.EmbedBuilder;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.DiscordSender;
import net.farlands.sanctuary.data.struct.Package;
import net.farlands.sanctuary.data.struct.*;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.mechanic.Mechanic;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.FileSystem;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Handles all data for the plugin
 */
public class DataHandler extends Mechanic {

    private final File                         rootDirectory;
    private final Map<UUID, OfflineFLPlayer>   flPlayerMap; // Map of all UUIDs to flps
    private final Map<Long, OfflineFLPlayer>   discordMap; // Map of all DiscordIDs to Flp
    private final Map<UUID, FLPlayerSession>   sessionMap; // Map of all UUIDs to active player sessions
    private final Map<UUID, FLPlayerSession>   cachedSessions; // Map of all UUIDs to cached sessions
    private final Map<UUID, EvidenceLocker>    evidenceLockers; // Player's evidence lockers
    private final Map<UUID, List<PlayerDeath>> deathDatabase; // Player's deaths
    private final List<UUID>                   openEvidenceLockers; // Currently open ELs by staff members
    private final Map<UUID, List<Package>>     packages; // Map of Recipient : Packages


    private boolean    allowNewPlayers; // If the server is allowing new people to join the server (bot spam)
    private byte[]     currentPatchnotesMD5; // Current MD5 hash for the patchnotes
    private Config     config; // Server config
    private PluginData pluginData; // Plugin private data

    // Item Storage
    private final Map<String, ItemCollection>  itemCollections; // Collections of items, used for rewards
    private final Map<String, ItemStack>       items; // Individual item storage, used for items like the armor stand book
    private final Map<String, List<ItemStack>> itemLists; // List of specific items, used for voting rewards

    private static final List<String> SCRIPTS = Arrays.asList("artifact.sh", "server.sh", "backup.sh", "restart.sh"); // Scripts needed to copy into the root from /scripts in the resources folder

    private static final String MAIN_CONFIG_FILE      = "mainConfig.json"; // Name of the main config file
    private static final String PLUGIN_DATA_FILE      = path(Directory.DATA, "private.json"); // Location of the private data file
    private static final String PLAYER_DATA_FILE      = path(Directory.PLAYER_DATA, "playerdata.json"); // Location of the playerdata file
    private static final String EVIDENCE_LOCKERS_FILE = path(Directory.DATA, "evidenceLockers.nbt"); // Location of the evidence lockers file
    private static final String DEATH_DATABASE        = path(Directory.DATA, "deaths.nbt"); // Location of the deaths file
    private static final String PACKAGES_FILE         = path(Directory.DATA, "packages.nbt"); // Location of the packages file
    private static final String ITEMS_FILE            = path(Directory.DATA, "items.nbt"); // Location of the custom items file

    /**
     * Runs when the plugin is initialised
     */
    private void init() {
        // Copy all of the scripts into the root of the server
        SCRIPTS.forEach(script -> {
            byte[] data;
            try {
                data = getResource("scripts/" + script);
            } catch (IOException ex) {
                Logging.error("Failed to load script: " + script);
                ex.printStackTrace();
                return;
            }
            File file = new File(System.getProperty("user.dir") + "/" + script);
            try {
                FileSystem.createFile(file);
                FileOutputStream fos = new FileOutputStream(file);
                for (byte b : data) {
                    fos.write(b);
                }
                fos.flush();
                fos.close();
            } catch (IOException ex) {
                Logging.error("Failed to update script: " + script);
                ex.printStackTrace();
            }
        });

        // Create required directories
        for (Directory directory : Directory.values()) {
            File dir = FileSystem.getFile(rootDirectory, directory.toString());
            if (!dir.exists() && !dir.mkdirs()) {
                throw new RuntimeException("Failed to create directory during the initialization of the data handler. " +
                                           "Did you give the process access to the FS?");
            }
        }

        // Initialise playerdata file
        File playerdataFile = new File(PLAYER_DATA_FILE);
        if (!playerdataFile.exists()) {
            try {
                if (!FileSystem.createFile(playerdataFile)) {
                    throw new RuntimeException("Failed to create player data file.");
                }
            } catch (IOException ex) {
                throw new RuntimeException("Failed to create player data file.", ex);
            }
        }

        // Load the plugin's data
        initNbt(EVIDENCE_LOCKERS_FILE);
        initNbt(DEATH_DATABASE);
        initNbt(PACKAGES_FILE);
        initNbt(ITEMS_FILE);

        Logging.log("Initialized data handler.");
    }

    /**
     * Read or create an NBT file.
     *
     * @param file Path to the file
     */
    private void initNbt(String file) {
        File f = FileSystem.getFile(rootDirectory, file);
        if (!f.exists()) {
            try {
                if (!f.createNewFile()) {
                    throw new RuntimeException("Failed to create " + file + ". Did you give the process access to the FS?");
                }
                BinaryTagIO.writer().write(CompoundBinaryTag.empty(), new FileOutputStream(f));
            } catch (IOException ex) {
                throw new RuntimeException("Failed to create " + file + ". Did you give the process access to the FS?", ex);
            }
        }
    }

    public DataHandler(File rootDirectory) {
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

        this.itemCollections = new HashMap<>();
        this.itemLists = new HashMap<>();
        this.items = new HashMap<>();

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
        FarLands.getScheduler().scheduleSyncRepeatingTask(this::update, 50L, 5 * 60 * 20L); // Update every 5 minutes

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            DataHandler dh = FarLands.getDataHandler();
            if (dh.arePatchnotesDifferent()) { // Handle patchnotes for the server
                try {
                    String patchNotes = FLUtils.removeColorCodes(new String(dh.getResource("patchnotes.txt"), StandardCharsets.UTF_8));
                    String[] lines = patchNotes.split("\n");

                    String currentSection = "\u200B"; // Default to 0-width space if no section name provided
                    Map<String, List<String>> sections = new HashMap<>(); // Store the sections
                    List<String> sectionsList = new ArrayList<>(); // This is to keep the sections in order

                    for (String line : lines) { // break it apart to find each section
                        if (line.matches("^[\\w\\d .,]+:$")) { // Match `Name:`
                            currentSection = line.replaceAll(":$", "");
                            sectionsList.add(currentSection);
                        } else {
                            sections.putIfAbsent(currentSection, new ArrayList<>());
                            sections.get(currentSection).add(line);
                        }
                    }

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Patch Notes")
                        .setDescription("Patch **#" + dh.getCurrentPatch() + "** has been released!")
                        .setColor(NamedTextColor.GOLD.value());

                    sectionsList.forEach(s -> {
                        String v = String.join("\n", sections.get(s));
                        if (v.length() >= 1018) {
                            v = v.substring(0, 1014) + "...";
                        }
                        eb.addField(s, "```" + v + "```", false);
                    });

                    FarLands.getDiscordHandler().sendMessageEmbed(DiscordChannel.ANNOUNCEMENTS, eb);
                    flPlayerMap.values().forEach(flp -> flp.viewedPatchnotes = false); // Reset everyone's view patchnotes setting
                } catch (IOException ex) {
                    Logging.error("Failed to post patch notes to #announcements");
                    ex.printStackTrace();
                }
            }
        }, 100L);

        int gcCycleTime = FarLands.getFLConfig().gcCycleTime;
        if (gcCycleTime > 0) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), System::gc, 5 * 60 * 20L, gcCycleTime * 60 * 20L); // Run GC every gcCycleTime minutes (after the first 5 minutes)
        }

        FarLands.getDebugger().echo("Offline FLP Count: " + this.getOfflineFLPlayers().size());
    }

    @Override
    public void onShutdown() {
        update();
        if (arePatchnotesDifferent()) {
            pluginData.lastPatchnotesMD5 = currentPatchnotesMD5; // Update patchnotes hash, so it doesn't post again (until the notes are changed)
            ++pluginData.lastPatch;
        }

        // Save plugin data
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
        if (!session.handle.vanished) {
            session.handle.lastLogin = System.currentTimeMillis();
        }

        final UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            FarLands.getDiscordHandler().updateStats();
            FLPlayerSession cached = sessionMap.remove(uuid);
            cached.deactivateAFKChecks();
            cachedSessions.put(uuid, cached);
        }, 5L); // Update the player's session

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            if (Bukkit.getPlayer(uuid) == null && cachedSessions.containsKey(uuid)) {
                cachedSessions.remove(uuid).destroy();
            }
        }, 60 * 20L); // Remove player session after 1 minute
    }

    @EventHandler
    public void onVillagerAcquireTrades(VillagerAcquireTradeEvent event) {
        if (pluginData.isSpawnTrader(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVillagerChangeCareer(VillagerCareerChangeEvent event) {
        if (pluginData.isSpawnTrader(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVillagerReplenishTrades(VillagerReplenishTradeEvent event) {
        if (pluginData.isSpawnTrader(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
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
        if (deaths.size() >= 3) {
            deaths.remove(0);
        }
        deaths.add(new PlayerDeath(event.getEntity())); // Update death db
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

    /**
     * Compare patchnotes hash with the previous version
     */
    public boolean arePatchnotesDifferent() {
        if (currentPatchnotesMD5 == null) {
            try {
                byte[] notes = getResource("patchnotes.txt");
                if (notes.length > 0) {
                    currentPatchnotesMD5 = FLUtils.hash(notes);
                }
            } catch (IOException ex) {
                Logging.error("Failed to compare patch notes.");
                ex.printStackTrace();
            }
        }
        return currentPatchnotesMD5 != null && !Arrays.equals(currentPatchnotesMD5, pluginData.lastPatchnotesMD5);
    }

    public int getCurrentPatch() {
        return arePatchnotesDifferent() ? pluginData.lastPatch + 1 : pluginData.lastPatch;
    }

    /**
     * Get a resource from the plugin's resource folder
     *
     * @param filePath The path to the file
     * @return The file's contents
     */
    public byte[] getResource(String filePath) throws IOException {
        InputStream istream = FarLands.class.getResourceAsStream("/" + filePath);
        if (istream == null) throw new FileNotFoundException("Failed to load resource: " + filePath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[65535];
        int len;
        while ((len = istream.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    /**
     * Get a player's session
     */
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

    /**
     * Get flp from CommandSender
     */
    public OfflineFLPlayer getOfflineFLPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return getOfflineFLPlayer(player.getUniqueId(), player.getName());
        } else if (sender instanceof DiscordSender ds) {
            return ds.getFlp();
        }
        return null;
    }

    /**
     * Get flp from DiscordID
     */
    public OfflineFLPlayer getOfflineFLPlayer(long discordID) {
        return discordMap.get(discordID);
    }

    /**
     * Get flp from UUID or add a new flp by username
     */
    public OfflineFLPlayer getOfflineFLPlayer(UUID uuid, String username) {
        OfflineFLPlayer flp = flPlayerMap.get(uuid);
        if (flp == null) {
            flp = new OfflineFLPlayer(uuid, username);
            flPlayerMap.put(uuid, flp);
        }
        return flp;
    }

    /**
     * Get flp by UUID
     */
    public OfflineFLPlayer getOfflineFLPlayer(UUID uuid) {
        return flPlayerMap.get(uuid);
    }

    /**
     * Get flp full username match (ignore case)
     */
    public OfflineFLPlayer getOfflineFLPlayer(String username) {
        return flPlayerMap.values().stream().filter(flp -> username.equalsIgnoreCase(flp.username)).findFirst().orElse(null);
    }

    /**
     * Get flp by partial name (ignore case)
     * <br>
     * For multiple matches, player with the top playtime is returned
     */
    public OfflineFLPlayer getOfflineFLPlayerMatching(String partial) {
        String lowerCaseUsername = partial.toLowerCase();
        List<OfflineFLPlayer> matches = new ArrayList<>();
        int matchLevel = 0;

        for (OfflineFLPlayer flp : flPlayerMap.values()) {
            if (flp.username.equals(partial)) {
                return flp;
            } else if (flp.username.equalsIgnoreCase(partial)) {
                if (matchLevel < 2) {
                    matches.clear();
                }
                matches.add(flp);
                matchLevel = 2;
            } else if (matchLevel <= 1 && flp.username.toLowerCase().startsWith(lowerCaseUsername)) {
                matches.add(flp);
                matchLevel = 1;
            }
        }

        return matches.stream()
            .max(Comparator.comparingInt(flp -> flp.secondsPlayed))
            .orElse(null);
    }

    /**
     * Get all offlineFLPlayers
     *
     * @return Immutable list
     */
    public List<OfflineFLPlayer> getOfflineFLPlayers() {
        return List.copyOf(flPlayerMap.values());
    }

    /**
     * Update the discord id of an flp
     */
    public void updateDiscordMap(long oldID, long newID, OfflineFLPlayer flp) {
        discordMap.remove(oldID);
        if (newID != 0) {
            discordMap.put(newID, flp);
        }
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
        CompoundBinaryTag nbt;
        try {
            nbt = BinaryTagIO.reader().read(new FileInputStream(FileSystem.getFile(rootDirectory, EVIDENCE_LOCKERS_FILE)));
        } catch (IOException ex) {
            Logging.error("Failed to load evidence locker data.");
            ex.printStackTrace();
            return;
        }

        nbt.keySet().forEach(key -> evidenceLockers.put(
            UUID.fromString(key),
            new EvidenceLocker((CompoundBinaryTag) nbt.get(key))
        ));
    }

    public void saveEvidenceLockers() {

        CompoundBinaryTag.Builder nbt = CompoundBinaryTag.builder();
        evidenceLockers.forEach((key, locker) -> nbt.put(key.toString(), locker.serialize()));

        try {
            BinaryTagIO.writer().write(nbt.build(), new FileOutputStream(FileSystem.getFile(rootDirectory, EVIDENCE_LOCKERS_FILE)));
        } catch (IOException ex) {
            Logging.error("Failed to save evidence lockers file.");
            ex.printStackTrace();
        }
    }

    private void loadDeathDatabase() {
        CompoundBinaryTag nbt;
        try {
            nbt = BinaryTagIO.reader().read(new FileInputStream(FileSystem.getFile(rootDirectory, DEATH_DATABASE)));
        } catch (IOException ex) {
            Logging.error("Failed to load death database.");
            ex.printStackTrace();
            return;
        }

        nbt.keySet().forEach(uuid -> {
            List<PlayerDeath> deaths = new ArrayList<>();
            nbt.getList(uuid).stream().map(base -> new PlayerDeath((CompoundBinaryTag) base)).forEach(deaths::add);
            deathDatabase.put(UUID.fromString(uuid), deaths);
        });
    }

    public void saveDeathDatabase() {
        CompoundBinaryTag.Builder nbt = CompoundBinaryTag.builder();
        deathDatabase.forEach((uuid, deathList) -> {
            ListBinaryTag serDeathList = ListBinaryTag.from(deathList.stream().map(PlayerDeath::serialize).toList());
            nbt.put(uuid.toString(), serDeathList);
        });

        try {
            BinaryTagIO.writer().write(nbt.build(), new FileOutputStream(FileSystem.getFile(rootDirectory, DEATH_DATABASE)));
        } catch (IOException ex) {
            Logging.error("Failed to save evidence lockers file.");
            ex.printStackTrace();
        }
    }

    public void loadPackages() {
        CompoundBinaryTag nbt;
        try {
            nbt = BinaryTagIO.reader().read(new FileInputStream(FileSystem.getFile(rootDirectory, PACKAGES_FILE)));
        } catch (IOException ex) {
            Logging.error("Failed to load items file.");
            ex.printStackTrace();
            return;
        }

        List<Package> returnToSender = new ArrayList<>();
        nbt.forEach((entry) -> {
            CompoundBinaryTag tag = (CompoundBinaryTag) entry.getValue();
            List<Package> pkgs = new ArrayList<>();
            tag.forEach((entry2) -> {
                CompoundBinaryTag packageNBT = (CompoundBinaryTag) entry2.getValue();
                String uuids = packageNBT.getString("sender");
                UUID uuid = uuids.isEmpty() ? null : UUID.fromString(uuids);
                Package pkg = new Package(
                    uuid,
                    uuid == null ? "" : FarLands.getDataHandler().getOfflineFLPlayer(uuid).username,
                    FLUtils.itemStackFromNBT(packageNBT.getByteArray("item")),
                    packageNBT.getString("message"),
                    packageNBT.getLong("sentTime"),
                    packageNBT.getBoolean("forceSend")
                );

                if (pkg.hasExpired()) {
                    returnToSender.add(pkg);
                } else {
                    pkgs.add(pkg);
                }
            });
            packages.put(UUID.fromString(entry.getKey()), pkgs);
        });
        returnToSender.forEach(pkg -> addPackage(pkg.senderUuid(),
                                                 new Package(null, "FarLands Packaging Service",
                                                             pkg.item(), "Return To Sender", true)
        ));
    }

    public void savePackages() {
        CompoundBinaryTag.Builder nbt = CompoundBinaryTag.builder();
        packages.forEach((uuid, individualPackages) -> {
            if (uuid != null) {
                CompoundBinaryTag.Builder serIndividualPackages = CompoundBinaryTag.builder();
                individualPackages.forEach(pkg -> {
                    CompoundBinaryTag packageNBT = CompoundBinaryTag.builder()
                        .putString("sender", pkg.senderUuid() == null ? "" : pkg.senderUuid().toString())
                        .putByteArray("item", FLUtils.itemStackToNBT(pkg.item()))
                        .putString("message", pkg.message())
                        .putLong("sentTime", pkg.sentTime())
                        .putBoolean("forceSend", pkg.forceSend())
                        .build();
                    serIndividualPackages.put(pkg.senderName(), packageNBT);
                });
                nbt.put(uuid.toString(), serIndividualPackages.build());
            }
        });

        try {
            BinaryTagIO.writer().write(nbt.build(), new FileOutputStream(FileSystem.getFile(rootDirectory, PACKAGES_FILE)));
        } catch (IOException ex) {
            Logging.error("Failed to save items file.");
            ex.printStackTrace();
        }
    }

    public void loadItems() {
        CompoundBinaryTag nbt;
        try {
            nbt = BinaryTagIO.reader().read(new FileInputStream(FileSystem.getFile(rootDirectory, ITEMS_FILE)));

            CompoundBinaryTag itemsNbt = (CompoundBinaryTag) nbt.get("items");
            if (itemsNbt != null) {
                itemsNbt.forEach(e -> items.put(e.getKey(), NBTDeserializer.item(e.getValue())));
            }

            CompoundBinaryTag itemListsNbt = (CompoundBinaryTag) nbt.get("itemLists");
            if (itemListsNbt != null) {
                itemListsNbt.forEach(e -> itemLists.put(e.getKey(), NBTDeserializer.itemsList(e.getValue())));
            }

            CompoundBinaryTag itemCollectionsNbt = (CompoundBinaryTag) nbt.get("itemCollections");
            if (itemCollectionsNbt != null) {
                itemCollectionsNbt.forEach(e -> itemCollections.put(e.getKey(), ItemCollection.fromNbt((CompoundBinaryTag) e.getValue())));
            }

        } catch (IOException | IllegalArgumentException ex) {
            Logging.error("Failed to load custom item data.");
            ex.printStackTrace();
        }

    }

    public void saveItems() {

        CompoundBinaryTag.Builder itemsNbt = CompoundBinaryTag.builder();
        items.forEach((key, value) -> itemsNbt.put(key, NBTSerializer.item(value)));

        CompoundBinaryTag.Builder itemListsNbt = CompoundBinaryTag.builder();
        itemLists.forEach((k, v) -> itemListsNbt.put(k, NBTSerializer.itemsList(v)));

        CompoundBinaryTag.Builder itemCollectionsNbt = CompoundBinaryTag.builder();
        itemCollections.forEach((key, value) -> itemCollectionsNbt.put(key, value.toNbt()));


        CompoundBinaryTag.Builder nbt = CompoundBinaryTag.builder();
        nbt.put("items", itemsNbt.build());
        nbt.put("itemLists", itemListsNbt.build());
        nbt.put("itemCollections", itemCollectionsNbt.build());

        try {
            BinaryTagIO.writer().write(nbt.build(), new FileOutputStream(FileSystem.getFile(rootDirectory, ITEMS_FILE)));
        } catch (IOException ex) {
            Logging.error("Failed to save custom items file.");
            ex.printStackTrace();
        }
    }

    public boolean addPackage(UUID recipient, Package pack) {
        List<Package> localPackages;
        if (!packages.containsKey(recipient)) {
            packages.put(recipient, new ArrayList<>());
        }
        localPackages = packages.get(recipient);

        for (Package lPackage : localPackages) {
            if (pack.senderUuid() != null && pack.senderUuid().equals(lPackage.senderUuid())) {
                return false;
            }
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

    public ItemStack getItem(String key, boolean logMissing) {
        if (items.containsKey(key)) {
            return items.get(key);
        } else {
            if (logMissing) {
                Logging.error("Missing item with key: '" + key + "'");
            }
        }
        return new ItemStack(Material.AIR); // Just to prevent errors
    }

    public ItemStack getItem(String key) {
        return getItem(key, true);
    }

    public Map<String, ItemStack> getItems() {
        return items;
    }

    public ItemCollection getItemCollection(String key, boolean logMissing) {
        if (itemCollections.containsKey(key)) {
            return itemCollections.get(key);
        } else {
            if (logMissing) {
                Logging.error("Missing item collection with key: '" + key + "'");
            }
        }
        return null;
    }

    public ItemCollection getItemCollection(String key) {
        return getItemCollection(key, true);
    }

    public Map<String, ItemCollection> getItemCollections() {
        return itemCollections;
    }

    public List<ItemStack> getItemList(String key, boolean logMissing) {
        if (itemLists.containsKey(key)) {
            return itemLists.get(key);
        } else if (logMissing) {
                Logging.error("Missing item list with key: '" + key + "'");
        }
        return null;
    }

    public List<ItemStack> getItemList(String key) {
        return getItemList(key, true);
    }

    public Map<String, List<ItemStack>> getItemLists() {
        return itemLists;
    }

    public void loadCriticalData() {
        config = FileSystem.loadJson(Config.class, FileSystem.getFile(rootDirectory, MAIN_CONFIG_FILE));
        pluginData = FileSystem.loadJson(PluginData.class, FileSystem.getFile(rootDirectory, PLUGIN_DATA_FILE));
    }

    public void saveCriticalData() {
        FileSystem.saveJson(config, FileSystem.getFile(rootDirectory, MAIN_CONFIG_FILE), true);
        FileSystem.saveJson(pluginData, FileSystem.getFile(rootDirectory, PLUGIN_DATA_FILE), true);
    }

    public void loadData() {
        Type type = Types.newParameterizedType(List.class, OfflineFLPlayer.class);
        JsonAdapter<List<OfflineFLPlayer>> adapter = FarLands.getMoshi().adapter(type);
        File file = FileSystem.getFile(rootDirectory, PLAYER_DATA_FILE);
        List<OfflineFLPlayer> players = new ArrayList<>();
        try {
            String playerdata = FileSystem.readUTF8(file);
            if (playerdata.isBlank()) playerdata = "[]";
            players = adapter.fromJson(playerdata);
            if (players == null) {
                players = new ArrayList<>();
            }
        } catch (IOException e) {
            Logging.error("Failed to load " + file.getName() + ".");
            e.printStackTrace();
        }
        players.forEach(flp -> {
            flPlayerMap.put(flp.uuid, flp);
            if (flp.deaths <= 0 && flp.secondsPlayed > 30 * 60) // Check players with less than 1 death and more than 30 minutes of playtime
            {
                flp.updateDeaths();
            }
            if (flp.isDiscordVerified()) {
                discordMap.put(flp.discordID, flp);
            }
        });

        // Update flp's shop counts
        ChestShops.getDataHandler()
            .getAllShops()
            .forEach(shop -> {
                getOfflineFLPlayer(shop.getOwner()).shops++;
            });

        loadEvidenceLockers();
        loadDeathDatabase();
        loadPackages();
        loadItems();
    }

    public void saveData() {
        // Create plain moshi instance without indentation
        Moshi.Builder playerDataBuilder = new Moshi.Builder();
        CustomAdapters.register(playerDataBuilder);

        FileSystem.saveJson(
            playerDataBuilder.build().adapter(Types.newParameterizedType(Collection.class, OfflineFLPlayer.class)),
            flPlayerMap.values(),
            FileSystem.getFile(rootDirectory, PLAYER_DATA_FILE)
        );

        saveEvidenceLockers();
        saveDeathDatabase();
        savePackages();
        saveItems();
    }

    public Map<UUID, FLPlayerSession> getCachedSessions() {
        return this.cachedSessions;
    }

    public Map<UUID, FLPlayerSession> getSessionMap() {
        return this.sessionMap;
    }

    private static String path(Object... parts) {
        return String.join(File.separator, Arrays.stream(parts).map(Object::toString).toArray(String[]::new));
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
