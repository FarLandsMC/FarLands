package net.farlands.odyssey.data;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.*;
import net.farlands.odyssey.util.FileSystem;
import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.Utils;
import net.minecraft.server.v1_14_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NBTTagList;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class serves as and API to interact with FarLands' data.
 */
public class DataHandler {
    private final PlayerDataHandler pdh;
    private final RandomAccessDataHandler radh;
    private final File rootDirectory;
    private final Map<UUID, FLPlayer> flPlayerMap;
    private byte[] currentPatchnotesMD5;
    private Config config;
    private PluginData pluginData;
    private NBTTagCompound evidenceLockers;
    private NBTTagCompound deathDatabase;
    private List<org.bukkit.inventory.ItemStack> voteRewards;
    private List<ItemReward> votePartyRewards;
    private org.bukkit.inventory.ItemStack patronCollectable;
    private Map<String, GameRewardSet> gameRewards;
    private Map<UUID, Map<String, org.bukkit.inventory.ItemStack>> packages;

    public static final List<String> WORLDS = Arrays.asList("world", "world_nether", "world_the_end", "farlands");
    private static final List<String> SCRIPTS = Arrays.asList("artifact.sh", "server.sh", "backup.sh", "restart.sh");
    private static final Map<String, String> DIRECTORIES = Utils.asMap(
        new Pair<>("playerdata", "playerdata"),
        new Pair<>("data", "data"), // General plugin data
        new Pair<>("tmp", "cache")
    );
    private static final String MAIN_CONFIG_FILE = "mainConfig.json";
    private static final String PLUGIN_DATA_FILE = DIRECTORIES.get("data") + File.separator + "private.json";
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
        initNbt(EVIDENCE_LOCKERS_FILE);
        initNbt(DEATH_DATABASE);
        initNbt(ITEMS_FILE);
        FarLands.log("Initialized data handler.");
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
        this.pdh = new PlayerDataHandler(FileSystem.getFile(rootDirectory, DIRECTORIES.get("data"), "playerdata.db"), this);
        this.radh = new RandomAccessDataHandler();
        this.rootDirectory = rootDirectory;
        this.flPlayerMap = new HashMap<>();
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

    public PlayerDataHandler getPDH() {
        return pdh;
    }

