package net.farlands.sanctuary.mechanic;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import com.kicas.rp.util.TextUtils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.FLCommandEvent;
import net.farlands.sanctuary.command.player.CommandMessage;
import net.farlands.sanctuary.command.staff.CommandVanish;
import net.farlands.sanctuary.data.Cooldown;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.discord.DiscordChannel;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.Pair;
import net.farlands.sanctuary.util.FLUtils;

import net.md_5.bungee.api.ChatMessageType;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AFK extends Mechanic {
    private final Map<UUID, Pair<String, Integer>> afkCheckList;

    private static AFK instance;

    public AFK() {
        this.afkCheckList = new HashMap<>();
    }

    @Override
    public void onStartup() {
        instance = this;

        Bukkit.getScheduler().runTaskTimerAsynchronously(FarLands.getInstance(), () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (afkCheckList.containsKey(player.getUniqueId())) {
                    sendFormatted(player, ChatMessageType.ACTION_BAR, "&(red,bold,magic)MM {&(reset)%0} MM",
                            afkCheckList.get(player.getUniqueId()).getFirst());
                }
            });
        }, 0L, 40L);
    }

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        setAFKCooldown(player);
    }

    @Override
    public void onPlayerQuit(Player player) {
        afkCheckList.remove(player.getUniqueId());
        FLPlayerSession session = FarLands.getDataHandler().getSession(player);
        if (session.afkCheckInitializerCooldown != null)
            session.afkCheckInitializerCooldown.cancel();
        session.afkCheckCooldown.cancel();
        session.afk = false;
    }

    @EventHandler
    public void onBlockBroken(BlockBreakEvent event) {
        resetInitializerCooldown(event.getPlayer());
    }

    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event) {
        resetInitializerCooldown(event.getPlayer());
    }

    @EventHandler
    public void onFLCommand(FLCommandEvent event) {
        if (CommandMessage.class.equals(event.getCommand()) && event.getSender() instanceof Player)
            resetInitializerCooldown((Player) event.getSender());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(event.getPlayer());
        if (afkCheckList.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            int answer;
            try {
                answer = Integer.parseInt(event.getMessage());
            } catch (NumberFormatException ex) {
                answer = Integer.MAX_VALUE;
            }
            int actualAnswer = afkCheckList.get(event.getPlayer().getUniqueId()).getSecond();
            if (answer != actualAnswer) {
                Bukkit.getScheduler().runTask(FarLands.getInstance(),
                        () -> event.getPlayer().kickPlayer(ChatColor.RED + "The correct answer was: " + actualAnswer));
            } else {
                setAFKCooldown(event.getPlayer());
                event.getPlayer().sendMessage(ChatColor.GREEN + "Correct.");
            }
            session.afkCheckCooldown.cancel();
            afkCheckList.remove(event.getPlayer().getUniqueId());
            return;
        }
        if (session.afk)
            setNotAFK(session);
        resetInitializerCooldown(session);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getYaw() != event.getTo().getYaw() || event.getFrom().getPitch() != event.getTo().getPitch()) {
            FLPlayerSession session = FarLands.getDataHandler().getSession(event.getPlayer());
            if (session.afk)
                setNotAFK(session);
            if (session.afkCheckCooldown != null && session.afkCheckCooldown.isComplete())
                session.afkCheckInitializerCooldown.resetCurrentTask();
        }
    }

    public static void setNotAFK(FLPlayerSession session) {
        session.afk = false;
        Logging.broadcast(flp -> !flp.handle.isIgnoring(session.player), " * %0 is no longer AFK.", session.handle.username);
    }

    public static void setAFKCooldown(Player player) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(player);

        if (session.afkCheckInitializerCooldown == null)
            session.afkCheckInitializerCooldown = new Cooldown(session.handle.rank.getAfkCheckInterval() * 60L * 20L);

        session.afkCheckInitializerCooldown.reset(() -> {
            if (player.isOnline()) {
                if ("farlands".equals(player.getWorld().getName()) || player.isGliding() || session.isInEvent) {
                    setAFKCooldown(player);
                    return;
                }

                if (session.handle.rank.isStaff()) {
                    // Put the player into vanish
                    FarLands.getCommandHandler().getCommand(CommandVanish.class).execute(session.player, null);
                    Logging.broadcastStaff(
                            TextUtils.format("&(red)%0 has gone AFK and is now vanished.", session.handle.username),
                            DiscordChannel.ALERTS
                    );
                    return;
                }

                if (session.afk) {
                    kickAFK(player);
                    return;
                }

                int a = FLUtils.RNG.nextInt(17), b = FLUtils.RNG.nextInt(17);
                boolean op = FLUtils.RNG.nextBoolean(); // true: +, false: -
                String check = ChatColor.RED.toString() + ChatColor.BOLD + "AFK Check: " + a + (op ? " + " : " - ") + b;
                player.sendMessage(check);
                player.sendTitle(check, "", 20, 120, 60);
                FarLands.getDebugger().echo("Sent AFK check to " + player.getName());
                instance.afkCheckList.put(player.getUniqueId(), new Pair<>(check, op ? a + b : a - b));
                session.afkCheckCooldown.reset(() -> kickAFK(player));
            }
        });
    }

    private static void resetInitializerCooldown(Player player) {
        resetInitializerCooldown(FarLands.getDataHandler().getSession(player));
    }

    private static void resetInitializerCooldown(FLPlayerSession session) {
        if (session.afkCheckCooldown != null && session.afkCheckCooldown.isComplete())
            session.afkCheckInitializerCooldown.resetCurrentTask();
    }

    private static void kickAFK(Player player) {
        if (player.isOnline()) {
            FarLands.getDebugger().echo("Kicking " + player.getName() + " for being AFK or answering the question incorrectly.");
            player.kickPlayer(ChatColor.RED + "Kicked for being AFK.");
            Logging.broadcastStaff(ChatColor.RED + player.getName() + " was kicked for being AFK.");
        }
    }
}
