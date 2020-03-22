var eval = function(exp) {
	return engine.eval(exp);
};

/* Global Shortcuts */
var org = Packages.org;
var net = Packages.net;
var fl = net.farlands.odyssey;
var FL = fl.FarLands;
var plugin = FL.getInstance();
var Bukkit = org.bukkit.Bukkit;
var server = Bukkit.getServer();
var ChatColor = org.bukkit.ChatColor;
var RH = fl.util.ReflectionHelper;
var Utils = fl.util.Utils;
var DH = FL.getDataHandler();

var invoke = function(method, target) {
    return RH.invoke(method, target.getClass(), target);
};

var mechanic = function(clazz) {
    return FL.getMechanicHandler().getMechanic(clazz);
};

var getEntityById = function(uuid) {
    entities = self.getWorld().getEntities();
    for(var i = 0;i < entities.size();++ i) {
        if(entities[i].getUniqueId().toString() === uuid)
            return entities[i];
    }
    return null;
};

var getRegionFile = function() {
    return "r." + (self.getLocation().getBlockX() >> 9) + "." + (self.getLocation().getBlockZ() >> 9) + ".mca"
};

var sendFormatted = function(recipient, input, values) {
    recipient.spigot().sendMessage(fl.util.TextUtils.format(input, values));
};

var flp = function(name) {
    return FL.getPDH().getFLPlayerMatching(name);
};

var session = function(name) {
    var p = player(name);
    return p == null ? null : FL.getDataHandler().getSession(p);
}

var flpLegacy = function(name) {
    return DH.getFLPlayerLegacy(name);
};

var player = function(name) {
    return Bukkit.getPlayer(name);
};

var getField = function(name, target) {
    return RH.getFieldValue(name, target.class, target);
};

var listFields = function(o) {
    var fields = o.class.getFields();
    for(var i=0; i < fields.length; ++i) {
        try {
            print(fields[i].getName() + ": " + RH.getFieldValue(fields[i],o));
        } catch(e) {}
    }
};

var print = function(x) {
    self.sendMessage(x == null ? "null" : x.toString());
};

/* Fit Native Functions to Bukkit API */

var toTicks = function(millis) {
    return Math.ceil(millis / 50);
};

var runTaskLater = function(callback, delayMS) {
    return Bukkit.getScheduler().runTaskLater(plugin, callback, toTicks(delayMS));
};

var runTaskTimer = function(callback, intervalMS) {
    var delayMS = toTicks(intervalMS);
    return Bukkit.getScheduler().runTaskTimer(plugin, callback, delayMS, delayMS);
};