    public RandomAccessDataHandler getRADH() {
        return radh;
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
                FarLands.error("Failed to compare patch notes.");
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

    public FLPlayer getFLPlayerLegacy(String username) {
        return flPlayerMap.values().stream().filter(flp -> username.equalsIgnoreCase(flp.getUsername())).findFirst().orElse(null);
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

    public List<FLPlayer> getLegacyFLPlayers() {
        return new ArrayList<>(flPlayerMap.values());
    }

    public NBTTagCompound getEvidenceLocker(FLPlayer flp) {
        NBTTagCompound locker;
        String uuid = flp.getUuid().toString();
        if(!evidenceLockers.hasKey(uuid)) {
            locker = new NBTTagCompound();
            for(Punishment p : flp.getPunishments())
                locker.set(p.toUniqueString(), new NBTTagList());
            evidenceLockers.set(uuid, locker);
        }else {
            locker = evidenceLockers.getCompound(uuid);
            boolean flag = false;
            for(Punishment p : flp.getPunishments()) {
                String ps = p.toUniqueString();
                if(!locker.hasKey(ps)) {
                    locker.set(ps, new NBTTagList());
                    flag = true;
                }
            }
            if(flag)
                evidenceLockers.set(uuid, locker);
        }
        return locker;
    }

    public void saveEvidenceLocker(FLPlayer flp, NBTTagCompound locker) {
        evidenceLockers.set(flp.getUuid().toString(), locker);
        saveEvidenceLockers();
    }

    private void loadEvidenceLockers() {
        try {
            evidenceLockers = NBTCompressedStreamTools.a(new FileInputStream(FileSystem.getFile(rootDirectory, EVIDENCE_LOCKERS_FILE)));
        }catch(IOException ex) {
            FarLands.error("Failed to load evidence locker data.");
            ex.printStackTrace(System.out);
        }
    }

    public void saveEvidenceLockers() {
        try {
            NBTCompressedStreamTools.a(evidenceLockers, new FileOutputStream(FileSystem.getFile(rootDirectory, EVIDENCE_LOCKERS_FILE)));
        }catch(IOException ex) {
            FarLands.error("Failed to save evidence lockers file.");
            ex.printStackTrace(System.out);
        }
    }

    private void loadDeathDatabase() {
        try {
            deathDatabase = NBTCompressedStreamTools.a(new FileInputStream(FileSystem.getFile(rootDirectory, DEATH_DATABASE)));
        }catch(IOException ex) {
            FarLands.error("Failed to load death database.");
            ex.printStackTrace(System.out);
        }
    }

    public void saveDeathDatabase() {
        try {
            NBTCompressedStreamTools.a(deathDatabase, new FileOutputStream(FileSystem.getFile(rootDirectory, DEATH_DATABASE)));
        }catch(IOException ex) {
            FarLands.error("Failed to save evidence lockers file.");
            ex.printStackTrace(System.out);
        }
    }

    public void loadItems() {
        NBTTagCompound nbt;
        try {
            nbt = NBTCompressedStreamTools.a(new FileInputStream(FileSystem.getFile(rootDirectory, ITEMS_FILE)));
        }catch(IOException ex) {
            FarLands.error("Failed to load death database.");
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
            Map<String, org.bukkit.inventory.ItemStack> pkgs = new HashMap<>();
            serPkgs.getKeys().forEach(key0 -> pkgs.put(key0, Utils.itemStackFromNBT(serPkgs.getCompound(key0))));
            packages.put(UUID.fromString(key), pkgs);
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
            pkgs.forEach((sender, item) -> serPkgs.set(sender, Utils.itemStackToNBT(item)));
            serPackages.set(key.toString(), serPkgs);
        });

        try {
            NBTCompressedStreamTools.a(nbt, new FileOutputStream(FileSystem.getFile(rootDirectory, ITEMS_FILE)));
        }catch(IOException ex) {
            FarLands.error("Failed to save items file.");
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

    public boolean addPackage(UUID recipient, String sender, org.bukkit.inventory.ItemStack stack) {
        packages.putIfAbsent(recipient, new HashMap<>());
        Map<String, org.bukkit.inventory.ItemStack> pkgs = packages.get(recipient);
        if(pkgs.containsKey(sender))
            return false;
        else{
            pkgs.put(sender, stack);
            return true;
        }
    }

    public Map<String, org.bukkit.inventory.ItemStack> getAndRemovePackages(UUID recipient) {
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
        // Load configs
        config = FileSystem.loadJson(Config.class, FileSystem.getFile(rootDirectory, MAIN_CONFIG_FILE));
        pluginData = FileSystem.loadJson(PluginData.class, FileSystem.getFile(rootDirectory, PLUGIN_DATA_FILE));
        loadEvidenceLockers();
        loadDeathDatabase();
        loadItems();

        // Load legacy player data
        for(File playerdata : FileSystem.listFiles(FileSystem.getFile(rootDirectory, DIRECTORIES.get("playerdata")))) {
            FLPlayer flp = FileSystem.loadJson(FLPlayer.class, playerdata);
            if(flp == null)
                continue;
            flPlayerMap.put(flp.getUuid(), flp);
        }
    }

    public void saveData() {
        // Save legacy data
        flPlayerMap.values().forEach(this::saveFLPlayer);

        FileSystem.saveJson(config, FileSystem.getFile(rootDirectory, MAIN_CONFIG_FILE));
        FileSystem.saveJson(pluginData, FileSystem.getFile(rootDirectory, PLUGIN_DATA_FILE));
        saveEvidenceLockers();
        saveDeathDatabase();
        saveItems();
    }

    public void saveFLPlayer(FLPlayer flp) {
        FileSystem.saveJson(flp, FileSystem.getFile(rootDirectory, DIRECTORIES.get("playerdata"), flp.getUuid().toString() + ".json"));
    }

    public void onShutdown() { // Called in FarLands#onDisable
        if(arePatchnotesDifferent()) {
            pluginData.setLastPatchnotesMD5(currentPatchnotesMD5);
            pluginData.incrementPatch();
        }
        pdh.onShutdown();
        saveData();
    }
}
