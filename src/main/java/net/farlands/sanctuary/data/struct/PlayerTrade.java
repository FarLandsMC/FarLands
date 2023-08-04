package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.TextComponentCutter;
import net.farlands.sanctuary.util.ComponentColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * A player trade posted in /tradepost.
 */
public class PlayerTrade {

    public final            UUID            owner;
    public                  int             clicks;
    public final            List<Component> message;
    public final            long            expirationDate;
    private transient       long            lastClickTime;
    private final transient HashSet<UUID>   clickers;
    private final transient HashSet<UUID>   mailSenders;

    private static final Component NO_ITALIC = Component.empty().decoration(TextDecoration.ITALIC, false);

    public PlayerTrade(UUID owner, Component tradeMessage) {
        this.owner = owner;
        this.clicks = 0;
        this.expirationDate = System.currentTimeMillis() + 10L * 24L * 60L * 60L * 1000L;
        this.lastClickTime = 0;
        this.clickers = new HashSet<>();
        this.mailSenders = new HashSet<>();
        this.message = new TextComponentCutter(40, 253)
            .cutComponent(tradeMessage)
            .stream()
            .map(NO_ITALIC::append)
            .toList();
    }

    PlayerTrade() {
        this.owner = null;
        this.clicks = 0;
        this.message = Collections.emptyList();
        this.expirationDate = 0;
        this.lastClickTime = 0;
        this.clickers = new HashSet<>();
        this.mailSenders = new HashSet<>();
    }

    public ItemStack generateHead() {
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(this.owner);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(this.owner));
        meta.displayName(flp.getFullDisplayName(false).decoration(TextDecoration.ITALIC, false));
        meta.lore(this.message);

        head.setItemMeta(meta);

        return head;
    }

    public void notifyOwner(Player clicker) {
        if (owner.equals(clicker.getUniqueId())) {
            return;
        }

        if (clickers.add(clicker.getUniqueId())) {
            clicks += 1;
        }

        long clickTime = System.currentTimeMillis();
        if (clickTime - lastClickTime < 15000L) {
            return;
        }
        lastClickTime = clickTime;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(owner);
        if (flp.getIgnoreStatus(clicker).includesChat()) {
            return;
        }

        Player trader = flp.getOnlinePlayer();
        if (trader == null) {
            if (mailSenders.add(clicker.getUniqueId())) {
                clicker.sendMessage(ComponentColor.gold("The person offering this trade is offline, they will see that you " +
                                    "would like to trade when they return."));
                flp.addMail(clicker.getUniqueId(), ComponentColor.green("[Tradepost] {:gold}", "I would like to trade with you."));
            }
            return;
        }

        OfflineFLPlayer clickerflp = FarLands.getDataHandler().getOfflineFLPlayer(clicker);
        trader.sendMessage(ComponentColor.gold("{:green} {} would like to trade with you.", "[Tradepost]", clickerflp));
        trader.playSound(trader.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
    }
}
