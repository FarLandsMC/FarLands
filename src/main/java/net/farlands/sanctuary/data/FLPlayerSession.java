package net.farlands.sanctuary.data;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.util.TextUtils;
import com.kicas.rp.util.TextUtils2;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.ChatHandler;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.data.struct.Package;
import net.farlands.sanctuary.data.struct.*;
import net.farlands.sanctuary.mechanic.Toggles;
import net.farlands.sanctuary.scheduling.TaskBase;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles toggle for an online player.
 */
public class FLPlayerSession {

    public final Player player;
    public final OfflineFLPlayer handle;
    public final PermissionAttachment permissionAttachment;
    public long vanishStart;
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
        sharehomeCooldown,
        spamCooldown;
    private final Map<Class<? extends Command>, Integer> commandCooldowns;
    public List<Component> afkMessages;

    // Internally managed fields
    private final List<Location> backLocations;
    private long lastBackLocationModification;

    public FLPlayerSession(Player player, OfflineFLPlayer handle) {
        this.player = player;
        this.handle = handle;
        this.permissionAttachment = player.addAttachment(FarLands.getInstance());
        this.vanishStart = handle.vanished ? System.currentTimeMillis() : -1;
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
        this.sharehomeCooldown = new Cooldown(60L * 20L);
        this.spamCooldown = new Cooldown(160L);
        this.flyAlertCooldown = new Cooldown(10L);
        this.flightDetectorMute = new Cooldown(0L);
        this.commandCooldowns = new HashMap<>();
        this.afkMessages = new ArrayList<>();

        this.backLocations = new ArrayList<>();
        this.lastBackLocationModification = 0L;
    }

    FLPlayerSession(Player player, FLPlayerSession cached) {
        this.player = player;
        this.handle = cached.handle;
        this.permissionAttachment = player.addAttachment(FarLands.getInstance());
        this.vanishStart = cached.handle.vanished ? System.currentTimeMillis() : -1;
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
        this.sharehomeCooldown = new Cooldown(60L * 20L);
        this.spamCooldown = new Cooldown(160L);
        this.flyAlertCooldown = new Cooldown(10L);
        this.flightDetectorMute = new Cooldown(0L);
        this.commandCooldowns = cached.commandCooldowns;
        this.afkMessages = cached.afkMessages;

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
        sharehomeCooldown.cancel();
        spamCooldown.cancel();
        flyAlertCooldown.cancel();
        flightDetectorMute.cancel();
        commandCooldowns.values().forEach(FarLands.getScheduler()::cancelTask);
    }

    /*
     * WARNING: DO NOT PUT ANY CALLS TO THE FARLANDS SCHEDULER IN THESE UPDATE METHODS OR IT WILL CAUSE SERVER CRASHES
     */

