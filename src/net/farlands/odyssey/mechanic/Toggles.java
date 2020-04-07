package net.farlands.odyssey.mechanic;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import com.mojang.authlib.GameProfile;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.ReflectionHelper;

import net.md_5.bungee.api.ChatMessageType;

import net.minecraft.server.v1_15_R1.EnumGamemode;
import net.minecraft.server.v1_15_R1.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_15_R1.PacketStatusOutServerInfo;
import net.minecraft.server.v1_15_R1.ServerPing;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class Toggles extends Mechanic {
    @Override
    public void onStartup() {
        FarLands.getScheduler().scheduleSyncRepeatingTask(() -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                Player recipient = (Player) FarLands.getDataHandler().getSession(player).replyToggleRecipient;
                if (FarLands.getDataHandler().getSession(player).autoSendStaffChat)
                    sendFormatted(player, ChatMessageType.ACTION_BAR, "&(gray)Staff chat auto-messaging is toggled on.");
                else if (FarLands.getDataHandler().getOfflineFLPlayer(player).vanished)
                    sendFormatted(player, ChatMessageType.ACTION_BAR, "&(gray)You are vanished.");
                else if (recipient != null)
                    sendFormatted(player, ChatMessageType.ACTION_BAR, "&(gray)You are messaging %0.",
                            recipient.getName());
            });
        }, 0L, 40L);

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(FarLands.getInstance(), PacketType.Play.Server.PLAYER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketPlayOutPlayerInfo packet = (PacketPlayOutPlayerInfo) event.getPacket().getHandle();
                PacketPlayOutPlayerInfo.EnumPlayerInfoAction action = (PacketPlayOutPlayerInfo.EnumPlayerInfoAction) ReflectionHelper
                        .getFieldValue("a", packet.getClass(), packet);
                if ((PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_GAME_MODE.equals(action) ||
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER.equals(action)) &&
                        !Rank.getRank(event.getPlayer()).isStaff()) {
                    List infoList = (List) ReflectionHelper.getFieldValue("b", packet.getClass(), packet);
                    for (Object infoData : infoList) {
                        if (EnumGamemode.SPECTATOR == ReflectionHelper.invoke("c", infoData.getClass(), infoData)) {
                            ReflectionHelper.setFieldValue("c", infoData.getClass(), infoData, EnumGamemode.SURVIVAL);
                        }
                    }
                }
            }
        });

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(FarLands.getInstance(), PacketType.Status.Server.SERVER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketStatusOutServerInfo packet = (PacketStatusOutServerInfo) event.getPacket().getHandle();
                ServerPing ping = (ServerPing) ReflectionHelper.getFieldValue("b", PacketStatusOutServerInfo.class, packet);
                ServerPing.ServerPingPlayerSample playerSample = (ServerPing.ServerPingPlayerSample)
                        ReflectionHelper.getFieldValue("b", ServerPing.class, ping);
                ReflectionHelper.setFieldValue("b", ServerPing.ServerPingPlayerSample.class, playerSample,
                        (int) Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getOfflineFLPlayer)
                                .filter(flp -> !flp.vanished).count());
                ReflectionHelper.setFieldValue("c", ServerPing.ServerPingPlayerSample.class, playerSample, new GameProfile[0]);
            }
        });
    }

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        Bukkit.getOnlinePlayers().forEach(Toggles::hidePlayers);
        showSpectators(player);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        showSpectators(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSendTabCompletes(TabCompleteEvent event) {
        if (Rank.getRank(event.getSender()).isStaff())
            return;
        List<String> completions = new ArrayList<>(event.getCompletions());
        completions.removeIf(str -> {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(str);
            return flp != null && flp.vanished;
        });
        event.setCompletions(completions);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        if (FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer()).vanished &&
                !FarLands.getDataHandler().getSession(event.getPlayer()).autoSendStaffChat) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You are vanished, you cannot chat in-game.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        event.setCancelled(event.getEntity() instanceof Player &&
                FarLands.getDataHandler().getOfflineFLPlayer((Player) event.getEntity()).god);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        event.setCancelled(event.getTarget() instanceof Player &&
                FarLands.getDataHandler().getOfflineFLPlayer((Player) event.getTarget()).god);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLightningStrike(LightningStrikeEvent event) {
        event.setCancelled(event.getCause() == LightningStrikeEvent.Cause.TRIDENT &&
                event.getWorld().getNearbyEntities(event.getLightning().getLocation(), 5.0, 5.0, 5.0,
                        entity -> entity.getType() == EntityType.PLAYER).stream()
                        .map(FarLands.getDataHandler()::getOfflineFLPlayer).anyMatch(flp -> !flp.pvp));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player damager = event.getDamager() instanceof Player ? (Player) event.getDamager() : null;
        if (damager == null && event.getDamager() instanceof Projectile) {
            Projectile p = (Projectile) event.getDamager();
            damager = p.getShooter() instanceof Player ? (Player) p.getShooter() : null;
        }
        if (damager == null || damager == event.getEntity())
            return;

        OfflineFLPlayer damagerFLP = FarLands.getDataHandler().getOfflineFLPlayer(damager),
                attackedFLP = FarLands.getDataHandler().getOfflineFLPlayer((Player) event.getEntity());
        if (!damagerFLP.pvp) {
            damager.sendMessage(ChatColor.RED + "You have PvP toggled off, activate it with /pvp.");
            event.setCancelled(true);
            event.getEntity().setFireTicks(-1); // Blocks fire damage
        } else if (!attackedFLP.pvp) {
            event.setCancelled(true);
            event.getEntity().setFireTicks(-1); // Blocks fire damage
        }
    }

    @SuppressWarnings("deprecation")
    public static void hidePlayers(Player player) {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
        if (flp.vanished)
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        else if (player.hasPotionEffect(PotionEffectType.INVISIBILITY) && player.getPotionEffect(PotionEffectType.INVISIBILITY).getDuration() > 8 * 60 * 20)
            player.removePotionEffect(PotionEffectType.INVISIBILITY);

        Bukkit.getOnlinePlayers().stream().filter(pl -> pl != player).forEach(pl -> {
            if (!flp.vanished)
                pl.showPlayer(player);
            else
                pl.hidePlayer(player);
        });
    }

    private static void showSpectators(Player player) {
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            if (GameMode.SPECTATOR == player.getGameMode()) {
                Bukkit.getOnlinePlayers().stream().filter(p -> Rank.getRank(p).isStaff()).forEach(p ->
                    ((CraftPlayer)p).getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_GAME_MODE, ((CraftPlayer)player).getHandle()
                    ))
                );
            }
        });
    }
}
