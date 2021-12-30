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
const Utils = fl.util.FLUtils;
const DH = FL.getDataHandler();

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

const flp = (name) =>
    DH.getOfflineFLPlayer(name);



const session = (name) => {
    const p = player(name);
    return p == null ? null : FL.getDataHandler().getSession(p);
}

const flpLegacy = (name) =>
    DH.getFLPlayerLegacy(name);

const player = (name) =>
    Bukkit.getPlayer(name);

const getField = (name, target) =>
    RH.getFieldValue(name, target.class, target);

const listFields = (obj) =>
    obj.class.getFields().forEach((field) => {
        try {
            print(field.getName() + ': ' + RH.getFieldValue(field, obj));
        } catch (ignored) {
        }
    });

const print = log = (x) =>
    self.sendMessage(x + '');

/* Fit Native Functions to Bukkit API */

const toTicks = (millis) =>
    Math.ceil(millis / 50);

const runTaskLater = (callback, delayMS) =>
    Bukkit.getScheduler().runTaskLater(plugin, callback, toTicks(delayMS));

const runTaskTimer = (callback, intervalMS) => {
    const delayMS = toTicks(intervalMS);
    return Bukkit.getScheduler().runTaskTimer(plugin, callback, delayMS, delayMS);
};