    public void update(boolean sendMessages) {
        // Data maintenance
        if (handle.currentMute != null && handle.currentMute.hasExpired()) {
            handle.currentMute = null;
            player.sendMessage(ChatColor.GREEN + "Your mute has expired.");
        }

        handle.ignoredPlayers.forEach(uuid -> handle.updateIgnoreStatus(uuid, IgnoreStatus.IgnoreType.ALL, true));
        handle.ignoredPlayers.clear();

        if (!handle.username.equals(player.getName())) {
            handle.username = player.getName();
        }

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
            if (Rank.VALUES[i].hasRequirements(player, handle) && !Rank.VALUES[i + 1].hasRequirements(player, handle)) {
                handle.setRank(Rank.VALUES[i]);
            }
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
        if (handle.nickname != null) {
            player.displayName(handle.nickname);
        } else {
            player.displayName(player.name());
        }
        handle.getDisplayRank().getTeam().addEntry(player.getName());
        player.playerListName(handle.getDisplayRank().colorName(handle.username));
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
            "world_the_end" .equals(player.getWorld().getName()))
        ) {
            flying = false;
        }
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(player.getLocation());
        if (flags != null && flags.hasFlag(RegionFlag.FLIGHT) && flags.isAllowed(RegionFlag.FLIGHT)) {
            flying = true;
        }
        player.setAllowFlight(flying || GameMode.CREATIVE.equals(player.getGameMode()) ||
                              GameMode.SPECTATOR.equals(player.getGameMode()));

        Toggles.hidePlayers(player);

        if (handle.ptime >= 0) {
            player.setPlayerTime(handle.ptime, false);
        }
        if (handle.pweather) {
            player.setPlayerWeather(WeatherType.CLEAR);
        }

        if (!handle.mail.isEmpty() && sendMessages && mailCooldown.isComplete()) {
            mailCooldown.reset();
            TextUtils.sendFormatted(player, "&(gold)You have mail. Read it with $(hovercmd,/mail read,{&(gray)Click to Run},&(yellow)/mail read)");
        }

        if (!handle.pendingSharehomes.isEmpty() && sendMessages && sharehomeCooldown.isComplete()) {
            sharehomeCooldown.reset();

            List<String> pendingHomes = new ArrayList<>();

            handle.pendingSharehomes.forEach((k, v) -> {
                String message = v.message() == null ? "" : "&(gold)Message: &(aqua)" + v.message().replaceAll(",", " ") + "\n";
                pendingHomes.add(
                    "{" +
                    "$(click:suggest_command,/sharehome accept " + k + " )" +
                    "$(hover:show_text," +
                    "&(gold)Sender: &(aqua)" + k + "\n" +
                    message + "&(gold)Name: &(aqua)" + v.home().getName() + "\n" +
                    "&(" +
                    "gray)Click to accept" +
                    ")&(aqua)" + k +
                    "}"
                );
            });

            try {
                TextUtils2.sendFormatted(
                    player,
                    "&(gold)You have pending homes from %0 %1: %2\n" +
                    "Hover over the %3 to view more info.",
                    handle.pendingSharehomes.size(),
                    handle.pendingSharehomes.size() == 1 ? "player" : "players",
                    String.join(", ", pendingHomes),
                    handle.pendingSharehomes.size() == 1 ? "name" : "names"
                );
            } catch (TextUtils2.ParserError e) {
                e.printStackTrace();
            }
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
        permissionAttachment.setPermission("petblocks.gui", isSponsor);
    }

    public void updatePlaytime() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
        handle.secondsPlayed = offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
    }

    public void givePackages() {
        List<Package> packages = FarLands.getDataHandler().getPackages(handle.uuid);
        boolean sentMessage = true;
        for (int i = packages.size(); --i >= 0; ) {
            if (packages.get(i).forceSend() || handle.packageToggle == PackageToggle.ACCEPT) {
                if (sentMessage) {
                    sentMessage = false;
                    // Notify the player how many packages they've been sent
                    Component message = ComponentColor.gold("Receiving ")
                        .append(ComponentColor.aqua(packages.size() + ""))
                        .append(ComponentColor.gold(" from "))
                        .append(ComponentColor.aqua(
                            packages.stream()
                                .filter(p -> p.forceSend() || handle.packageToggle == PackageToggle.ACCEPT)
                                .map(Package::senderName)
                                .collect(Collectors.joining(", ")))
                        );
                    player.sendMessage(message);
                    addAFKMessage(message);
                }
                // Give the packages and send the messages
                final String message = packages.get(i).message();
                if (message != null && !message.isEmpty()) {
                    Component messageC = ComponentColor.gold("Item ")
                        .append(ComponentUtils.item(packages.get(i).item()))
                        .append(ComponentColor.gold(" was sent with the following message "))
                        .append(ChatHandler.handleReplacements(message, handle));
                    player.sendMessage(messageC);
                    addAFKMessage(messageC);
                }
                FLUtils.giveItem(player, packages.get(i).item(), true);
                packages.remove(i);
            }
        }
        if (handle.packageToggle == PackageToggle.ASK) {
            if (!packages.isEmpty()) {
                // Notify the player how many packages they've been sent
                Component message = ComponentColor.gold("Receiving ")
                    .append(ComponentColor.aqua(packages.size() + ""))
                    .append(ComponentColor.gold(" package%s from ", packages.size() == 1 ? "" : "s"))
                    .append(ComponentColor.aqua(
                        packages.stream()
                            .map(Package::senderName)
                            .collect(Collectors.joining(", ")))
                    )
                    .append(ComponentColor.gold(". Use /paccept|decline [player] to accept or decline the packages."));
                player.sendMessage(message);
                addAFKMessage(message);
            }
        }
    }

    public boolean unsit() {
        if (seatExit == null) {
            return false;
        }

        Entity chair = player.getVehicle();
        if (chair != null) {
            chair.eject();
            // Event handler takes care of removal
        }
        return true;
    }

    public void updateVanish() {
        if (!handle.vanished) {
            lastUnvanish = System.currentTimeMillis();
            this.removeVanishPlaytime();
        } else {
            this.vanishStart = System.currentTimeMillis();
        }
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
        for (int i = 0; i < amount; ++i) {
            FarLands.getFLConfig().voteConfig.voteRewards().forEach(reward -> FLUtils.giveItem(player, reward, false));
        }
        player.giveExpLevels(FarLands.getFLConfig().voteConfig.voteXPBoost * amount);
    }

    public void setCommandCooldown(Command command, long delay) {
        TaskBase task = FarLands.getScheduler().getTask(commandCooldowns.getOrDefault(command.getClass(), -1));
        if (task == null) {
            commandCooldowns.put(command.getClass(), FarLands.getScheduler().scheduleAsyncDelayedTask(() -> {
                if (player.isOnline()) {
                    Component component = ComponentColor.gold("You may use ")
                        .append(ComponentUtils.suggestCommand("/" + command.getName()))
                        .append(ComponentColor.gold(" again."));
                    player.sendMessage(component);
                    addAFKMessage(component);
                }
            }, delay));
        } else {
            task.reset();
        }
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
        if (task != null) {
            task.complete();
        }
    }

    public void addBackLocation(Location location) {
        long time = System.currentTimeMillis();
        if (time - lastBackLocationModification > 250) {
            backLocations.add(location);
            if (backLocations.size() > 5) {
                backLocations.remove(0);
            }

            lastBackLocationModification = time;
        }
    }

    public Location getBackLocation() {
        lastBackLocationModification = System.currentTimeMillis();
        return backLocations.isEmpty() ? null : backLocations.remove(backLocations.size() - 1);
    }

    public void removeVanishPlaytime() {
        int vanishDuration = (int) ((System.currentTimeMillis() - vanishStart) / 1000 * 20); // Ticks
        if (vanishDuration > 0) {
            this.player.decrementStatistic(Statistic.PLAY_ONE_MINUTE, vanishDuration);
        }
    }

    public void addAFKMessage(Component message) {
        if (this.afk) {
            afkMessages.add(message);
        }
    }

    public void sendAFKMessages() {
        if (!afkMessages.isEmpty()) {
            player.sendMessage(ComponentColor.green("While you were gone..."));
            player.sendMessage(
                Component.join(
                    JoinConfiguration.separator(Component.newline()),
                    afkMessages
                )
            );
            afkMessages.clear();
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
        }
    }
}
