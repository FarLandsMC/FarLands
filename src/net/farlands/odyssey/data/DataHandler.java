package net.farlands.odyssey.data;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.DiscordSender;
import net.farlands.odyssey.data.struct.*;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.mechanic.Mechanic;
import net.farlands.odyssey.util.FileSystem;
import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.TextUtils;
import net.farlands.odyssey.util.Utils;
import net.minecraft.server.v1_14_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NBTTagList;

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
    private byte[] currentPatchnotesMD5;
    private Config config;
    private PluginData pluginData;
    private NBTTagCompound evidenceLockers;
    private NBTTagCompound deathDatabase;
    private List<org.bukkit.inventory.ItemStack> voteRewards;
    private List<ItemReward> votePartyRewards;
    private org.bukkit.inventory.ItemStack patronCollectable;
    private Map<String, GameRewardSet> gameRewards;
    private Map<UUID, Map<String, Pair<org.bukkit.inventory.ItemStack, String>>> packages;

    public static final List<String> WORLDS = Arrays.asList("world", "world_nether", "world_the_end", "farlands");
    private static final List<String> SCRIPTS = Arrays.asList("artifact.sh", "server.sh", "backup.sh", "restart.sh");
    private static final Map<String, String> DIRECTORIES = Utils.asMap(
        new Pair<>("playerdata", "playerdata"),
        new Pair<>("data", "data"), // General plugin data
        new Pair<>("tmp", "cache")
    );
    private static final String MAIN_CONFIG_FILE = "mainConfig.json";
    private static final String PLUGIN_DATA_FILE = DIRECTORIES.get("data") + File.separator + "private.json";
    private static final String PLAYER_DATA_FILE = DIRECTORIES.get("data") + File.separator + "playerdata.json";
    private static final String EVIDENCE_LOCKERS_FILE = DIRECTORIES.get("data") + File.separator + "evidenceLockers.nbt";
    private static final String DEATH_DATABASE = DIRECTORIES.get("data") + File.separator + "deaths.nbt";
    private static final String ITEMS_FILE = DIRECTORIES.get("data") + File.separator + "items.nbt";

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
                throw new RuntimeException("Failed to create directory during the initialization of the data handler. Did you give the process access to the FS?");
        }
        File playerdataFile = new File(PLAYER_DATA_FILE);
        if(!playerdataFile.exists()) {
            try {
                playerdataFile.createNewFile();
            } catch (IOException ex) {
                throw new RuntimeException("Failed to create player data file.", ex);
            }
        }
        initNbt(EVIDENCE_LOCKERS_FILE);
        initNbt(DEATH_DATABASE);
        initNbt(ITEMS_FILE);
        Chat.log("Initialized data handler.");
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
        this.currentPatchnotesMD5 = null;
        this.voteRewards = new ArrayList<>();
        this.votePartyRewards = new ArrayList<>();
        this.patronCollectable = null;
        this.gameRewards = new HashMap<>();
        this.packages = new HashMap<>();
        init();
        loadData();
        saveData();
    }

    @Override
    public void onStartup() {
        FarLands.getScheduler().scheduleAsyncRepeatingTask(this::update, 50L, 5L * 60L * 20L);
        Rank.createTeams();

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            DataHandler dh = FarLands.getDataHandler();
            if(dh.arePatchnotesDifferent()) {
                try {
                    FarLands.getDiscordHandler().sendMessageRaw("announcements", "@everyone Patch **#" + dh.getCurrentPatch() +
                            "** has been released!\n```" + Chat.removeColorCodes(new String(dh.getResource("patchnotes.txt"), "UTF-8")) + "```");
                    flPlayerMap.values().forEach(flp -> flp.setViewedPatchnotes(false));
                }catch(IOException ex) {
                    Chat.error("Failed to post patch notes to #announcements");
                    ex.printStackTrace(System.out);
                }
            }
        }, 100L);

        int gcCycleTime = FarLands.getFLConfig().getGcCycleTime();
        if(gcCycleTime > 0)
            Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), System::gc, 20L * 60L * 5L, 20L * 60L * gcCycleTime);
    }

    @Override
    public void onShutdown() {
        update();
        if(arePatchnotesDifferent()) {
            pluginData.setLastPatchnotesMD5(currentPatchnotesMD5);
            pluginData.incrementPatch();
        }
        pdh.onShutdown();
        saveData();
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FLPlayerSession session = getSession(player);

        // Give packages and update
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            Map<String, Pair<ItemStack, String>> packages = FarLands.getDataHandler().getAndRemovePackages(player.getUniqueId());
            if(!packages.isEmpty()) {
                // Notify the player how many packages they're getting
                player.spigot().sendMessage(TextUtils.format("&(gold)Receiving {&(aqua)%0} $(inflect,noun,0,package) from {&(aqua)%1}",
                        packages.size(), packages.keySet().stream().map(sender -> "{" + sender + "}").collect(Collectors.joining(", "))));

                // Give the packages and send the messages
                packages.values().forEach(item -> {
                    final String message = item.getSecond();
                    if (message != null && !message.isEmpty())
                        player.spigot().sendMessage(TextUtils.format("&(gold)Item {&(aqua)%0} was sent with the following message {&(aqua)%1}",
                                Utils.itemName(item.getFirst()), message));
                    Utils.giveItem(player, item.getFirst(), true);
                });
            }

            session.update(true);
        });

        FarLands.getDiscordHandler().updateStats();
    }

    @Override
    public void onPlayerQuit(Player player) {
        FLPlayerSession session = getSession(player);
        session.updatePlaytime();
        session.handle.setLastLocation(player.getLocation());
        if(!session.handle.isVanished())
            session.handle.setLastLogin(System.currentTimeMillis());
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            sessionMap.remove(player.getUniqueId());
            FarLands.getDiscordHandler().updateStats();
        }, 5L);
    }

    @EventHandler
    public void onVillagerAcquireTrades(VillagerAcquireTradeEvent event) {
        if(FarLands.getDataHandler().getPluginData().isSpawnTrader(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onVillagerChangeCareer(VillagerCareerChangeEvent event) {
        if(FarLands.getDataHandler().getPluginData().isSpawnTrader(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onVillagerReplenishTrades(VillagerReplenishTradeEvent event) {
        if(FarLands.getDataHandler().getPluginData().isSpawnTrader(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled=true)
    @SuppressWarnings("unchecked")
    public void onTeleport(PlayerTeleportEvent event) {
        FLPlayerSession session = getSession(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> session.update(false), 1L);
        if(session.backLocations.size() >= 5)
            session.backLocations.remove(0);
        if(!session.backLocations.contains(event.getFrom()))
            session.backLocations.add(event.getFrom());
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
    @SuppressWarnings("unchecked")
    public void onPlayerDeath(PlayerDeathEvent event) {
        FLPlayerSession session = getSession(event.getEntity());
        if(session.backLocations.size() >= 5)
            session.backLocations.remove(0);
        session.backLocations.add(event.getEntity().getLocation());
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
                    currentPatchnotesMD5 = Utils.hash(notes);
            }catch(IOException ex) {
                Chat.error("Failed to compare patch notes.");
                ex.printStackTrace(System.out);
            }
        }
        return currentPatchnotesMD5 != null && !Arrays.equals(currentPatchnotesMD5, pluginData.getLastPatchnotesMD5());
    }

    public int getCurrentPatch() {
        return arePatchnotesDifferent() ? pluginData.getLastPatch() + 1 : pluginData.getLastPatch();
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
        FLPlayerSession session = sessionMap.get(player.getUniqueId());
        if(session == null) {
            session = new FLPlayerSession(player, getOfflineFLPlayer(player));
            sessionMap.put(player.getUniqueId(), session);
        }
        return session;
    }

    public FLPlayerSession getSession(UUID uuid) {
        return sessionMap.get(uuid);
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

    public Collection<OfflineFLPlayer> getOfflineFLPlayers() {
        return flPlayerMap.values();
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

    public List<OfflineFLPlayer> getLegacyFLPlayers() {
        return new ArrayList<>(flPlayerMap.values());
    }

    public NBTTagCompound getEvidenceLocker(OfflineFLPlayer flp) {
        NBTTagCompound locker;
        String uuid = flp.getUuid().toString();

        // The evidence locker does not exist yet, so make it
        if(!evidenceLockers.hasKey(uuid)) {
            locker = new NBTTagCompound();
            for(Punishment p : flp.getPunishments())
                locker.set(p.toUniqueString(), new NBTTagList()); // List of items
            evidenceLockers.set(uuid, locker);
        }
        // The locker already exists so retrieve it
        else {
            locker = evidenceLockers.getCompound(uuid);

            // Make sure it's up to date with the latest punishments
            boolean newLockerAdded = false;
            for(Punishment p : flp.getPunishments()) {
                String ps = p.toUniqueString();
                if(!locker.hasKey(ps)) {
                    locker.set(ps, new NBTTagList());
                    newLockerAdded = true;
                }
            }

            if(newLockerAdded)
                evidenceLockers.set(uuid, locker);
        }

        return locker;
    }

    public void saveEvidenceLocker(OfflineFLPlayer flp, NBTTagCompound locker) {
        evidenceLockers.set(flp.getUuid().toString(), locker);
        saveEvidenceLockers();
    }

    private void loadEvidenceLockers() {
        try {
            evidenceLockers = NBTCompressedStreamTools.a(new FileInputStream(FileSystem.getFile(rootDirectory, EVIDENCE_LOCKERS_FILE)));
        }catch(IOException ex) {
            Chat.error("Failed to load evidence locker data.");
            ex.printStackTrace(System.out);
        }
    }

    public void saveEvidenceLockers() {
        try {
            NBTCompressedStreamTools.a(evidenceLockers, new FileOutputStream(FileSystem.getFile(rootDirectory, EVIDENCE_LOCKERS_FILE)));
        }catch(IOException ex) {
            Chat.error("Failed to save evidence lockers file.");
            ex.printStackTrace(System.out);
        }
    }

    private void loadDeathDatabase() {
        try {
            deathDatabase = NBTCompressedStreamTools.a(new FileInputStream(FileSystem.getFile(rootDirectory, DEATH_DATABASE)));
        }catch(IOException ex) {
            Chat.error("Failed to load death database.");
            ex.printStackTrace(System.out);
        }
    }

    public void saveDeathDatabase() {
        try {
            NBTCompressedStreamTools.a(deathDatabase, new FileOutputStream(FileSystem.getFile(rootDirectory, DEATH_DATABASE)));
        }catch(IOException ex) {
            Chat.error("Failed to save evidence lockers file.");
            ex.printStackTrace(System.out);
        }
    }

    public void loadItems() {
        NBTTagCompound nbt;
        try {
            nbt = NBTCompressedStreamTools.a(new FileInputStream(FileSystem.getFile(rootDirectory, ITEMS_FILE)));
        }catch(IOException ex) {
            Chat.error("Failed to load items file.");
            ex.printStackTrace(System.out);
            return;
        }
        if(nbt.isEmpty())
            return;

        patronCollectable = Utils.itemStackFromNBT(nbt.getCompound("patronCollectable"));
        nbt.getList("voteRewards", 10).forEach(base -> voteRewards.add(Utils.itemStackFromNBT((NBTTagCompound)base)));
        nbt.getList("votePartyRewards", 10).forEach(base -> votePartyRewards.add(new ItemReward((NBTTagCompound)base)));
        NBTTagCompound serGameRewards = nbt.getCompound("gameRewards");
        serGameRewards.getKeys().forEach(key -> gameRewards.put(key, new GameRewardSet(serGameRewards.getCompound(key))));

        NBTTagCompound serPackages = nbt.getCompound("packages");
        serPackages.getKeys().forEach(key -> {
            NBTTagCompound serPkgs = serPackages.getCompound(key);
            Map<String, Pair<ItemStack, String>> pkgs = new HashMap<>();
            serPkgs.getKeys().forEach(key0 -> pkgs.put(key0, new Pair<>(Utils.itemStackFromNBT(serPkgs.getCompound(key0)), "")));
            packages.put(UUID.fromString(key), pkgs);
        });

        NBTTagCompound mesPackages = nbt.getCompound("packagesMes");
        mesPackages.getKeys().forEach(key -> {
            NBTTagCompound mesPkgs = mesPackages.getCompound(key);
            Map<String, Pair<ItemStack, String>> item = packages.get(UUID.fromString(key));
            mesPkgs.getKeys().forEach(key0 -> item.get(key0).setSecond(mesPkgs.getString(key0)));
            packages.put(UUID.fromString(key), item);
        });
    }

    public void saveItems() {
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.set("patronCollectable", Utils.itemStackToNBT(patronCollectable));

        NBTTagList voteRewards = new NBTTagList();
        this.voteRewards.stream().map(Utils::itemStackToNBT).forEach(voteRewards::add);
        nbt.set("voteRewards", voteRewards);

        NBTTagList votePartyRewards = new NBTTagList();
        this.votePartyRewards.stream().map(ItemReward::asTagCompound).forEach(votePartyRewards::add);
        nbt.set("votePartyRewards", votePartyRewards);

        NBTTagCompound gameRewards = new NBTTagCompound();
        this.gameRewards.forEach((key, value) -> gameRewards.set(key, value.asTagCompound()));
        nbt.set("gameRewards", gameRewards);

        NBTTagCompound serPackages = new NBTTagCompound();
        packages.forEach((key, pkgs) -> {
            NBTTagCompound serPkgs = new NBTTagCompound();
            pkgs.forEach((sender, item) -> serPkgs.set(sender, Utils.itemStackToNBT(item.getFirst())));
            serPackages.set(key.toString(), serPkgs);
        });
        nbt.set("packages", serPackages);

        NBTTagCompound mesPackages = new NBTTagCompound();
        packages.forEach((key, pkgs) -> {
            NBTTagCompound mesPkgs = new NBTTagCompound();
            pkgs.forEach((sender, item) -> mesPkgs.setString(sender, item.getSecond()));
           mesPackages.set(key.toString(), mesPkgs);
        });
        nbt.set("packagesMes", mesPackages);

        try {
            NBTCompressedStreamTools.a(nbt, new FileOutputStream(FileSystem.getFile(rootDirectory, ITEMS_FILE)));
        }catch(IOException ex) {
            Chat.error("Failed to save items file.");
            ex.printStackTrace(System.out);
        }
    }

    public List<ItemStack> getVoteRewards() {
        return voteRewards;
    }

    public List<ItemReward> getVotePartyRewards() {
        return votePartyRewards;
    }

    public ItemStack getPatronCollectable() {
        return patronCollectable.clone();
    }

    public void setPatronCollectable(ItemStack patronCollectable) {
        this.patronCollectable = patronCollectable;
    }

    public Set<String> getGames() {
        return gameRewards.keySet();
    }

    public GameRewardSet getGameRewardSet(String game) {
        return gameRewards.get(game);
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

    public void addDeath(Player player) {
        if(!deathDatabase.hasKey(player.getUniqueId().toString()))
            deathDatabase.set(player.getUniqueId().toString(), new NBTTagList());
        NBTTagList deaths = deathDatabase.getList(player.getUniqueId().toString(), 10);
        if(deaths.size() >= 3)
            deaths.remove(0);
        deaths.add((new PlayerDeath(player)).serialize());
    }

    public List<PlayerDeath> getDeaths(UUID uuid) {
        return deathDatabase.hasKey(uuid.toString())
                ? deathDatabase.getList(uuid.toString(), 10).stream().map(base -> new PlayerDeath((NBTTagCompound)base))
                        .collect(Collectors.toList())
                : Collections.emptyList();
    }

    public void setDeaths(UUID uuid, List<PlayerDeath> deaths) {
        NBTTagList newDeaths = new NBTTagList();
        deaths.stream().map(PlayerDeath::serialize).forEach(newDeaths::add);
        deathDatabase.set(uuid.toString(), newDeaths);
    }

    public void loadData() {
        // Convert SQL things
        ResultSet rs = pdh.query("select uuid from playerdata");
        List<UUID> sqlUuids = new ArrayList<>();
        try {
            while (rs.next()) {
                byte[] uuid = rs.getBytes(1);
                sqlUuids.add(Utils.getUuid(uuid, 0));
            }
            rs.close();
        }catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        sqlUuids.forEach(uuid -> {
            OfflineFLPlayer flp = pdh.getFLPlayer(uuid);
            flp.notes.addAll(pdh.getNotes(uuid));
            flPlayerMap.put(uuid, flp);
        });

        try {
            String flPlayerMapData = FileSystem.readUTF8(FileSystem.getFile(rootDirectory, PLAYER_DATA_FILE));
            Collection<OfflineFLPlayer> flps = FarLands.getGson().fromJson(flPlayerMapData, new TypeToken<Collection<OfflineFLPlayer>>(){}.getType());
            flps.forEach(flp -> {
                flPlayerMap.put(flp.uuid, flp);
                if(flp.isDiscordVerified())
                    discordMap.put(flp.discordID, flp);
            });
        }catch (IOException ex) {
            throw new RuntimeException("Failed to load player data.", ex);
        }

        config = FileSystem.loadJson(Config.class, FileSystem.getFile(rootDirectory, MAIN_CONFIG_FILE));
        pluginData = FileSystem.loadJson(PluginData.class, FileSystem.getFile(rootDirectory, PLUGIN_DATA_FILE));
        loadEvidenceLockers();
        loadDeathDatabase();
        loadItems();
    }

    public void saveData() {
        FileSystem.saveJson((new GsonBuilder()).create(), flPlayerMap.values(), FileSystem.getFile(rootDirectory, PLAYER_DATA_FILE));
        FileSystem.saveJson(config, FileSystem.getFile(rootDirectory, MAIN_CONFIG_FILE));
        FileSystem.saveJson(pluginData, FileSystem.getFile(rootDirectory, PLUGIN_DATA_FILE));
        saveEvidenceLockers();
        saveDeathDatabase();
        saveItems();
    }
}
