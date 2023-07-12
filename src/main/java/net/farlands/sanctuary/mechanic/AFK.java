package net.farlands.sanctuary.mechanic;

import com.kicas.rp.util.Pair;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.FLCommandEvent;
import net.farlands.sanctuary.command.player.CommandMessage;
import net.farlands.sanctuary.data.Cooldown;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.ComponentUtils;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles afk checks, events, and kicks
 */
public class AFK extends Mechanic {

    private final Map<UUID, Pair<String, Integer>> afkCheckMap; // Players that are currently being checked for afk

    private static AFK instance; // Instance of the mechanic

    public AFK() {
        this.afkCheckMap = new HashMap<>();
    }

    @Override
    public void onStartup() {
        instance = this;

        Bukkit.getScheduler().runTaskTimerAsynchronously(FarLands.getInstance(), () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (this.afkCheckMap.containsKey(player.getUniqueId())) {
                    player.sendActionBar(
                        ComponentColor.red("MM ", TextDecoration.OBFUSCATED, TextDecoration.BOLD)
                            .append(ComponentColor.white(this.afkCheckMap.get(player.getUniqueId()).getFirst()))
                            .append(ComponentColor.red("MM ", TextDecoration.OBFUSCATED, TextDecoration.BOLD))
                    );
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
        afkCheckMap.remove(player.getUniqueId());
        FLPlayerSession session = FarLands.getDataHandler().getSession(player);
        if (session.afkCheckInitializerCooldown != null) {
            session.afkCheckInitializerCooldown.cancel();
        }
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
    public void onBookEdit(PlayerEditBookEvent event) {
        resetInitializerCooldown(event.getPlayer());
    }

    @EventHandler
    public void onFLCommand(FLCommandEvent event) {
        if (CommandMessage.class.equals(event.getCommand()) && event.getSender() instanceof Player) {
            resetInitializerCooldown((Player) event.getSender());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(event.getPlayer());
        if (this.afkCheckMap.containsKey(event.getPlayer().getUniqueId())) { // If the player is in the afk check map
            event.setCancelled(true);
            int answer = Command.parseNumber(ComponentUtils.toText(event.message()), Integer::parseInt, Integer.MAX_VALUE); // Parse their message as a number
            int actualAnswer = this.afkCheckMap.get(event.getPlayer().getUniqueId()).getSecond(); // Get the actual answer
            if (answer != actualAnswer) { // If they're different, kick the player
                Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> event.getPlayer().kick(ComponentColor.red("The correct answer was: {}.", actualAnswer)));
            } else { // Otherwise, reset their cooldown
                setAFKCooldown(event.getPlayer());
                event.getPlayer().sendMessage(ComponentColor.green("Correct."));
            }
            session.afkCheckCooldown.cancel();
            this.afkCheckMap.remove(event.getPlayer().getUniqueId());
            return;
        }
        if (session.afk) { // If they're afk, unset it
            setNotAFK(session);
        }
        resetInitializerCooldown(session);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getYaw() != event.getTo().getYaw() || event.getFrom().getPitch() != event.getTo().getPitch()) {
            FLPlayerSession session = FarLands.getDataHandler().getSession(event.getPlayer());
            if (session.afk) {
                setNotAFK(session);
            }
            resetInitializerCooldown(session);
        }
    }

    /**
     * Set a session to not afk and alert in chat
     */
    public static void setNotAFK(FLPlayerSession session) {
        session.afk = false;
        session.sendAFKMessages();
        if (!session.handle.vanished) {
            Logging.broadcast(flp -> !flp.handle.getIgnoreStatus(session.player).includesChat(), " * %s is no longer AFK.", session.handle.username);
        }
    }

    /**
     * Reset a player's afk cooldown
     */
    public static void setAFKCooldown(Player player) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(player);

        if (session.afkCheckInitializerCooldown == null) {
            session.afkCheckInitializerCooldown = new Cooldown(session.handle.rank.getAfkCheckInterval() * 60L * 20L);
        }

        session.afkCheckInitializerCooldown.reset(() -> {
            if (player.isOnline()) {
                if (Worlds.FARLANDS.matches(player.getWorld()) || player.isGliding() || session.isInEvent) {
                    setAFKCooldown(player);
                    return;
                }

                if (session.afk) {
                    kickAFK(player, true);
                    return;
                }

                int a = FLUtils.RNG.nextInt(17), b = FLUtils.RNG.nextInt(17);
                boolean op = FLUtils.RNG.nextBoolean(); // true: +, false: -
                String check = ChatColor.RED.toString() + ChatColor.BOLD + "AFK Check: " + a + (op ? " + " : " - ") + b;
                player.sendMessage(check);
                player.sendTitle(check, "", 20, 120, 60);
                FarLands.getDebugger().echo("Sent AFK check to " + player.getName());
                instance.afkCheckMap.put(player.getUniqueId(), new Pair<>(check, op ? a + b : a - b));
                session.afkCheckCooldown.reset(() -> kickAFK(player, true));
            }
        });
    }

    /**
     * Reset a player's initializer cooldown
     */
    private static void resetInitializerCooldown(Player player) {
        resetInitializerCooldown(FarLands.getDataHandler().getSession(player));
    }

    /**
     * Reset a player's initializer cooldown
     */
    private static void resetInitializerCooldown(FLPlayerSession session) {
        if (session.afkCheckCooldown != null && session.afkCheckCooldown.isComplete() &&
            session.afkCheckInitializerCooldown != null) {
            session.afkCheckInitializerCooldown.resetCurrentTask();
        }
    }

    /**
     * Kick a player for being afk
     *
     * @param player The player to kick
     * @param debug  whether to echo to debug
     */
    private static void kickAFK(Player player, boolean debug) {
        if (player.isOnline()) {
            if (debug) {
                FarLands.getDebugger().echo("Kicking " + player.getName() + " for being AFK or answering the question incorrectly.");
            }
            player.kick(ComponentColor.red("Kicked for being AFK."));
            Logging.broadcastStaff(ComponentColor.red(player.getName() + " was kicked for being AFK."));
        }
    }
}
