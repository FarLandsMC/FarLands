package net.farlands.sanctuary.data.struct;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.chat.ChatControl;
import net.farlands.sanctuary.util.ComponentColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * A player trade posted in /tradepost.
 */
public class PlayerTrade {
    public UUID owner;
    public int clicks;
    public final List<String> message;
    public final long expirationDate;
    private transient long lastClickTime;
    private final transient HashSet<UUID> clickers;
    private final transient HashSet<UUID> mailSenders;

    public PlayerTrade(UUID owner, String tradeMessage) {
        this.owner = owner;
        this.clicks = 0;
        this.message = new ArrayList<>();
        this.expirationDate = System.currentTimeMillis() + 10L * 24L * 60L * 60L * 1000L;
        this.lastClickTime = 0;
        this.clickers = new HashSet<>();
        this.mailSenders = new HashSet<>();

        // Enforce length limit
        if (tradeMessage.length() > 253)
            tradeMessage = tradeMessage.substring(0, 253) + "...";

        // Remove additional spaces and limit caps and character flood
        tradeMessage = ChatControl.limitCaps(ChatControl.limitFlood(tradeMessage.replaceAll("\\s+", " ").trim()));

        while (!tradeMessage.isEmpty()) {
            // Try to find a space to break at
            int index = 40;
            for (; index < 48 && index < tradeMessage.length(); ++index) {
                if (tradeMessage.charAt(index) == ' ')
                    break;
            }

            // Add the next section of the message
            if (index >= tradeMessage.length()) {
                this.message.add(tradeMessage);
                tradeMessage = "";
            } else if (index == 48) {
                this.message.add(tradeMessage.substring(0, 40) + "-");
                tradeMessage = tradeMessage.substring(40);
            } else {
                this.message.add(tradeMessage.substring(0, index));
                tradeMessage = tradeMessage.substring(index + 1);
            }
        }
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
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        OfflinePlayer player = Bukkit.getOfflinePlayer(owner);
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.RESET + player.getName());
        meta.setLore(message);
        head.setItemMeta(meta);
        return head;
    }

    public void notifyOwner(Player clicker) {
        if (owner.equals(clicker.getUniqueId()))
            return;

        if (clickers.add(clicker.getUniqueId()))
            clicks += 1;

        long clickTime = System.currentTimeMillis();
        if (clickTime - lastClickTime < 15000L)
            return;
        lastClickTime = clickTime;

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(owner);
        if (flp.getIgnoreStatus(clicker).includesChat())
            return;

        Player trader = flp.getOnlinePlayer();
        if (trader == null) {
            if (mailSenders.add(clicker.getUniqueId())) {
                clicker.sendMessage(ChatColor.GOLD + "The person offering this trade is offline, they will see that you " +
                        "would like to trade when they log back in.");
                flp.addMail(clicker.getUniqueId(), ComponentColor.green("[Tradepost] ").append(ComponentColor.gold("I would like to trade with you.")));
            }
            return;
        }

        trader.sendMessage(ChatColor.GOLD + clicker.getName() + " would like to trade with you.");
        trader.playSound(trader.getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
    }
}
