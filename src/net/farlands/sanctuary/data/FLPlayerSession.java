package net.farlands.sanctuary.data;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.util.TextUtils;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.struct.Package;
import net.farlands.sanctuary.data.struct.PackageToggle;
import net.farlands.sanctuary.data.struct.TeleportRequest;
import net.farlands.sanctuary.mechanic.Toggles;
import net.farlands.sanctuary.scheduling.TaskBase;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.*;
import java.util.stream.Collectors;

public class FLPlayerSession {
    public final Player player;
    public final OfflineFLPlayer handle;
    public final PermissionAttachment permissionAttachment;
    public long lastTimeRecorded;
    public long lastUnvanish;
    public double spamAccumulation;
    public boolean afk;
    public boolean flying;
    public boolean showStaffChat;
    public boolean autoSendStaffChat;
    public boolean isInEvent;
    public CommandSender replyToggleRecipient;
    public Location seatExit;
    public TeleportRequest outgoingTeleportRequest;
    public List<TeleportRequest> incomingTeleportRequests;

    // Transient fields
    public TransientField<Player> givePetRecipient;
    public TransientField<CommandSender> lastMessageSender;
    public TransientField<String> lastDeletedHomeName;

    // Cooldowns
    public Cooldown afkCheckCooldown,
            afkCheckInitializerCooldown,
            flightDetectorMute,
            flyAlertCooldown,
            mailCooldown,
            spamCooldown;
    private final Map<Class<? extends Command>, Integer> commandCooldowns;

    // Internally managed fields
    private final List<Location> backLocations;
    private long lastBackLocationModification;

    public FLPlayerSession(Player player, OfflineFLPlayer handle) {
        this.player = player;
        this.handle = handle;
        this.permissionAttachment = player.addAttachment(FarLands.getInstance());
        this.lastTimeRecorded = System.currentTimeMillis();
        this.lastUnvanish = 0;
        this.spamAccumulation = 0.0;
        this.afk = false;
        this.flying = handle.flightPreference;
        this.showStaffChat = true;
        this.autoSendStaffChat = false;
        this.isInEvent = false;
        this.replyToggleRecipient = null;
        this.seatExit = null;
        this.outgoingTeleportRequest = null;
        this.incomingTeleportRequests = new ArrayList<>();

        this.givePetRecipient = new TransientField<>();
        this.lastMessageSender = new TransientField<>();
        this.lastDeletedHomeName = new TransientField<>();

        this.afkCheckInitializerCooldown = null;
        this.afkCheckCooldown = new Cooldown(30L * 20L);
        this.mailCooldown = new Cooldown(60L * 20L);
        this.spamCooldown = new Cooldown(160L);
        this.flyAlertCooldown = new Cooldown(10L);
        this.flightDetectorMute = new Cooldown(0L);
        this.commandCooldowns = new HashMap<>();

        this.backLocations = new ArrayList<>();
        this.lastBackLocationModification = 0L;
    }

    FLPlayerSession(Player player, FLPlayerSession cached) {
        this.player = player;
        this.handle = cached.handle;
        this.permissionAttachment = player.addAttachment(FarLands.getInstance());
        this.lastTimeRecorded = System.currentTimeMillis();
        this.lastUnvanish = 0;
        this.spamAccumulation = cached.spamAccumulation;
        this.afk = cached.afk;
        this.flying = cached.flying;
        this.showStaffChat = cached.showStaffChat;
        this.autoSendStaffChat = cached.autoSendStaffChat;
        this.isInEvent = cached.isInEvent;
        this.replyToggleRecipient = cached.replyToggleRecipient;
        this.seatExit = null;
        this.outgoingTeleportRequest = null;
        this.incomingTeleportRequests = new ArrayList<>();

        this.givePetRecipient = new TransientField<>();
        this.lastMessageSender = new TransientField<>();
        this.lastDeletedHomeName = new TransientField<>();

        this.afkCheckInitializerCooldown = null;
        this.afkCheckCooldown = new Cooldown(30L * 20L);
        this.mailCooldown = new Cooldown(60L * 20L);
        this.spamCooldown = new Cooldown(160L);
        this.flyAlertCooldown = new Cooldown(10L);
        this.flightDetectorMute = new Cooldown(0L);
        this.commandCooldowns = cached.commandCooldowns;

        this.backLocations = cached.backLocations;
        this.lastBackLocationModification = 0L;
    }

    public void deactivateAFKChecks() {
        if (afkCheckInitializerCooldown != null) {
            afkCheckInitializerCooldown.cancel();
            afkCheckInitializerCooldown = null;
        }
        afkCheckCooldown.cancel();
    }

