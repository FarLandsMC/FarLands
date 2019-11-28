package net.farlands.odyssey.mechanic;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.FLCommandEvent;
import net.farlands.odyssey.command.player.CommandMessage;
import net.farlands.odyssey.data.RandomAccessDataHandler;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.Pair;
import net.farlands.odyssey.util.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
    private final RandomAccessDataHandler radh;

    private static AFK instance;

    public AFK() {
        this.afkCheckList = new HashMap<>();
        this.radh = FarLands.getDataHandler().getRADH();
    }

    @Override
    public void onStartup() {
        instance = this;

        Bukkit.getScheduler().runTaskTimerAsynchronously(FarLands.getInstance(), () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if(afkCheckList.containsKey(player.getUniqueId())) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.RED +
                            ChatColor.BOLD.toString() + ChatColor.MAGIC + "MM " + ChatColor.RESET +
                            afkCheckList.get(player.getUniqueId()).getFirst() + ChatColor.MAGIC + " MM"));
                }
            });
        }, 0L, 40L);
    }

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        radh.store(false, "afkCmd", player.getUniqueId().toString()); // This allows /afk to function properly
        setAFKCooldown(player);
    }

    @Override
    public void onPlayerQuit(Player player) {
        afkCheckList.remove(player.getUniqueId());
        radh.removeCooldown("afk", player.getUniqueId().toString());
        radh.removeCooldown("afkKick", player.getUniqueId().toString());
    }

    @EventHandler
    public void onBlockBroken(BlockBreakEvent event) {
        if(!Rank.getRank(event.getPlayer()).isStaff())
            radh.resetCooldown("afk", event.getPlayer().getUniqueId().toString());
    }
    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event) {
        if(!Rank.getRank(event.getPlayer()).isStaff())
            radh.resetCooldown("afk", event.getPlayer().getUniqueId().toString());
    }

    @EventHandler
    public void onFLCommand(FLCommandEvent event) {
        if(CommandMessage.class.equals(event.getCommand()) && event.getSender() instanceof Player)
            radh.resetCooldown("afk", ((Player)event.getSender()).getUniqueId().toString());
    }

    @EventHandler(priority=EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        if(afkCheckList.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            int answer;
            try {
                answer = Integer.parseInt(event.getMessage());
            }catch(NumberFormatException ex) {
                answer = Integer.MAX_VALUE;
            }
            int actualAnswer = afkCheckList.get(event.getPlayer().getUniqueId()).getSecond();
            if(answer != actualAnswer) {
                Bukkit.getScheduler().runTask(FarLands.getInstance(),
                        () -> event.getPlayer().kickPlayer(ChatColor.RED + "The correct answer was: " + actualAnswer));
            }else{
                setAFKCooldown(event.getPlayer());
                event.getPlayer().sendMessage(ChatColor.GREEN + "Correct.");
            }
            radh.removeCooldown("afkKick", event.getPlayer().getUniqueId().toString());
            afkCheckList.remove(event.getPlayer().getUniqueId());
            return;
        }
        if(radh.retrieveBoolean("afkCmd", event.getPlayer().getUniqueId().toString()))
            setNotAFK(event.getPlayer());
        if(!Rank.getRank(event.getPlayer()).isStaff() && event.getMessage().length() >= 5)
            radh.resetCooldown("afk", event.getPlayer().getUniqueId().toString());
        /*if(!event.isCancelled())
            FarLands.getMechanicHandler().getMechanic(Chat.class).onChat(event);*/
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(radh.retrieveBoolean("afkCmd", event.getPlayer().getUniqueId().toString()))
            setNotAFK(event.getPlayer());
    }

    public static void setNotAFK(Player player) {
        FarLands.getDataHandler().getRADH().store(false, "afkCmd", player.getUniqueId().toString());
        FarLands.broadcast(flp -> !flp.isIgnoring(player.getUniqueId()), " * " + player.getName() + " is no longer AFK.", false);
    }

    public static void setAFKCooldown(Player player) {
        Rank rank = Rank.getRank(player);
        if(!rank.hasAfkChecks())
            return;
        RandomAccessDataHandler radh = FarLands.getDataHandler().getRADH();
        radh.setCooldown(rank.getAfkCheckInterval() * 60L * 20L, "afk", player.getUniqueId().toString(), () -> {
            if(player.isOnline() && player.isValid()) {
                if("farlands".equals(player.getWorld().getName()) || player.isGliding() ||
                        radh.retrieveBoolean("ingame", player.getUniqueId().toString())) {
                    setAFKCooldown(player);
                    return;
                }
                if(radh.retrieveBoolean("afkCmd", player.getUniqueId().toString())) {
                    kickAFK(player);
                    return;
                }
                int a = Utils.RNG.nextInt(17), b = Utils.RNG.nextInt(17);
                boolean op = Utils.RNG.nextBoolean(); // true: +, false: -
                String check = ChatColor.RED.toString() + ChatColor.BOLD + "AFK Check: " + a + (op ? " + " : " - ") + b;
                player.sendMessage(check);
                player.sendTitle(check, "", 20, 120, 60);
                FarLands.getDebugger().echo("Sent AFK check to " + player.getName());
                instance.afkCheckList.put(player.getUniqueId(), new Pair<>(check, op ? a + b : a - b));
                radh.setCooldown(30L * 20L, "afkKick", player.getUniqueId().toString(), () -> kickAFK(player));
            }
        });
    }

    private static void kickAFK(Player player) {
        if(player.isOnline()) {
            FarLands.getDebugger().echo("Kicking " + player.getName() + " for being AFK or answering the question incorrectly.");
            player.kickPlayer(ChatColor.RED + "Kicked for being AFK.");
            FarLands.broadcastStaff(ChatColor.RED + player.getName() + " was kicked for being AFK.");
        }
    }
}
