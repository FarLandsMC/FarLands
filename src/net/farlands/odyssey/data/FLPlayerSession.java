package net.farlands.odyssey.data;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.struct.TeleportRequest;
import net.farlands.odyssey.mechanic.Toggles;
import net.farlands.odyssey.util.TextUtils;
import net.farlands.odyssey.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
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
    public boolean afk;
    public boolean flying;
    public boolean staffChatToggledOn;
    public boolean isInEvent;
    public CommandSender replyToggleRecipient;
    public Location seatExit;
    public List<Location> backLocations;
    public List<TeleportRequest> teleportRequests;

    // Transient fields
    public TransientField<Player> givePetRecipient;
    public TransientField<CommandSender> lastMessageSender;
    public TransientField<String> lastDeletedHomeName;

    // Cooldowns
    public Cooldown afkCheckInitializerCooldown, afkCheckCooldown, mailCooldown, spamCooldown, flyAlertCooldown,
            flightDetectorMute;
    private final Map<Class<? extends Command>, Integer> commandCooldowns;

    // Internally managed fields
    private boolean backIgnoreTP;

    public FLPlayerSession(Player player, OfflineFLPlayer handle) {
        this.player = player;
        this.handle = handle;
        this.lastTimeRecorded = System.currentTimeMillis();
        this.spamAccumulation = 0.0;
        this.afk = false;
        this.flying = handle.flightPreference;
        this.staffChatToggledOn = false;
        this.isInEvent = false;
        this.replyToggleRecipient = null;
        this.seatExit = null;
        this.backLocations = new ArrayList<>();
        this.teleportRequests = new ArrayList<>();

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

        this.backIgnoreTP = false;
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

    public void sendTeleportRequest(Player sender, TeleportRequest.TeleportType type) {
        TeleportRequest request = new TeleportRequest(type, sender, player);
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

    public synchronized boolean ignoreTeleportForBackLocations() {
        boolean old = backIgnoreTP;
        backIgnoreTP = false;
        return old;
    }

    public synchronized void setIgnoreTeleportForBackLocations() {
        backIgnoreTP = true;
    }
}
