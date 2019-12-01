package net.farlands.odyssey.mechanic;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.mojang.authlib.GameProfile;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.util.ReflectionHelper;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_14_R1.EnumGamemode;
import net.minecraft.server.v1_14_R1.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_14_R1.PacketStatusOutServerInfo;
import net.minecraft.server.v1_14_R1.ServerPing;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class Toggles extends Mechanic {
    @Override
    public void onStartup() {
        FarLands.getScheduler().scheduleSyncRepeatingTask(() -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                String message = null;
                Player recipient = (Player)FarLands.getDataHandler().getRADH().retrieve("replytoggle", player.getName());
                if(FarLands.getDataHandler().getRADH().retrieveBoolean("staffchat", player.getUniqueId().toString()))
                    message = ChatColor.GRAY + "Staff chat is toggled on.";
                else if(FarLands.getPDH().getFLPlayer(player).isVanished())
                    message = ChatColor.GRAY + "You are vanished.";
                else if (recipient != null) {
                    message = ChatColor.GRAY + "You are messaging " + recipient.getName() + ".";
                }
                if(message != null)
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            });
        }, 0L, 40L);

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(FarLands.getInstance(), PacketType.Play.Server.PLAYER_INFO) {
            @Override
            @SuppressWarnings("unchecked")
            public void onPacketSending(PacketEvent event) {
                PacketPlayOutPlayerInfo packet = (PacketPlayOutPlayerInfo)event.getPacket().getHandle();
                PacketPlayOutPlayerInfo.EnumPlayerInfoAction action = (PacketPlayOutPlayerInfo.EnumPlayerInfoAction)ReflectionHelper
                        .getFieldValue("a", packet.getClass(), packet);
                if((PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_GAME_MODE.equals(action) ||
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER.equals(action)) &&
                        !Rank.getRank(event.getPlayer()).isStaff()) {
                    List infoList = (List)ReflectionHelper.getFieldValue("b", packet.getClass(), packet);
                    for(Object infoData : infoList) {
                        if(EnumGamemode.SPECTATOR.equals(ReflectionHelper.invoke("c", infoData.getClass(), infoData))) {
                            ReflectionHelper.setFieldValue("c", infoData.getClass(), infoData, EnumGamemode.SURVIVAL);
                        }
                    }
                }
            }
        });

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(FarLands.getInstance(), PacketType.Status.Server.SERVER_INFO) {
            @Override
            @SuppressWarnings("unchecked")
            public void onPacketSending(PacketEvent event) {
                PacketStatusOutServerInfo packet = (PacketStatusOutServerInfo)event.getPacket().getHandle();
                ServerPing ping = (ServerPing)ReflectionHelper.getFieldValue("b", PacketStatusOutServerInfo.class, packet);
                ServerPing.ServerPingPlayerSample playerSample = (ServerPing.ServerPingPlayerSample)
                        ReflectionHelper.getFieldValue("b", ServerPing.class, ping);
                ReflectionHelper.setFieldValue("b", ServerPing.ServerPingPlayerSample.class, playerSample,
                        (int) Bukkit.getOnlinePlayers().stream().map(FarLands.getPDH()::getFLPlayer)
                                .filter(flp -> !flp.isVanished()).count());
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
    public void onGameModeChane(PlayerGameModeChangeEvent event) {
        showSpectators(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onSendTabCompletes(TabCompleteEvent event) {
        if(Rank.getRank(event.getSender()).isStaff())
            return;
        List<String> completions = event.getCompletions();
        completions.removeIf(str -> {
            OfflineFLPlayer flp = FarLands.getPDH().getFLPlayer(str);
            return flp != null && flp.isVanished();
        });
        event.setCompletions(completions);
    }

    @EventHandler(priority=EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        if(FarLands.getPDH().getFLPlayer(event.getPlayer()).isVanished() &&
                !FarLands.getDataHandler().getRADH().retrieveBoolean("staffchat", event.getPlayer().getUniqueId().toString())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You are vanished, you cannot chat in-game.");
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onEntityDamage(EntityDamageEvent event) {
        if(event.getEntity() instanceof Player && FarLands.getPDH().getFLPlayer((Player)event.getEntity()).isGod())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled=true)
    public void onEntityTarget(EntityTargetEvent event) {
        if(event.getTarget() instanceof Player && FarLands.getPDH().getFLPlayer((Player)event.getTarget()).isGod())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled=true)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        if(!(event.getEntity() instanceof Player))
            return;

        Player damager = event.getDamager() instanceof Player ? (Player)event.getDamager() : null;
        if(damager == null && event.getDamager() instanceof Projectile) {
            Projectile p = (Projectile)event.getDamager();
            damager = p.getShooter() instanceof Player ? (Player)p.getShooter() : null;
        }
        if(damager == null || damager == event.getEntity())
            return;
        
        OfflineFLPlayer damagerFLP = FarLands.getPDH().getFLPlayer(damager),
                 attackedFLP = FarLands.getPDH().getFLPlayer((Player)event.getEntity());
        if(!(damagerFLP.isPvPing() && attackedFLP.isPvPing())) {
            if(!damagerFLP.isPvPing())
                damager.sendMessage(ChatColor.RED + "You have PvP toggled off, activate it with /pvp.");
            event.setCancelled(true);
            event.getEntity().setFireTicks(-1); // Blocks fire damage
        }
    }

    @SuppressWarnings("deprecation")
    public static void hidePlayers(Player player) {
        OfflineFLPlayer flp = FarLands.getPDH().getFLPlayer(player);
        if(flp.isVanished())
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        else
            player.removePotionEffect(PotionEffectType.INVISIBILITY);

        Bukkit.getOnlinePlayers().stream().filter(pl -> pl != player).forEach(pl -> {
            if(!flp.isVanished())
                pl.showPlayer(player);
            else
                pl.hidePlayer(player);
        });
    }

    private static void showSpectators(Player player) {
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            if(GameMode.SPECTATOR.equals(player.getGameMode())) {
                Bukkit.getOnlinePlayers().stream().filter(p -> Rank.getRank(p).isStaff()).forEach(p ->
                    ((CraftPlayer)p).getHandle().playerConnection.sendPacket(new PacketPlayOutPlayerInfo(
                        PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_GAME_MODE, ((CraftPlayer)player).getHandle()
                    ))
                );
            }
        });
    }
}