    void destroy() {
        deactivateAFKChecks();
        givePetRecipient.discard();
        lastMessageSender.discard();
        lastDeletedHomeName.discard();
        mailCooldown.cancel();
        spamCooldown.cancel();
        flyAlertCooldown.cancel();
        flightDetectorMute.cancel();
        commandCooldowns.values().forEach(FarLands.getScheduler()::cancelTask);
    }

    /*
     * WARNING: DO NOT PUT ANY CALLS TO THE FARLANDS SCHEDULER IN THESE UPDATE METHODS OR IT WILL CAUSE SERVER CRASHES
     */

    public void update(boolean sendMessages) {
        handle.update();
        if (!handle.username.equals(player.getName()))
            handle.username = player.getName();

        updatePlaytime();
        updatePermissions();

        // Give donors their claim blocks
        if (handle.bonusClaimBlocksReceived < handle.rank.getClaimBlockBonus()) {
            RegionProtection.getDataManager().modifyClaimBlocks(
                    handle.uuid,
                    handle.rank.getClaimBlockBonus() - handle.bonusClaimBlocksReceived
            );
            handle.bonusClaimBlocksReceived = handle.rank.getClaimBlockBonus();
        }

        // Update rank
        for (int i = handle.rank.ordinal() + 1; i < Rank.VALUES.length - 1; ++i) {
            if (Rank.VALUES[i].hasRequirements(player, handle) && !Rank.VALUES[i + 1].hasRequirements(player, handle))
                handle.setRank(Rank.VALUES[i]);
        }

        handle.setLastLocation(player.getLocation());

        // Give vote rewards
        if (handle.voteRewards > 0) {
            if (sendMessages) {
                player.sendMessage(ChatColor.GOLD + "Receiving " + handle.voteRewards + " vote reward" +
                        (handle.voteRewards > 1 ? "s!" : "!"));
            }
            giveVoteRewards(handle.voteRewards);
            handle.voteRewards = 0;
        }

        player.setOp(handle.rank.hasOP());
        if (handle.nickname != null && !handle.nickname.isEmpty())
            player.setDisplayName(handle.nickname);
        else
            player.setDisplayName(player.getName());
        handle.getDisplayRank().getTeam().addEntry(player.getName());
        player.setPlayerListName(handle.getDisplayRank().getNameColor() + handle.username);
        handle.lastIP = player.getAddress().getAddress().getHostAddress();

        flying = handle.flightPreference;
        if (!handle.rank.isStaff()) {
            player.setGameMode(GameMode.SURVIVAL);
            if (handle.rank != Rank.MEDIA) {
                flying = false;
                handle.vanished = false;
            }
        }
        if (!handle.rank.isStaff() && (
                FarLands.getWorld().equals(player.getWorld()) ||
                "world_the_end".equals(player.getWorld().getName()))
        ) {
            flying = false;
        }
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(player.getLocation());
        if (flags != null && flags.hasFlag(RegionFlag.FLIGHT) && flags.isAllowed(RegionFlag.FLIGHT))
            flying = true;
        player.setAllowFlight(flying || GameMode.CREATIVE.equals(player.getGameMode()) ||
                GameMode.SPECTATOR.equals(player.getGameMode()));

        Toggles.hidePlayers(player);

        if (handle.ptime >= 0)
            player.setPlayerTime(handle.ptime, false);
        if (handle.pweather)
            player.setPlayerWeather(WeatherType.CLEAR);

        if (!handle.mail.isEmpty() && sendMessages && mailCooldown.isComplete()) {
            mailCooldown.reset();
            TextUtils.sendFormatted(player, "&(gold)You have mail. Read it with $(hovercmd,/mail read,{&(gray)Click to Run},&(yellow)/mail read)");
        }
    }

    private void updatePermissions() {
        // Head Database
        permissionAttachment.setPermission("headdb.open", handle.rank.specialCompareTo(Rank.DONOR) >= 0);

        // Core Protect
        permissionAttachment.setPermission("coreprotect.inspect", handle.rank.isStaff());
        permissionAttachment.setPermission("coreprotect.lookup", handle.rank.isStaff());

        // PetBlock stuff
        permissionAttachment.setPermission("petblocks.admin.command.pets", handle.rank.specialCompareTo(Rank.MOD) >= 0);
        permissionAttachment.setPermission("petblocks.admin.command.reload", handle.rank.specialCompareTo(Rank.ADMIN) >= 0);

        boolean isSponsor = handle.rank.specialCompareTo(Rank.SPONSOR) >= 0;
        permissionAttachment.setPermission("petblocks.command.use", isSponsor);
        permissionAttachment.setPermission("petblocks.command.rename", isSponsor);
        permissionAttachment.setPermission("petblocks.command.toggle", isSponsor);
        permissionAttachment.setPermission("petblocks.command.call", isSponsor);
    }

