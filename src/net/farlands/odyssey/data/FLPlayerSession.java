package net.farlands.odyssey.data;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.struct.TeleportRequest;
import net.farlands.odyssey.mechanic.Toggles;
import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.TextUtils;
import net.farlands.odyssey.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FLPlayerSession {
    public final Player player;
    public final OfflineFLPlayer handle;
    public long lastTimeRecorded;
    public double spamAccumulation;
    public boolean flying;
    public boolean staffChatToggledOn;
    public boolean afk;
    public Player replyToggleRecipient;
    public List<Location> backLocations;
    public List<TeleportRequest> teleportRequests;

    // Cooldowns
    public Cooldown afkCheckCooldown, mailCooldown, spamCooldown;
    private final Map<Class<? extends Command>, Integer> commandCooldowns;

    private final Map<Class<?>, Pair<Object, Integer>> tempData;

    public FLPlayerSession(Player player, OfflineFLPlayer handle) {
        this.player = player;
        this.handle = handle;
        this.lastTimeRecorded = System.currentTimeMillis();
        this.spamAccumulation = 0.0;
        this.flying = handle.flightPreference;
        this.staffChatToggledOn = true;
        this.replyToggleRecipient = null;
        this.backLocations = new ArrayList<>();
        this.teleportRequests = new ArrayList<>();

        this.afkCheckCooldown = handle.rank.hasAfkChecks() ? new Cooldown(handle.rank.getAfkCheckInterval()) : null;
        this.mailCooldown = new Cooldown(60L * 20L);
        this.spamCooldown = new Cooldown(160L);
        this.commandCooldowns = new HashMap<>();
        this.tempData = new HashMap<>();
    }

    public void update(boolean sendMessages) {
        handle.update();

        updatePlaytime();

        // Update rank
        for (int i = handle.rank.ordinal() + 1; i < Rank.VALUES.length - 1; ++i) {
            if (Rank.VALUES[i].hasRequirements(player, handle) && !Rank.VALUES[i + 1].hasRequirements(player, handle))
                handle.setRank(Rank.VALUES[i]);
        }

        handle.setLastLocation(player.getLocation());

        // Give vote rewards
        if (handle.voteRewards > 0) {
            if (sendMessages)
                player.sendMessage(ChatColor.GOLD + "Receiving " + handle.voteRewards + " vote reward" +
                        (handle.voteRewards > 1 ? "s!" : "!"));
            giveVoteRewards(handle.voteRewards);
            handle.voteRewards = 0;
        }

        player.setOp(handle.rank.hasOP());
        if (!handle.nickname.isEmpty())
            player.setDisplayName(handle.nickname);
        else
            player.setDisplayName(player.getName());
        handle.lastIP = player.getAddress().toString().split("/")[1].split(":")[0];
        flying = handle.flightPreference;
        if (!handle.rank.isStaff()) {
            player.setGameMode(GameMode.SURVIVAL);
            if (handle.rank != Rank.MEDIA) {
                flying = false;
                handle.vanished = false;
            }
        }
        if ((FarLands.getWorld().equals(player.getWorld()) || "world_the_end".equals(player.getWorld().getName())) &&
                !Rank.getRank(player).isStaff())
            flying = false;
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(player.getLocation());
        if (flags != null && flags.hasFlag(RegionFlag.FLIGHT) && flags.isAllowed(RegionFlag.FLIGHT))
            flying = true;
        player.setAllowFlight(flying || GameMode.CREATIVE.equals(player.getGameMode()) ||
                GameMode.SPECTATOR.equals(player.getGameMode()));
        (handle.topVoter && !handle.rank.isStaff() ? Rank.VOTER : handle.rank).getTeam().addEntry(player.getName());
        Toggles.hidePlayers(player);
        if (!handle.mail.isEmpty() && sendMessages && mailCooldown.isComplete()) {
            mailCooldown.reset();
            TextUtils.sendFormatted(player, "&(gold)You have mail. Read it with $(hovercmd,/mail read,{&(gray)Click to Run},&(yellow)/mail read)");
        }
    }

    public void updatePlaytime() {
        long ctmillis = System.currentTimeMillis();
        if (!(handle.vanished || player.isDead()))
            handle.secondsPlayed += (ctmillis - lastTimeRecorded) / 1000L;
        lastTimeRecorded = ctmillis;
    }

    public void giveVoteRewards(int amount) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        for (int i = 0; i < amount; ++i)
            FarLands.getDataHandler().getVoteRewards().forEach(reward -> Utils.giveItem(player, reward, false));
        player.giveExpLevels(FarLands.getFLConfig().getVoteConfig().getVoteXPBoost() * amount);
    }

    public void sendTeleportRequest(TeleportRequest.TeleportType type, Player recipient) {
        TeleportRequest request = new TeleportRequest(type, player, recipient);
        if (request.open())
            teleportRequests.add(request);
    }

    public void setCommandCooldown(Command command, long delay) {
        Integer taskUid = commandCooldowns.get(command.getClass());
        if(taskUid == null)
            commandCooldowns.put(command.getClass(), FarLands.getScheduler().scheduleAsyncDelayedTask(Utils.NO_ACTION, delay));
        else
            FarLands.getScheduler().getTask(taskUid).reset();
    }

    public boolean isCommandCooldownComplete(Command command) {
        return !commandCooldowns.containsKey(command.getClass()) ||
                FarLands.getScheduler().taskTimeRemaining(commandCooldowns.get(command.getClass())) == 0L;
    }

    public long commandCooldownTimeRemaining(Command command) {
        Integer taskUid = commandCooldowns.get(command.getClass());
        return taskUid == null ? 0L : FarLands.getScheduler().taskTimeRemaining(taskUid);
    }

    public synchronized void putTempData(Object assigner, Object data, long expirationTime, Runnable onExpire) {
        int expirationTaskUid = FarLands.getScheduler().scheduleAsyncDelayedTask(() -> {
            onExpire.run();

            synchronized (FLPlayerSession.this) {
                tempData.remove(assigner.getClass());
            }
        }, expirationTime);

        tempData.put(assigner.getClass(), new Pair<>(data, expirationTaskUid));
    }

    public void putTempData(Object assigner, Object data, long expirationTime) {
        putTempData(assigner, data, expirationTime, Utils.NO_ACTION);
    }

    public synchronized boolean hasTempData(Class<?> assignerClass) {
        return tempData.containsKey(assignerClass);
    }

    public boolean hasTempData(Object assigner) {
        return hasTempData(assigner.getClass());
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> T getTempData(Class<?> assignerClass) {
        return (T)tempData.get(assignerClass).getFirst();
    }

    public <T> T getTempData(Object assigner) {
        return getTempData(assigner.getClass());
    }

    public synchronized void discardTempData(Class<?> assignerClass) {
        Pair<Object, Integer> data = getTempData(assignerClass);
        if(data != null) {
            FarLands.getScheduler().cancelTask(data.getSecond());
            tempData.remove(assignerClass);
        }
    }

    public void discardTempData(Object assigner) {
        discardTempData(assigner.getClass());
    }
}
