const eval = (exp) => {
    return engine.eval(exp);
};

// Global Shortcuts
const org = Packages.org;
const net = Packages.net;
const com = Packages.com;

const fl = net.farlands.sanctuary;
const FL = fl.FarLands;

const rp = com.kicas.rp;
const RP = rp.RegionProtection;
const RH = rp.util.ReflectionHelper;

const cs = com.kicasmads.cs;
const CS = cs.ChestShops;

const plugin = FL.getInstance();
const RegionProtection = RP.getInstance();
const ChestShops = CS.getInstance();

const Bukkit = org.bukkit.Bukkit;
const server = Bukkit.getServer();
const ChatColor = org.bukkit.ChatColor;
const FLUtils = fl.util.FLUtils;
const DH = FL.getDataHandler();

const List = java.util.List;
const ArrayList = java.util.ArrayList;
const HashMap = java.util.HashMap;
const HashSet = java.util.HashSet;
const Set = java.util.Set;
const Map = java.util.Map;
const ItemStack = org.bukkit.inventory.ItemStack;
const Material = org.bukkit.Material;
const ItemCollection = fl.data.struct.ItemCollection;
const ItemReward = fl.data.struct.ItemReward;
const Worlds = fl.data.Worlds

const privateConfig = { // Config of things within the JS File
    logColors: {
        'undefined': ChatColor.LIGHT_GRAY,
        'null': ChatColor.LIGHT_GRAY,
        'boolean': ChatColor.PURPLE,
        'number': ChatColor.YELLOW,
        'bigint': ChatColor.GOLD,
        'string': ChatColor.GREEN,
        'symbol': ChatColor.DARK_PURPLE,
        'function': ChatColor.AQUA,
        'object': ChatColor.WHITE,
    }
}

const itemData = {
    help: `This object has a few functions and properties for manipulating the item data.
    addToList(String listKey, ItemStack itemStack) will add an item to a list with the key specified
    addVoteReward(int rarity, ItemStack itemStack) will add a vote reward with the given rarity.
    `,
    items /* Map<String, ItemStack> */ : DH.getItems(),
    itemLists /* Map<String, List<ItemStack>> */ : DH.getItemLists(),
    itemCollections /* Map<String, ItemCollection> */ : DH.getItemCollections(),
    addToList: (listKey, itemStack) => {
        const list = itemData.itemLists.get(listKey);
        if (list) {
            list.add(itemStack);
        } else {
            const newList = new ArrayList();
            newList.add(itemStack);
            itemData.itemLists.put(listKey, new ArrayList(List.of(itemStack)));
        }
    },
    addVoteReward: (rarity, itemStack) => {
        const itemCollection = itemData.itemCollections.get('voteRewards');
        if (itemCollection) {
            itemCollection.simpleRewards().add(new ItemReward(rarity, itemStack));
        } else {
            const newItemCollection = new ItemCollection(null, null, List.of(new ItemReward(rarity, itemStack)));
            itemData.itemCollections.put('voteRewards', newItemCollection);
        }
    }
}

const inventory = {
    getHand: () => self.getInventory().getItemInMainHand().clone(),
    getOffHand: () => self.getInventory().getItemInOffHand().clone(),
    getArmor: () => List.of(self.getInventory().getArmorContents()).stream().map(s => s.clone()).toList(),
    getContents: () => List.of(self.getInventory().getContents()).stream().map(s => s.clone()).toList(),
    getHotbar: () => {
        let list = new ArrayList();
        for (let i = 0; i < 9; ++i) {
            const item = self.getInventory().getItem(i);
            if (item !== null && FLUtils.material(item) !== Material.AIR) {
                list.add(item.clone());
            }
        }
        return list;
    },
}

// Helper Methods
const uuid = java.util.UUID.fromString;

const invoke = (method, target) =>
    RH.invoke(method, target.getClass(), target);

const mechanic = (clazz) =>
    FL.getMechanicHandler().getMechanic(clazz);

const getEntityById = (uuid) => // TODO: Invalid
    self.getWorld().getEntities().find((e) => e.getUniqueId().toString() === uuid);

const getRegionFile = () =>
    'r.' + (self.getLocation().getBlockX() >> 9) + '.' + (self.getLocation().getBlockZ() >> 9) + '.mca';

const sendFormatted = (recipient, input, values) =>
    recipient.spigot().sendMessage(fl.util.TextUtils.format(input, values));

const flp = (name) => DH.getOfflineFLPlayer(name);

const session = (name) => {
    const p = player(name);
    return p == null ? null : FL.getDataHandler().getSession(p);
}

const player = (name) => Bukkit.getPlayer(name);

const getField = (name, target) => RH.getFieldValue(name, target.class, target);

const listFields = (obj) =>
    List.of(obj.class.getFields()).forEach((field) => {
        try {
            print(field.getName() + ': ' + RH.getFieldValue(field, obj));
        } catch (ignored) {
        }
    });

const str = (obj) => {
    let string = JSON.stringify(obj, null, 2);
    if(!string) string = obj + '';
    return string;
};

const console = {};

const print = log = console.log = (x) => {
    let color = privateConfig.logColors[typeof x];
    self.sendMessage(color + str(x));
};

/* Fit Native Functions to Bukkit API */

const toTicks = (millis) => Math.ceil(millis / 50);

const runTaskLater = (callback, delayMS) =>
    Bukkit.getScheduler().runTaskLater(plugin, callback, toTicks(delayMS));

const runTaskTimer = (callback, intervalMS) => {
    const delayMS = toTicks(intervalMS);
    return Bukkit.getScheduler().runTaskTimer(plugin, callback, delayMS, delayMS);
};

const toFile = (string) => getField('FILE_PREFIX', fl.command.staff.CommandJS) + string