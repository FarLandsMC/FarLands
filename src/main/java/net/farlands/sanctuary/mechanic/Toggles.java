package net.farlands.sanctuary.mechanic;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Handles plugin toggles.
 */
public class Toggles extends Mechanic {

    @Override
    public void onStartup() {
        FarLands.getScheduler().scheduleSyncRepeatingTask(() -> Bukkit.getOnlinePlayers().forEach(player -> { // Create task to update actionbar
            FLPlayerSession session = FarLands.getDataHandler().getSession(player);
            if (session.autoSendStaffChat) {
                player.sendActionBar(ComponentColor.gray("Staff chat auto-messaging is toggled on."));
            } else if (session.handle.vanished) {
                player.sendActionBar(ComponentColor.gray("You are vanished."));
            } else if (session.replyToggleRecipient != null) {
                player.sendActionBar(ComponentColor.gray("You are messaging {}.", session.replyToggleRecipient.getName()));
            }
        }), 0L, 40L);

        // Update tab list to show all players in survival
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(FarLands.getInstance(), PacketType.Play.Server.PLAYER_INFO) {
            @Override
            @SuppressWarnings("unchecked")
            public void onPacketSending(PacketEvent event) {
                ClientboundPlayerInfoUpdatePacket packet = (ClientboundPlayerInfoUpdatePacket) event.getPacket().getHandle();
                EnumSet<ClientboundPlayerInfoUpdatePacket.Action> action = packet.actions();
                if ((action.contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE) ||
                     action.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) &&
                    !Rank.getRank(event.getPlayer()).isStaff()) {
                    List<ClientboundPlayerInfoUpdatePacket.Entry> infoList = packet.entries();
                    for (int i = 0; i < infoList.size(); ++i) {
                        ClientboundPlayerInfoUpdatePacket.Entry currentInfoData = infoList.get(i);
                        if (GameType.SPECTATOR == currentInfoData.gameMode()) {
                            ClientboundPlayerInfoUpdatePacket.Entry newInfoData = new ClientboundPlayerInfoUpdatePacket.Entry(
                                event.getPlayer().getUniqueId(),
                                currentInfoData.profile(),
                                currentInfoData.listed(),
                                currentInfoData.latency(),
                                GameType.SURVIVAL,
                                currentInfoData.displayName(),
                                currentInfoData.chatSession()
                            );
                            infoList.set(i, newInfoData);
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
                ping.setPlayers(
                    Bukkit
                        .getOnlinePlayers()
                        .stream()
                        .map(FarLands.getDataHandler()::getOfflineFLPlayer)
                        .filter(flp -> !flp.vanished)
                        .map(OfflineFLPlayer::getOnlinePlayer)
                        .map(WrappedGameProfile::fromPlayer)
                        .toList()
                );
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
    public void onSendTabCompletes(TabCompleteEvent event) { // Make the tab completion exclude vanished players
        if (Rank.getRank(event.getSender()).isStaff()) {
            return;
        }
        List<String> completions = new ArrayList<>(event.getCompletions());
        completions.removeIf(str -> {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(str);
            return flp != null && flp.vanished;
        });
        event.setCompletions(completions);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // Handle god mode
        event.setCancelled(event.getEntity() instanceof Player
                           && FarLands.getDataHandler().getOfflineFLPlayer(event.getEntity()).god);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        // Handle god mode
        event.setCancelled(event.getTarget() instanceof Player
                           && FarLands.getDataHandler().getOfflineFLPlayer(event.getTarget()).god);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLightningStrike(LightningStrikeEvent event) {
        // Handle pvp from tridents
        event.setCancelled(event.getCause() == LightningStrikeEvent.Cause.TRIDENT
                           && event.getWorld().getNearbyEntities(event.getLightning().getLocation(), 5.0, 5.0, 5.0,
                                                                 entity -> entity.getType() == EntityType.PLAYER).stream()
                               .map(FarLands.getDataHandler()::getOfflineFLPlayer).anyMatch(flp -> !flp.pvp));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return; // Ignore non-player damage

        Player damager = event.getDamager() instanceof Player ? (Player) event.getDamager() : null;
        if (damager == null && event.getDamager() instanceof Projectile p) {
            damager = p.getShooter() instanceof Player ? (Player) p.getShooter() : null;
        }
        if (damager == null || damager == event.getEntity()) {
            return;
        }

        // Handle pvp and pvp messages
        OfflineFLPlayer damagerFLP = FarLands.getDataHandler().getOfflineFLPlayer(damager),
            attackedFLP = FarLands.getDataHandler().getOfflineFLPlayer(event.getEntity());
        if (!damagerFLP.pvp) {
            damager.sendMessage(ChatColor.RED + "You have PvP toggled off, activate it with /pvp on.");
            event.setCancelled(true);
            event.getEntity().setFireTicks(-1); // Blocks fire damage
        } else if (!attackedFLP.pvp) {
            event.setCancelled(true);
            event.getEntity().setFireTicks(-1); // Blocks fire damage
        }
    }

    /**
     * Show or hide the player depending on if they are vanished
     */
    @SuppressWarnings("deprecation")
    public static void hidePlayers(Player player) {
        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
            if (flp.vanished) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
            } else if (player.hasPotionEffect(PotionEffectType.INVISIBILITY) && player.getPotionEffect(PotionEffectType.INVISIBILITY).getDuration() > 8 * 60 * 20) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }

            Bukkit.getOnlinePlayers().stream().filter(pl -> pl != player).forEach(pl -> {
                if (!flp.vanished || FarLands.getDataHandler().getOfflineFLPlayer(pl).rank.isStaff()) {
                    pl.showPlayer(player);
                } else {
                    pl.hidePlayer(player);
                }
            });
        });
    }

    private static void showSpectators(Player player) {
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, ((CraftPlayer) player).getHandle()
        );

        Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> {
            if (GameMode.SPECTATOR == player.getGameMode()) {
                Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(p -> Rank.getRank(p).isStaff())
                    .forEach(p -> {
                        pm.sendServerPacket(p, PacketContainer.fromPacket(packet));
                    });
            }
        });
    }
}
