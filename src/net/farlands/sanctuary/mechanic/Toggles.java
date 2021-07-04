package net.farlands.sanctuary.mechanic;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import com.comphenix.protocol.wrappers.WrappedServerPing;
import com.mojang.authlib.GameProfile;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.ReflectionHelper;

import net.md_5.bungee.api.ChatMessageType;

import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo;
import net.minecraft.network.protocol.status.PacketStatusOutServerInfo;
import net.minecraft.network.protocol.status.ServerPing;
import net.minecraft.world.level.EnumGamemode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_17_R1.advancement.CraftAdvancement;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class Toggles extends Mechanic {
    private Map<UUID, List<Advancement>> lastAdvancementMessage;

    public Toggles() {
        this.lastAdvancementMessage = new HashMap<>();
    }

    @Override
    public void onStartup() {
        FarLands.getScheduler().scheduleSyncRepeatingTask(() -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                FLPlayerSession session = FarLands.getDataHandler().getSession(player);
                if (session.autoSendStaffChat)
                    sendFormatted(player, ChatMessageType.ACTION_BAR, "&(gray)Staff chat auto-messaging is toggled on.");
                else if (session.handle.vanished)
                    sendFormatted(player, ChatMessageType.ACTION_BAR, "&(gray)You are vanished.");
                else if (session.replyToggleRecipient != null)
                    sendFormatted(player, ChatMessageType.ACTION_BAR, "&(gray)You are messaging %0.",
                            session.replyToggleRecipient.getName());
            });
        }, 0L, 40L);

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(FarLands.getInstance(), PacketType.Play.Server.PLAYER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketPlayOutPlayerInfo packet = (PacketPlayOutPlayerInfo) event.getPacket().getHandle();
                PacketPlayOutPlayerInfo.EnumPlayerInfoAction action = (PacketPlayOutPlayerInfo.EnumPlayerInfoAction) ReflectionHelper
                        .getFieldValue("a", packet.getClass(), packet);
                if ((PacketPlayOutPlayerInfo.EnumPlayerInfoAction.b.equals(action) || // EnumPlayerInfoAction.b = EnumPlayerInfoAction.UPDATE_GAME_MODE
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a.equals(action)) && // EnumPlayerInfoAction.a = EnumPlayerInfoAction.ADD_PLAYER
                        !Rank.getRank(event.getPlayer()).isStaff()) {
                    List infoList = (List) ReflectionHelper.getFieldValue("b", packet.getClass(), packet);
                    for (Object infoData : infoList) {
                        // EnumGamemode.d = spectator and EnumGamemode.a = survival
                        if (EnumGamemode.d == ReflectionHelper.invoke("c", infoData.getClass(), infoData)) {
                            ReflectionHelper.setFieldValue("c", infoData.getClass(), infoData, EnumGamemode.a);
                        }
                    }
                }
            }
        });

        // Set the amount of online players to exclude vanished staff
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(FarLands.getInstance(), PacketType.Status.Server.SERVER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                WrappedServerPing ping = event.getPacket().getServerPings().read(0);
                ping.setPlayersOnline((int) Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getOfflineFLPlayer)
                    .filter(flp -> !flp.vanished).count());
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

    @EventHandler(ignoreCancelled = true)
    public void onAdvancementGet(PlayerAdvancementDoneEvent event) {
        if (FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer()).vanished)
            return;

        if (!"minecraft".equalsIgnoreCase(event.getAdvancement().getKey().getNamespace()))
            return;

        Advancement handle = ((CraftAdvancement) event.getAdvancement()).getHandle();
        if (handle.c() == null)
            return;

        List<Advancement> playerCache = lastAdvancementMessage.get(event.getPlayer().getUniqueId());
        if (playerCache == null) {
            playerCache = new ArrayList<>();
        } else if (playerCache.contains(handle))
            return;
        playerCache.add(handle);
        lastAdvancementMessage.put(event.getPlayer().getUniqueId(), playerCache);

        FarLands.getScheduler().scheduleSyncDelayedTask(() ->
                        lastAdvancementMessage.get(event.getPlayer().getUniqueId()).remove(handle),
                20);

        IChatBaseComponent message = ((CraftPlayer) event.getPlayer()).getHandle().getScoreboardDisplayName().mutableCopy()
                // Clear chat modifiers
                .addSibling(new ChatComponentText(" has made the advancement ")
                        .setChatModifier(FLUtils.chatModifier("white")))
                .addSibling(((CraftAdvancement) event.getAdvancement()).getHandle().j());
        Bukkit.getOnlinePlayers().stream()
                .map(player -> ((CraftPlayer) player).getHandle())
                .forEach(player -> player.sendMessage(message, new UUID(0L, 0L)));
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
            damager.sendMessage(ChatColor.RED + "You have PvP toggled off, activate it with /pvp on.");
            event.setCancelled(true);
            event.getEntity().setFireTicks(-1); // Blocks fire damage
        } else if (!attackedFLP.pvp) {
            event.setCancelled(true);
            event.getEntity().setFireTicks(-1); // Blocks fire damage
        }
    }

    @SuppressWarnings("deprecation")
    public static void hidePlayers(Player player) {
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
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
        });
    }

    private static void showSpectators(Player player) {
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            if (GameMode.SPECTATOR == player.getGameMode()) {
                Bukkit.getOnlinePlayers().stream().filter(p -> Rank.getRank(p).isStaff()).forEach(p ->
                    ((CraftPlayer)p).getHandle().b.sendPacket(new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.b, ((CraftPlayer)player).getHandle()
                    ))
                );
            }
        });
    }
}