    public void updatePlaytime() {
        if (handle.rank.isStaff()) {
            long ctmillis = System.currentTimeMillis();
            if (!handle.vanished && !player.isDead())
                handle.secondsPlayed += (ctmillis - lastTimeRecorded) / 1000L;
            lastTimeRecorded = ctmillis;
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
            handle.secondsPlayed = offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
        }
    }

    public void givePackages() {
        List<Package> packages = FarLands.getDataHandler().getPackages(handle.uuid);
        boolean sentMessage = true;
        for (int i = packages.size(); --i >= 0; ) {
            if (packages.get(i).forceSend || handle.packageToggle == PackageToggle.ACCEPT) {
                if (sentMessage) {
                    sentMessage = false;
                    // Notify the player how many packages they've been sent
                    TextUtils.sendFormatted(
                            player, "&(gold)Receiving {&(aqua)%0} $(inflect,noun,0,package) from {&(aqua)%1}.",
                            packages.size(),
                            packages.stream().filter(p -> p.forceSend).map(laPackage -> "{" + laPackage.senderName + "}")
                                    .collect(Collectors.joining(", "))
                    );
                }
                // Give the packages and send the messages
                final String message = packages.get(i).message;
                if (message != null && !message.isEmpty())
                    TextUtils.sendFormatted(player, "&(gold)Item {&(aqua)%0} was sent with the following message {&(aqua)%1}",
                            FLUtils.itemName(packages.get(i).item), message);
                FLUtils.giveItem(player, packages.get(i).item, true);
                packages.remove(i);
            }
        }
        if (handle.packageToggle == PackageToggle.ASK) {
            if (!packages.isEmpty()) {
                // Notify the player how many packages they've been sent
                TextUtils.sendFormatted(
                        player, "&(gold)Receiving {&(aqua)%0} $(inflect,noun,0,package) from {&(aqua)%1}." +
                                " Use /paccecpt|pdecline <player> to accept or decline the packages.",
                        packages.size(),
                        packages.stream().map(lPackage -> "{" + lPackage.senderName + "}").collect(Collectors.joining(", "))
                );
            }
        }
    }

    public boolean unsit() {
        if (seatExit == null)
            return false;

        Entity chair = player.getVehicle();
        if (chair != null) {
            chair.eject();
            // Event handler takes care of removal
        }
        return true;
    }

    public void updateVanish() {
        if (!handle.vanished)
            lastUnvanish = System.currentTimeMillis();
    }

    public boolean isVanishedWithBuffer() {
        return handle.vanished || System.currentTimeMillis() - lastUnvanish < 1000;
    }

    public void allowCoRollback() {
        permissionAttachment.setPermission("coreprotect.rollback", true);
    }

    public boolean canCoRollback() {
        return player.hasPermission("coreprotect.rollback");
    }

    public void resetCoRollback() {
        permissionAttachment.setPermission("coreprotect.rollback", player.isOp());
    }

    public void giveVoteRewards(int amount) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        for (int i = 0; i < amount; ++i)
            FarLands.getFLConfig().voteConfig.voteRewards.forEach(reward -> FLUtils.giveItem(player, reward.getStack(), false));
        player.giveExpLevels(FarLands.getFLConfig().voteConfig.voteXPBoost * amount);
    }

    public void setCommandCooldown(Command command, long delay) {
        TaskBase task = FarLands.getScheduler().getTask(commandCooldowns.getOrDefault(command.getClass(), -1));
        if (task == null) {
            commandCooldowns.put(command.getClass(), FarLands.getScheduler().scheduleAsyncDelayedTask(() -> {
                if (player.isOnline())
                    TextUtils.sendFormatted(player, "&(gold)You may use {&(aqua)/%0} again.", command.getName());
            }, delay));
        } else
            task.reset();
    }

    public boolean isCommandCooldownComplete(Command command) {
        return !commandCooldowns.containsKey(command.getClass()) ||
                FarLands.getScheduler().taskTimeRemaining(commandCooldowns.get(command.getClass())) == 0L;
    }

    public long commandCooldownTimeRemaining(Command command) {
        Integer taskUid = commandCooldowns.get(command.getClass());
        return taskUid == null ? 0L : FarLands.getScheduler().taskTimeRemaining(taskUid);
    }

    public void completeCooldown(Command command) {
        TaskBase task = FarLands.getScheduler().getTask(commandCooldowns.getOrDefault(command.getClass(), -1));
        if (task != null)
            task.complete();
    }

    public void addBackLocation(Location location) {
        long time = System.currentTimeMillis();
        if (time - lastBackLocationModification > 250) {
            backLocations.add(location);
            if (backLocations.size() > 5)
                backLocations.remove(0);

            lastBackLocationModification = time;
        }
    }

    public Location getBackLocation() {
        lastBackLocationModification = System.currentTimeMillis();
        return backLocations.isEmpty() ? null : backLocations.remove(backLocations.size() - 1);
    }
}
